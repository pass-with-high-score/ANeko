package org.nqmgaming.aneko.presentation

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import dagger.hilt.android.AndroidEntryPoint
import org.nqmgaming.aneko.core.service.AnimationService
import org.nqmgaming.aneko.core.shortcuts.ShortcutManagerHelper
import org.nqmgaming.aneko.core.shortcuts.ShortcutManagerHelper.SHORTCUT_ID_TOGGLE

@AndroidEntryPoint
class ShortcutHandlerActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    private val viewModel: AnekoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences(packageName + "_preferences", MODE_PRIVATE)

        // Check overlay permission first
        if (!Settings.canDrawOverlays(this)) {
            val overlayIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:$packageName".toUri()
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(overlayIntent)
            finish()
            return
        }

        // Check notification permission (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request notification permission
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }

        // Handle the shortcut action
        handleShortcutIntent(intent)
        finish()
    }

    private fun handleShortcutIntent(intent: Intent?) {
        val action = intent?.action ?: return
        val currentlyEnabled = prefs.getBoolean(AnimationService.PREF_KEY_ENABLE, false)

        when (action) {
            ShortcutManagerHelper.ACTION_TOGGLE_FROM_SHORTCUT -> {
                prefs.edit {
                    putBoolean(AnimationService.PREF_KEY_ENABLE, !currentlyEnabled)
                    if (!currentlyEnabled) {
                        putBoolean(AnimationService.PREF_KEY_VISIBLE, true)
                    }
                }
                if (!currentlyEnabled) {
                    viewModel.enableAnimation()
                } else {
                    viewModel.disableAnimation()
                }
                startService(
                    Intent(this, AnimationService::class.java)
                        .setAction(if (!currentlyEnabled) AnimationService.ACTION_START else AnimationService.ACTION_STOP)
                )
                ShortcutManagerHelper.reportShortcutUsed(this, SHORTCUT_ID_TOGGLE)
            }

            ShortcutManagerHelper.ACTION_SKIN_FROM_SHORTCUT -> {
                // Get skin package from intent
                val skinPackage = intent.getStringExtra(ShortcutManagerHelper.EXTRA_SKIN_PACKAGE)
                if (skinPackage != null) {
                    val currentSkin = prefs.getString(AnimationService.PREF_KEY_SKIN_COMPONENT, "")

                    // Check if clicking the same skin shortcut while service is running
                    val isSameSkinRunning = currentSkin == skinPackage && currentlyEnabled

                    if (isSameSkinRunning) {
                        // Toggle OFF - stop service
                        viewModel.disableAnimation()
                        prefs.edit {
                            putBoolean(AnimationService.PREF_KEY_ENABLE, false)
                            putBoolean(AnimationService.PREF_KEY_VISIBLE, false)
                        }
                        stopService(
                            Intent(this, AnimationService::class.java)
                                .setAction(AnimationService.ACTION_STOP)
                        )
                    } else {
                        // Toggle ON - select skin and start service
                        viewModel.onSelectSkin(skinPackage)
                        viewModel.enableAnimation()
                        prefs.edit {
                            putBoolean(AnimationService.PREF_KEY_ENABLE, true)
                            putBoolean(AnimationService.PREF_KEY_VISIBLE, true)
                        }
                        startService(
                            Intent(this, AnimationService::class.java)
                                .setAction(AnimationService.ACTION_START)
                        )
                    }

                    // Report shortcut usage
                    val shortcutId = "skin_${skinPackage.replace("/", "_")}"
                    ShortcutManagerHelper.reportShortcutUsed(this, shortcutId)
                }
            }
        }
    }
}
