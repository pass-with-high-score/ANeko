package org.nqmgaming.aneko.data

import android.content.ComponentName
import android.graphics.drawable.Drawable

data class SkinInfo(
    val icon: Drawable,
    val label: String,
    val component: ComponentName
)