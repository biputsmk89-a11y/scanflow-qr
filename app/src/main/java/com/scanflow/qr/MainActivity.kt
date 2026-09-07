package com.scanflow.qr

import android.content.Intent
import android.net.Uri
import android.os.Build
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import com.scanflow.qr.core.designsystem.ScanFlowQRTheme
import com.scanflow.qr.core.navigation.ScanFlowNavGraph
import com.scanflow.qr.core.security.AppLockManager
import com.scanflow.qr.core.security.BiometricAuthManager
import com.scanflow.qr.core.utils.LocaleHelper
import com.scanflow.qr.feature.auth.AppLockScreen
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

    @Inject
    lateinit var appLockManager: AppLockManager

    private var currentSettings = AppSettings()
    private val sharedImageUriState = kotlinx.coroutines.flow.MutableStateFlow<Uri?>(null)
    private val shortcutRouteState = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    override fun onStart() {
        super.onStart()
        appLockManager.onAppForegrounded(currentSettings)
    }

    override fun onStop() {
        super.onStop()
        appLockManager.onAppBackgrounded()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            sharedImageUriState.value = uri
        }

        // App Shortcuts (Long-press Launcher) & Quick Settings Tile
        when (intent.action) {
            "com.scanflow.qr.action.SCAN" -> shortcutRouteState.value = com.scanflow.qr.core.navigation.Screen.Scanner.route
            "com.scanflow.qr.action.CREATE" -> shortcutRouteState.value = com.scanflow.qr.core.navigation.Screen.CreateQr.route
            "com.scanflow.qr.action.HISTORY" -> shortcutRouteState.value = com.scanflow.qr.core.navigation.Screen.History.route
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncomingIntent(intent)

        setContent {
            val sharedImageUri by sharedImageUriState.collectAsState()
            val shortcutRoute by shortcutRouteState.collectAsState()
            val settings by settingsRepository.settingsFlow.collectAsState(
                initial = AppSettings()
            )

            val isDarkTheme = when (settings.themeMode) {
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            val isUnlockedState by appLockManager.isUnlocked.collectAsState()
            val scope = rememberCoroutineScope()

            LaunchedEffect(settings) {
                currentSettings = settings
                appLockManager.init(settings)
                appLockManager.onSettingsChanged(settings)
            }

            LaunchedEffect(settings.language) {
                LocaleHelper.applyLocale(this@MainActivity, settings.language)
            }

            val isUnlocked = isUnlockedState ?: !(settings.isAppLockEnabled || settings.isBiometricEnabled)

            val currentContext = LocalContext.current
            val targetLocale = remember(settings.language) { java.util.Locale(settings.language) }
            val localizedConfiguration = remember(targetLocale) {
                android.content.res.Configuration(resources.configuration).apply {
                    setLocale(targetLocale)
                }
            }
            val localizedContext = remember(localizedConfiguration) {
                currentContext.createConfigurationContext(localizedConfiguration)
            }

            CompositionLocalProvider(
                LocalConfiguration provides localizedConfiguration,
                LocalContext provides localizedContext
            ) {
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
                                },
                                sharedImageUri = sharedImageUri,
                                onSharedUriHandled = { sharedImageUriState.value = null },
                                shortcutRoute = shortcutRoute,
                                onShortcutRouteHandled = { shortcutRouteState.value = null }
                            )
                        } else {
                            AppLockScreen(
                                settings = settings,
                                onUnlockSuccess = { appLockManager.unlock() }
                            )
                        }
                    }
                }
            }
        }
    }
}
