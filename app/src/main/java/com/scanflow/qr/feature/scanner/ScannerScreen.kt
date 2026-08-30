package com.scanflow.qr.feature.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.scanflow.qr.core.designsystem.Dimens
import com.scanflow.qr.core.designsystem.EmptyStateView
import java.util.concurrent.Executors

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

    // Update Torch
    LaunchedEffect(uiState.isTorchEnabled) {
        cameraControl?.cameraControl?.enableTorch(uiState.isTorchEnabled)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // CameraX Viewfinder
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                val cameraExecutor = Executors.newSingleThreadExecutor()

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(
                                cameraExecutor,
                                CameraAnalyzer { raw, format ->
                                    viewModel.onBarcodeDetected(context, raw, format, onNavigateResult)
                                }
                            )
                        }

                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(uiState.cameraLens)
                        .build()

                    try {
                        cameraProvider.unbindAll()
                        cameraControl = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Frame
        ScannerOverlayView()

        // Top Navigation & Action Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Dimens.Spacing40, start = Dimens.Spacing16, end = Dimens.Spacing16)
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
                        imageVector = Icons.Default.ArrowBack,
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
                            text = if (uiState.isBatchMode) "📦 Multi-Scan (Batch)" else "⚡ Single Scan",
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

            // Batch Mode Counter Pill
            if (uiState.isBatchMode) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF00C853).copy(alpha = 0.9f)
                    ) {
                        Text(
                            text = "✨ ${uiState.batchCount} Barcodes Scanned in Session",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
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
                .padding(bottom = Dimens.Spacing32, start = Dimens.Spacing20, end = Dimens.Spacing20),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.isBatchMode && uiState.batchCount > 0) {
                // Batch Summary Tray
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF131B2E).copy(alpha = 0.95f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Dimens.Spacing16)
                ) {
                    Column(modifier = Modifier.padding(Dimens.Spacing16)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Last Scanned: ${uiState.lastBatchItem?.title ?: "Item"}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Total ${uiState.batchCount} items in this batch",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF00E5FF)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary,
                                onClick = {
                                    uiState.lastScannedId?.let { onNavigateResult(it) }
                                }
                            ) {
                                Text(
                                    text = "Review Batch",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier.padding(bottom = Dimens.Spacing16)
                ) {
                    Text(
                        text = if (uiState.isBatchMode) "Point camera at barcodes continuously (Cashier/Inventory Mode)"
                               else "Align QR code or barcode inside the frame to scan",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            // Gallery Button
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Black.copy(alpha = 0.6f),
                onClick = { galleryLauncher.launch("image/*") }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Dimens.Spacing20, vertical = Dimens.Spacing12),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(Dimens.Spacing8))
                    Text(
                        text = "Scan from Gallery",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
