package org.nqmgaming.aneko.util.extension

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

fun Context.checkNotificationPermission(
    requestPermissionLauncher: ActivityResultLauncher<String>,
    onGranted: () -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
    }
    onGranted()
}

fun Context.getUserLaunchableApps(): List<ResolveInfo> {
    val intent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    val listApp = packageManager.queryIntentActivities(intent, 0)
    // remove itself
    listApp.removeIf { resolveInfo ->
        resolveInfo.activityInfo.packageName == packageName
    }
    return listApp
}


