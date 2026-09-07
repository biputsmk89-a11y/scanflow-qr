package com.scanflow.qr.scanner

import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import com.google.common.truth.Truth.assertThat
import com.scanflow.qr.feature.scanner.ScannerUiState
import org.junit.Test
import java.util.concurrent.TimeUnit

class CameraFocusAndZoomTest {

    @Test
    fun `default camera lens is back camera`() {
        val state = ScannerUiState()
        assertThat(state.cameraLens).isEqualTo(CameraSelector.LENS_FACING_BACK)
    }

    @Test
    fun `focus metering action builds with auto-focus and auto-exposure flags`() {
        val factory = SurfaceOrientedMeteringPointFactory(1080f, 1920f)
        val point = factory.createPoint(540f, 960f)

        val action = FocusMeteringAction.Builder(
            point,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
        )
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()

        assertThat(action.meteringPointsAf).isNotEmpty()
        assertThat(action.meteringPointsAe).isNotEmpty()
        assertThat(action.autoCancelDurationInMillis).isEqualTo(3000L)
    }

    @Test
    fun `zoom ratio clamped within valid bounds`() {
        val minZoom = 1.0f
        val maxZoom = 8.0f

        val initialZoom = 1.0f
        val deltaZoom = 2.0f
        val clampedZoom = (initialZoom * deltaZoom).coerceIn(minZoom, maxZoom)

        assertThat(clampedZoom).isEqualTo(2.0f)

        val overZoom = (initialZoom * 15.0f).coerceIn(minZoom, maxZoom)
        assertThat(overZoom).isEqualTo(maxZoom)

        val underZoom = (initialZoom * 0.2f).coerceIn(minZoom, maxZoom)
        assertThat(underZoom).isEqualTo(minZoom)
    }
}
