package com.nqmgaming.sample

import android.app.Application
import timber.log.Timber

class SkinApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
