package org.nqmgaming.aneko.core.festive

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.core.content.edit
import org.nqmgaming.aneko.R
import java.util.Calendar

object ChristmasSeasonHelper {
    const val PREF_KEY_CHRISTMAS_HAT = "motion.christmas_hat"

    /**
     * Checks whether the given date falls into the Christmas holiday season.
     * Seasonal Easter egg for Christmas 2026 (or December in any year >= 2026).
     */
    fun isChristmasSeason(calendar: Calendar = Calendar.getInstance()): Boolean {
        val month = calendar.get(Calendar.MONTH) // Calendar.DECEMBER is 11
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        // Active throughout December, and early January (up to Jan 6)
        return (month == Calendar.DECEMBER) || (month == Calendar.JANUARY && day <= 6)
    }

    /**
     * Checks if the Christmas hat is enabled:
     * - If explicitly configured in preferences, use that value.
     * - Otherwise, automatically enabled during the Christmas season.
     */
    fun isChristmasHatEnabled(prefs: SharedPreferences, calendar: Calendar = Calendar.getInstance()): Boolean {
        return if (prefs.contains(PREF_KEY_CHRISTMAS_HAT)) {
            prefs.getBoolean(PREF_KEY_CHRISTMAS_HAT, false)
        } else {
            isChristmasSeason(calendar)
        }
    }

    /**
     * Toggle the Christmas hat Easter egg, persisting the preference and showing a festive toast.
     */
    fun toggleChristmasHat(context: Context, prefs: SharedPreferences): Boolean {
        val current = isChristmasHatEnabled(prefs)
        val newValue = !current
        prefs.edit { putBoolean(PREF_KEY_CHRISTMAS_HAT, newValue) }
        val msgRes = if (newValue) {
            R.string.christmas_easter_egg_enabled
        } else {
            R.string.christmas_easter_egg_disabled
        }
        Toast.makeText(context, msgRes, Toast.LENGTH_SHORT).show()
        return newValue
    }
}
