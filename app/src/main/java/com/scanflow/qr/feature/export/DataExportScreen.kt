package com.scanflow.qr.feature.export

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scanflow.qr.core.designsystem.CyanAccent
import com.scanflow.qr.core.designsystem.ElectricBlue
import com.scanflow.qr.core.designsystem.SuccessGreen

/**
 * Data Export Screen
 * Implements 3 Stitch screens in a unified, 100% interactive real-time flow:
 * 1. Data Export (Configuration)
 * 2. Export Progress (Animated progress with 3 live task stages)
 * 3. Export Ready (Encrypted download, share sheet, and completion)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataExportScreen(
    viewModel: DataExportViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.downloadSuccessMessage) {
        uiState.downloadSuccessMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Data Export",
                        fontWeight = FontWeight.Bold,
                        color = ElectricBlue,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.stage == ExportStage.IN_PROGRESS) {
                            viewModel.cancelExport()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    Spacer(modifier = Modifier.width(48.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = uiState.stage,
                transitionSpec = {
                    fadeIn(animationSpec = tween(280)) togetherWith fadeOut(animationSpec = tween(280))
                },
                label = "export_flow_stage"
            ) { stage ->
                when (stage) {
                    ExportStage.CONFIG -> {
                        DataExportConfigContent(
                            uiState = uiState,
                            onToggleHistory = viewModel::toggleScanHistory,
                            onToggleMyQrs = viewModel::toggleMyQrs,
                            onToggleAnalytics = viewModel::toggleAnalytics,
                            onSelectFormat = viewModel::selectFormat,
                            onSelectDateRange = viewModel::selectDateRange,
                            onStartExport = viewModel::startExport
                        )
                    }
                    ExportStage.IN_PROGRESS -> {
                        ExportProgressContent(
                            uiState = uiState,
                            onCancelExport = viewModel::cancelExport
                        )
                    }
                    ExportStage.READY -> {
                        ExportReadyContent(
                            uiState = uiState,
                            onDownload = viewModel::downloadFile,
                            onShare = { viewModel.shareFile(context) },
                            onDone = {
                                viewModel.resetToConfig()
                                onNavigateBack()
                            }
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// 1. DATA EXPORT CONFIGURATION CONTENT (Google Stitch: Data Export)
// -----------------------------------------------------------------------------------------
@Composable
private fun DataExportConfigContent(
    uiState: DataExportUiState,
    onToggleHistory: (Boolean) -> Unit,
    onToggleMyQrs: (Boolean) -> Unit,
    onToggleAnalytics: (Boolean) -> Unit,
    onSelectFormat: (ExportFormat) -> Unit,
    onSelectDateRange: (ExportDateRange) -> Unit,
    onStartExport: () -> Unit
) {
    var dateMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Section 1: Select Data to Export
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Select Data to Export",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                shadowElevation = 2.dp
            ) {
                Column {
                    // Checkbox 1: Scan History
                    ExportDataCheckRow(
                        title = "Scan History",
                        subtitle = "Records of all scanned codes",
                        icon = Icons.Default.History,
                        checked = uiState.includeScanHistory,
                        onCheckedChange = onToggleHistory
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                    // Checkbox 2: My QR Codes
                    ExportDataCheckRow(
                        title = "My QR Codes",
                        subtitle = "Codes you have generated",
                        icon = Icons.Default.QrCode,
                        checked = uiState.includeMyQrs,
                        onCheckedChange = onToggleMyQrs
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                    // Checkbox 3: Analytics Data
                    ExportDataCheckRow(
                        title = "Analytics Data",
                        subtitle = "Usage statistics and metrics",
                        icon = Icons.Default.BarChart,
                        checked = uiState.includeAnalytics,
                        onCheckedChange = onToggleAnalytics
                    )
                }
            }
        }

        // Section 2: Format
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Format",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExportFormatCard(
                    modifier = Modifier.weight(1f),
                    format = ExportFormat.JSON,
                    icon = Icons.Default.DataObject,
                    isSelected = uiState.selectedFormat == ExportFormat.JSON,
                    onClick = { onSelectFormat(ExportFormat.JSON) }
                )

                ExportFormatCard(
                    modifier = Modifier.weight(1f),
                    format = ExportFormat.CSV,
                    icon = Icons.Default.TableView,
                    isSelected = uiState.selectedFormat == ExportFormat.CSV,
                    onClick = { onSelectFormat(ExportFormat.CSV) }
                )

                ExportFormatCard(
                    modifier = Modifier.weight(1f),
                    format = ExportFormat.PDF,
                    icon = Icons.Default.PictureAsPdf,
                    isSelected = uiState.selectedFormat == ExportFormat.PDF,
                    onClick = { onSelectFormat(ExportFormat.PDF) }
                )
            }
        }

        // Section 3: Date Range
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Date Range",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Box {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { dateMenuExpanded = true },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.selectedDateRange.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "Select date range",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                DropdownMenu(
                    expanded = dateMenuExpanded,
                    onDismissRequest = { dateMenuExpanded = false }
                ) {
                    ExportDateRange.values().forEach { range ->
                        DropdownMenuItem(
                            text = { Text(text = range.label) },
                            onClick = {
                                onSelectDateRange(range)
                                dateMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sticky Bottom Export Data Button
        Button(
            onClick = onStartExport,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricBlue,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Export Data",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ExportDataCheckRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = ElectricBlue,
                uncheckedColor = MaterialTheme.colorScheme.outline
            )
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun ExportFormatCard(
    modifier: Modifier = Modifier,
    format: ExportFormat,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) ElectricBlue else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        label = "format_border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) ElectricBlue.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
        label = "format_bg"
    )

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        shadowElevation = if (isSelected) 3.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = format.label,
                tint = if (isSelected) ElectricBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = format.label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) ElectricBlue else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// -----------------------------------------------------------------------------------------
// 2. EXPORT PROGRESS CONTENT (Google Stitch: Export Progress)
// -----------------------------------------------------------------------------------------
@Composable
private fun ExportProgressContent(
    uiState: DataExportUiState,
    onCancelExport: () -> Unit
) {
    // Rotating spinner for in-progress step
    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sync_angle"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = uiState.progress,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "progress_anim"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            shadowElevation = 8.dp
        ) {
            Box(modifier = Modifier.padding(28.dp)) {
                // Ambient Glow
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(ElectricBlue.copy(alpha = 0.15f), Color.Transparent)
                            )
                        )
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Exporting Data",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Please wait while we prepare your file.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Central Circular Loader with Percentage
                    Box(
                        modifier = Modifier.size(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Background track
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            strokeWidth = 8.dp,
                        )

                        // Animated progress indicator
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = ElectricBlue,
                            strokeWidth = 8.dp,
                        )

                        Text(
                            text = "${uiState.progressPercentage}%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = ElectricBlue
                        )
                    }

                    // Checklist 3 Tasks
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Task 1: Gathering Scan History
                        ProgressTaskRow(
                            title = "Gathering Scan History...",
                            isCompleted = uiState.currentStep.ordinal > ProgressStep.GATHERING.ordinal,
                            isInProgress = uiState.currentStep == ProgressStep.GATHERING,
                            rotationAngle = rotationAngle
                        )

                        // Task 2: Formatting QR Assets
                        ProgressTaskRow(
                            title = "Formatting QR Assets...",
                            isCompleted = uiState.currentStep.ordinal > ProgressStep.FORMATTING.ordinal,
                            isInProgress = uiState.currentStep == ProgressStep.FORMATTING,
                            rotationAngle = rotationAngle
                        )

                        // Task 3: Encrypting file
                        ProgressTaskRow(
                            title = "Encrypting file...",
                            isCompleted = uiState.currentStep.ordinal > ProgressStep.ENCRYPTING.ordinal,
                            isInProgress = uiState.currentStep == ProgressStep.ENCRYPTING,
                            rotationAngle = rotationAngle
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Cancel Button
                    OutlinedButton(
                        onClick = onCancelExport,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = "Cancel Export",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressTaskRow(
    title: String,
    isCompleted: Boolean,
    isInProgress: Boolean,
    rotationAngle: Float
) {
    val bgColor = when {
        isInProgress -> ElectricBlue.copy(alpha = 0.08f)
        isCompleted -> SuccessGreen.copy(alpha = 0.08f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }

    val contentColor = when {
        isInProgress -> ElectricBlue
        isCompleted -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.outline
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = if (isInProgress) BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.3f)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                isCompleted -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                isInProgress -> {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = ElectricBlue,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(rotationAngle)
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isInProgress || isCompleted) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor
            )
        }
    }
}

// -----------------------------------------------------------------------------------------
// 3. EXPORT READY CONTENT (Google Stitch: Export Ready)
// -----------------------------------------------------------------------------------------
@Composable
private fun ExportReadyContent(
    uiState: DataExportUiState,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onDone: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Large Checkmark Icon Banner (Stitch Spec: 80dp filled check_circle)
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Export Ready",
                    tint = ElectricBlue,
                    modifier = Modifier.size(80.dp)
                )

                // Title & Description
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Export Ready",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Your data has been packaged and is ready for download. The file is encrypted for your security.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    uiState.exportedFileName?.let { name ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = ElectricBlue,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Actions Column
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Primary Action: Download File
                    Button(
                        onClick = onDownload,
                        enabled = !uiState.isDownloading,
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
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (uiState.isDownloading) "Saving..." else "Download File",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Secondary Action: Share
                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(2.dp, ElectricBlue),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ElectricBlue
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = ElectricBlue
                            )
                            Text(
                                text = "Share",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ElectricBlue
                            )
                        }
                    }

                    // Ghost Action: Done
                    TextButton(
                        onClick = onDone,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(
                            text = "Done",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ElectricBlue
                        )
                    }
                }
            }
        }
    }
}
