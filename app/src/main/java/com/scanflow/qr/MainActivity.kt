package com.scanflow.qr

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.scanflow.qr.core.designsystem.ScanFlowQRTheme
import com.scanflow.qr.core.navigation.ScanFlowNavGraph
import com.scanflow.qr.core.security.BiometricAuthManager
import com.scanflow.qr.domain.model.AppSettings
import com.scanflow.qr.domain.model.AppThemeMode
import com.scanflow.qr.domain.repository.SettingsRepository
import com.scanflow.qr.domain.usecase.UpdateSettingsUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var updateSettingsUseCase: UpdateSettingsUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val settings by settingsRepository.settingsFlow.collectAsState(
                initial = AppSettings()
            )

            val isDarkTheme = when (settings.themeMode) {
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            var isUnlocked by remember { mutableStateOf(!settings.isAppLockEnabled) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(settings.isAppLockEnabled) {
                if (settings.isAppLockEnabled && !isUnlocked) {
                    if (settings.isBiometricEnabled && BiometricAuthManager.isBiometricAvailable(this@MainActivity)) {
                        BiometricAuthManager.authenticate(
                            activity = this@MainActivity,
                            onSuccess = { isUnlocked = true },
                            onError = { isUnlocked = true }
                        )
                    } else {
                        isUnlocked = true // PIN / Guest fallback
                    }
                } else {
                    isUnlocked = true
                }
            }

            ScanFlowQRTheme(
                darkTheme = isDarkTheme,
                dynamicColor = settings.isDynamicColorEnabled
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isUnlocked) {
                        val navController = rememberNavController()

                        ScanFlowNavGraph(
                            navController = navController,
                            isOnboardingCompleted = settings.isOnboardingCompleted,
                            onCompleteOnboarding = {
                                scope.launch {
                                    updateSettingsUseCase.completeOnboarding()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
