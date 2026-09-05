package com.scanflow.qr.feature.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.qr.core.designsystem.Dimens
import com.scanflow.qr.core.designsystem.ElectricBlue
import com.scanflow.qr.core.designsystem.ScanFlowCard
import com.scanflow.qr.core.designsystem.ScanFlowPrimaryButton
import com.scanflow.qr.core.designsystem.ScanFlowTextField
import com.scanflow.qr.core.designsystem.SectionHeader
import com.scanflow.qr.core.designsystem.SuccessGreen
import com.scanflow.qr.domain.model.AppSettings
import com.scanflow.qr.domain.usecase.GetSettingsUseCase
import com.scanflow.qr.domain.usecase.UpdateSettingsUseCase
import com.scanflow.qr.feature.settings.SettingToggleRow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SecurityViewModel @Inject constructor(
    getSettingsUseCase: GetSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase
) : ViewModel() {

    val settings: StateFlow<AppSettings> = getSettingsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettings()
        )

    fun toggleAppLock(enabled: Boolean) {
        viewModelScope.launch { updateSettingsUseCase.updateAppLock(enabled) }
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch { updateSettingsUseCase.updateBiometric(enabled) }
    }

    fun setPin(pin: String?) {
        viewModelScope.launch { updateSettingsUseCase.updatePin(pin) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    viewModel: SecurityViewModel,
    onNavigateBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    var showPinDialog by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Set 4-Digit Security PIN") },
            text = {
                Column {
                    Text("Enter a 4-digit PIN to secure ScanFlow QR:")
                    Spacer(modifier = Modifier.height(12.dp))
                    ScanFlowTextField(
                        value = enteredPin,
                        onValueChange = { if (it.length <= 4) enteredPin = it },
                        label = "PIN Code",
                        placeholder = "e.g. 1234",
                        leadingIcon = Icons.Default.Pin
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (enteredPin.length == 4) {
                            viewModel.setPin(enteredPin)
                            viewModel.toggleAppLock(true)
                            showPinDialog = false
                            enteredPin = ""
                        }
                    }
                ) {
                    Text("Save PIN", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security Center", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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

            // Security Status Card
            ScanFlowCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(Dimens.Spacing20),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(Dimens.Spacing16))
                    Column {
                        Text(
                            text = "Smart Protection Active",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Safe URL heuristics, Phishing detector, and Local-only encryption.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.Spacing24))

            // App Lock & Biometrics
            SectionHeader(title = "App Lock & Authentication")
            Spacer(modifier = Modifier.height(Dimens.Spacing8))
            ScanFlowCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = Dimens.Spacing16, vertical = Dimens.Spacing8)) {
                    SettingToggleRow(
                        title = "Enable App Lock",
                        subtitle = "Require authentication upon app launch",
                        icon = Icons.Default.Lock,
                        checked = settings.isAppLockEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && settings.pinCode == null) {
                                showPinDialog = true
                            } else {
                                viewModel.toggleAppLock(enabled)
                            }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    SettingToggleRow(
                        title = "Biometric Unlock",
                        subtitle = "Unlock with Fingerprint or Face",
                        icon = Icons.Default.Fingerprint,
                        checked = settings.isBiometricEnabled,
                        onCheckedChange = { viewModel.toggleBiometric(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.Spacing16))
            ScanFlowPrimaryButton(
                text = if (settings.pinCode != null) "Change Security PIN" else "Set Security PIN",
                icon = Icons.Default.Pin,
                onClick = { showPinDialog = true },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Dimens.Spacing28))

            // Protection Explanations
            SectionHeader(title = "Security Safeguards")
            Spacer(modifier = Modifier.height(Dimens.Spacing8))
            ScanFlowCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Dimens.Spacing16)) {
                    SecurityRuleRow("No Auto-Execute", "Payloads and links are never opened without explicit consent.")
                    Spacer(modifier = Modifier.height(Dimens.Spacing8))
                    SecurityRuleRow("HTTPS Protocol Verification", "Warns immediately on unencrypted HTTP or suspicious TLDs.")
                    Spacer(modifier = Modifier.height(Dimens.Spacing8))
                    SecurityRuleRow("Zero-Knowledge Local Storage", "All database tables are kept strictly offline on device.")
                }
            }

            Spacer(modifier = Modifier.height(Dimens.Spacing32))
        }
    }
}

@Composable
fun SecurityRuleRow(title: String, description: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = ElectricBlue,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(Dimens.Spacing8))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
