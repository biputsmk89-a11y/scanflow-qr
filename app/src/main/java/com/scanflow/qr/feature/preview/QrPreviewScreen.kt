package com.scanflow.qr.feature.preview

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scanflow.qr.core.designsystem.CyanAccent
import com.scanflow.qr.core.designsystem.Dimens
import com.scanflow.qr.core.designsystem.ElectricBlue
import com.scanflow.qr.core.designsystem.ErrorRed
import com.scanflow.qr.core.designsystem.LoadingStateView
import com.scanflow.qr.core.designsystem.ScanFlowCard
import com.scanflow.qr.core.designsystem.ScanFlowChip
import com.scanflow.qr.core.designsystem.ScanFlowPrimaryButton
import com.scanflow.qr.core.designsystem.ScanFlowSecondaryButton
import com.scanflow.qr.core.designsystem.SectionHeader
import com.scanflow.qr.domain.model.QrCornerStyle
import com.scanflow.qr.domain.model.QrPatternStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrPreviewScreen(
    qrId: Long,
    viewModel: QrPreviewViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(qrId) {
        viewModel.loadQr(qrId)
    }

    LaunchedEffect(uiState.exportSuccessMessage) {
        uiState.exportSuccessMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR Studio & Preview", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val qr = uiState.qrCode
                    if (qr != null) {
                        IconButton(onClick = { viewModel.toggleFavorite() }) {
                            Icon(
                                imageVector = if (qr.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (qr.isFavorite) ErrorRed else MaterialTheme.colorScheme.onSurface
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

        val qr = uiState.qrCode
        val bitmap = uiState.previewBitmap

        if (qr == null || bitmap == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("QR Code not found", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                ScanFlowSecondaryButton(text = "Go Back", onClick = onNavigateBack)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.Spacing20),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(Dimens.Spacing8))

            // Main Preview Card
            ScanFlowCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(Dimens.Spacing24),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(Dimens.QrPreviewCardSize)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(uiState.styleConfig.backgroundColor))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "QR Code Preview",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(Dimens.Spacing16))
                    Text(
                        text = qr.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(Dimens.Spacing4))
                    Text(
                        text = "${qr.type.displayName} · High Resolution",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.Spacing24))

            // Customization: Foreground Color Picker
            SectionHeader(title = "Foreground Color")
            Spacer(modifier = Modifier.height(Dimens.Spacing8))
            val colors = listOf(
                Color.Black,
                ElectricBlue,
                CyanAccent,
                Color(0xFF8B5CF6),
                Color(0xFF10B981),
                Color(0xFFEF4444),
                Color(0xFFF59E0B)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing12)
            ) {
                colors.forEach { color ->
                    val isSelected = uiState.styleConfig.foregroundColor == color.toArgb()
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { viewModel.updateForegroundColor(color.toArgb()) }
                            .then(
                                if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                else Modifier
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.Spacing20))

            // Customization: Dot Pattern Styles
            SectionHeader(title = "Pattern Style")
            Spacer(modifier = Modifier.height(Dimens.Spacing8))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8)
            ) {
                QrPatternStyle.entries.forEach { style ->
                    ScanFlowChip(
                        text = style.title,
                        selected = uiState.styleConfig.patternStyle == style,
                        onClick = { viewModel.updatePatternStyle(style) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.Spacing20))

            // Customization: Corner Eye Styles
            SectionHeader(title = "Corner Eye Style")
            Spacer(modifier = Modifier.height(Dimens.Spacing8))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8)
            ) {
                QrCornerStyle.entries.forEach { eyeStyle ->
                    ScanFlowChip(
                        text = eyeStyle.title,
                        selected = uiState.styleConfig.cornerEyeStyle == eyeStyle,
                        onClick = { viewModel.updateEyeStyle(eyeStyle) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.Spacing28))

            // Export and Share Actions
            ScanFlowPrimaryButton(
                text = "Save to Gallery (PNG)",
                icon = Icons.Default.Download,
                onClick = { viewModel.exportToGallery(context) },
                modifier = Modifier.fillMaxWidth(),
                gradient = true
            )

            Spacer(modifier = Modifier.height(Dimens.Spacing12))

            ScanFlowSecondaryButton(
                text = "Share QR Code",
                icon = Icons.Default.Share,
                onClick = { viewModel.shareQr(context) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Dimens.Spacing32))
        }
    }
}
