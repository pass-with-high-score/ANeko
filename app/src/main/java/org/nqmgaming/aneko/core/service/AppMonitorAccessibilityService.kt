package org.nqmgaming.aneko.core.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import timber.log.Timber


class AppMonitorAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        Timber.d("AppMonitorAccessibilityService: onAccessibilityEvent: $event")
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageNameCS = event.packageName
        Timber.d("AppMonitorAccessibilityService: packageNameCS: $packageNameCS")
        if (packageNameCS == null) return

        val packageName = packageNameCS.toString()
        Timber.d("AppMonitorAccessibilityService: packageName: $packageName")

        if (packageName == "com.android.developers.androidify") {
            Timber.d("AppMonitorAccessibilityService: Androidify app detected")
            // Gửi broadcast để ẩn Neko
            val intent = Intent("org.nqmgaming.aneko.HIDE_NEKO")
            intent.setPackage("org.nqmgaming.aneko")
            sendBroadcast(intent)
        } else {
            // Gửi broadcast để hiện Neko lại
            Timber.d("AppMonitorAccessibilityService: App detected: $packageName")
            val intent = Intent("org.nqmgaming.aneko.SHOW_NEKO")
            intent.setPackage("org.nqmgaming.aneko")
            sendBroadcast(intent)
            sendBroadcast(Intent("org.nqmgaming.aneko.SHOW_NEKO"))
        }
    }

    override fun onInterrupt() {
        // không cần làm gì
    }
}
