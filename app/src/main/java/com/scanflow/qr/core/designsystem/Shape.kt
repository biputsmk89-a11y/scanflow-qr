package com.scanflow.qr.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val ScanFlowShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

object Dimens {
    val Spacing2 = 2.dp
    val Spacing4 = 4.dp
    val Spacing8 = 8.dp
    val Spacing12 = 12.dp
    val Spacing16 = 16.dp
    val Spacing20 = 20.dp
    val Spacing24 = 24.dp
    val Spacing32 = 32.dp
    val Spacing40 = 40.dp
    val Spacing48 = 48.dp

    val MinTouchTarget = 48.dp
    val ScannerFrameSize = 260.dp
    val QrPreviewCardSize = 240.dp
    val ButtonHeight = 52.dp
    val SmallButtonHeight = 40.dp
    val CornerRadiusCard = 20.dp
    val CornerRadiusButton = 16.dp
}
