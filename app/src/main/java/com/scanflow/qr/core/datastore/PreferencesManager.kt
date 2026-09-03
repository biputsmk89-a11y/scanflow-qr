package com.scanflow.qr.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.scanflow.qr.core.common.Constants
import com.scanflow.qr.domain.model.AppSettings
import com.scanflow.qr.domain.model.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.emptyPreferences
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.PREFERENCES_NAME)

class PreferencesManager(private val context: Context) {

    private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    private val KEY_VIBRATE = booleanPreferencesKey("vibrate_on_scan")
    private val KEY_BEEP = booleanPreferencesKey("beep_on_scan")
    private val KEY_AUTO_OPEN = booleanPreferencesKey("auto_open_url")
    private val KEY_AUTO_COPY = booleanPreferencesKey("auto_copy")
    private val KEY_APP_LOCK = booleanPreferencesKey("is_app_lock_enabled")
    private val KEY_BIOMETRIC = booleanPreferencesKey("is_biometric_enabled")
    private val KEY_PIN_CODE = stringPreferencesKey("pin_code")
    private val KEY_ONBOARDING = booleanPreferencesKey("is_onboarding_completed")

    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is java.io.IOException) {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { pref ->
            AppSettings(
                themeMode = when (pref[KEY_THEME_MODE]) {
                    "LIGHT" -> AppThemeMode.LIGHT
                    "SYSTEM" -> AppThemeMode.SYSTEM
                    else -> AppThemeMode.DARK
                },
                vibrateOnScan = pref[KEY_VIBRATE] ?: true,
                beepOnScan = pref[KEY_BEEP] ?: true,
                autoOpenUrl = pref[KEY_AUTO_OPEN] ?: false,
                autoCopyToClipboard = pref[KEY_AUTO_COPY] ?: false,
                isAppLockEnabled = pref[KEY_APP_LOCK] ?: false,
                isBiometricEnabled = pref[KEY_BIOMETRIC] ?: false,
                pinCode = pref[KEY_PIN_CODE],
                isOnboardingCompleted = pref[KEY_ONBOARDING] ?: false
            )
        }

    suspend fun updateThemeMode(themeMode: AppThemeMode) {
        context.dataStore.edit { pref ->
            pref[KEY_THEME_MODE] = themeMode.name
        }
    }

    suspend fun updateVibrate(enabled: Boolean) {
        context.dataStore.edit { pref -> pref[KEY_VIBRATE] = enabled }
    }

    suspend fun updateBeep(enabled: Boolean) {
        context.dataStore.edit { pref -> pref[KEY_BEEP] = enabled }
    }

    suspend fun updateAutoOpen(enabled: Boolean) {
        context.dataStore.edit { pref -> pref[KEY_AUTO_OPEN] = enabled }
    }

    suspend fun updateAutoCopy(enabled: Boolean) {
        context.dataStore.edit { pref -> pref[KEY_AUTO_COPY] = enabled }
    }

    suspend fun updateAppLock(enabled: Boolean) {
        context.dataStore.edit { pref -> pref[KEY_APP_LOCK] = enabled }
    }

    suspend fun updateBiometric(enabled: Boolean) {
        context.dataStore.edit { pref -> pref[KEY_BIOMETRIC] = enabled }
    }

    private fun hashPin(pin: String): String {
        return if (pin.length == 64 && pin.all { it.isDigit() || it in 'a'..'f' }) {
            pin // Already hashed
        } else {
            val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }
        }
    }

    suspend fun updatePinCode(pin: String?) {
        context.dataStore.edit { pref ->
            if (pin != null) pref[KEY_PIN_CODE] = hashPin(pin) else pref.remove(KEY_PIN_CODE)
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { pref -> pref[KEY_ONBOARDING] = completed }
    }
}
