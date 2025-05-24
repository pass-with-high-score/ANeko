package org.nqmgaming.aneko.core.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.SharedPreferences
import android.view.accessibility.AccessibilityEvent
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import org.nqmgaming.aneko.core.service.AnimationService.PREF_KEY_ENABLED_APPS
import timber.log.Timber

class AppMonitorAccessibilityService : AccessibilityService() {
    companion object {
        val connected = MutableStateFlow(false)
    }

    private lateinit var prefs: SharedPreferences
    private var enabledApps: Set<String> = emptySet()

    private val ignoredPackages = setOf(
        "com.android.systemui",
        "com.miui.securitycenter",
        "com.google.android.permissioncontroller"
    )


    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                val packageName = event.packageName?.toString() ?: return
                Timber.d("onAccessibilityEvent: $packageName")

                if (ignoredPackages.contains(packageName)) return

                val shouldBlockNeko = enabledApps.contains(packageName)

                val action = if (shouldBlockNeko) {
                    "org.nqmgaming.aneko.HIDE_NEKO"
                } else {
                    "org.nqmgaming.aneko.SHOW_NEKO"
                }

                val intent = Intent(action).apply {
                    setPackage("org.nqmgaming.aneko")
                    putExtra("packageName", packageName)
                }
                sendBroadcast(intent)
            }
        }
    }


    override fun onInterrupt() {
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        connected.tryEmit(true)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        enabledApps = prefs.getStringSet(PREF_KEY_ENABLED_APPS, emptySet()) ?: emptySet()

        // Listen for changes to prefs
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        connected.tryEmit(false)
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    }

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == PREF_KEY_ENABLED_APPS) {
            enabledApps = prefs.getStringSet(PREF_KEY_ENABLED_APPS, emptySet()) ?: emptySet()
        }
    }
}
