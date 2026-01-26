package org.nqmgaming.aneko.core.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    private const val SELECTED_LANGUAGE = "selected_language"

    fun setLocale(context: Context, languageCode: String) {
        // Save preference
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString(SELECTED_LANGUAGE, languageCode)
            .apply()
    }

    fun getLocale(context: Context): String {
        return context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString(SELECTED_LANGUAGE, "en") ?: "en"
    }

    fun applyLocale(context: Context): Context {
        val languageCode = getLocale(context)
        val locale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }
}