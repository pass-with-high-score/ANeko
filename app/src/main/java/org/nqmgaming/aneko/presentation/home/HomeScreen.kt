package org.nqmgaming.aneko.presentation.home

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import org.nqmgaming.aneko.core.service.AnimationService
import org.nqmgaming.aneko.presentation.AnekoViewModel
import org.nqmgaming.aneko.presentation.home.component.HomeAppBar
import org.nqmgaming.aneko.presentation.home.component.HomeContent
import org.nqmgaming.aneko.presentation.home.component.SelectLanguageDialog
import org.nqmgaming.aneko.core.util.extension.checkNotificationPermission

@Destination<RootGraph>
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AnekoViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
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

    var showLanguageDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HomeAppBar(
                onToggleTheme = viewModel::toggleTheme,
                isDarkTheme = isDarkTheme,
                onShowLanguageDialog = {
                    showLanguageDialog = true
                }
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            HomeContent(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(bottom = 82.dp),
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
                onSelectSkin = { packageName ->
                    viewModel.onSelectSkin(packageName)
                },
                skins = uiState.value.skins,
                onRequestDeleteSkin = {
                    viewModel.onDeselectSkin(it, context)
                }
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

