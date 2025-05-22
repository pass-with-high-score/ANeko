package org.nqmgaming.aneko.core.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.SharedPreferences
import android.view.accessibility.AccessibilityEvent
import androidx.preference.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow


class AppMonitorAccessibilityService : AccessibilityService() {
    companion object {
        val connected = MutableStateFlow(false)
    }

    private lateinit var prefs: SharedPreferences
    private var enabledApps: Set<String> = emptySet()

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        val shouldBlockNeko = enabledApps.contains(packageName)

        val action = if (shouldBlockNeko) {
            "org.nqmgaming.aneko.HIDE_NEKO"
        } else {
            "org.nqmgaming.aneko.SHOW_NEKO"
        }

        val intent = Intent(action).apply {
            setPackage("org.nqmgaming.aneko")
        }
        sendBroadcast(intent)
    }

    override fun onInterrupt() {
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        connected.tryEmit(true)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        enabledApps = prefs.getStringSet("enabled_apps", emptySet()) ?: emptySet()

        // Listen for changes to prefs
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        connected.tryEmit(false)
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
    }

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "enabled_apps") {
            enabledApps = prefs.getStringSet("enabled_apps", emptySet()) ?: emptySet()
        }
    }
}
