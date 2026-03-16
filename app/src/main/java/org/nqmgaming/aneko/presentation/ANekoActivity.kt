package org.nqmgaming.aneko.presentation

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.edit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import dagger.hilt.android.AndroidEntryPoint
import org.nqmgaming.aneko.core.service.AnimationService
import org.nqmgaming.aneko.core.util.LocaleHelper
import org.nqmgaming.aneko.core.util.extension.checkNotificationPermission
import org.nqmgaming.aneko.presentation.app.HomeAppScreen
import org.nqmgaming.aneko.presentation.permission.OnboardingSkinScreen
import org.nqmgaming.aneko.presentation.permission.PermissionScreen
import org.nqmgaming.aneko.presentation.setting.LanguageScreen
import org.nqmgaming.aneko.presentation.ui.theme.ANekoTheme

@AndroidEntryPoint
class ANekoActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(packageName + "_preferences", MODE_PRIVATE)

        if (prefs.getBoolean(AnimationService.PREF_KEY_ENABLE, false)) {
            startAnimationService()
        }

        setContent {
            val viewModel: AnekoViewModel = hiltViewModel()
            val buildInSkin =
                viewModel.uiState.collectAsState().value.skins.firstOrNull { it.isBuiltin }
            val isListEmpty = viewModel.uiState.collectAsState().value.skins.isEmpty()

            LaunchedEffect(key1 = buildInSkin == null, key2 = isListEmpty) {
                if (buildInSkin == null && isListEmpty) {
                    viewModel.importSkinFromAssets(
                        this@ANekoActivity,
                        folderName = "aneko",
                    )
                }
            }
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            val accentColor by viewModel.accentColor.collectAsState()
            val isDynamicColor by viewModel.isDynamicColor.collectAsState()

            val requestNotificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                viewModel.updateNotificationPermission(isGranted)
                viewModel.enableAnimation()
                startAnimationService()
            }

            ANekoTheme(
                darkTheme = isDarkTheme,
                dynamicColor = isDynamicColor,
                accentColor = accentColor
            ) {
                val backStack = rememberNavBackStack(PermissionKey)

                NavDisplay(
                    backStack = backStack,
                    onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
                    entryProvider = entryProvider {
                        entry<PermissionKey> {
                            PermissionScreen(
                                onNavigateToHome = {
                                    backStack.removeLastOrNull()
                                    backStack.add(HomeAppKey)
                                },
                                onNavigateToOnboardingSkin = {
                                    backStack.removeLastOrNull()
                                    backStack.add(OnboardingSkinKey)
                                },
                                onNavigateToLanguage = {
                                    backStack.add(LanguageKey)
                                }
                            )
                        }
                        entry<OnboardingSkinKey> {
                            OnboardingSkinScreen(
                                onNavigateToHome = {
                                    backStack.removeLastOrNull()
                                    backStack.add(HomeAppKey)
                                },
                            )
                        }
                        entry<LanguageKey> {
                            LanguageScreen(
                                onNavigateBack = {
                                    backStack.removeLastOrNull()
                                }
                            )
                        }
                        entry<HomeAppKey> {
                            HomeAppScreen(
                                onToggleAnimation = { enabled ->
                                    if (enabled) {
                                        checkNotificationPermission(
                                            requestNotificationPermissionLauncher
                                        ) {
                                            viewModel.enableAnimation()
                                            startAnimationService()
                                        }
                                    } else {
                                        viewModel.disableAnimation()
                                        stopAnimationService()
                                    }
                                }
                            )
                        }

                    }
                )

            }
        }
    }

    private fun startAnimationService() {
        prefs.edit { putBoolean(AnimationService.PREF_KEY_VISIBLE, true) }
        startService(
            Intent(this, AnimationService::class.java)
                .setAction(AnimationService.ACTION_START)
        )
    }

    private fun stopAnimationService() {
        prefs.edit { putBoolean(AnimationService.PREF_KEY_VISIBLE, false) }
        stopService(
            Intent(this, AnimationService::class.java)
                .setAction(AnimationService.ACTION_STOP)
        )
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(base))
    }
}
