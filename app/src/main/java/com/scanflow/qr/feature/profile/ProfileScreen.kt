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
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scanflow.qr.core.designsystem.CyanAccent
import com.scanflow.qr.core.designsystem.Dimens
import com.scanflow.qr.core.designsystem.ElectricBlue
import com.scanflow.qr.core.designsystem.ScanFlowCard
import com.scanflow.qr.core.designsystem.SectionHeader
import com.scanflow.qr.core.designsystem.SuccessGreen
import com.scanflow.qr.feature.settings.SettingNavRow

@Composable
fun ProfileScreen(
    onNavigateToMyQr: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
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
            Column {
                Text(
                    text = "Guest User",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "Offline Mode · Local Vault",
                        style = MaterialTheme.typography.labelSmall,
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Dimens.Spacing28))

        // Navigation Sections
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
                Divider(color = MaterialTheme.colorScheme.outline)
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
                Divider(color = MaterialTheme.colorScheme.outline)
                SettingNavRow(
                    title = "App Preferences",
                    subtitle = "Theme, Haptics, and Sound settings",
                    icon = Icons.Default.Settings,
                    onClick = onNavigateToSettings
                )
                Divider(color = MaterialTheme.colorScheme.outline)
                SettingNavRow(
                    title = "Cloud Synchronization",
                    subtitle = "Offline-First Local Architecture (Ready)",
                    icon = Icons.Default.CloudSync,
                    onClick = {}
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
