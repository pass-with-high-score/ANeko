package org.nqmgaming.aneko.core.festive

import android.content.SharedPreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ChristmasSeasonHelperTest {

    private class FakeSharedPreferences(
        private val values: MutableMap<String, Any?> = mutableMapOf()
    ) : SharedPreferences {
        override fun getAll(): MutableMap<String, *> = values
        override fun getString(key: String?, defValue: String?): String? = values[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            values[key] as? MutableSet<String> ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = values.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor(values)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        fun put(key: String, value: Any?) {
            values[key] = value
        }
    }

    private class FakeEditor(private val values: MutableMap<String, Any?>) : SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any?>()
        override fun putString(key: String?, value: String?) = apply { temp[key ?: ""] = value }
        override fun putStringSet(key: String?, values: MutableSet<String>?) = apply { temp[key ?: ""] = values }
        override fun putInt(key: String?, value: Int) = apply { temp[key ?: ""] = value }
        override fun putLong(key: String?, value: Long) = apply { temp[key ?: ""] = value }
        override fun putFloat(key: String?, value: Float) = apply { temp[key ?: ""] = value }
        override fun putBoolean(key: String?, value: Boolean) = apply { temp[key ?: ""] = value }
        override fun remove(key: String?) = apply { temp[key ?: ""] = null }
        override fun clear() = apply { values.clear() }
        override fun commit(): Boolean {
            values.putAll(temp)
            return true
        }
        override fun apply() {
            values.putAll(temp)
        }
    }

    @Test
    fun isChristmasSeason_returnsTrue_forDecemberDates() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.DECEMBER, 25)
        }
        assertTrue(ChristmasSeasonHelper.isChristmasSeason(calendar))

        calendar.set(2026, Calendar.DECEMBER, 1)
        assertTrue(ChristmasSeasonHelper.isChristmasSeason(calendar))

        calendar.set(2026, Calendar.DECEMBER, 31)
        assertTrue(ChristmasSeasonHelper.isChristmasSeason(calendar))
    }

    @Test
    fun isChristmasSeason_returnsTrue_forEarlyJanuary() {
        val calendar = Calendar.getInstance().apply {
            set(2027, Calendar.JANUARY, 5)
        }
        assertTrue(ChristmasSeasonHelper.isChristmasSeason(calendar))
    }

    @Test
    fun isChristmasSeason_returnsFalse_forNonChristmasDates() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 9)
        }
        assertFalse(ChristmasSeasonHelper.isChristmasSeason(calendar))

        calendar.set(2026, Calendar.JULY, 4)
        assertFalse(ChristmasSeasonHelper.isChristmasSeason(calendar))

        calendar.set(2027, Calendar.JANUARY, 15)
        assertFalse(ChristmasSeasonHelper.isChristmasSeason(calendar))
    }

    @Test
    fun isChristmasHatEnabled_defaultsToSeasonCheck_whenPrefNotSet() {
        val prefs = FakeSharedPreferences()

        val decCalendar = Calendar.getInstance().apply {
            set(2026, Calendar.DECEMBER, 25)
        }
        assertTrue(ChristmasSeasonHelper.isChristmasHatEnabled(prefs, decCalendar))

        val novCalendar = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 9)
        }
        assertFalse(ChristmasSeasonHelper.isChristmasHatEnabled(prefs, novCalendar))
    }

    @Test
    fun isChristmasHatEnabled_respectsExplicitPreference_overSeason() {
        val prefs = FakeSharedPreferences()

        // Explicitly true in November
        prefs.put(ChristmasSeasonHelper.PREF_KEY_CHRISTMAS_HAT, true)
        val novCalendar = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 9)
        }
        assertTrue(ChristmasSeasonHelper.isChristmasHatEnabled(prefs, novCalendar))

        // Explicitly false in December
        prefs.put(ChristmasSeasonHelper.PREF_KEY_CHRISTMAS_HAT, false)
        val decCalendar = Calendar.getInstance().apply {
            set(2026, Calendar.DECEMBER, 25)
        }
        assertFalse(ChristmasSeasonHelper.isChristmasHatEnabled(prefs, decCalendar))
    }
}
