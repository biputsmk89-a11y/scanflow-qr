package com.scanflow.qr.feature.auth

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.scanflow.qr.core.designsystem.CyanAccent
import com.scanflow.qr.core.designsystem.Dimens
import com.scanflow.qr.core.designsystem.ElectricBlue
import com.scanflow.qr.core.security.BiometricAuthManager
import com.scanflow.qr.domain.model.AppSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLockScreen(
    settings: AppSettings = AppSettings(),
    onUnlockSuccess: () -> Unit,
    onCancel: (() -> Unit)? = null,
    onNavigateToPin: (() -> Unit)? = null,
    isTestingMode: Boolean = false
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()

    var isPinEntryMode by remember { mutableStateOf(false) }

    if (isPinEntryMode) {
        PinEntryScreen(
            settings = settings,
            onPinSuccess = onUnlockSuccess,
            onNavigateBack = { isPinEntryMode = false }
        )
        return
    }

    var showPinSheet by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var authFeedback by remember { mutableStateOf<String?>(null) }

    // Pulsing Ripple Animation for Biometric Fingerprint
    val infiniteTransition = rememberInfiniteTransition(label = "fingerprint_ripple")
    val rippleScale1 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_scale_1"
    )
    val rippleAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_alpha_1"
    )

    val rippleScale2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_scale_2"
    )
    val rippleAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_alpha_2"
    )

    val isBiometricReady = remember(context, settings.isBiometricEnabled) {
        settings.isBiometricEnabled && activity != null && BiometricAuthManager.isBiometricAvailable(activity)
    }

    fun triggerBiometrics() {
        if (isBiometricReady && activity != null) {
            BiometricAuthManager.authenticate(
                activity = activity,
                title = "Unlock ScanFlow QR",
                subtitle = "Authenticate to access encrypted records",
                onSuccess = {
                    scope.launch {
                        authFeedback = "Autentikasi berhasil"
                        delay(250)
                        onUnlockSuccess()
                    }
                },
                onError = { err ->
                    authFeedback = err
                    // Fallback to PIN if user cancels or biometric fails
                    showPinSheet = true
                }
            )
        } else {
            // Biometric not configured or available on hardware, open PIN sheet
            showPinSheet = true
        }
    }

    // Auto prompt on first composition
    LaunchedEffect(Unit) {
        if (!isTestingMode) {
            if (isBiometricReady) {
                triggerBiometrics()
            } else if (settings.isAppLockEnabled && settings.pinCode != null) {
                showPinSheet = true
            }
        }
    }

    // PIN Keypad Bottom Sheet
    if (showPinSheet) {
        PinKeypadBottomSheet(
            enteredPin = enteredPin,
            pinError = pinError,
            onDigitClick = { digit ->
                if (enteredPin.length < 4) {
                    val newPin = enteredPin + digit
                    enteredPin = newPin
                    pinError = null
                    if (newPin.length == 4) {
                        val targetPin = if (!settings.pinCode.isNullOrBlank()) settings.pinCode else "1234"
                        if (newPin == targetPin) {
                            scope.launch {
                                delay(200)
                                showPinSheet = false
                                onUnlockSuccess()
                            }
                        } else {
                            scope.launch {
                                delay(150)
                                pinError = "PIN salah. Coba lagi (default: 1234)"
                                enteredPin = ""
                            }
                        }
                    }
                }
            },
            onDeleteClick = {
                if (enteredPin.isNotEmpty()) {
                    enteredPin = enteredPin.dropLast(1)
                    pinError = null
                }
            },
            onDismiss = {
                showPinSheet = false
                enteredPin = ""
                pinError = null
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Subtle decorative bottom ambient gradient (Stitch Design)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    )
                )
        )

        // Close button if previewing or cancelable
        if (onCancel != null) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Tutup",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Main Center Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimens.Spacing24)
                .padding(top = 40.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Section: Logo, Enterprise Security Badge & Brand Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Logo Card Container (Stitch Design)
                Surface(
                    modifier = Modifier.size(92.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    shadowElevation = 3.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Enterprise Security Badge Pill (Stitch Design)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ElectricBlue.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "ENTERPRISE SECURITY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ElectricBlue,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Headline: ScanFlow QR (Stitch Gradient Text)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "ScanFlow ",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "QR",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ElectricBlue,
                        letterSpacing = (-0.5).sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Authenticate to access your encrypted QR vaults & records",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                if (authFeedback != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = authFeedback ?: "",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 2. Center Section: Large Pulsing Fingerprint Visual (Google Stitch Design)
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clickable { triggerBiometrics() },
                contentAlignment = Alignment.Center
            ) {
                // Outer Ripple Ring 1
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(rippleScale1)
                        .clip(CircleShape)
                        .background(ElectricBlue.copy(alpha = rippleAlpha1))
                )

                // Outer Ripple Ring 2
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(rippleScale2)
                        .clip(CircleShape)
                        .background(ElectricBlue.copy(alpha = rippleAlpha2))
                )

                // Central Fingerprint Button Container
                Surface(
                    modifier = Modifier.size(86.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    shadowElevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Scan Fingerprint",
                            tint = ElectricBlue,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }

            // 3. Bottom Action Buttons: Use Biometrics (Solid) & Use PIN instead (Outlined)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Primary Button: Use Biometrics
                Button(
                    onClick = { triggerBiometrics() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricBlue,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Use Biometrics",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Secondary Button: Use PIN instead
                OutlinedButton(
                    onClick = { onNavigateToPin?.invoke() ?: run { isPinEntryMode = true } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(2.dp, ElectricBlue),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ElectricBlue
                    )
                ) {
                    Text(
                        text = "Use PIN instead",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ElectricBlue
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// PIN Keypad Bottom Sheet (Interactive 4-Digit Security Unlock)
// -----------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PinKeypadBottomSheet(
    enteredPin: String,
    pinError: String?,
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(top = 24.dp, bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar with Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ElectricBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Enter Security PIN",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Enter your 4-digit security PIN to access records",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 4-Dot Indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    val isFilled = index < enteredPin.length
                    val dotColor = when {
                        pinError != null -> Color(0xFFBA1A1A)
                        isFilled -> ElectricBlue
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (isFilled) dotColor else Color.Transparent)
                            .then(
                                if (!isFilled) Modifier.background(Color.Transparent)
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!isFilled) {
                            Surface(
                                modifier = Modifier.size(16.dp),
                                shape = CircleShape,
                                color = Color.Transparent,
                                border = BorderStroke(2.dp, dotColor)
                            ) {}
                        }
                    }
                }
            }

            // Error Message
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (pinError != null) {
                    Text(
                        text = pinError,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFBA1A1A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Number Keypad (3x4 Grid)
            val keypadRows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("C", "0", "DEL")
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                keypadRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { key ->
                            KeypadButton(
                                key = key,
                                onClick = {
                                    when (key) {
                                        "DEL" -> onDeleteClick()
                                        "C" -> onDismiss()
                                        else -> onDigitClick(key)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    key: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (key) {
                "DEL" -> {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                "C" -> {
                    Text(
                        text = "Batal",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                else -> {
                    Text(
                        text = key,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
