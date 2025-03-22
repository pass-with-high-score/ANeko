/*
 *  Copyright 2015 Erkan Molla
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.renewal.aneko;

import android.content.Intent;

import static android.os.BatteryManager.*;

class BatteryInfo {
    public int status;
    public int health;
    public boolean present;
    public int level;
    public int scale;
    public int iconSmallResId;
    public int plugged;
    public int voltage;
    public int temperature;
    public String technology;

    BatteryInfo(final Intent intent) {
        status = intent.getIntExtra(EXTRA_STATUS, 0);
        health = intent.getIntExtra(EXTRA_HEALTH, 0);
        present = intent.getBooleanExtra(EXTRA_PRESENT, false);
        level = intent.getIntExtra(EXTRA_LEVEL, 0);
        scale = intent.getIntExtra(EXTRA_SCALE, 0);
        iconSmallResId = intent.getIntExtra(EXTRA_ICON_SMALL, 0);
        plugged = intent.getIntExtra(EXTRA_PLUGGED, 0);
        voltage = intent.getIntExtra(EXTRA_VOLTAGE, 0);
        temperature = intent.getIntExtra(EXTRA_TEMPERATURE, 0);
        technology = intent.getStringExtra(EXTRA_TECHNOLOGY);
    }

}
