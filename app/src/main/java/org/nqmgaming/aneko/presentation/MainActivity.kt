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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.core.service.AnimationService
import org.nqmgaming.aneko.presentation.home.HomeScreen
import org.nqmgaming.aneko.presentation.ui.theme.ANekoTheme

class ANekoActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            prefs.edit { putBoolean(AnimationService.PREF_KEY_NOTIFICATION_ENABLE, isGranted) }
            startAnimationService()
        }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(packageName + "_preferences", MODE_PRIVATE)

        setContent {
            ANekoTheme(
                dynamicColor = false
            ) {
                var isEnabled by remember {
                    mutableStateOf(
                        prefs.getBoolean(
                            AnimationService.PREF_KEY_ENABLE,
                            false
                        )
                    )
                }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    text = getString(R.string.app_name),
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Black
                                    ),
                                )
                            }
                        )
                    }
                ) { innerPadding ->
                    HomeScreen(
                        modifier = Modifier.padding(innerPadding),
                        isEnabled = isEnabled,
                        onChangeEnable = { enabled ->
                            isEnabled = enabled
                            prefs.edit { putBoolean(AnimationService.PREF_KEY_ENABLE, enabled) }
                            if (enabled) {
                                checkNotificationPermission()
                            } else {
                                prefs.edit { putBoolean(AnimationService.PREF_KEY_VISIBLE, false) }
                                stopService(
                                    Intent(
                                        this,
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
                            if (prefs.getBoolean(AnimationService.PREF_KEY_ENABLE, false)) {
                                stopService(
                                    Intent(
                                        this,
                                        AnimationService::class.java
                                    ).setAction(AnimationService.ACTION_TOGGLE)
                                )

                            }
                        }
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
}