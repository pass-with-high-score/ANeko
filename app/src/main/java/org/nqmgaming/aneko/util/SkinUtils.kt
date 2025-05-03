package org.nqmgaming.aneko.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nqmgaming.aneko.core.service.AnimationService
import org.nqmgaming.aneko.data.SkinInfo

suspend fun loadSkinList(context: Context): List<SkinInfo> = withContext(Dispatchers.IO) {
    val pm = context.packageManager
    val intent = Intent(AnimationService.ACTION_GET_SKIN)
    pm.queryIntentActivities(intent, 0).map { resolveInfo ->
        val packageName = resolveInfo.activityInfo.packageName
        val versionName = try {
            pm.getPackageInfo(packageName, 0).versionName ?: "Unknown"
        } catch (_: PackageManager.NameNotFoundException) {
            "Unknown"
        }
        SkinInfo(
            icon = resolveInfo.loadIcon(pm),
            label = resolveInfo.loadLabel(pm).toString(),
            component = ComponentName(packageName, resolveInfo.activityInfo.name),
            versionName = versionName
        )
    }
}