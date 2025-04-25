package org.nqmgaming.aneko.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.nqmgaming.aneko.core.service.AnimationService
import org.nqmgaming.aneko.data.SkinInfo

suspend fun loadSkinList(context: Context): List<SkinInfo> = withContext(Dispatchers.IO) {
    val pm = context.packageManager
    val intent = Intent(AnimationService.ACTION_GET_SKIN)
    pm.queryIntentActivities(intent, 0).map {
        SkinInfo(
            icon = it.loadIcon(pm),
            label = it.loadLabel(pm).toString(),
            component = ComponentName(it.activityInfo.packageName, it.activityInfo.name)
        )
    }
}


