package com.scanflow.qr.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.widget.Toast
import androidx.print.PrintHelper
import com.scanflow.qr.ScanFlowApplication
import com.scanflow.qr.core.security.AppLockManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilitas cetak fisik langsung ke printer nirkabel (Wi-Fi, Bluetooth, Mopria)
 * menggunakan Android PrintHelper bawaan sistem operasi.
 */
object AppPrintHelper {

    fun isPrintSupported(): Boolean {
        return try {
            PrintHelper.systemSupportsPrint()
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Mencetak Bitmap QR code atau Barcode langsung ke printer sistem Android.
     * Mengatur bypass AppLock sementara agar aplikasi tidak terkunci saat dialog pencetakan sistem muncul.
     */
    fun printQrBitmap(
        context: Context,
        jobName: String,
        bitmap: Bitmap,
        title: String? = null,
        subtitle: String? = null,
        content: String? = null,
        appLockManager: AppLockManager? = null
    ) {
        if (!isPrintSupported()) {
            Toast.makeText(context, "Pencetakan tidak didukung di perangkat ini", Toast.LENGTH_SHORT).show()
            return
        }

        // Bypass AppLock sementara agar perpindahan ke PrintDialog sistem Android tidak memicu penguncian
        val effectiveLockManager = appLockManager
            ?: (context.applicationContext as? ScanFlowApplication)?.appLockManager
        effectiveLockManager?.setTemporarilyBypassed(true)

        try {
            val printHelper = PrintHelper(context).apply {
                scaleMode = PrintHelper.SCALE_MODE_FIT
                colorMode = PrintHelper.COLOR_MODE_COLOR
            }

            val printableBitmap = createPrintablePage(
                qrBitmap = bitmap,
                title = title ?: "ScanFlow QR",
                subtitle = subtitle,
                content = content
            )

            val safeJobName = "ScanFlow_" + jobName.replace(Regex("[^a-zA-Z0-9_]"), "_").take(30)
            printHelper.printBitmap(safeJobName, printableBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal memulai pencetakan: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Memformat kanvas dokumen cetak proporsional dengan kop judul, QR/Barcode di tengah,
     * dan ringkasan data di bagian bawah agar rapi saat dicetak di kertas A4, Letter, maupun struk kasir.
     */
    fun createPrintablePage(
        qrBitmap: Bitmap,
        title: String,
        subtitle: String?,
        content: String?
    ): Bitmap {
        val width = 1200
        val height = 1600
        val pageBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(pageBitmap)
        canvas.drawColor(Color.WHITE)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#111827")
            textAlign = Paint.Align.CENTER
        }

        // 1. App Header Branding
        textPaint.textSize = 28f
        textPaint.color = Color.parseColor("#0040DF") // Electric Blue
        textPaint.isFakeBoldText = true
        canvas.drawText("ScanFlow QR Studio", width / 2f, 130f, textPaint)

        // 2. Document Title
        textPaint.textSize = 44f
        textPaint.color = Color.parseColor("#111827")
        textPaint.isFakeBoldText = true
        val safeTitle = if (title.length > 32) title.take(32) + "..." else title
        canvas.drawText(safeTitle, width / 2f, 200f, textPaint)

        // 3. Subtitle / Format
        if (!subtitle.isNullOrBlank()) {
            textPaint.textSize = 26f
            textPaint.color = Color.parseColor("#6B7280")
            textPaint.isFakeBoldText = false
            canvas.drawText(subtitle, width / 2f, 250f, textPaint)
        }

        // 4. Center QR / Barcode Bitmap with preserved aspect ratio
        val srcW = qrBitmap.width
        val srcH = qrBitmap.height
        val maxTargetW = 900
        val maxTargetH = 800
        val scale = minOf(maxTargetW.toFloat() / srcW, maxTargetH.toFloat() / srcH)
        val finalW = (srcW * scale).toInt()
        val finalH = (srcH * scale).toInt()

        val left = (width - finalW) / 2
        val top = 310 + (maxTargetH - finalH) / 2
        val destRect = Rect(left, top, left + finalW, top + finalH)
        canvas.drawBitmap(qrBitmap, null, destRect, null)

        // 5. Content Data Box
        var currentY = 310f + maxTargetH + 70f
        if (!content.isNullOrBlank()) {
            textPaint.textSize = 28f
            textPaint.color = Color.parseColor("#374151")
            textPaint.isFakeBoldText = false
            val cleanContent = if (content.length > 55) content.take(55) + "..." else content
            canvas.drawText(cleanContent, width / 2f, currentY, textPaint)
            currentY += 45f
        }

        // 6. Footer: Timestamp & Instructions
        textPaint.textSize = 22f
        textPaint.color = Color.parseColor("#9CA3AF")
        textPaint.isFakeBoldText = false
        val dateStr = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        canvas.drawText("Dicetak pada: $dateStr • ScanFlow Pro Suite", width / 2f, currentY + 30f, textPaint)

        return pageBitmap
    }
}
