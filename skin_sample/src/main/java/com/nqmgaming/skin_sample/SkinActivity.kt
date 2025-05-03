package com.nqmgaming.skin_sample

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import timber.log.Timber
import androidx.core.net.toUri

class SkinActivity : Activity() {
    companion object {
        private const val ANEKO_PACKAGE = "org.nqmgaming.aneko"
        private const val ANEKO_ACTIVITY = "org.nqmgaming.aneko.MainActivity"
        private val ANEKO_MARKET_URI = "market://search?q=$ANEKO_PACKAGE".toUri()
    }

    override fun onResume() {
        super.onResume()

        val packageFound = try {
            packageManager.getPackageInfo(ANEKO_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.e(e)
            false
        }

        val (msgId, intent) = if (packageFound) {
            R.string.msg_usage to Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setClassName(ANEKO_PACKAGE, ANEKO_ACTIVITY)
            }
        } else {
            R.string.msg_no_package to Intent(Intent.ACTION_VIEW, ANEKO_MARKET_URI)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.app_name)
            .setMessage(msgId)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                try {
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Timber.e(e)
                    Toast.makeText(this, R.string.msg_unexpected_err, Toast.LENGTH_SHORT).show()
                }
                finish()
            }
            .setOnCancelListener { finish() }
            .show()
    }
}
