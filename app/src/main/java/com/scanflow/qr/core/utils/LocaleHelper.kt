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

    fun getLocale(langCode: String): Locale {
        return when (langCode.lowercase()) {
            "id", "in" -> Locale("id")
            else -> Locale("en")
        }
    }

    /**
     * Menerapkan bahasa pilihan ke runtime aplikasi secara menyeluruh:
     * 1. Mengubah Locale default JVM.
     * 2. Menggunakan Android 13+ (API 33+) LocaleManager per-app language API.
     * 3. Memperbarui Resource Configuration pada context saat ini.
     */
    fun applyLocale(context: Context, langCode: String): Context {
        val targetLocale = getLocale(langCode)
        Locale.setDefault(targetLocale)

        val res = context.resources
        val config = Configuration(res.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val localeManager = context.getSystemService(Context.LOCALE_SERVICE) as? LocaleManager
                val currentTag = localeManager?.applicationLocales?.toLanguageTags()
                val targetTag = if (langCode == "id") "id" else "en"
                if (currentTag != targetTag) {
                    localeManager?.applicationLocales = LocaleList.forLanguageTags(targetTag)
                }
            } catch (_: Exception) {}
        }

        config.setLocale(targetLocale)
        config.setLayoutDirection(targetLocale)
        @Suppress("DEPRECATION")
        res.updateConfiguration(config, res.displayMetrics)

        return context.createConfigurationContext(config)
    }

    fun getDisplayName(langCode: String): String {
        return SUPPORTED_LANGUAGES[langCode] ?: "English (US)"
    }
}
