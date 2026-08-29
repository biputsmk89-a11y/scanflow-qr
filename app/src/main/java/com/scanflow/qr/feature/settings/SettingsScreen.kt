package com.scanflow.qr.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.qr.core.designsystem.Dimens
import com.scanflow.qr.core.designsystem.ElectricBlue
import com.scanflow.qr.core.designsystem.ScanFlowCard
import com.scanflow.qr.core.designsystem.SectionHeader
import com.scanflow.qr.domain.model.AppSettings
import com.scanflow.qr.domain.model.AppThemeMode
import com.scanflow.qr.domain.usecase.GetSettingsUseCase
import com.scanflow.qr.domain.usecase.UpdateSettingsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    getSettingsUseCase: GetSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase
) : ViewModel() {

    val settings: StateFlow<AppSettings> = getSettingsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    fun setTheme(mode: AppThemeMode) {
        viewModelScope.launch { updateSettingsUseCase.updateTheme(mode) }
    }

    fun toggleVibrate(enabled: Boolean) {
        viewModelScope.launch { updateSettingsUseCase.updateVibrate(enabled) }
    }

    fun toggleBeep(enabled: Boolean) {
        viewModelScope.launch { updateSettingsUseCase.updateBeep(enabled) }
    }

    fun toggleAutoOpen(enabled: Boolean) {
        viewModelScope.launch { updateSettingsUseCase.updateAutoOpen(enabled) }
    }

    fun toggleAutoCopy(enabled: Boolean) {
        viewModelScope.launch { updateSettingsUseCase.updateAutoCopy(enabled) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Preferences", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.Spacing20)
        ) {
            Spacer(modifier = Modifier.height(Dimens.Spacing8))

            // Scanner Behavior
            SectionHeader(title = "Scanner Controls")
            Spacer(modifier = Modifier.height(Dimens.Spacing8))
            ScanFlowCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = Dimens.Spacing16, vertical = Dimens.Spacing8)) {
                    SettingToggleRow(
                        title = "Vibrate on Scan",
                        subtitle = "Haptic feedback when code is detected",
                        icon = Icons.Default.Vibration,
                        checked = settings.vibrateOnScan,
                        onCheckedChange = { viewModel.toggleVibrate(it) }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline)
                    SettingToggleRow(
                        title = "Beep Sound",
                        subtitle = "Audio cue when code is detected",
                        icon = Icons.Default.VolumeUp,
                        checked = settings.beepOnScan,
                        onCheckedChange = { viewModel.toggleBeep(it) }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline)
                    SettingToggleRow(
                        title = "Auto-Copy Scanned Text",
                        subtitle = "Automatically copy payload to clipboard",
                        icon = Icons.Default.Notifications,
                        checked = settings.autoCopyToClipboard,
                        onCheckedChange = { viewModel.toggleAutoCopy(it) }
                    )
                    Divider(color = MaterialTheme.colorScheme.outline)
                    SettingToggleRow(
                        title = "Auto-Open Safe URLs",
                        subtitle = "Automatically open safe HTTPS links",
                        icon = Icons.Default.OpenInBrowser,
                        checked = settings.autoOpenUrl,
                        onCheckedChange = { viewModel.toggleAutoOpen(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.Spacing24))

            // Appearance & Theme
            SectionHeader(title = "Appearance")
            Spacer(modifier = Modifier.height(Dimens.Spacing8))
            ScanFlowCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = Dimens.Spacing16, vertical = Dimens.Spacing8)) {
                    SettingNavRow(
                        title = "Theme Mode",
                        subtitle = when (settings.themeMode) {
                            AppThemeMode.DARK -> "Dark Theme (Default)"
                            AppThemeMode.LIGHT -> "Light Theme"
                            AppThemeMode.SYSTEM -> "Follow System"
                        },
                        icon = Icons.Default.DarkMode,
                        onClick = {
                            val nextMode = when (settings.themeMode) {
                                AppThemeMode.DARK -> AppThemeMode.LIGHT
                                AppThemeMode.LIGHT -> AppThemeMode.SYSTEM
                                AppThemeMode.SYSTEM -> AppThemeMode.DARK
                            }
                            viewModel.setTheme(nextMode)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.Spacing24))

            // Security & Analytics Hub
            SectionHeader(title = "Security & Insights")
            Spacer(modifier = Modifier.height(Dimens.Spacing8))
            ScanFlowCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = Dimens.Spacing16, vertical = Dimens.Spacing8)) {
                    SettingNavRow(
                        title = "Security Center & App Lock",
                        subtitle = if (settings.isAppLockEnabled) "App Lock Protected" else "Configure Biometrics & PIN",
                        icon = Icons.Default.Security,
                        onClick = onNavigateToSecurity
                    )
                    Divider(color = MaterialTheme.colorScheme.outline)
                    SettingNavRow(
                        title = "Scan & Creation Analytics",
                        subtitle = "View local usage statistics & trends",
                        icon = Icons.Default.Analytics,
                        onClick = onNavigateToAnalytics
                    )
                    Divider(color = MaterialTheme.colorScheme.outline)
                    SettingNavRow(
                        title = "About ScanFlow QR",
                        subtitle = "Version 1.0.0 · Production Ready",
                        icon = Icons.Default.Info,
                        onClick = onNavigateToAbout
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.Spacing32))
        }
    }
}

@Composable
fun SettingToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.Spacing12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(Dimens.Spacing16))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
fun SettingNavRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Dimens.Spacing12),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(Dimens.Spacing16))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
