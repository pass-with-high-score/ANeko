package org.nqmgaming.aneko.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nqmgaming.aneko.R
import org.nqmgaming.aneko.core.service.AnimationService
import org.nqmgaming.aneko.data.SkinInfo

suspend fun loadSkinList(context: Context): List<SkinInfo> = withContext(Dispatchers.IO) {
    val pm = context.packageManager
    val intent = Intent(AnimationService.ACTION_GET_SKIN)

    var skinList = pm.queryIntentActivities(intent, 0).map { resolveInfo ->
        val packageName = resolveInfo.activityInfo.packageName
        val versionName = try {
            pm.getPackageInfo(packageName, 0).versionName
                ?: context.getString(R.string.unknown_skin_version)
        } catch (_: PackageManager.NameNotFoundException) {
            context.getString(R.string.unknown_skin_version)
        }
        SkinInfo(
            icon = resolveInfo.loadIcon(pm),
            label = resolveInfo.loadLabel(pm).toString(),
            component = ComponentName(packageName, resolveInfo.activityInfo.name),
            versionName = versionName
        )
    }

    return@withContext skinList
}
