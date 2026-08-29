package com.scanflow.qr.feature.scanner

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.scanflow.qr.core.designsystem.CyanAccent
import com.scanflow.qr.core.designsystem.Dimens

@Composable
fun ScannerOverlayView(
    modifier: Modifier = Modifier,
    laserColor: Color = CyanAccent
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LaserTransition")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserProgress"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val frameSize = Dimens.ScannerFrameSize.toPx()
            val left = (size.width - frameSize) / 2
            val top = (size.height - frameSize) / 2
            val cornerLength = 36.dp.toPx()
            val cornerStroke = 4.dp.toPx()

            // Dim background outside scan frame
            drawRect(
                color = Color.Black.copy(alpha = 0.6f),
                size = size
            )

            // Transparent cut out
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(frameSize, frameSize),
                cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                blendMode = BlendMode.Clear
            )

            // Subtle border frame
            drawRoundRect(
                color = Color.White.copy(alpha = 0.25f),
                topLeft = Offset(left, top),
                size = Size(frameSize, frameSize),
                cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Top-Left Corner
            drawLine(
                color = laserColor,
                start = Offset(left, top + cornerLength),
                end = Offset(left, top + 16.dp.toPx()),
                strokeWidth = cornerStroke
            )
            drawLine(
                color = laserColor,
                start = Offset(left, top),
                end = Offset(left + cornerLength, top),
                strokeWidth = cornerStroke
            )

            // Top-Right Corner
            drawLine(
                color = laserColor,
                start = Offset(left + frameSize - cornerLength, top),
                end = Offset(left + frameSize, top),
                strokeWidth = cornerStroke
            )
            drawLine(
                color = laserColor,
                start = Offset(left + frameSize, top),
                end = Offset(left + frameSize, top + cornerLength),
                strokeWidth = cornerStroke
            )

            // Bottom-Left Corner
            drawLine(
                color = laserColor,
                start = Offset(left, top + frameSize - cornerLength),
                end = Offset(left, top + frameSize),
                strokeWidth = cornerStroke
            )
            drawLine(
                color = laserColor,
                start = Offset(left, top + frameSize),
                end = Offset(left + cornerLength, top + frameSize),
                strokeWidth = cornerStroke
            )

            // Bottom-Right Corner
            drawLine(
                color = laserColor,
                start = Offset(left + frameSize - cornerLength, top + frameSize),
                end = Offset(left + frameSize, top + frameSize),
                strokeWidth = cornerStroke
            )
            drawLine(
                color = laserColor,
                start = Offset(left + frameSize, top + frameSize - cornerLength),
                end = Offset(left + frameSize, top + frameSize),
                strokeWidth = cornerStroke
            )

            // Animated Scanning Laser Line
            val laserY = top + (frameSize * laserProgress)
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        laserColor.copy(alpha = 0.8f),
                        laserColor,
                        laserColor.copy(alpha = 0.8f),
                        Color.Transparent
                    )
                ),
                start = Offset(left + 16.dp.toPx(), laserY),
                end = Offset(left + frameSize - 16.dp.toPx(), laserY),
                strokeWidth = 3.dp.toPx()
            )
        }
    }
}
