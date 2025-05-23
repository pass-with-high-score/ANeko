package org.nqmgaming.aneko.presentation.setting.app_selector

import android.content.pm.ResolveInfo
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.blankj.utilcode.util.KeyboardUtils
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.presentation.ANekoActivity
import org.nqmgaming.aneko.presentation.setting.app_selector.component.AppItem
import org.nqmgaming.aneko.presentation.ui.component.AnimatedIcon
import org.nqmgaming.aneko.presentation.ui.component.AppBarTextField
import org.nqmgaming.aneko.presentation.ui.theme.ANekoTheme
import org.nqmgaming.aneko.util.extension.autoFocus
import org.nqmgaming.aneko.util.extension.throttle

@Destination<RootGraph>
@Composable
fun AppSelectorScreen(
    modifier: Modifier = Modifier,
    navigator: DestinationsNavigator,
    viewModel: AppSelectorViewModel = hiltViewModel(),
) {

    val uiState by viewModel.uiState.collectAsState()
    AppSelector(
        modifier = modifier,
        onNavigateBack = {
            navigator.navigateUp()
        },
        launchableApps = uiState.launchableApps,
        enabledPackageNames = uiState.enabledPackageNames,
        onSwitch = { packageName, isEnabled ->
            viewModel.onEvent(AppSelectorUiAction.OnEnableApp(packageName, isEnabled))
        },
        onReset = {
            viewModel.onEvent(AppSelectorUiAction.OnReset)
        },
        showSearchBar = uiState.showSearchBar,
        searchQuery = uiState.searchQuery,
        onSearchQueryChange = {
            viewModel.onEvent(AppSelectorUiAction.OnSearchQueryChange(it))
        },
        onToggleSearchBar = {
            viewModel.onEvent(AppSelectorUiAction.OnToggleSearchBar(it))
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelector(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    launchableApps: List<ResolveInfo> = emptyList(),
    enabledPackageNames: Set<String> = emptySet(),
    onSwitch: (String, Boolean) -> Unit = { _, _ -> },
    onReset: () -> Unit = {},
    showSearchBar: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onToggleSearchBar: (Boolean) -> Unit = {},
) {
    val context = LocalActivity.current as ANekoActivity
    val packageManager = context.packageManager
    val softwareKeyboardController = LocalSoftwareKeyboardController.current

    val filteredApps = if (searchQuery.isNotEmpty()) {
        launchableApps.filter { resolveInfo ->
            val appName = resolveInfo.loadLabel(packageManager).toString() +
                    resolveInfo.activityInfo.packageName
            appName.contains(searchQuery, ignoreCase = true)
        }
    } else {
        launchableApps
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    if (showSearchBar) {
                        BackHandler {
                            if (KeyboardUtils.isSoftInputVisible(context)) {
                                softwareKeyboardController?.hide()
                            } else {
                                onToggleSearchBar(false)
                            }
                        }
                        AppBarTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            hint = "Search Apps",
                            modifier = Modifier.autoFocus()
                        )
                    } else {
                        Text(
                            text = "Select Apps",
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = throttle {
                        if (showSearchBar) {
                            if (searchQuery.isEmpty()) {
                                onToggleSearchBar(false)
                            } else {
                                onSearchQueryChange("")
                            }
                        } else {
                            onToggleSearchBar(true)
                        }
                    }) {
                        AnimatedIcon(
                            id = R.drawable.ic_anim_search_close,
                            atEnd = showSearchBar,
                        )
                    }
                    IconButton(
                        onClick = onReset
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            items(
                filteredApps.size,
                key = {
                    val info = filteredApps[it].activityInfo
                    "${info.packageName}/${info.name}"
                }
            ) { index ->
                val resolveInfo = filteredApps[index]
                val appName = resolveInfo.loadLabel(packageManager).toString()
                val appIcon = resolveInfo.loadIcon(packageManager)
                val packageName = resolveInfo.activityInfo.packageName

                AppItem(
                    appName = appName,
                    appIcon = appIcon,
                    packageName = packageName,
                    isEnabled = enabledPackageNames.contains(packageName),
                    onSwitch = {
                        onSwitch(
                            packageName,
                            it
                        )
                    }
                )
            }
        }
    }
}


@Preview(showSystemUi = true, showBackground = true)
@Composable
private fun AppSelectorPreview() {
    ANekoTheme(dynamicColor = false) {
        AppSelector()
    }
}
