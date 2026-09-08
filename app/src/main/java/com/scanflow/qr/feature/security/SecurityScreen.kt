package com.scanflow.qr.feature.security

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import com.scanflow.qr.domain.model.LockTimeout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanflow.qr.core.designsystem.Dimens
import com.scanflow.qr.core.designsystem.ElectricBlue
import com.scanflow.qr.core.designsystem.SuccessGreen
import com.scanflow.qr.core.security.BiometricAuthManager
import com.scanflow.qr.domain.model.AppSettings
import com.scanflow.qr.domain.usecase.GetSettingsUseCase
import com.scanflow.qr.domain.usecase.UpdateSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
        viewModelScope.launch {
            updateSettingsUseCase.updateBiometric(enabled)
            if (enabled) {
                updateSettingsUseCase.updateAppLock(true)
            }
        }
    }

    fun setPin(pin: String?) {
        viewModelScope.launch { updateSettingsUseCase.updatePin(pin) }
    }

    fun setLockTimeout(seconds: Long) {
        viewModelScope.launch { updateSettingsUseCase.updateLockTimeout(seconds) }
    }

    fun toggleSaveScanHistory(enabled: Boolean) {
        viewModelScope.launch { updateSettingsUseCase.updateSaveScanHistory(enabled) }
    }

    fun toggleSendAnonymousAnalytics(enabled: Boolean) {
        viewModelScope.launch { updateSettingsUseCase.updateSendAnonymousAnalytics(enabled) }
    }

    fun toggleSafeUrlDetection(enabled: Boolean) {
        viewModelScope.launch { updateSettingsUseCase.updateSafeUrlDetection(enabled) }
    }

    fun toggleSuspiciousQrWarning(enabled: Boolean) {
        viewModelScope.launch { updateSettingsUseCase.updateSuspiciousQrWarning(enabled) }
    }

    fun toggleClipboardProtection(enabled: Boolean) {
        viewModelScope.launch { updateSettingsUseCase.updateClipboardProtection(enabled) }
    }
}

private data class SecurityDetailInfo(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val details: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    viewModel: SecurityViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAppLock: () -> Unit = {},
    onNavigateToPinEntry: () -> Unit = {}
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val isBiometricSupported = remember(context) {
        BiometricAuthManager.isBiometricAvailable(context)
    }

    // Self-healing: if biometric was saved as enabled on hardware lacking biometric support, disable it
    LaunchedEffect(isBiometricSupported, settings.isBiometricEnabled) {
        if (!isBiometricSupported && settings.isBiometricEnabled) {
            viewModel.toggleBiometric(false)
        }
    }

    var showPinDialog by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var activeDetailDialog by remember { mutableStateOf<SecurityDetailInfo?>(null) }

    // PIN Setup Dialog for Biometric & App Lock Fallback
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showPinDialog = false
                enteredPin = ""
                pinError = null
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Pin,
                    contentDescription = null,
                    tint = ElectricBlue,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = if (settings.pinCode != null) "Ubah PIN Keamanan" else "Pasang 4-Digit PIN Keamanan",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isBiometricSupported) {
                            "Masukkan 4 digit PIN sebagai kode autentikasi cadangan untuk Kunci Biometrik:"
                        } else {
                            "Masukkan 4 digit PIN untuk mengamankan akses aplikasi Anda (sensor biometrik tidak tersedia di perangkat ini):"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                enteredPin = it
                                pinError = null
                            }
                        },
                        label = { Text("4-Digit PIN") },
                        placeholder = { Text("1234") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = pinError != null,
                        supportingText = pinError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (enteredPin.length == 4) {
                            viewModel.setPin(enteredPin)
                            if (isBiometricSupported) {
                                viewModel.toggleBiometric(true)
                            } else {
                                viewModel.toggleBiometric(false)
                                viewModel.toggleAppLock(true)
                            }
                            showPinDialog = false
                            enteredPin = ""
                            pinError = null
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (isBiometricSupported) {
                                        "PIN Keamanan tersimpan & Kunci Biometrik aktif"
                                    } else {
                                        "PIN Keamanan tersimpan & Kunci Aplikasi aktif (Biometrik tidak didukung di perangkat ini)"
                                    }
                                )
                            }
                        } else {
                            pinError = "PIN harus terdiri dari tepat 4 angka"
                        }
                    }
                ) {
                    Text("Simpan & Aktifkan", color = ElectricBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPinDialog = false
                        enteredPin = ""
                        pinError = null
                    }
                ) {
                    Text("Batal")
                }
            }
        )
    }

    // Security Detail Dialog (when user clicks an ACTIVE bento card)
    activeDetailDialog?.let { info ->
        AlertDialog(
            onDismissRequest = { activeDetailDialog = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ElectricBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = info.icon,
                        contentDescription = null,
                        tint = ElectricBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            title = {
                Text(
                    text = info.title,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = info.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    info.details.forEach { detail ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { activeDetailDialog = null }) {
                    Text("Close", color = ElectricBlue, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Security Center",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = ElectricBlue
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.Spacing16),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // 1. Main Status Card (Google Stitch Design)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE6F4EA)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Device Protected",
                            tint = Color(0xFF137333),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Your device is protected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Real-time scanning protection and link verification are active.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. Bento Feature Cards
            // Row 1: Safe URL Detection & Suspicious QR Warning
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: Safe URL Detection
                SecurityBentoSquareCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Link,
                    title = "Safe URL Detection",
                    description = "Automatically verifies links before opening.",
                    tag = "ACTIVE",
                    onClick = {
                        activeDetailDialog = SecurityDetailInfo(
                            title = "Safe URL Detection Engine",
                            description = "Real-time heuristic link validation protects you from phishing, deceptive shorteners, and dangerous URI schemes before any browser intent is launched.",
                            icon = Icons.Default.Link,
                            details = listOf(
                                "Detects suspicious top-level domains (.zip, .top, .xyz, etc.)",
                                "Verifies HTTPS SSL/TLS encryption status",
                                "Flags unmasked direct IP addresses and deceptive URL shorteners",
                                "Blocks raw executable schemes (javascript:, data:, intent:)"
                            )
                        )
                    }
                )

                // Card 2: Suspicious QR Warning
                SecurityBentoSquareCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.QrCodeScanner,
                    title = "Suspicious QR Warning",
                    description = "Alerts you to potentially harmful codes.",
                    tag = "ACTIVE",
                    onClick = {
                        activeDetailDialog = SecurityDetailInfo(
                            title = "Suspicious QR Warning Engine",
                            description = "Scans QR matrix payloads for disguised malicious command injections, hidden wifi auto-connect configurations, and fraudulent payment redirections.",
                            icon = Icons.Default.QrCodeScanner,
                            details = listOf(
                                "Heuristic payload inspection during live camera framing",
                                "Immediate amber/red warning banners on suspicious content",
                                "No automatic execution without your explicit confirmation",
                                "Full payload inspectability before triggering actions"
                            )
                        )
                    }
                )
            }

            // Card 3: Biometric Lock (Full Width Card with Toggle)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isBiometricSupported) Icons.Default.Fingerprint else Icons.Default.Lock,
                                    contentDescription = if (isBiometricSupported) "Biometric Lock" else "App Lock",
                                    tint = if (settings.isBiometricEnabled || settings.isAppLockEnabled) ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = if (isBiometricSupported) "Biometric Lock" else "Kunci Aplikasi (PIN)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isBiometricSupported) {
                                        "Amankan aplikasi dengan Sidik Jari atau Face ID."
                                    } else {
                                        "Amankan aplikasi dengan PIN (Sensor biometrik tidak tersedia)."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = if (isBiometricSupported) settings.isBiometricEnabled else settings.isAppLockEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    if (settings.pinCode == null) {
                                        showPinDialog = true
                                    } else {
                                        if (isBiometricSupported) {
                                            viewModel.toggleBiometric(true)
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Kunci Biometrik diaktifkan")
                                            }
                                        } else {
                                            viewModel.toggleBiometric(false)
                                            viewModel.toggleAppLock(true)
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Kunci Aplikasi (PIN) diaktifkan")
                                            }
                                        }
                                    }
                                } else {
                                    if (isBiometricSupported) {
                                        viewModel.toggleBiometric(false)
                                        viewModel.toggleAppLock(false)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Kunci Biometrik dinonaktifkan")
                                        }
                                    } else {
                                        viewModel.toggleAppLock(false)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Kunci Aplikasi dinonaktifkan")
                                        }
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ElectricBlue
                            )
                        )
                    }

                    if (settings.isBiometricEnabled || settings.isAppLockEnabled) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = ElectricBlue
                                )
                                Text(
                                    text = "Batas Waktu Kunci (Timeout)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "Kunci ulang aplikasi otomatis setelah berada di latar belakang selama:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LockTimeout.entries.forEach { timeout ->
                                    val isSelected = settings.lockTimeoutSeconds == timeout.seconds
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .clickable {
                                                viewModel.setLockTimeout(timeout.seconds)
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Timeout diatur: ${timeout.labelId}")
                                                }
                                            },
                                        color = if (isSelected) ElectricBlue.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        border = BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) ElectricBlue else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                        ),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = ElectricBlue
                                                )
                                            }
                                            Text(
                                                text = timeout.labelId,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onNavigateToPinEntry) {
                                Icon(
                                    imageVector = Icons.Default.Pin,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = ElectricBlue
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Test PIN Entry",
                                    color = ElectricBlue,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            TextButton(onClick = onNavigateToAppLock) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = ElectricBlue
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Test App Lock",
                                    color = ElectricBlue,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }

            // Row 2: Private History & Clipboard Protection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 4: Private History
                SecurityBentoSquareCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.History,
                    title = "Private History",
                    description = "Encrypted local storage for your scans.",
                    tag = "ACTIVE",
                    onClick = {
                        activeDetailDialog = SecurityDetailInfo(
                            title = "Private Encrypted Storage",
                            description = "Your scanned records and created QR codes are stored locally on your device with high-grade cryptographic sandboxing.",
                            icon = Icons.Default.History,
                            details = listOf(
                                "Zero-knowledge architecture: no unauthorized cloud uploads",
                                "Local Room SQLite database protected by Android Keystore",
                                "All records can be backed up or cleared instantly on demand"
                            )
                        )
                    }
                )

                // Card 5: Clipboard Protection
                SecurityBentoSquareCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ContentCopy,
                    title = "Clipboard Protection",
                    description = "Clears sensitive data after 5 minutes.",
                    tag = "ACTIVE",
                    onClick = {
                        activeDetailDialog = SecurityDetailInfo(
                            title = "Clipboard Protection Guard",
                            description = "Prevents third-party apps from harvesting sensitive information copied from QR codes by enforcing security timeouts.",
                            icon = Icons.Default.ContentCopy,
                            details = listOf(
                                "Protects passwords, OTP secrets, and private keys",
                                "Auto-clears clipboard buffers after 5 minutes of inactivity",
                                "Warns if unauthorized clipboard reading is detected"
                            )
                        )
                    }
                )
            }

            // 3. Privacy Settings Section (Google Stitch Design)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Privacy Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    shadowElevation = 2.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Toggle 1: Save scan history
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Save scan history",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Switch(
                                checked = settings.saveScanHistory,
                                onCheckedChange = { enabled ->
                                    viewModel.toggleSaveScanHistory(enabled)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (enabled) "Scan history recording enabled" else "Private Mode: Scan history recording stopped"
                                        )
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ElectricBlue
                                )
                            )
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                            thickness = 1.dp
                        )

                        // Toggle 2: Send anonymous analytics
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Send anonymous analytics",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Switch(
                                checked = settings.sendAnonymousAnalytics,
                                onCheckedChange = { enabled ->
                                    viewModel.toggleSendAnonymousAnalytics(enabled)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (enabled) "Anonymous analytics sharing active" else "Anonymous analytics sharing turned off"
                                        )
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ElectricBlue
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SecurityBentoSquareCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    description: String,
    tag: String = "ACTIVE",
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(ElectricBlue.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = ElectricBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = ElectricBlue.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ElectricBlue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight
                )
            }
        }
    }
}
