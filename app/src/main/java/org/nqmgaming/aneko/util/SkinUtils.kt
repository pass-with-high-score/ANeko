package org.nqmgaming.aneko.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import org.nqmgaming.aneko.core.service.AnimationService
import org.nqmgaming.aneko.data.SkinInfo

fun createSkinList(context: Context, onFinished: () -> Unit): List<SkinInfo> {
    val pm = context.packageManager
    val intent = Intent(AnimationService.ACTION_GET_SKIN)
    val skinList = pm.queryIntentActivities(intent, 0).map { info ->
        SkinInfo(
            icon = info.loadIcon(pm),
            label = info.loadLabel(pm).toString(),
            component = ComponentName(
                info.activityInfo.packageName,
                info.activityInfo.name
            )
        )
    }
    onFinished()
    return skinList
}

