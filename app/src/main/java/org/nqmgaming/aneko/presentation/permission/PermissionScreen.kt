package org.nqmgaming.aneko.presentation.permission

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.HomeScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PermissionScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.data.PermissionPage
import org.nqmgaming.aneko.presentation.AnekoViewModel
import org.nqmgaming.aneko.presentation.home.component.SelectLanguageDialog
import org.nqmgaming.aneko.presentation.permission.component.CatRunning
import org.nqmgaming.aneko.presentation.permission.component.PagerDots
import org.nqmgaming.aneko.presentation.permission.component.PermissionPageContent
import org.nqmgaming.aneko.presentation.ui.theme.ANekoTheme
import kotlin.time.Duration.Companion.seconds

@Destination<RootGraph>(start = true)
@Composable
fun PermissionScreen(
    navigator: DestinationsNavigator,
    viewModel: AnekoViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var checking by remember { mutableStateOf(true) }
    val isFinishedSetup = viewModel.isFinishedSetup.collectAsState().value

    suspend fun checkAndNavigate() = run {
        checking = true
        delay(2.seconds)

        if (isFinishedSetup || Settings.canDrawOverlays(context)) {
            if (!isFinishedSetup) viewModel.finishedSetup()
            navigator.navigate(HomeScreenDestination()) {
                popUpTo(PermissionScreenDestination) { inclusive = true }
            }
        } else {
            checking = false
        }
    }


    LaunchedEffect(Unit) {
        checkAndNavigate()
    }


    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                MainScope().launch {
                    checkAndNavigate()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    if (checking) {
        CatRunning()
    } else {
        PermissionPagerUI(
            onOpenSettings = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:${context.packageName}".toUri()
                )
                context.startActivity(intent)
            },
            onSkip = {
                viewModel.finishedSetup()
                navigator.navigate(HomeScreenDestination()) {
                    popUpTo(PermissionScreenDestination) {
                        inclusive = true
                    }
                }
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionPagerUI(
    onOpenSettings: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    val pages = remember {
        listOf(
            PermissionPage(
                titleRes = R.string.permission_onboarding_page1_title,
                bodyRes = R.string.permission_onboarding_page1_body,
                imageRes = null,
                imageDescRes = null,
                primaryLabelRes = R.string.permission_onboarding_page1_primary
            ),
            PermissionPage(
                titleRes = R.string.permission_onboarding_page2_title,
                bodyRes = R.string.permission_onboarding_page2_body,
                imageRes = R.drawable.select_aneko,
                imageDescRes = R.string.permission_onboarding_page2_image_desc,
                primaryLabelRes = R.string.permission_onboarding_page2_primary
            ),
            PermissionPage(
                titleRes = R.string.permission_onboarding_page3_title,
                bodyRes = R.string.permission_onboarding_page3_body,
                imageRes = R.drawable.grant_permission,
                imageDescRes = R.string.permission_onboarding_page3_image_desc,
                primaryLabelRes = R.string.permission_onboarding_page3_primary
            )
        )
    }

    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.permission_onboarding_appbar_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        showLanguageDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = stringResource(R.string.toggle_theme_title)
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onSkip) { Text(stringResource(R.string.permission_onboarding_skip)) }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                pageSpacing = 16.dp
            ) { page ->
                PermissionPageContent(
                    page = pages[page],
                )
            }

            // Bottom controls
            PagerDots(
                total = pages.size,
                selected = pagerState.currentPage,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(4.dp))

            val isLast = pagerState.currentPage == pages.lastIndex

            Button(
                onClick = {
                    if (isLast) {
                        onOpenSettings()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (isLast) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    stringResource(pages[pagerState.currentPage].primaryLabelRes),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = stringResource(R.string.permission_onboarding_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (showLanguageDialog) {
            SelectLanguageDialog(
                onDismiss = {
                    showLanguageDialog = false
                }
            )
        }
    }
}

@Preview
@Composable
private fun PermissionScreenPreview() {
    ANekoTheme {
        PermissionPagerUI(onOpenSettings = {}, onSkip = {})
    }
}
