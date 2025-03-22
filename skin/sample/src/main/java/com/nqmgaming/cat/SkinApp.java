package com.nqmgaming.cat;

import android.app.Application;

import timber.log.Timber;

public class SkinApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }
}
