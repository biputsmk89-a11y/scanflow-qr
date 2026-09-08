package com.scanflow.qr.core.utils

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object LocaleHelper {

    val SUPPORTED_LANGUAGES = mapOf(
        "en" to "English (US)",
        "id" to "Bahasa Indonesia"
    )

    /**
     * Menerapkan bahasa pilihan ke runtime aplikasi secara menyeluruh:
     * 1. Mengubah Locale default JVM.
     * 2. Menggunakan Android 13+ (API 33+) LocaleManager per-app language API.
     * 3. Memperbarui Resource Configuration pada context saat ini.
     */
    fun applyLocale(context: Context, langCode: String): Context {
        val targetLocale = Locale(langCode)
        Locale.setDefault(targetLocale)

        val res = context.resources
        val config = Configuration(res.configuration)
        val currentLang = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.locales.get(0)?.language
        } else {
            @Suppress("DEPRECATION")
            config.locale?.language
        }

        if (currentLang == langCode) {
            return context
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val localeManager = context.getSystemService(Context.LOCALE_SERVICE) as? LocaleManager
                if (localeManager?.applicationLocales?.toLanguageTags() != langCode) {
                    localeManager?.applicationLocales = LocaleList.forLanguageTags(langCode)
                }
            } catch (_: Exception) {}
        }

        config.setLocale(targetLocale)
        @Suppress("DEPRECATION")
        res.updateConfiguration(config, res.displayMetrics)

        return context.createConfigurationContext(config)
    }

    fun getDisplayName(langCode: String): String {
        return SUPPORTED_LANGUAGES[langCode] ?: "English (US)"
    }
}
