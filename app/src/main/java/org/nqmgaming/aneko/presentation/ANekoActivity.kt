package org.nqmgaming.aneko.presentation

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DarkMode
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.core.service.AnimationService
import org.nqmgaming.aneko.presentation.home.HomeScreen
import org.nqmgaming.aneko.presentation.home.component.ExpandableFab
import org.nqmgaming.aneko.presentation.home.component.SmallFab
import org.nqmgaming.aneko.presentation.ui.theme.ANekoTheme

class ANekoActivity : ComponentActivity() {
    companion object {
        const val PREF_KEY_THEME = "theme"
    }

    private lateinit var prefs: SharedPreferences

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            prefs.edit { putBoolean(AnimationService.PREF_KEY_NOTIFICATION_ENABLE, isGranted) }
            startAnimationService()
        }

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == AnimationService.PREF_KEY_ENABLE) {
            val isEnabled = prefs.getBoolean(AnimationService.PREF_KEY_ENABLE, false)
            runOnUiThread {
                // Update UI with the latest value
                isEnabledState?.value = isEnabled
            }
        }
    }

    private var isEnabledState: MutableState<Boolean>? = null

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(packageName + "_preferences", MODE_PRIVATE)

        if (prefs.getBoolean(AnimationService.PREF_KEY_ENABLE, false)) {
            startAnimationService()
        }

        prefs.registerOnSharedPreferenceChangeListener(prefsListener)

        setContent {
            val defaultDark = isSystemInDarkTheme()
            var isDarkTheme by rememberSaveable {
                mutableStateOf(
                    prefs.getString(
                        PREF_KEY_THEME,
                        if (defaultDark) "dark" else "light"
                    ) == "dark"
                )
            }
            val context = LocalContext.current
            var isFabOpen by remember { mutableStateOf(false) }

            ANekoTheme(
                darkTheme = isDarkTheme,
                dynamicColor = false
            ) {
                isEnabledState = remember {
                    mutableStateOf(prefs.getBoolean(AnimationService.PREF_KEY_ENABLE, false))
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    text = getString(R.string.app_name),
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Black
                                    ),
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = {
                                    isDarkTheme = !isDarkTheme
                                    prefs.edit {
                                        putString(
                                            PREF_KEY_THEME,
                                            if (isDarkTheme) "dark" else "light"
                                        )
                                    }
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
                                                getString(R.string.app_store_uri)
                                            )
                                        },
                                        null
                                    ).also { startActivity(it) }
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
                        HomeScreen(
                            modifier = Modifier.padding(innerPadding),
                            isEnabled = isEnabledState?.value == true,
                            onChangeEnable = { enabled ->
                                isEnabledState?.value = enabled
                                prefs.edit { putBoolean(AnimationService.PREF_KEY_ENABLE, enabled) }
                                if (enabled) {
                                    checkNotificationPermission()
                                } else {
                                    prefs.edit {
                                        putBoolean(
                                            AnimationService.PREF_KEY_VISIBLE,
                                            false
                                        )
                                    }
                                    stopService(
                                        Intent(
                                            context,
                                            AnimationService::class.java
                                        ).setAction(AnimationService.ACTION_STOP)
                                    )
                                }
                            },
                            onSkinSelected = { component ->
                                prefs.edit {
                                    putString(
                                        AnimationService.PREF_KEY_SKIN_COMPONENT,
                                        component.flattenToString()
                                    )
                                }
                            },
                        )
                    }
                    AnimatedVisibility(
                        visible = isFabOpen,
                        enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                        exit = fadeOut(animationSpec = tween(durationMillis = 300)),
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(1f)
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f))
                                .clickable { isFabOpen = false }

                        )
                    }
                    ExpandableFab(
                        modifier = Modifier
                            .zIndex(2f)
                            .padding(
                                end = 16.dp,
                                bottom = 50.dp
                            ),
                        isOpen = isFabOpen,
                        onToggle = { isFabOpen = !isFabOpen },
                        children = listOf(
                            {
                                SmallFab(
                                    icon = Icons.Filled.Draw,
                                    onClick = { /*...*/ },
                                    text = "Edit",
                                    isExpanded = isFabOpen
                                )
                            },
                            {
                                SmallFab(
                                    icon = Icons.Filled.Create,
                                    onClick = { /*...*/ },
                                    text = "Create",
                                    isExpanded = isFabOpen
                                )
                            },
                        )
                    )
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        startAnimationService()
    }

    private fun startAnimationService() {
        prefs.edit { putBoolean(AnimationService.PREF_KEY_VISIBLE, true) }
        startService(
            Intent(
                this,
                AnimationService::class.java
            ).setAction(AnimationService.ACTION_START)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    }
}