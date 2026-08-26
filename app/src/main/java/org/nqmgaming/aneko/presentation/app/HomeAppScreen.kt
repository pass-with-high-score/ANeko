package org.nqmgaming.aneko.presentation.app

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.core.util.extension.getStringResource
import org.nqmgaming.aneko.presentation.AnekoViewModel
import org.nqmgaming.aneko.presentation.BottomTab
import org.nqmgaming.aneko.presentation.ExploreKey
import org.nqmgaming.aneko.presentation.HomeKey
import org.nqmgaming.aneko.presentation.LanguageKey
import org.nqmgaming.aneko.presentation.SkinDetailKey
import org.nqmgaming.aneko.presentation.ThemeKey
import org.nqmgaming.aneko.presentation.components.AppBottomBar
import org.nqmgaming.aneko.presentation.detail.SkinDetailScreen
import org.nqmgaming.aneko.presentation.explore.ExploreSkinScreen
import org.nqmgaming.aneko.presentation.home.HomeScreen
import org.nqmgaming.aneko.presentation.setting.LanguageScreen
import org.nqmgaming.aneko.presentation.setting.ThemeScreen
import timber.log.Timber

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomeAppScreen(
    viewModel: AnekoViewModel = hiltViewModel(),
    onToggleAnimation: (Boolean) -> Unit = {}
) {
    val homeStack = rememberNavBackStack(HomeKey)
    val exploreStack = rememberNavBackStack(ExploreKey)
    var currentTab by rememberSaveable { mutableStateOf(BottomTab.HOME) }
    val isAnimationEnabled by viewModel.isEnabledState.collectAsStateWithLifecycle()


    val currentBackStack = when (currentTab) {
        BottomTab.HOME -> homeStack
        BottomTab.EXPLORE -> exploreStack
    }

    val bottomBarScreens = listOf(
        BottomTab.HOME,
        BottomTab.EXPLORE,
    )

    var showBottomBar by rememberSaveable { mutableStateOf(true) }



    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris: List<Uri>? ->
            uris?.forEach { uri ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                    Timber.e(e, "Failed to persist URI permission")
                }

                scope.launch(Dispatchers.IO) {
                    val pkg = viewModel.importSkinFromUri(context, uri)
                    withContext(Dispatchers.Main) {
                        if (pkg != null) {
                            Toast.makeText(
                                context,
                                context.getStringResource(R.string.imported_skin_file, pkg),
                                Toast.LENGTH_SHORT
                            ).show()
                            Timber.d("Imported skin package: $pkg")
                        } else {
                            Toast.makeText(
                                context,
                                context.getStringResource(R.string.failed_to_import_skin_file),
                                Toast.LENGTH_SHORT,
                            ).show()
                            Timber.e("Failed to import selected skin file")
                        }
                    }
                }
            }
        }
    )

    BackHandler(enabled = currentTab != BottomTab.HOME && currentBackStack.size <= 1) {
        currentTab = BottomTab.HOME
        showBottomBar = true
    }

    Scaffold(
        bottomBar = {
            if (!showBottomBar) return@Scaffold
            AppBottomBar(
                items = bottomBarScreens,
                tabSelected = currentTab,
                isAnimationEnabled = isAnimationEnabled,
                onToggleAnimation = { enabled ->
                    viewModel.updateAnimationEnabled(enabled)
                    onToggleAnimation(enabled)
                },
                onItemClick = { tab ->
                    currentTab = tab
                }
            )
        },
        floatingActionButton = {
            if (currentTab == BottomTab.HOME && showBottomBar) {
                FloatingActionButton(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary,
                    onClick = {
                        filePickerLauncher.launch(
                            arrayOf(
                                "application/zip",
                                "application/x-zip-compressed",
                                "image/png",
                                "image/webp",
                            )
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null
                    )
                }
            }
        }
    ) {
        NavDisplay(
            backStack = currentBackStack,
            onBack = { if (currentBackStack.size > 1) currentBackStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<HomeKey> {
                    HomeScreen(
                        onNavigateToTheme = {
                            showBottomBar = false
                            currentBackStack.add(ThemeKey)
                        },
                        onNavigateToExplore = {
                            currentBackStack.add(ExploreKey)
                        },
                        onNavigateToLanguage = {
                            showBottomBar = false
                            currentBackStack.add(LanguageKey)
                        },
                        onNavigateToDetail = { pkg ->
                            showBottomBar = false
                            currentBackStack.add(SkinDetailKey(pkg))
                        }
                    )
                }
                entry<LanguageKey> {
                    LanguageScreen(
                        onNavigateBack = {
                            showBottomBar = true
                            currentBackStack.removeLastOrNull()
                        }
                    )
                }
                entry<ExploreKey> {
                    ExploreSkinScreen(
                        onNavigateToDetail = { pkg ->
                            showBottomBar = false
                            currentBackStack.add(SkinDetailKey(pkg, isOnline = true))
                        }
                    )
                }
                entry<ThemeKey> {
                    ThemeScreen(
                        onNavigateBack = {
                            showBottomBar = true
                            currentBackStack.removeLastOrNull()
                        }
                    )
                }
                entry<SkinDetailKey> { key ->
                    SkinDetailScreen(
                        packageName = key.packageName,
                        onNavigateBack = {
                            showBottomBar = true
                            currentBackStack.removeLastOrNull()
                        }
                    )
                }
            }
        )
    }
}
