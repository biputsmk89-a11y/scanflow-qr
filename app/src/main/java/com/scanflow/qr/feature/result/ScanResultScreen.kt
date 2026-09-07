package com.scanflow.qr.feature.result

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sms
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import com.scanflow.qr.core.utils.IntentHelper
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.scanflow.qr.core.utils.WifiConnectionStatus
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scanflow.qr.core.common.toFormattedDateString
import com.scanflow.qr.core.designsystem.CyanAccent
import com.scanflow.qr.core.designsystem.Dimens
import com.scanflow.qr.core.designsystem.ElectricBlue
import com.scanflow.qr.core.designsystem.ErrorRed
import com.scanflow.qr.core.designsystem.LoadingStateView
import com.scanflow.qr.core.designsystem.ScanFlowSecondaryButton
import com.scanflow.qr.domain.model.QrType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultScreen(
    scanId: Long,
    viewModel: ScanResultViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(scanId) {
        viewModel.loadScanResult(scanId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Scan Result",
                        fontWeight = FontWeight.Bold,
                        color = ElectricBlue
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    val scanItem = uiState.scanItem
                    if (scanItem != null) {
                        IconButton(onClick = { viewModel.printScanResult(context) }) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "Cetak ke Printer",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                imageVector = if (scanItem.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (scanItem.isFavorite) ErrorRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {
                            viewModel.deleteScan {
                                Toast.makeText(context, "Scan deleted", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = ErrorRed
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.isLoading) {
            LoadingStateView(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        val scanItem = uiState.scanItem
        val parsed = uiState.parsedData

        if (scanItem == null || parsed == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Result not found or deleted.", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                ScanFlowSecondaryButton(text = "Go Back", onClick = onNavigateBack)
            }
            return@Scaffold
        }

        val isSafe = uiState.securityAssessment?.isSecure ?: true
        val statusAccentColor = if (isSafe) Color(0xFF10B981) else ErrorRed

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimens.Spacing20)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(Dimens.Spacing12))

            // 1. Stitch Result Type Pill Chip
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CyanAccent.copy(alpha = 0.15f),
                modifier = Modifier.padding(bottom = Dimens.Spacing16)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (parsed.type) {
                            QrType.WEBSITE -> Icons.Default.Language
                            QrType.WIFI -> Icons.Default.Wifi
                            QrType.CONTACT -> Icons.Default.Person
                            QrType.EMAIL -> Icons.Default.Email
                            QrType.PHONE -> Icons.Default.Call
                            QrType.SMS -> Icons.Default.Sms
                            QrType.WHATSAPP -> Icons.Default.Sms
                            QrType.LOCATION -> Icons.Default.LocationOn
                            else -> Icons.Default.QrCode
                        },
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = parsed.type.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF006875)
                    )
                }
            }

            // 2. Stitch Main Result Card (with Green / Status left border)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp))
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Left status border accent
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(210.dp)
                            .background(statusAccentColor)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Selectable Inner URL / Data Box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SelectionContainer {
                                Text(
                                    text = scanItem.content,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Security Indicator Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isSafe) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = statusAccentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSafe) "Safe Link Verified" else "Caution / Suspicious Payload",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = statusAccentColor
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Metadata (Date & Time)
                        Text(
                            text = "Scanned on ${scanItem.createdAt.toFormattedDateString()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Main Action Button (Open Link / Connect WiFi / Action)
                        val isWifi = parsed.type == QrType.WIFI
                        val wifiStatus = uiState.wifiConnectionStatus
                        val isWifiConnecting = isWifi && wifiStatus == WifiConnectionStatus.CONNECTING

                        val buttonContainerColor = when {
                            isWifi && wifiStatus == WifiConnectionStatus.CONNECTED -> Color(0xFF00C853)
                            isWifi && wifiStatus == WifiConnectionStatus.FAILED -> MaterialTheme.colorScheme.error
                            isWifi && wifiStatus == WifiConnectionStatus.CONNECTING -> ElectricBlue.copy(alpha = 0.8f)
                            else -> ElectricBlue
                        }

                        Button(
                            onClick = {
                                viewModel.performPrimaryAction(context)
                            },
                            enabled = !isWifiConnecting,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = buttonContainerColor,
                                contentColor = Color.White,
                                disabledContainerColor = ElectricBlue.copy(alpha = 0.6f),
                                disabledContentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            if (isWifiConnecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Menghubungkan ke Wi-Fi...",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            } else {
                                val is1DBarcode = parsed.type == QrType.BARCODE || scanItem.format in listOf("EAN_13", "EAN_8", "UPC_A", "UPC_E", "CODE_128", "CODE_39", "CODE_93", "ITF", "CODABAR")
                                val actionIcon = when {
                                    isWifi && wifiStatus == WifiConnectionStatus.CONNECTED -> Icons.Default.CheckCircle
                                    isWifi && wifiStatus == WifiConnectionStatus.FAILED -> Icons.Default.Warning
                                    is1DBarcode -> Icons.Default.Search
                                    parsed.type == QrType.WEBSITE || parsed.type == QrType.SOCIAL -> Icons.AutoMirrored.Filled.OpenInNew
                                    parsed.type == QrType.WIFI -> Icons.Default.Wifi
                                    parsed.type == QrType.EMAIL -> Icons.Default.Email
                                    parsed.type == QrType.PHONE -> Icons.Default.Call
                                    parsed.type == QrType.SMS -> Icons.Default.Sms
                                    parsed.type == QrType.WHATSAPP -> Icons.Default.Sms
                                    parsed.type == QrType.LOCATION -> Icons.Default.LocationOn
                                    parsed.type == QrType.CONTACT -> Icons.Default.Person
                                    parsed.type == QrType.CALENDAR -> Icons.Default.CalendarToday
                                    parsed.type == QrType.PAYMENT -> Icons.Default.Payments
                                    else -> Icons.Default.ContentCopy
                                }
                                val actionText = when {
                                    isWifi && wifiStatus == WifiConnectionStatus.CONNECTED -> "Terhubung ke Wi-Fi"
                                    isWifi && wifiStatus == WifiConnectionStatus.FAILED -> "Coba Sambungkan Lagi"
                                    is1DBarcode -> "Cari Produk di Web"
                                    parsed.type == QrType.WEBSITE -> "Open Link"
                                    parsed.type == QrType.WIFI -> "Sambungkan ke Wi-Fi"
                                    parsed.type == QrType.EMAIL -> "Send Email"
                                    parsed.type == QrType.PHONE -> "Call Number"
                                    parsed.type == QrType.SMS -> "Send SMS"
                                    parsed.type == QrType.WHATSAPP -> "Buka Chat WhatsApp"
                                    parsed.type == QrType.LOCATION -> "View on Maps"
                                    parsed.type == QrType.CONTACT -> "Save Contact"
                                    parsed.type == QrType.CALENDAR -> "Add to Calendar"
                                    parsed.type == QrType.PAYMENT -> "Pay Now"
                                    parsed.type == QrType.SOCIAL -> "Open Social Profile"
                                    else -> "Copy Content"
                                }
                                Icon(
                                    imageVector = actionIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = actionText,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        val is1DBarcode = parsed.type == QrType.BARCODE || scanItem.format in listOf("EAN_13", "EAN_8", "UPC_A", "UPC_E", "CODE_128", "CODE_39", "CODE_93", "ITF", "CODABAR")
                        if (is1DBarcode) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.searchProductGoogle(context) },
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(15.dp), tint = ElectricBlue)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Google", style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.searchProductBarcodeLookup(context) },
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(15.dp), tint = ElectricBlue)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Lookup", style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.searchProductOpenFoodFacts(context) },
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(15.dp), tint = ElectricBlue)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Food Facts", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        // Wi-Fi Connection Status Feedback & Manual Settings Shortcut
                        if (isWifi) {
                            if (uiState.wifiConnectionMessage != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                val bannerBg = when (wifiStatus) {
                                    WifiConnectionStatus.CONNECTED -> Color(0xFF00C853).copy(alpha = 0.12f)
                                    WifiConnectionStatus.FAILED -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                                    else -> ElectricBlue.copy(alpha = 0.12f)
                                }
                                val bannerBorder = when (wifiStatus) {
                                    WifiConnectionStatus.CONNECTED -> Color(0xFF00C853)
                                    WifiConnectionStatus.FAILED -> MaterialTheme.colorScheme.error
                                    else -> ElectricBlue
                                }
                                val bannerIcon = when (wifiStatus) {
                                    WifiConnectionStatus.CONNECTED -> Icons.Default.CheckCircle
                                    WifiConnectionStatus.FAILED -> Icons.Default.Warning
                                    else -> Icons.Default.Wifi
                                }

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = bannerBg,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, bannerBorder.copy(alpha = 0.35f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = bannerIcon,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = bannerBorder
                                        )
                                        Text(
                                            text = uiState.wifiConnectionMessage ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = { viewModel.openWifiSettings(context) },
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Buka Setelan Wi-Fi Sistem",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(Dimens.Spacing24))

            // 3. Stitch Action Grid (Copy, Share, Print, Save)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StitchResultActionButton(
                    title = "Copy",
                    icon = Icons.Default.ContentCopy,
                    onClick = {
                        viewModel.copyContent(context)
                    },
                    modifier = Modifier.weight(1f)
                )

                StitchResultActionButton(
                    title = "Share",
                    icon = Icons.Default.Share,
                    onClick = {
                        viewModel.shareContent(context)
                    },
                    modifier = Modifier.weight(1f)
                )

                StitchResultActionButton(
                    title = "Print",
                    icon = Icons.Default.Print,
                    iconTint = ElectricBlue,
                    onClick = {
                        viewModel.printScanResult(context)
                    },
                    modifier = Modifier.weight(1f)
                )

                StitchResultActionButton(
                    title = if (scanItem.isFavorite) "Saved" else "Save",
                    icon = if (scanItem.isFavorite) Icons.Default.Favorite else Icons.Default.BookmarkAdd,
                    iconTint = if (scanItem.isFavorite) ErrorRed else ElectricBlue,
                    onClick = {
                        viewModel.toggleFavorite()
                        Toast.makeText(context, if (scanItem.isFavorite) "Removed from favorites" else "Saved to favorites", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(Dimens.Spacing24))

            // 4. Structured Data Details & Partial Extraction Card
            if (parsed.displayDetails.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Data Terurai (Ekstraksi Cepat)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = ElectricBlue.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "${parsed.displayDetails.size} Bidang",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ElectricBlue,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        parsed.displayDetails.entries.forEachIndexed { index, (key, value) ->
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                    SelectionContainer {
                                        Text(
                                            text = value,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (key.contains("Phone", ignoreCase = true)) {
                                        IconButton(
                                            onClick = { IntentHelper.callPhone(context, value) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Call,
                                                contentDescription = "Panggil $value",
                                                tint = ElectricBlue,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    if (key.contains("Email", ignoreCase = true) || key.contains("Recipient", ignoreCase = true)) {
                                        IconButton(
                                            onClick = { IntentHelper.sendEmail(context, value) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Email,
                                                contentDescription = "Kirim Email ke $value",
                                                tint = ElectricBlue,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { viewModel.copyPartialContent(context, key, value) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "Salin $key",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.Spacing16))
            }

            // 5. Raw Details Accordion
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Scan Details",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Format: ${scanItem.format}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Category: ${parsed.type.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

/**
 * Stitch Quick Action Button in Result Screen
 */
@Composable
private fun StitchResultActionButton(
    title: String,
    icon: ImageVector,
    iconTint: Color = ElectricBlue,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

