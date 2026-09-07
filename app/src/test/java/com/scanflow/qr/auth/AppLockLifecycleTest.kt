package com.scanflow.qr.auth

import com.google.common.truth.Truth.assertThat
import com.scanflow.qr.core.security.AppLockManager
import com.scanflow.qr.domain.model.AppSettings
import org.junit.Before
import org.junit.Test

class AppLockLifecycleTest {

    private lateinit var appLockManager: AppLockManager

    @Before
    fun setUp() {
        appLockManager = AppLockManager()
    }

    @Test
    fun `init with app lock disabled sets isUnlocked to true`() {
        val settings = AppSettings(isAppLockEnabled = false, isBiometricEnabled = false)
        appLockManager.init(settings)

        assertThat(appLockManager.isUnlocked.value).isTrue()
    }

    @Test
    fun `init with app lock enabled sets isUnlocked to false`() {
        val settings = AppSettings(isAppLockEnabled = true, pinCode = "1234")
        appLockManager.init(settings)

        assertThat(appLockManager.isUnlocked.value).isFalse()
    }

    @Test
    fun `unlock sets isUnlocked to true`() {
        val settings = AppSettings(isAppLockEnabled = true, pinCode = "1234")
        appLockManager.init(settings)
        assertThat(appLockManager.isUnlocked.value).isFalse()

        appLockManager.unlock()
        assertThat(appLockManager.isUnlocked.value).isTrue()
    }

    @Test
    fun `explicit lock sets isUnlocked to false`() {
        val settings = AppSettings(isAppLockEnabled = false)
        appLockManager.init(settings)
        assertThat(appLockManager.isUnlocked.value).isTrue()

        appLockManager.lock()
        assertThat(appLockManager.isUnlocked.value).isFalse()
    }

    @Test
    fun `backgrounding with timeout 0 immediately re-locks app on foreground`() {
        val settings = AppSettings(isAppLockEnabled = true, pinCode = "1234", lockTimeoutSeconds = 0L)
        appLockManager.init(settings)
        appLockManager.unlock()
        assertThat(appLockManager.isUnlocked.value).isTrue()

        // App goes to background
        appLockManager.onAppBackgrounded()

        // App returns to foreground
        appLockManager.onAppForegrounded(settings)

        assertThat(appLockManager.isUnlocked.value).isFalse()
    }

    @Test
    fun `backgrounding with timeout 60s does not lock if foregrounded immediately`() {
        val settings = AppSettings(isAppLockEnabled = true, pinCode = "1234", lockTimeoutSeconds = 60L)
        appLockManager.init(settings)
        appLockManager.unlock()
        assertThat(appLockManager.isUnlocked.value).isTrue()

        // App goes to background
        appLockManager.onAppBackgrounded()

        // App returns immediately (elapsed < 60s)
        appLockManager.onAppForegrounded(settings)

        assertThat(appLockManager.isUnlocked.value).isTrue()
    }

    @Test
    fun `temporary bypass prevents locking on foreground`() {
        val settings = AppSettings(isAppLockEnabled = true, pinCode = "1234", lockTimeoutSeconds = 0L)
        appLockManager.init(settings)
        appLockManager.unlock()

        // External picker opened (e.g. system file/image picker)
        appLockManager.setTemporarilyBypassed(true)
        appLockManager.onAppBackgrounded()

        // Return from picker
        appLockManager.onAppForegrounded(settings)

        // Must still be unlocked
        assertThat(appLockManager.isUnlocked.value).isTrue()
    }

    @Test
    fun `disabling app lock in settings immediately unlocks app`() {
        val lockedSettings = AppSettings(isAppLockEnabled = true, pinCode = "1234")
        appLockManager.init(lockedSettings)
        assertThat(appLockManager.isUnlocked.value).isFalse()

        val unlockedSettings = lockedSettings.copy(isAppLockEnabled = false, isBiometricEnabled = false)
        appLockManager.onSettingsChanged(unlockedSettings)

        assertThat(appLockManager.isUnlocked.value).isTrue()
    }
}
