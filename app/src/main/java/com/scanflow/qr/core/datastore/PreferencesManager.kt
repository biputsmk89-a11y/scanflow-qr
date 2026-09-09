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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.PREFERENCES_NAME)

class PreferencesManager(
    private val context: Context? = null,
    private val customDataStore: DataStore<Preferences>? = null
) {
    private val dataStore: DataStore<Preferences>
        get() = customDataStore ?: (context?.dataStore ?: throw IllegalStateException("Context or DataStore must be provided"))

    private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    private val KEY_VIBRATE = booleanPreferencesKey("vibrate_on_scan")
    private val KEY_BEEP = booleanPreferencesKey("beep_on_scan")
    private val KEY_AUTO_OPEN = booleanPreferencesKey("auto_open_url")
    private val KEY_AUTO_COPY = booleanPreferencesKey("auto_copy")
    private val KEY_APP_LOCK = booleanPreferencesKey("is_app_lock_enabled")
    private val KEY_BIOMETRIC = booleanPreferencesKey("is_biometric_enabled")
    private val KEY_PIN_CODE = stringPreferencesKey("pin_code")
    private val KEY_ONBOARDING = booleanPreferencesKey("is_onboarding_completed")
    private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("is_dynamic_color_enabled")
    private val KEY_AUTO_SCAN = booleanPreferencesKey("auto_scan")
    private val KEY_LANGUAGE = stringPreferencesKey("app_language")
    private val KEY_SAVE_SCAN_HISTORY = booleanPreferencesKey("save_scan_history")
    private val KEY_SEND_ANONYMOUS_ANALYTICS = booleanPreferencesKey("send_anonymous_analytics")
    private val KEY_SAFE_URL_DETECTION = booleanPreferencesKey("safe_url_detection")
    private val KEY_SUSPICIOUS_QR_WARNING = booleanPreferencesKey("suspicious_qr_warning")
    private val KEY_CLIPBOARD_PROTECTION = booleanPreferencesKey("clipboard_protection")
    private val KEY_LOCK_TIMEOUT = longPreferencesKey("lock_timeout_seconds")

    // Cloud Auth & Session
    private val KEY_USER_ID = stringPreferencesKey("auth_user_id")
    private val KEY_USER_EMAIL = stringPreferencesKey("auth_user_email")
    private val KEY_USER_DISPLAY_NAME = stringPreferencesKey("auth_user_display_name")
    private val KEY_USER_IS_GUEST = booleanPreferencesKey("auth_user_is_guest")
    private val KEY_USER_TOKEN = stringPreferencesKey("auth_user_token")
    private val KEY_USER_AVATAR_URI = stringPreferencesKey("auth_user_avatar_uri")
    private val KEY_LAST_SYNC_TIME = longPreferencesKey("auth_last_sync_time")
    private val KEY_USERS_REGISTRY = stringPreferencesKey("registered_users_registry")

    val settingsFlow: Flow<AppSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
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
                isOnboardingCompleted = pref[KEY_ONBOARDING] ?: false,
                isDynamicColorEnabled = pref[KEY_DYNAMIC_COLOR] ?: true,
                autoScan = pref[KEY_AUTO_SCAN] ?: true,
                language = pref[KEY_LANGUAGE] ?: "en",
                saveScanHistory = pref[KEY_SAVE_SCAN_HISTORY] ?: true,
                sendAnonymousAnalytics = pref[KEY_SEND_ANONYMOUS_ANALYTICS] ?: true,
                safeUrlDetection = pref[KEY_SAFE_URL_DETECTION] ?: true,
                suspiciousQrWarning = pref[KEY_SUSPICIOUS_QR_WARNING] ?: true,
                clipboardProtection = pref[KEY_CLIPBOARD_PROTECTION] ?: true,
                lockTimeoutSeconds = pref[KEY_LOCK_TIMEOUT] ?: 0L
            )
        }

    suspend fun updateThemeMode(themeMode: AppThemeMode) {
        dataStore.edit { pref ->
            pref[KEY_THEME_MODE] = themeMode.name
        }
    }

    suspend fun updateVibrate(enabled: Boolean) {
        dataStore.edit { pref -> pref[KEY_VIBRATE] = enabled }
    }

    suspend fun updateBeep(enabled: Boolean) {
        dataStore.edit { pref -> pref[KEY_BEEP] = enabled }
    }

    suspend fun updateAutoOpen(enabled: Boolean) {
        dataStore.edit { pref -> pref[KEY_AUTO_OPEN] = enabled }
    }

    suspend fun updateAutoCopy(enabled: Boolean) {
        dataStore.edit { pref -> pref[KEY_AUTO_COPY] = enabled }
    }

    suspend fun updateAppLock(enabled: Boolean) {
        dataStore.edit { pref -> pref[KEY_APP_LOCK] = enabled }
    }

    suspend fun updateBiometric(enabled: Boolean) {
        dataStore.edit { pref -> pref[KEY_BIOMETRIC] = enabled }
    }

    suspend fun updateLockTimeout(seconds: Long) {
        dataStore.edit { pref -> pref[KEY_LOCK_TIMEOUT] = seconds }
    }

    suspend fun updateDynamicColor(enabled: Boolean) {
        dataStore.edit { pref -> pref[KEY_DYNAMIC_COLOR] = enabled }
    }

    suspend fun updateAutoScan(enabled: Boolean) {
        dataStore.edit { pref -> pref[KEY_AUTO_SCAN] = enabled }
    }

    suspend fun updateLanguage(lang: String) {
        dataStore.edit { pref -> pref[KEY_LANGUAGE] = lang }
    }

    suspend fun updateSaveScanHistory(enabled: Boolean) {
        dataStore.edit { pref -> pref[KEY_SAVE_SCAN_HISTORY] = enabled }
    }

    suspend fun updateSendAnonymousAnalytics(enabled: Boolean) {
        dataStore.edit { pref -> pref[KEY_SEND_ANONYMOUS_ANALYTICS] = enabled }
    }

    suspend fun updateSafeUrlDetection(enabled: Boolean) {
        dataStore.edit { pref -> pref[KEY_SAFE_URL_DETECTION] = enabled }
    }

    suspend fun updateSuspiciousQrWarning(enabled: Boolean) {
        dataStore.edit { pref -> pref[KEY_SUSPICIOUS_QR_WARNING] = enabled }
    }

    suspend fun updateClipboardProtection(enabled: Boolean) {
        dataStore.edit { pref -> pref[KEY_CLIPBOARD_PROTECTION] = enabled }
    }

    suspend fun updateProfile(displayName: String, email: String) {
        dataStore.edit { pref ->
            pref[KEY_USER_DISPLAY_NAME] = displayName
            pref[KEY_USER_EMAIL] = email
        }
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
        dataStore.edit { pref ->
            if (pin != null) pref[KEY_PIN_CODE] = hashPin(pin) else pref.remove(KEY_PIN_CODE)
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { pref -> pref[KEY_ONBOARDING] = completed }
    }

    // Auth Session Management
    val currentUserFlow: Flow<AuthUser> = dataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { pref ->
            val userId = pref[KEY_USER_ID]
            if (userId != null) {
                AuthUser(
                    id = userId,
                    email = pref[KEY_USER_EMAIL],
                    displayName = pref[KEY_USER_DISPLAY_NAME] ?: "User",
                    isGuest = pref[KEY_USER_IS_GUEST] ?: false,
                    token = pref[KEY_USER_TOKEN],
                    avatarUri = pref[KEY_USER_AVATAR_URI]
                )
            } else {
                AuthUser(
                    id = "local_guest_user",
                    email = null,
                    displayName = "Guest User",
                    isGuest = true,
                    avatarUri = pref[KEY_USER_AVATAR_URI]
                )
            }
        }

    suspend fun saveUserSession(user: AuthUser) {
        dataStore.edit { pref ->
            pref[KEY_USER_ID] = user.id
            if (user.email != null) pref[KEY_USER_EMAIL] = user.email else pref.remove(KEY_USER_EMAIL)
            if (user.displayName != null) pref[KEY_USER_DISPLAY_NAME] = user.displayName else pref.remove(KEY_USER_DISPLAY_NAME)
            pref[KEY_USER_IS_GUEST] = user.isGuest
            if (user.token != null) pref[KEY_USER_TOKEN] = user.token else pref.remove(KEY_USER_TOKEN)
            if (user.avatarUri != null) pref[KEY_USER_AVATAR_URI] = user.avatarUri else pref.remove(KEY_USER_AVATAR_URI)
        }
    }

    suspend fun clearUserSession() {
        dataStore.edit { pref ->
            pref.remove(KEY_USER_ID)
            pref.remove(KEY_USER_EMAIL)
            pref.remove(KEY_USER_DISPLAY_NAME)
            pref.remove(KEY_USER_TOKEN)
            pref.remove(KEY_USER_AVATAR_URI)
            pref[KEY_USER_IS_GUEST] = true
        }
    }

    suspend fun updateAvatarUri(uri: String?) {
        dataStore.edit { pref ->
            if (uri != null) {
                pref[KEY_USER_AVATAR_URI] = uri
            } else {
                pref.remove(KEY_USER_AVATAR_URI)
            }
        }
    }

    // Cloud Sync Tracking
    val lastSyncTimeFlow: Flow<Long?> = dataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { pref -> pref[KEY_LAST_SYNC_TIME] }

    suspend fun updateLastSyncTime(timestamp: Long) {
        dataStore.edit { pref -> pref[KEY_LAST_SYNC_TIME] = timestamp }
    }

    // User Registry for credential validation
    fun hashPassword(password: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun saveRegisteredCredentials(email: String, passwordHash: String, name: String) {
        dataStore.edit { pref ->
            val existing = pref[KEY_USERS_REGISTRY] ?: "{}"
            val map: MutableMap<String, UserRegistryRecord> = try {
                jsonConfig.decodeFromString<Map<String, UserRegistryRecord>>(existing).toMutableMap()
            } catch (e: Exception) {
                mutableMapOf()
            }
            map[email.lowercase().trim()] = UserRegistryRecord(hash = passwordHash, name = name)
            pref[KEY_USERS_REGISTRY] = jsonConfig.encodeToString(map)
        }
    }

    suspend fun getRegisteredUserRecord(email: String): Pair<String, String>? {
        val pref = dataStore.data.first()
        val registry = pref[KEY_USERS_REGISTRY] ?: return null
        return try {
            val map = jsonConfig.decodeFromString<Map<String, UserRegistryRecord>>(registry)
            val record = map[email.lowercase().trim()]
            if (record != null) record.hash to record.name else null
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private val jsonConfig = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

        fun isValidEmail(email: String): Boolean {
            val clean = email.trim()
            if (clean.isEmpty()) return false
            return try {
                android.util.Patterns.EMAIL_ADDRESS?.matcher(clean)?.matches() ?: EMAIL_REGEX.matches(clean)
            } catch (e: Throwable) {
                EMAIL_REGEX.matches(clean)
            }
        }
    }
}

@kotlinx.serialization.Serializable
internal data class UserRegistryRecord(
    val hash: String,
    val name: String
)

