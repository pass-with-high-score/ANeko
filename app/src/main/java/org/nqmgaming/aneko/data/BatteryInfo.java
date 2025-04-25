package org.nqmgaming.aneko.data;

import android.content.Intent;

import static android.os.BatteryManager.EXTRA_LEVEL;

public class BatteryInfo {
    public int level;

    public BatteryInfo(final Intent intent) {
        level = intent.getIntExtra(EXTRA_LEVEL, 0);
    }

}
