package org.nqmgaming.aneko.presentation.home

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.CreateSkinScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.core.service.AnimationService
import org.nqmgaming.aneko.presentation.AnekoViewModel
import org.nqmgaming.aneko.presentation.home.component.HomeContent
import org.nqmgaming.aneko.presentation.home.component.SelectLanguageDialog
import org.nqmgaming.aneko.util.extension.checkNotificationPermission
import timber.log.Timber
import java.util.Locale


@Destination<RootGraph>(start = true)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AnekoViewModel = hiltViewModel(),
    navigator: DestinationsNavigator,
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
                    Row {
                        IconButton(onClick = {
                            viewModel.toggleTheme()
                        }) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = stringResource(R.string.toggle_theme_title)
                            )
                        }
                        IconButton(onClick = {
                            showLanguageDialog = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = stringResource(R.string.toggle_theme_title)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navigator.navigate(CreateSkinScreenDestination())
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.create_skin_title),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = {
                        val uri = context.getString(R.string.telegram_group_link).toUri()
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        context.startActivity(intent)
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_telegram),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = {
                        val baseUri = context.getString(R.string.app_store_uri)
                        val locale = Locale.getDefault().language
                        val fullUri = "$baseUri&hl=$locale&gl=US"
                        Timber.d("Share URI: $fullUri")

                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    fullUri
                                )
                            },
                            null
                        ).also { context.startActivity(it) }
                    }) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = stringResource(R.string.share_title),
                            modifier = Modifier.size(24.dp)
                        )
                    }
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
