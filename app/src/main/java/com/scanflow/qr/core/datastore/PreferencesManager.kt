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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.emptyPreferences
import java.io.IOException

import androidx.datastore.preferences.core.longPreferencesKey
import com.scanflow.qr.domain.model.AuthUser

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

    // Cloud Auth & Session
    private val KEY_USER_ID = stringPreferencesKey("auth_user_id")
    private val KEY_USER_EMAIL = stringPreferencesKey("auth_user_email")
    private val KEY_USER_DISPLAY_NAME = stringPreferencesKey("auth_user_display_name")
    private val KEY_USER_IS_GUEST = booleanPreferencesKey("auth_user_is_guest")
    private val KEY_USER_TOKEN = stringPreferencesKey("auth_user_token")
    private val KEY_LAST_SYNC_TIME = longPreferencesKey("auth_last_sync_time")
    private val KEY_USERS_REGISTRY = stringPreferencesKey("registered_users_registry")

    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            exception.printStackTrace()
            emit(androidx.datastore.preferences.core.emptyPreferences())
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

    // Auth Session Management
    val currentUserFlow: Flow<AuthUser> = context.dataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { pref ->
            val userId = pref[KEY_USER_ID]
            if (userId != null) {
                AuthUser(
                    id = userId,
                    email = pref[KEY_USER_EMAIL],
                    displayName = pref[KEY_USER_DISPLAY_NAME] ?: "User",
                    isGuest = pref[KEY_USER_IS_GUEST] ?: false,
                    token = pref[KEY_USER_TOKEN]
                )
            } else {
                AuthUser(
                    id = "local_guest_user",
                    email = null,
                    displayName = "Guest User",
                    isGuest = true
                )
            }
        }

    suspend fun saveUserSession(user: AuthUser) {
        context.dataStore.edit { pref ->
            pref[KEY_USER_ID] = user.id
            if (user.email != null) pref[KEY_USER_EMAIL] = user.email else pref.remove(KEY_USER_EMAIL)
            if (user.displayName != null) pref[KEY_USER_DISPLAY_NAME] = user.displayName else pref.remove(KEY_USER_DISPLAY_NAME)
            pref[KEY_USER_IS_GUEST] = user.isGuest
            if (user.token != null) pref[KEY_USER_TOKEN] = user.token else pref.remove(KEY_USER_TOKEN)
        }
    }

    suspend fun clearUserSession() {
        context.dataStore.edit { pref ->
            pref.remove(KEY_USER_ID)
            pref.remove(KEY_USER_EMAIL)
            pref.remove(KEY_USER_DISPLAY_NAME)
            pref.remove(KEY_USER_TOKEN)
            pref[KEY_USER_IS_GUEST] = true
        }
    }

    // Cloud Sync Tracking
    val lastSyncTimeFlow: Flow<Long?> = context.dataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { pref -> pref[KEY_LAST_SYNC_TIME] }

    suspend fun updateLastSyncTime(timestamp: Long) {
        context.dataStore.edit { pref -> pref[KEY_LAST_SYNC_TIME] = timestamp }
    }

    // User Registry for credential validation
    fun hashPassword(password: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun saveRegisteredCredentials(email: String, passwordHash: String, name: String) {
        context.dataStore.edit { pref ->
            val existing = pref[KEY_USERS_REGISTRY] ?: "{}"
            val json = try { org.json.JSONObject(existing) } catch (e: Exception) { org.json.JSONObject() }
            val userRecord = org.json.JSONObject().apply {
                put("hash", passwordHash)
                put("name", name)
            }
            json.put(email.lowercase().trim(), userRecord)
            pref[KEY_USERS_REGISTRY] = json.toString()
        }
    }

    suspend fun getRegisteredUserRecord(email: String): Pair<String, String>? {
        val pref = context.dataStore.data.first()
        val registry = pref[KEY_USERS_REGISTRY] ?: return null
        return try {
            val json = org.json.JSONObject(registry)
            val key = email.lowercase().trim()
            if (json.has(key)) {
                val record = json.getJSONObject(key)
                record.getString("hash") to record.optString("name", "User")
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
