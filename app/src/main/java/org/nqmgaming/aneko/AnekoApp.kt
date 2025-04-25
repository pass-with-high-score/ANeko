package org.nqmgaming.aneko

import android.app.Application
import timber.log.Timber

class AnekoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}