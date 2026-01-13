package org.nqmgaming.aneko.util

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

fun Context.openUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    this.startActivity(intent)
}