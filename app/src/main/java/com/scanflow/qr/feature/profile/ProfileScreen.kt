package com.scanflow.qr.feature.profile

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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scanflow.qr.core.designsystem.CyanAccent
import com.scanflow.qr.core.designsystem.Dimens
import com.scanflow.qr.core.designsystem.ElectricBlue
import com.scanflow.qr.core.designsystem.ScanFlowCard
import com.scanflow.qr.core.designsystem.SectionHeader
import com.scanflow.qr.core.designsystem.SuccessGreen
import com.scanflow.qr.core.di.AppViewModelProvider
import com.scanflow.qr.domain.model.SyncStatus
import com.scanflow.qr.feature.settings.SettingNavRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    onNavigateToMyQr: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToAuth: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = Dimens.Spacing20)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(Dimens.Spacing32))

        // User Avatar & Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(ElectricBlue, CyanAccent))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.width(Dimens.Spacing16))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = uiState.user.displayName ?: "ScanFlow User",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (!uiState.user.email.isNullOrEmpty()) {
                    Text(
                        text = uiState.user.email ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    val badgeText = when {
                        uiState.isSyncing -> "Menyinkronkan..."
                        uiState.syncStatus == SyncStatus.SYNCED -> "Cloud Vault Aktif"
                        uiState.user.isGuest -> "Mode Tamu · Vault Lokal"
                        else -> "Akun Terverifikasi"
                    }
                    val badgeColor = when {
                        uiState.isSyncing -> CyanAccent
                        uiState.syncStatus == SyncStatus.SYNCED -> SuccessGreen
                        uiState.user.isGuest -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> ElectricBlue
                    }
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Feedback notification banner
        if (uiState.feedbackMessage != null) {
            Spacer(modifier = Modifier.height(Dimens.Spacing16))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = uiState.feedbackMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.clearFeedback() }) {
                        Text("Tutup", style = MaterialTheme.typography.labelSmall, color = CyanAccent)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(Dimens.Spacing24))

        // Cloud Synchronization Section
        SectionHeader(title = "Sinkronisasi Cloud & Pemulihan")
        Spacer(modifier = Modifier.height(Dimens.Spacing8))
        ScanFlowCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = Dimens.Spacing16, vertical = Dimens.Spacing8)) {
                val lastSyncFormatted = uiState.lastSyncTime?.let {
                    SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(it))
                } ?: "Belum pernah"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Dimens.Spacing12),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = if (uiState.syncStatus == SyncStatus.SYNCED) SuccessGreen else CyanAccent,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(Dimens.Spacing16))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cloud Vault Snapshot",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Terakhir sinkron: $lastSyncFormatted",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (uiState.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = ElectricBlue,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { viewModel.syncNow() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sinkronkan Sekarang",
                                tint = ElectricBlue
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                SettingNavRow(
                    title = "Cadangkan ke Cloud Vault",
                    subtitle = "Ekspor data riwayat & QR ke Google Drive / File",
                    icon = Icons.Default.CloudUpload,
                    onClick = { viewModel.backupData() }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                SettingNavRow(
                    title = "Pulihkan dari Cloud Vault",
                    subtitle = "Impor data yang tersimpan dari Cloud Vault",
                    icon = Icons.Default.CloudDownload,
                    onClick = { viewModel.restoreData() }
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.Spacing24))

        // Account & Login Section
        SectionHeader(title = "Akun & Keamanan")
        Spacer(modifier = Modifier.height(Dimens.Spacing8))
        ScanFlowCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = Dimens.Spacing16, vertical = Dimens.Spacing8)) {
                if (uiState.user.isGuest) {
                    SettingNavRow(
                        title = "Masuk / Daftar Akun ScanFlow",
                        subtitle = "Aktifkan sinkronisasi otomatis multi-perangkat",
                        icon = Icons.Default.Person,
                        onClick = onNavigateToAuth
                    )
                } else {
                    SettingNavRow(
                        title = "Keluar dari Akun (${uiState.user.displayName})",
                        subtitle = "Kembali ke mode tamu offline",
                        icon = Icons.AutoMirrored.Filled.Logout,
                        onClick = { viewModel.signOut { onNavigateToAuth() } }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Dimens.Spacing24))

        // Content Section
        SectionHeader(title = "My Content")
        Spacer(modifier = Modifier.height(Dimens.Spacing8))
        ScanFlowCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = Dimens.Spacing16, vertical = Dimens.Spacing8)) {
                SettingNavRow(
                    title = "My Created QRs",
                    subtitle = "Manage your generated QR codes",
                    icon = Icons.Default.QrCode,
                    onClick = onNavigateToMyQr
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                SettingNavRow(
                    title = "Favorite Records",
                    subtitle = "Pinned scans and custom codes",
                    icon = Icons.Default.Favorite,
                    onClick = onNavigateToFavorites
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.Spacing24))

        SectionHeader(title = "System & Security")
        Spacer(modifier = Modifier.height(Dimens.Spacing8))
        ScanFlowCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = Dimens.Spacing16, vertical = Dimens.Spacing8)) {
                SettingNavRow(
                    title = "Security & App Lock",
                    subtitle = "Configure Biometrics and 4-Digit PIN",
                    icon = Icons.Default.Security,
                    onClick = onNavigateToSecurity
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                SettingNavRow(
                    title = "App Preferences",
                    subtitle = "Theme, Haptics, and Sound settings",
                    icon = Icons.Default.Settings,
                    onClick = onNavigateToSettings
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.Spacing24))

        SectionHeader(title = "Information")
        Spacer(modifier = Modifier.height(Dimens.Spacing8))
        ScanFlowCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = Dimens.Spacing16, vertical = Dimens.Spacing8)) {
                SettingNavRow(
                    title = "About ScanFlow QR",
                    subtitle = "Architecture, Version & Security Specs",
                    icon = Icons.Default.Info,
                    onClick = onNavigateToAbout
                )
            }
        }

        Spacer(modifier = Modifier.height(Dimens.Spacing48))
    }
}
