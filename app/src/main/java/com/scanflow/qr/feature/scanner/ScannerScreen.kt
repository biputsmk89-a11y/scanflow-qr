package com.scanflow.qr.feature.scanner

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.scanflow.qr.core.utils.VibratorHelper
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.scanflow.qr.core.designsystem.Dimens
import com.scanflow.qr.core.designsystem.ElectricBlue
import com.scanflow.qr.core.designsystem.EmptyStateView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateResult: (Long) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.scanImageFromGallery(context, uri, onNavigateResult)
            }
        }
    )

    LaunchedEffect(Unit) {
        viewModel.resumeScanning()
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(Dimens.Spacing24),
            contentAlignment = Alignment.Center
        ) {
            EmptyStateView(
                title = "Camera Permission Required",
                description = "ScanFlow QR requires access to your camera to scan QR codes and barcodes in real-time.",
                icon = Icons.Default.PhotoCamera,
                actionText = "Grant Permission",
                onActionClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
            )
        }
        return
    }

    var cameraControl: Camera? by remember { mutableStateOf(null) }
    var cameraProviderInstance by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewViewInstance by remember { mutableStateOf<PreviewView?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Tap-to-Focus State
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var focusSuccessful by remember { mutableStateOf<Boolean?>(null) }
    var focusTrigger by remember { mutableStateOf(0) }

    // Auto-dismiss focus reticle after 2 seconds
    LaunchedEffect(focusTrigger) {
        if (focusPoint != null) {
            delay(2000)
            focusPoint = null
            focusSuccessful = null
        }
    }

    val onFocusTap: (Offset) -> Unit = { offset ->
        focusPoint = offset
        focusSuccessful = null
        focusTrigger++

        val control = cameraControl?.cameraControl
        val preview = previewViewInstance
        if (control != null && preview != null) {
            try {
                val point = preview.meteringPointFactory.createPoint(offset.x, offset.y)
                val action = FocusMeteringAction.Builder(
                    point,
                    FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                )
                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                    .build()

                val future = control.startFocusAndMetering(action)
                future.addListener({
                    try {
                        val result = future.get()
                        focusSuccessful = result.isFocusSuccessful
                        if (result.isFocusSuccessful) {
                            VibratorHelper.vibrateSuccess(context)
                        }
                    } catch (_: Exception) {
                        focusSuccessful = false
                    }
                }, ContextCompat.getMainExecutor(context))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Lifecycle cleanup: turn off torch, unbind camera provider, and shut down executor thread
    DisposableEffect(cameraExecutor) {
        onDispose {
            try {
                cameraControl?.cameraControl?.enableTorch(false)
            } catch (_: Exception) {}
            try {
                cameraProviderInstance?.unbindAll()
            } catch (_: Exception) {}
            cameraExecutor.shutdown()
        }
    }

    // Update Torch
    LaunchedEffect(uiState.isTorchEnabled) {
        cameraControl?.cameraControl?.enableTorch(uiState.isTorchEnabled)
    }

    // Update Zoom Ratio
    LaunchedEffect(uiState.zoomRatio) {
        cameraControl?.cameraControl?.setZoomRatio(uiState.zoomRatio)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onFocusTap(offset)
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom != 1.0f) {
                        viewModel.applyZoomDelta(zoom)
                    }
                }
            }
    ) {
        // CameraX Viewfinder with key on cameraLens to re-bind when flipping camera
        key(uiState.cameraLens) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    previewViewInstance = previewView
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            cameraProviderInstance = cameraProvider
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also { analysis ->
                                    if (!cameraExecutor.isShutdown) {
                                        analysis.setAnalyzer(
                                            cameraExecutor,
                                            CameraAnalyzer { raw, format ->
                                                viewModel.onBarcodeDetected(context, raw, format, onNavigateResult)
                                            }
                                        )
                                    }
                                }

                            val cameraSelector = CameraSelector.Builder()
                                .requireLensFacing(uiState.cameraLens)
                                .build()

                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                            camera.cameraInfo.zoomState.observe(lifecycleOwner) { state ->
                                if (state != null) {
                                    viewModel.setZoomBounds(state.minZoomRatio, state.maxZoomRatio)
                                }
                            }
                            cameraControl = camera
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Overlay Frame
        ScannerOverlayView()

        // Animated Tap-to-Focus Reticle
        focusPoint?.let { point ->
            FocusReticleView(offset = point, isSuccess = focusSuccessful)
        }

        // Top Navigation & Action Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = Dimens.Spacing16, start = Dimens.Spacing16, end = Dimens.Spacing16)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                // Batch Mode Switch Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (uiState.isBatchMode) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.6f),
                    onClick = { viewModel.toggleBatchMode() },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (uiState.isBatchMode) "📦 Batch (${uiState.batchCount})" else "⚡ Pindai Tunggal",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing12)) {
                    // Torch Button
                    IconButton(
                        onClick = { viewModel.toggleTorch() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (uiState.isTorchEnabled) MaterialTheme.colorScheme.primary
                                else Color.Black.copy(alpha = 0.5f)
                            )
                    ) {
                        Icon(
                            imageVector = if (uiState.isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Torch",
                            tint = Color.White
                        )
                    }

                    // Switch Camera Lens
                    IconButton(
                        onClick = { viewModel.switchCamera() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Switch Camera",
                            tint = Color.White
                        )
                    }
                }
            }

            // Live Scan Feedback Banner (Instant Toast / Notification in camera)
            AnimatedVisibility(
                visible = uiState.scanBannerMessage != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Spacer(modifier = Modifier.height(Dimens.Spacing12))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (uiState.isDuplicateWarning) Color(0xFFE65100).copy(alpha = 0.95f)
                            else Color(0xFF00C853).copy(alpha = 0.95f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.dismissBanner() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Dimens.Spacing16, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (uiState.isDuplicateWarning) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(Dimens.Spacing12))
                        Text(
                            text = uiState.scanBannerMessage.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Bottom Help Hint & Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = Dimens.Spacing24, start = Dimens.Spacing16, end = Dimens.Spacing16),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Zoom Slider & Quick Zoom Buttons (1x, 2x, 5x)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Black.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .fillMaxWidth(0.9f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1.0f, 2.0f, 5.0f).forEach { preset ->
                                val isSelected = kotlin.math.abs(uiState.zoomRatio - preset) < 0.15f
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) ElectricBlue else Color.White.copy(alpha = 0.12f),
                                    modifier = Modifier.clickable { viewModel.setZoomRatio(preset) }
                                ) {
                                    Text(
                                        text = "${preset.toInt()}x",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (isSelected) Color.Black else Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = String.format(Locale.US, "%.1fx Zoom", uiState.zoomRatio),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ElectricBlue
                        )
                    }

                    Slider(
                        value = uiState.zoomRatio,
                        onValueChange = { viewModel.setZoomRatio(it) },
                        valueRange = uiState.minZoomRatio..uiState.maxZoomRatio,
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricBlue,
                            activeTrackColor = ElectricBlue,
                            inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                    )
                }
            }

            if (uiState.isBatchMode) {
                // Batch Mode Control Deck
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF131B2E).copy(alpha = 0.95f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(Dimens.Spacing16)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Mode Beruntun (Massal)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${uiState.batchCount} barcode tersimpan dalam sesi ini",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF00E5FF)
                                )
                            }

                            // Duplicate Prevention Filter Chip
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (!uiState.allowDuplicates) Color(0xFF2E7D32) else Color.DarkGray,
                                onClick = { viewModel.toggleAllowDuplicates() }
                            ) {
                                Text(
                                    text = if (!uiState.allowDuplicates) "🚫 Tolak Duplikat" else "🔁 Izinkan Duplikat",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(Dimens.Spacing12))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Review Batch Button
                            FilledTonalButton(
                                onClick = { viewModel.setBatchSheetVisible(true) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = "Daftar (${uiState.batchCount})",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Instant Batch CSV Share Button
                            if (uiState.batchCount > 0) {
                                FilledTonalButton(
                                    onClick = { viewModel.shareBatchCsv(context) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Color(0xFF00E5FF).copy(alpha = 0.2f),
                                        contentColor = Color(0xFF00E5FF)
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Bagikan CSV",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "CSV", fontWeight = FontWeight.Bold)
                                }

                                // Clear Batch
                                OutlinedButton(
                                    onClick = { viewModel.clearBatch() },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFFFF8A80)
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = "Clear Session",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Stitch Instruction Text
                Text(
                    text = "Place the QR Code inside the frame",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Stitch Glassmorphic Quick Controls Pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.55f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Torch
                        IconButton(
                            onClick = { viewModel.toggleTorch() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Torch",
                                tint = if (uiState.isTorchEnabled) Color(0xFF00E3FD) else Color.White
                            )
                        }

                        // Divider
                        Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.White.copy(alpha = 0.25f)))

                        // Gallery
                        IconButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Gallery",
                                tint = Color.White
                            )
                        }

                        // Divider
                        Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.White.copy(alpha = 0.25f)))

                        // Camera Switch
                        IconButton(
                            onClick = { viewModel.switchCamera() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Mode Selector Pill (Stitch Design)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (!uiState.isBatchMode) ElectricBlue else Color.Transparent,
                            onClick = { if (uiState.isBatchMode) viewModel.toggleBatchMode() }
                        ) {
                            Text(
                                text = "QR Code",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (uiState.isBatchMode) ElectricBlue else Color.Transparent,
                            onClick = { if (!uiState.isBatchMode) viewModel.toggleBatchMode() }
                        ) {
                            Text(
                                text = "Batch Scan",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Review Batch Scans & Export
    if (uiState.isBatchSheetVisible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(
            onDismissRequest = { viewModel.setBatchSheetVisible(false) },
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "📦 Hasil Pindai Batch",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${uiState.batchCount} item",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    IconButton(onClick = { viewModel.setBatchSheetVisible(false) }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.Spacing12))

                // Duplicate Toggle Option
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.Spacing16, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tolak Duplikasi Kode",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Abaikan pemindaian ganda (cocok untuk tiket/absensi)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = !uiState.allowDuplicates,
                            onCheckedChange = { viewModel.toggleAllowDuplicates() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.Spacing12))

                // Action Export Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.Spacing8)
                ) {
                    // Direct CSV Share File Button
                    FilledTonalButton(
                        onClick = { viewModel.shareBatchCsv(context) },
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Bagikan CSV", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }

                    // Copy Summary Button
                    OutlinedButton(
                        onClick = {
                            if (uiState.batchItems.isNotEmpty()) {
                                val summary = viewModel.getBatchPlainTextSummary()
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("ScanFlow Batch Summary", summary)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "✅ Ringkasan teks disalin!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Belum ada item untuk disalin", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Salin Teks", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.Spacing16))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(Dimens.Spacing12))

                // Scanned Items List
                if (uiState.batchItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Dimens.Spacing32),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.QrCode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Belum ada kode yang dipindai",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Arahkan kamera ke barcode untuk memindai massal.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = uiState.batchItems,
                            key = { _, item -> item.scanId }
                        ) { index, item ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setBatchSheetVisible(false)
                                        onNavigateResult(item.scanId)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${uiState.batchItems.size - index}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Text(
                                                    text = item.data.type.name,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = timeFormat.format(Date(item.scannedAt)),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = item.data.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = item.data.rawContent,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Delete from batch
                                    IconButton(
                                        onClick = { viewModel.removeItemFromBatch(item.scanId) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Animated DSLR-style Focus Reticle shown at the touch coordinates
 * when the user taps on the camera viewfinder to focus.
 */
@Composable
private fun FocusReticleView(
    offset: Offset,
    isSuccess: Boolean?
) {
    var isScaledDown by remember { mutableStateOf(false) }

    LaunchedEffect(offset) {
        isScaledDown = false
        delay(16)
        isScaledDown = true
    }

    val scale by animateFloatAsState(
        targetValue = if (isScaledDown) 1.0f else 1.45f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "focus_scale"
    )

    val reticleColor = when (isSuccess) {
        true -> Color(0xFF00E676) // Crisp emerald green on focus lock
        false -> Color(0xFFFF5252) // Soft coral red on focus fail
        null -> ElectricBlue // Vibrant cyan-blue while focusing
    }

    val reticleSize = 72.dp
    val density = LocalDensity.current
    val reticleSizePx = with(density) { reticleSize.toPx() }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Canvas(
            modifier = Modifier
                .size(reticleSize)
                .graphicsLayer {
                    translationX = offset.x - (reticleSizePx / 2f)
                    translationY = offset.y - (reticleSizePx / 2f)
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            val strokeWidth = 2.dp.toPx()
            val bracketLength = size.width * 0.28f
            val radius = size.width / 2f
            val center = Offset(radius, radius)

            // 1. Subtle Outer Circle
            drawCircle(
                color = reticleColor.copy(alpha = 0.35f),
                radius = radius,
                style = Stroke(width = 1.dp.toPx())
            )

            // 2. Center Crosshair Dot
            drawCircle(
                color = reticleColor,
                radius = 3.dp.toPx()
            )

            // 3. DSLR Viewfinder Corner Brackets (Top-Left, Top-Right, Bottom-Left, Bottom-Right)
            val cornerInset = 4.dp.toPx()

            // Top-Left Corner
            drawLine(
                color = reticleColor,
                start = Offset(cornerInset, cornerInset + bracketLength),
                end = Offset(cornerInset, cornerInset),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = reticleColor,
                start = Offset(cornerInset, cornerInset),
                end = Offset(cornerInset + bracketLength, cornerInset),
                strokeWidth = strokeWidth
            )

            // Top-Right Corner
            drawLine(
                color = reticleColor,
                start = Offset(size.width - cornerInset - bracketLength, cornerInset),
                end = Offset(size.width - cornerInset, cornerInset),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = reticleColor,
                start = Offset(size.width - cornerInset, cornerInset),
                end = Offset(size.width - cornerInset, cornerInset + bracketLength),
                strokeWidth = strokeWidth
            )

            // Bottom-Left Corner
            drawLine(
                color = reticleColor,
                start = Offset(cornerInset, size.height - cornerInset - bracketLength),
                end = Offset(cornerInset, size.height - cornerInset),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = reticleColor,
                start = Offset(cornerInset, size.height - cornerInset),
                end = Offset(cornerInset + bracketLength, size.height - cornerInset),
                strokeWidth = strokeWidth
            )

            // Bottom-Right Corner
            drawLine(
                color = reticleColor,
                start = Offset(size.width - cornerInset - bracketLength, size.height - cornerInset),
                end = Offset(size.width - cornerInset, size.height - cornerInset),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = reticleColor,
                start = Offset(size.width - cornerInset, size.height - cornerInset - bracketLength),
                end = Offset(size.width - cornerInset, size.height - cornerInset),
                strokeWidth = strokeWidth
            )

            // 4. Directional Tick Marks (North, South, East, West)
            val tickLen = 6.dp.toPx()
            // North
            drawLine(reticleColor, Offset(center.x, 0f), Offset(center.x, tickLen), strokeWidth)
            // South
            drawLine(reticleColor, Offset(center.x, size.height - tickLen), Offset(center.x, size.height), strokeWidth)
            // West
            drawLine(reticleColor, Offset(0f, center.y), Offset(tickLen, center.y), strokeWidth)
            // East
            drawLine(reticleColor, Offset(size.width - tickLen, center.y), Offset(size.width, center.y), strokeWidth)
        }
    }
}

