package com.scanflow.qr.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.net.Uri
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scanflow.qr.core.common.toFormattedDateString
import com.scanflow.qr.core.designsystem.CyanAccent
import com.scanflow.qr.core.designsystem.Dimens
import com.scanflow.qr.core.designsystem.ElectricBlue
import com.scanflow.qr.core.designsystem.EmptyStateView
import com.scanflow.qr.core.designsystem.ErrorRed
import com.scanflow.qr.core.designsystem.SoftGold
import com.scanflow.qr.core.designsystem.SoftGoldLight
import com.scanflow.qr.core.designsystem.SoftGoldMedium
import com.scanflow.qr.core.designsystem.SoftGoldDark
import com.scanflow.qr.core.designsystem.SoftGoldContainer
import com.scanflow.qr.R
import androidx.compose.ui.res.stringResource
import com.scanflow.qr.domain.model.QrType
import com.scanflow.qr.domain.model.ScanHistoryItem
import java.util.Calendar

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToScan: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToMyQr: () -> Unit,
    onNavigateToResult: (Long) -> Unit,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToCreateBarcode: () -> Unit = onNavigateToCreate
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showBarcodeDialog by remember { mutableStateOf(false) }

    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greetingText = if (!uiState.displayName.isNullOrBlank()) {
        val firstName = uiState.displayName?.trim()?.split(" ")?.firstOrNull() ?: uiState.displayName
        when (currentHour) {
            in 5..11 -> stringResource(R.string.greeting_morning_user, firstName ?: "")
            in 12..14 -> stringResource(R.string.greeting_afternoon_user, firstName ?: "")
            in 15..17 -> stringResource(R.string.greeting_evening_user, firstName ?: "")
            else -> stringResource(R.string.greeting_night_user, firstName ?: "")
        }
    } else {
        when (currentHour) {
            in 5..11 -> stringResource(R.string.greeting_morning)
            in 12..14 -> stringResource(R.string.greeting_afternoon)
            in 15..17 -> stringResource(R.string.greeting_evening)
            else -> stringResource(R.string.greeting_night)
        }
    }

    val filteredScans = remember(searchQuery, uiState.recentScans) {
        if (searchQuery.isBlank()) {
            uiState.recentScans
        } else {
            uiState.recentScans.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.content.contains(searchQuery, ignoreCase = true) ||
                        it.type.displayName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = Dimens.Spacing20),
        contentPadding = PaddingValues(top = Dimens.Spacing20, bottom = 100.dp)
    ) {
        // 1. Google Stitch Header (Greeting + User Profile)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = greetingText,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.home_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Avatar Icon with precision CircleShape, soft gold luxury border, seamlessly blended with background
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(
                            elevation = 3.dp,
                            shape = CircleShape,
                            ambientColor = SoftGold.copy(alpha = 0.25f),
                            spotColor = SoftGoldMedium.copy(alpha = 0.35f)
                        )
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    SoftGoldContainer,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                listOf(SoftGoldLight, SoftGoldMedium, SoftGoldDark)
                            ),
                            shape = CircleShape
                        )
                        .clickable(onClick = onNavigateToProfile),
                    contentAlignment = Alignment.Center
                ) {
                    val avatarPath = uiState.avatarUri
                    if (!avatarPath.isNullOrEmpty() && (File(avatarPath).exists() || avatarPath.startsWith("content://"))) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(if (avatarPath.startsWith("content://")) Uri.parse(avatarPath) else File(avatarPath))
                                .crossfade(true)
                                .build(),
                            contentDescription = "Foto Profil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(2.5.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profil",
                            tint = SoftGoldMedium,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(Dimens.Spacing16))
        }

        // 2. Google Stitch Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(20.dp)),
                placeholder = {
                    Text(
                        text = stringResource(R.string.home_search_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.shortcut_scan_short),
                        tint = MaterialTheme.colorScheme.outline
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.home_search_clear),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = ElectricBlue,
                    unfocusedBorderColor = Color.Transparent
                ),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(Dimens.Spacing20))
        }

        // 3. Google Stitch Hero Card ("Scan QR Instantly")
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0040DF),
                                    Color(0xFF2D5BFF),
                                    Color(0xFF0052FF)
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    // Decorative Background Glow & QR Watermark (Stitch Style)
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .align(Alignment.CenterEnd)
                            .background(Color(0xFF00E3FD).copy(alpha = 0.15f), CircleShape)
                    )
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        tint = Color(0xFF00E3FD).copy(alpha = 0.35f),
                        modifier = Modifier
                            .size(110.dp)
                            .align(Alignment.CenterEnd)
                    )

                    // Hero Content
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.68f)
                            .align(Alignment.CenterStart)
                    ) {
                        Text(
                            text = stringResource(R.string.home_hero_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.home_hero_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(18.dp))

                        // Pill Scan Button
                        Button(
                            onClick = onNavigateToScan,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF0040DF)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFF0040DF)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.home_hero_action),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(Dimens.Spacing24))
        }

        // 4. Google Stitch Quick Actions Grid (3x2)
        item {
            Text(
                text = stringResource(R.string.home_quick_actions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(Dimens.Spacing12))

            // Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StitchActionCard(
                    title = stringResource(R.string.action_scan_camera),
                    icon = Icons.Default.QrCodeScanner,
                    iconTint = ElectricBlue,
                    containerColor = ElectricBlue.copy(alpha = 0.12f),
                    onClick = onNavigateToScan,
                    modifier = Modifier.weight(1f)
                )
                StitchActionCard(
                    title = stringResource(R.string.action_create_qr),
                    icon = Icons.Default.AddBox,
                    iconTint = Color(0xFF0097A7),
                    containerColor = Color(0xFF00E3FD).copy(alpha = 0.15f),
                    onClick = onNavigateToCreate,
                    modifier = Modifier.weight(1f)
                )
                StitchActionCard(
                    title = stringResource(R.string.action_barcode_1d),
                    icon = Icons.Default.ViewWeek,
                    iconTint = Color(0xFFE65100),
                    containerColor = Color(0xFFFF9800).copy(alpha = 0.15f),
                    onClick = { showBarcodeDialog = true },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StitchActionCard(
                    title = stringResource(R.string.action_my_qr),
                    icon = Icons.Default.QrCode2,
                    iconTint = ElectricBlue,
                    containerColor = ElectricBlue.copy(alpha = 0.12f),
                    onClick = onNavigateToMyQr,
                    modifier = Modifier.weight(1f)
                )
                StitchActionCard(
                    title = stringResource(R.string.nav_history),
                    icon = Icons.Default.History,
                    iconTint = Color(0xFF5C6BC0),
                    containerColor = Color(0xFF5C6BC0).copy(alpha = 0.15f),
                    onClick = onNavigateToHistory,
                    modifier = Modifier.weight(1f)
                )
                StitchActionCard(
                    title = stringResource(R.string.action_favorites),
                    icon = Icons.Default.Favorite,
                    iconTint = ErrorRed,
                    containerColor = ErrorRed.copy(alpha = 0.15f),
                    onClick = onNavigateToFavorites,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(Dimens.Spacing24))
        }

        // 5. Google Stitch Recent Activity Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) stringResource(R.string.home_search_results, filteredScans.size) else stringResource(R.string.home_recent_activity),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = onNavigateToHistory) {
                    Text(
                        text = stringResource(R.string.home_see_all),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = ElectricBlue
                    )
                }
            }
            Spacer(modifier = Modifier.height(Dimens.Spacing8))
        }

        // 6. Recent Activity List Items (with Stitch Accent Borders)
        if (filteredScans.isEmpty()) {
            item {
                EmptyStateView(
                    title = if (searchQuery.isBlank()) stringResource(R.string.home_empty_title) else stringResource(R.string.home_search_empty_title),
                    description = if (searchQuery.isBlank()) stringResource(R.string.home_empty_desc) else stringResource(R.string.home_search_empty_desc),
                    icon = Icons.Default.QrCodeScanner,
                    actionText = stringResource(R.string.home_hero_action),
                    onActionClick = onNavigateToScan
                )
            }
        } else {
            items(filteredScans, key = { it.id }) { scan ->
                StitchRecentActivityCard(
                    scan = scan,
                    onClick = { onNavigateToResult(scan.id) },
                    onToggleFavorite = { viewModel.toggleFavorite(scan.id, scan.isFavorite) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }

    if (showBarcodeDialog) {
        AlertDialog(
            onDismissRequest = { showBarcodeDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewWeek,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = stringResource(R.string.dialog_barcode_title),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.dialog_barcode_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            showBarcodeDialog = false
                            onNavigateToCreateBarcode()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AddBox, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_create_qr), fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            showBarcodeDialog = false
                            onNavigateToScan()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.action_scan_camera), fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBarcodeDialog = false }) {
                    Text(stringResource(R.string.dialog_barcode_close))
                }
            }
        )
    }
}

/**
 * Stitch Quick Action Card with rounded corners and circular tinted icon
 */
@Composable
private fun StitchActionCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 1.dp
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Stitch Recent Activity Card with colorful left border accent
 */
@Composable
private fun StitchRecentActivityCard(
    scan: ScanHistoryItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val accentColor = when (scan.type) {
        QrType.WEBSITE -> Color(0xFF10B981) // Emerald Green for Links
        QrType.WIFI -> CyanAccent
        QrType.CONTACT -> ElectricBlue
        QrType.PAYMENT -> Color(0xFFFF9800)
        QrType.EMAIL -> Color(0xFF2979FF)
        QrType.PHONE, QrType.SMS -> Color(0xFF00BCD4)
        else -> Color(0xFF8B5CF6) // Violet for generic text
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Accent Bar (Stitch Design)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Category Icon in Rounded Container
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (scan.type) {
                        QrType.WEBSITE -> Icons.Default.Language
                        QrType.WIFI -> Icons.Default.Wifi
                        QrType.CONTACT -> Icons.Default.Person
                        else -> Icons.Default.QrCode
                    },
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = scan.title.ifBlank { scan.content },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${scan.type.displayName} • ${scan.createdAt.toFormattedDateString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Favorite Button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (scan.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (scan.isFavorite) ErrorRed else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
