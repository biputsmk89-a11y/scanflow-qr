package com.scanflow.qr.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * State controller for [ErrorBoundary] to capture and recover from UI/data errors.
 */
class ErrorBoundaryState {
    var hasError by mutableStateOf(false)
    var errorCause by mutableStateOf<Throwable?>(null)

    fun reportError(throwable: Throwable) {
        hasError = true
        errorCause = throwable
    }

    fun reset() {
        hasError = false
        errorCause = null
    }
}

val LocalErrorBoundary = compositionLocalOf<ErrorBoundaryState?> { null }

/**
 * Error Boundary component in Jetpack Compose to safely isolate UI components
 * and display a localized recovery interface with retry action.
 */
@Composable
fun ErrorBoundary(
    modifier: Modifier = Modifier,
    fallbackTitle: String = "Komponen Mengalami Kendala",
    fallbackDescription: String = "Gagal memuat elemen antarmuka ini. Silakan coba muat ulang.",
    content: @Composable () -> Unit
) {
    val state = remember { ErrorBoundaryState() }

    CompositionLocalProvider(LocalErrorBoundary provides state) {
        if (state.hasError) {
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error Boundary Warning",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(40.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = fallbackTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = state.errorCause?.localizedMessage ?: fallbackDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { state.reset() },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(text = "Coba Lagi")
                    }
                }
            }
        } else {
            Box(modifier = modifier) {
                content()
            }
        }
    }
}
