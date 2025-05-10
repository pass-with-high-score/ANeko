package org.nqmgaming.aneko.presentation.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.CreateSkinScreenDestination
import com.ramcosta.composedestinations.generated.destinations.EditSkinScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SkinDetailScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.core.service.AnimationService
import org.nqmgaming.aneko.presentation.AnekoViewModel
import org.nqmgaming.aneko.presentation.home.component.ExpandableFab
import org.nqmgaming.aneko.presentation.home.component.HomeContent
import org.nqmgaming.aneko.presentation.home.component.SmallFab
import org.nqmgaming.aneko.util.extension.checkNotificationPermission


@Destination<RootGraph>(start = true)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AnekoViewModel = hiltViewModel(),
    navigator: DestinationsNavigator,
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val isFabOpen by viewModel.isFabOpen.collectAsState()
    val isEnabledState by viewModel.isEnabledState.collectAsState()
    val context = LocalContext.current

    fun startAnimationService() {
        viewModel.enableAnimation()
        context.startService(
            Intent(
                context,
                AnimationService::class.java
            ).setAction(AnimationService.ACTION_START)
        )
    }

    val requestNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.updateNotificationPermission(isGranted)
        startAnimationService()
    }

    val importSkinLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            navigator.navigate(SkinDetailScreenDestination(skinPath = it.toString()))
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.toggleTheme()
                    }) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = stringResource(R.string.toggle_theme_title)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    context.getString(R.string.app_store_uri)
                                )
                            },
                            null
                        ).also { context.startActivity(it) }
                    }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = stringResource(R.string.share_title)
                        )
                    }
                }
            )
        },
        floatingActionButton = {

        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            HomeContent(
                modifier = Modifier.padding(innerPadding),
                isEnabled = isEnabledState,
                onChangeEnable = { enabled ->
                    viewModel.updateAnimationEnabled(enabled)
                    if (enabled) {
                        context.checkNotificationPermission(requestNotificationPermissionLauncher) {
                            startAnimationService()
                        }
                    } else {
                        viewModel.disableAnimation()
                        context.stopService(
                            Intent(
                                context,
                                AnimationService::class.java
                            ).setAction(AnimationService.ACTION_STOP)
                        )
                    }
                },
                onSkinSelected = { component ->
                    viewModel.updateSkin(component)
                },
            )
        }
//        AnimatedVisibility(
//            visible = isFabOpen,
//            enter = fadeIn(animationSpec = tween(durationMillis = 300)),
//            exit = fadeOut(animationSpec = tween(durationMillis = 300)),
//            modifier = Modifier
//                .fillMaxSize()
//                .zIndex(1f)
//        ) {
//            Box(
//                Modifier
//                    .fillMaxSize()
//                    .background(Color.Black.copy(alpha = 0.4f))
//                    .clickable { viewModel.toggleFabState() }
//
//            )
//        }
//        ExpandableFab(
//            modifier = Modifier
//                .zIndex(2f)
//                .padding(
//                    end = 16.dp,
//                    bottom = 50.dp
//                ),
//            isOpen = isFabOpen,
//            onToggle = { viewModel.toggleFabState() },
//            children = listOf(
//                {
//                    SmallFab(
//                        icon = Icons.Filled.Draw,
//                        onClick = {
//                            navigator.navigate(EditSkinScreenDestination())
//                            viewModel.toggleFabState()
//                        },
//                        text = "Edit",
//                        isExpanded = isFabOpen
//                    )
//                },
//                {
//                    SmallFab(
//                        icon = Icons.Filled.Create,
//                        onClick = {
//                            navigator.navigate(CreateSkinScreenDestination())
//                            viewModel.toggleFabState()
//                        },
//                        text = "Create",
//                        isExpanded = isFabOpen
//                    )
//                },
//                {
//                    SmallFab(
//                        icon = Icons.Filled.Download,
//                        onClick = {
//                            importSkinLauncher.launch("*/*")
//                            viewModel.toggleFabState()
//                        },
//                        text = "Import",
//                        isExpanded = isFabOpen
//                    )
//                },
//            )
//        )
    }
}

