package org.renewal.aneko;

import android.app.Application;

import com.kieronquinn.monetcompat.core.MonetCompat;

import timber.log.Timber;

public class Applications extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        MonetCompat.enablePaletteCompat();
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
    }
}
