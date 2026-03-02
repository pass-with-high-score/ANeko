package org.nqmgaming.aneko.presentation.home

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import org.nqmgaming.aneko.presentation.AnekoViewModel
import org.nqmgaming.aneko.presentation.home.component.HomeAppBar
import org.nqmgaming.aneko.presentation.home.component.HomeContent
import org.nqmgaming.aneko.presentation.home.component.NotificationAlertDialog
import org.nqmgaming.aneko.presentation.home.component.SelectLanguageDialog

@Destination<RootGraph>
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AnekoViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()
    val isDynamicColor by viewModel.isDynamicColor.collectAsState()
    val isFirstLaunch = viewModel.isFirstLaunch.collectAsState().value
    var isShowingDialog by rememberSaveable { mutableStateOf(isFirstLaunch) }
    val context = LocalContext.current

    var showLanguageDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HomeAppBar(
                onToggleTheme = viewModel::toggleTheme,
                isDarkTheme = isDarkTheme,
                accentColor = accentColor,
                isDynamicColor = isDynamicColor,
                onAccentSelected = viewModel::setAccentColor,
                onToggleDynamicColor = viewModel::toggleDynamicColor,
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
                onToggleSkin = { packageName ->
                    viewModel.onToggleSkin(packageName, context)
                },
                skins = uiState.value.skins,
                onRequestDeleteSkin = {
                    viewModel.onDeselectSkin(it, context)
                },
            )
        }

        if (showLanguageDialog) {
            SelectLanguageDialog(
                onDismiss = {
                    showLanguageDialog = false
                }
            )
        }
        if (isFirstLaunch && isShowingDialog) {
            // show welcome dialog
            NotificationAlertDialog {
                isShowingDialog = false
                viewModel.setFirstLaunchDone()
            }
        }
    }
}

