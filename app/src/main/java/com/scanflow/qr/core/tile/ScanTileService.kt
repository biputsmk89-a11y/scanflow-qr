package com.scanflow.qr.core.tile

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.scanflow.qr.MainActivity

/**
 * Android Quick Settings Tile Service.
 * Memungkinkan pengguna memindai QR code secara instan dari panel notifikasi status bar
 * baik dalam keadaan aplikasi tertutup maupun dari layar kunci (lock screen).
 */
class ScanTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val tile = qsTile ?: return
        tile.state = Tile.STATE_INACTIVE
        tile.label = "ScanFlow QR"
        tile.contentDescription = "Pindai Cepat QR / Barcode"
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile
        tile?.state = Tile.STATE_ACTIVE
        tile?.updateTile()

        val intent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_SCAN
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        if (isLocked) {
            unlockAndRun {
                launchScannerActivity(intent)
            }
        } else {
            launchScannerActivity(intent)
        }

        tile?.state = Tile.STATE_INACTIVE
        tile?.updateTile()
    }

    private fun launchScannerActivity(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+ (API 34+)
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    companion object {
        const val ACTION_SCAN = "com.scanflow.qr.action.SCAN"
        const val ACTION_CREATE = "com.scanflow.qr.action.CREATE"
        const val ACTION_HISTORY = "com.scanflow.qr.action.HISTORY"
    }
}
