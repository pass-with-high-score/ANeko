package org.nqmgaming.aneko.service;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class QuickSettingService extends TileService {
    private Tile tile;
    private SharedPreferences prefs;

    SharedPreferences.OnSharedPreferenceChangeListener prefsListener = (sharedPreferences, key) -> {
        assert key != null;
        if (key.equals(AnimationService.PREF_KEY_ENABLE)) {
            if (isServiceAvailable()) {
                if (prefs.getBoolean(AnimationService.PREF_KEY_ENABLE, false)) {
                    tile.setState(Tile.STATE_ACTIVE);
                    Intent intent = new Intent(this, AnimationService.class).setAction(AnimationService.ACTION_START);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(intent);
                    } else {
                        startService(intent);
                    }
                } else tile.setState(Tile.STATE_INACTIVE);
            } else tile.setState(Tile.STATE_UNAVAILABLE);
            tile.updateTile();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        tile = getQsTile();

        if (isServiceAvailable())
            tile.setState(prefs.getBoolean(AnimationService.PREF_KEY_ENABLE, false) ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        else tile.setState(Tile.STATE_UNAVAILABLE);

        tile.updateTile();
        prefs.registerOnSharedPreferenceChangeListener(prefsListener);
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        tile = getQsTile();
        if (!isServiceAvailable()) tile.setState(Tile.STATE_UNAVAILABLE);
    }

    @Override
    public void onClick() {
        super.onClick();
        int tileState = tile.getState();
        if (tileState != Tile.STATE_UNAVAILABLE) {
            prefs.edit().putBoolean(AnimationService.PREF_KEY_ENABLE, tileState == Tile.STATE_INACTIVE).apply();
        }
    }

    private boolean isServiceAvailable() {
        return Settings.canDrawOverlays(this);
    }
}