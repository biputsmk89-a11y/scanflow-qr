package com.scanflow.qr.feature.result

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scanflow.qr.core.common.toFormattedDateString
import com.scanflow.qr.core.designsystem.Dimens
import com.scanflow.qr.core.designsystem.ElectricBlue
import com.scanflow.qr.core.designsystem.ErrorRed
import com.scanflow.qr.core.designsystem.LoadingStateView
import com.scanflow.qr.core.designsystem.ScanFlowCard
import com.scanflow.qr.core.designsystem.ScanFlowPrimaryButton
import com.scanflow.qr.core.designsystem.ScanFlowSecondaryButton
import com.scanflow.qr.core.designsystem.SecurityBanner
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
                title = { Text("Scan Result", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val scanItem = uiState.scanItem
                    if (scanItem != null) {
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                imageVector = if (scanItem.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (scanItem.isFavorite) ErrorRed else MaterialTheme.colorScheme.onSurface
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Dimens.Spacing20)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(Dimens.Spacing8))

            // Security Assessment Banner
            val security = uiState.securityAssessment
            if (security != null) {
                SecurityBanner(
                    isSecure = security.isSecure,
                    message = security.summary
                )
                Spacer(modifier = Modifier.height(Dimens.Spacing16))
            }

            // Main Result Card
            ScanFlowCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(Dimens.Spacing20)) {
                    // Header Type & Format Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ElectricBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getTypeIcon(parsed.type),
                                    contentDescription = null,
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(Dimens.Spacing8))
                            Text(
                                text = parsed.type.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = scanItem.format,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Dimens.Spacing16))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(Dimens.Spacing16))

                    // Title
                    Text(
                        text = parsed.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(Dimens.Spacing12))

                    // Raw Content Box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(Dimens.Spacing12)) {
                            Text(
                                text = parsed.rawContent,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Parsed Details Key-Values
                    if (parsed.displayDetails.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(Dimens.Spacing16))
                        parsed.displayDetails.forEach { (key, value) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Dimens.Spacing12))
                    Text(
                        text = "Scanned on ${scanItem.createdAt.toFormattedDateString()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.Spacing24))

            // Primary Action Button (Dynamic based on QR Type)
            val actionLabel = getPrimaryActionLabel(parsed.type)
            val actionIcon = getPrimaryActionIcon(parsed.type)

            ScanFlowPrimaryButton(
                text = actionLabel,
                icon = actionIcon,
                onClick = { viewModel.performPrimaryAction(context) },
                modifier = Modifier.fillMaxWidth(),
                gradient = true
            )

            Spacer(modifier = Modifier.height(Dimens.Spacing12))

            // Secondary Actions (Copy & Share)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing12)
            ) {
                ScanFlowSecondaryButton(
                    text = "Copy",
                    icon = Icons.Default.ContentCopy,
                    onClick = {
                        viewModel.copyContent(context)
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f)
                )

                ScanFlowSecondaryButton(
                    text = "Share",
                    icon = Icons.Default.Share,
                    onClick = { viewModel.shareContent(context) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(Dimens.Spacing32))
        }
    }
}

fun getTypeIcon(type: QrType): ImageVector {
    return when (type) {
        QrType.WEBSITE -> Icons.Default.Language
        QrType.WIFI -> Icons.Default.Wifi
        QrType.CONTACT -> Icons.Default.PersonAdd
        QrType.EMAIL -> Icons.Default.Email
        QrType.PHONE -> Icons.Default.Call
        QrType.SMS -> Icons.Default.Sms
        QrType.LOCATION -> Icons.Default.LocationOn
        QrType.CALENDAR -> Icons.Default.Event
        QrType.PAYMENT -> Icons.Default.Payment
        QrType.SOCIAL -> Icons.Default.Share
        else -> Icons.Default.QrCode
    }
}

fun getPrimaryActionLabel(type: QrType): String {
    return when (type) {
        QrType.WEBSITE, QrType.SOCIAL -> "Open Website"
        QrType.PHONE -> "Call Number"
        QrType.EMAIL -> "Send Email"
        QrType.SMS -> "Send SMS"
        QrType.LOCATION -> "Open Location in Maps"
        QrType.CONTACT -> "Save Contact"
        QrType.CALENDAR -> "Add to Calendar"
        QrType.PAYMENT -> "Open Payment / Pay Now"
        QrType.WIFI -> "Connect / Configure Wi-Fi"
        else -> "Copy Raw Text"
    }
}

fun getPrimaryActionIcon(type: QrType): ImageVector {
    return when (type) {
        QrType.WEBSITE, QrType.SOCIAL -> Icons.Default.Language
        QrType.PHONE -> Icons.Default.Call
        QrType.EMAIL -> Icons.Default.Email
        QrType.SMS -> Icons.Default.Sms
        QrType.LOCATION -> Icons.Default.LocationOn
        QrType.CONTACT -> Icons.Default.PersonAdd
        QrType.CALENDAR -> Icons.Default.Event
        QrType.PAYMENT -> Icons.Default.Payment
        QrType.WIFI -> Icons.Default.Wifi
        else -> Icons.Default.ContentCopy
    }
}
