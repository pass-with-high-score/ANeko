package org.nqmgaming.aneko.presentation.setting.app_selector

import android.content.pm.ResolveInfo
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.nqmgaming.aneko.presentation.setting.app_selector.component.AppItem
import org.nqmgaming.aneko.presentation.ui.theme.ANekoTheme

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
) {
    val context = LocalContext.current
    val packageManager = context.packageManager

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Select Apps",
                    )
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
                launchableApps.size,
                key = {
                    val info = launchableApps[it].activityInfo
                    "${info.packageName}/${info.name}"
                }
            ) { index ->
                val resolveInfo = launchableApps[index]
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
