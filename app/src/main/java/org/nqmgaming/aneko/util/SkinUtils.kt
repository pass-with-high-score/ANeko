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
    val prefs =
        context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)

    var defaultComponent = prefs.getString(AnimationService.PREF_KEY_SKIN_COMPONENT, null)

    val skinList = pm.queryIntentActivities(intent, 0).map { resolveInfo ->
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

    if (defaultComponent == null) {
        skinList.firstOrNull {
            it.component.packageName == context.packageName
        }?.let {
            val fallbackComponent = it.component.flattenToString()
            if (fallbackComponent != context.packageName) {
                defaultComponent = fallbackComponent
            }
        }
    }

    if (defaultComponent != null) {
        val (defaultSkin, others) = skinList.partition {
            it.component.flattenToString() == defaultComponent
        }
        return@withContext defaultSkin + others
    }

    return@withContext skinList
}

