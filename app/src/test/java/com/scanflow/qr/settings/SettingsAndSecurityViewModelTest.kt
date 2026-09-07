package com.scanflow.qr.settings

import com.google.common.truth.Truth.assertThat
import com.scanflow.qr.di.FakeSettingsRepository
import com.scanflow.qr.domain.model.AppSettings
import com.scanflow.qr.domain.model.AppThemeMode
import com.scanflow.qr.domain.usecase.GetSettingsUseCase
import com.scanflow.qr.domain.usecase.UpdateSettingsUseCase
import com.scanflow.qr.feature.security.SecurityViewModel
import com.scanflow.qr.feature.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsAndSecurityViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var getSettingsUseCase: GetSettingsUseCase
    private lateinit var updateSettingsUseCase: UpdateSettingsUseCase
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var securityViewModel: SecurityViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        settingsRepository = FakeSettingsRepository(
            initialSettings = AppSettings(
                themeMode = AppThemeMode.LIGHT,
                vibrateOnScan = false,
                beepOnScan = false,
                autoOpenUrl = false,
                autoCopyToClipboard = false,
                isAppLockEnabled = false,
                isBiometricEnabled = false,
                pinCode = null
            )
        )
        getSettingsUseCase = GetSettingsUseCase(settingsRepository)
        updateSettingsUseCase = UpdateSettingsUseCase(settingsRepository)

        settingsViewModel = SettingsViewModel(getSettingsUseCase, updateSettingsUseCase)
        securityViewModel = SecurityViewModel(getSettingsUseCase, updateSettingsUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `SettingsViewModel updates theme mode`() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            settingsViewModel.settings.collect {}
        }
        advanceUntilIdle()

        assertThat(settingsViewModel.settings.value.themeMode).isEqualTo(AppThemeMode.LIGHT)

        settingsViewModel.setTheme(AppThemeMode.DARK)
        advanceUntilIdle()

        assertThat(settingsViewModel.settings.value.themeMode).isEqualTo(AppThemeMode.DARK)
    }

    @Test
    fun `SettingsViewModel toggles Material You dynamic color preference`() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            settingsViewModel.settings.collect {}
        }
        advanceUntilIdle()

        assertThat(settingsViewModel.settings.value.isDynamicColorEnabled).isTrue()

        settingsViewModel.toggleDynamicColor(false)
        advanceUntilIdle()

        assertThat(settingsViewModel.settings.value.isDynamicColorEnabled).isFalse()

        settingsViewModel.toggleDynamicColor(true)
        advanceUntilIdle()

        assertThat(settingsViewModel.settings.value.isDynamicColorEnabled).isTrue()
    }

    @Test
    fun `SettingsViewModel toggles scanning preferences`() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            settingsViewModel.settings.collect {}
        }
        advanceUntilIdle()

        settingsViewModel.toggleVibrate(true)
        settingsViewModel.toggleBeep(true)
        settingsViewModel.toggleAutoOpen(true)
        settingsViewModel.toggleAutoCopy(true)
        advanceUntilIdle()

        val updated = settingsViewModel.settings.value
        assertThat(updated.vibrateOnScan).isTrue()
        assertThat(updated.beepOnScan).isTrue()
        assertThat(updated.autoOpenUrl).isTrue()
        assertThat(updated.autoCopyToClipboard).isTrue()
    }

    @Test
    fun `SecurityViewModel updates PIN and enables app lock`() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            securityViewModel.settings.collect {}
        }
        advanceUntilIdle()

        assertThat(securityViewModel.settings.value.isAppLockEnabled).isFalse()
        assertThat(securityViewModel.settings.value.pinCode).isNull()

        securityViewModel.setPin("5678")
        securityViewModel.toggleAppLock(true)
        advanceUntilIdle()

        val updated = securityViewModel.settings.value
        assertThat(updated.pinCode).isEqualTo("5678")
        assertThat(updated.isAppLockEnabled).isTrue()
    }

    @Test
    fun `SecurityViewModel toggles biometric authentication`() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            securityViewModel.settings.collect {}
        }
        advanceUntilIdle()

        securityViewModel.toggleBiometric(true)
        advanceUntilIdle()

        assertThat(securityViewModel.settings.value.isBiometricEnabled).isTrue()
    }

    @Test
    fun `SecurityViewModel updates lock timeout`() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            securityViewModel.settings.collect {}
        }
        advanceUntilIdle()

        assertThat(securityViewModel.settings.value.lockTimeoutSeconds).isEqualTo(0L)

        securityViewModel.setLockTimeout(300L)
        advanceUntilIdle()

        assertThat(securityViewModel.settings.value.lockTimeoutSeconds).isEqualTo(300L)
    }
}
