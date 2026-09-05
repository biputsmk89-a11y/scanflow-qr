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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Polyline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.scanflow.qr.domain.model.QrExportFormat
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
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.dismissSuccessMessage()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR Studio & Preview", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                Text("Kode QR tidak ditemukan", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                ScanFlowSecondaryButton(text = "Kembali", onClick = onNavigateBack)
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
                        text = "${qr.type.displayName} · Siap Cetak & Ekspor Multi-Format",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.Spacing24))

            // Customization: Foreground Color Picker
            SectionHeader(title = "Warna Utama (Foreground)")
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
            SectionHeader(title = "Gaya Pola Titik (Pattern Style)")
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
            SectionHeader(title = "Gaya Sudut Sensor (Corner Eye Style)")
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

            // Multi-Format Export and Share Actions
            ScanFlowPrimaryButton(
                text = "📦 Simpan & Ekspor Berkas (PNG/SVG/PDF)",
                icon = Icons.Default.Download,
                onClick = { viewModel.setExportSheetVisible(true) },
                modifier = Modifier.fillMaxWidth(),
                gradient = true
            )

            Spacer(modifier = Modifier.height(Dimens.Spacing12))

            ScanFlowSecondaryButton(
                text = "Bagikan Cepat (PNG)",
                icon = Icons.Default.Share,
                onClick = { viewModel.shareQr(context, QrExportFormat.PNG) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Dimens.Spacing32))
        }

        // Modal Bottom Sheet: Multi-Format Vector & Printable Document Export
        if (uiState.isExportSheetVisible) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { viewModel.setExportSheetVisible(false) },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.Spacing20)
                        .padding(bottom = Dimens.Spacing32)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Format Ekspor & Cetak Industri",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Pilih format sesuai kebutuhan publikasi Anda",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { viewModel.setExportSheetVisible(false) }) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup")
                        }
                    }

                    Spacer(modifier = Modifier.height(Dimens.Spacing16))

                    // 1. Format Option: SVG (Vector)
                    ExportFormatCard(
                        title = "SVG (Vektor Murni Industri)",
                        badge = "Resolusi Tak Terbatas",
                        badgeColor = Color(0xFF00C853),
                        description = "Resolusi tak terbatas tanpa pecah. Sangat disarankan untuk cetak spanduk, banner, sablon, laser cutting, dan desain grafis (CorelDraw, Illustrator, Figma).",
                        icon = Icons.Default.Polyline,
                        onDownload = {
                            viewModel.setExportSheetVisible(false)
                            viewModel.exportQr(QrExportFormat.SVG)
                        },
                        onShare = {
                            viewModel.setExportSheetVisible(false)
                            viewModel.shareQr(context, QrExportFormat.SVG)
                        }
                    )

                    Spacer(modifier = Modifier.height(Dimens.Spacing12))

                    // 2. Format Option: PDF (A4 Document)
                    ExportFormatCard(
                        title = "PDF (Dokumen Siap Cetak A4)",
                        badge = "Format Cetak A4",
                        badgeColor = Color(0xFF0066FF),
                        description = "Halaman dokumen rapi berukuran standar A4 berbingkai lengkap dengan judul, petunjuk pemindaian, dan batas potong untuk dipajang di kelas/kantor.",
                        icon = Icons.Default.PictureAsPdf,
                        onDownload = {
                            viewModel.setExportSheetVisible(false)
                            viewModel.exportQr(QrExportFormat.PDF)
                        },
                        onShare = {
                            viewModel.setExportSheetVisible(false)
                            viewModel.shareQr(context, QrExportFormat.PDF)
                        }
                    )

                    Spacer(modifier = Modifier.height(Dimens.Spacing12))

                    // 3. Format Option: PNG (Raster HD)
                    ExportFormatCard(
                        title = "PNG (Gambar Raster HD)",
                        badge = "Standar Layar",
                        badgeColor = Color(0xFF8B5CF6),
                        description = "Format standar gambar tajam untuk dikirim melalui WhatsApp, diunggah ke media sosial, atau disimpan di galeri foto smartphone.",
                        icon = Icons.Default.Image,
                        onDownload = {
                            viewModel.setExportSheetVisible(false)
                            viewModel.exportQr(QrExportFormat.PNG)
                        },
                        onShare = {
                            viewModel.setExportSheetVisible(false)
                            viewModel.shareQr(context, QrExportFormat.PNG)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportFormatCard(
    title: String,
    badge: String,
    badgeColor: Color,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onDownload: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = badgeColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onDownload,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Unduh", style = MaterialTheme.typography.labelMedium)
                }

                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.IosShare,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Bagikan", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
