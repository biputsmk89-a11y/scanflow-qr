package com.scanflow.qr.core.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.scanflow.qr.domain.model.QrCornerStyle
import com.scanflow.qr.domain.model.QrPatternStyle
import com.scanflow.qr.domain.model.QrStyleConfig

object QrCodeGenerator {

    fun generateQrBitmap(
        content: String,
        config: QrStyleConfig = QrStyleConfig()
    ): Bitmap? {
        if (content.isEmpty()) return null

        return try {
            val hints = hashMapOf<EncodeHintType, Any>(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
                EncodeHintType.MARGIN to config.margin
            )

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, config.size, config.size, hints)

            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Draw Background
            val bgPaint = Paint().apply {
                color = config.backgroundColor
                style = Paint.Style.FILL
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // Draw Modules
            val fgPaint = Paint().apply {
                color = config.foregroundColor
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            val moduleSizeX = width.toFloat() / bitMatrix.width
            val moduleSizeY = height.toFloat() / bitMatrix.height

            for (x in 0 until bitMatrix.width) {
                for (y in 0 until bitMatrix.height) {
                    if (bitMatrix.get(x, y)) {
                        val left = x * moduleSizeX
                        val top = y * moduleSizeY
                        val right = left + moduleSizeX
                        val bottom = top + moduleSizeY
                        val rect = RectF(left, top, right, bottom)

                        val isCornerEye = isCornerEyeModule(x, y, bitMatrix.width, bitMatrix.height)

                        if (isCornerEye) {
                            when (config.cornerEyeStyle) {
                                QrCornerStyle.SQUARE -> canvas.drawRect(rect, fgPaint)
                                QrCornerStyle.ROUNDED -> canvas.drawRoundRect(rect, moduleSizeX * 0.4f, moduleSizeY * 0.4f, fgPaint)
                                QrCornerStyle.CIRCLE -> canvas.drawOval(rect, fgPaint)
                            }
                        } else {
                            when (config.patternStyle) {
                                QrPatternStyle.SQUARE -> canvas.drawRect(rect, fgPaint)
                                QrPatternStyle.ROUNDED -> canvas.drawRoundRect(rect, moduleSizeX * 0.35f, moduleSizeY * 0.35f, fgPaint)
                                QrPatternStyle.DOTS -> {
                                    val inset = moduleSizeX * 0.1f
                                    canvas.drawOval(
                                        RectF(left + inset, top + inset, right - inset, bottom - inset),
                                        fgPaint
                                    )
                                }
                                QrPatternStyle.DIAMOND -> {
                                    val path = android.graphics.Path().apply {
                                        moveTo(left + moduleSizeX / 2, top)
                                        lineTo(right, top + moduleSizeY / 2)
                                        lineTo(left + moduleSizeX / 2, bottom)
                                        lineTo(left, top + moduleSizeY / 2)
                                        close()
                                    }
                                    canvas.drawPath(path, fgPaint)
                                }
                            }
                        }
                    }
                }
            }

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun isCornerEyeModule(x: Int, y: Int, matrixWidth: Int, matrixHeight: Int): Boolean {
        // Top-Left corner (7x7)
        if (x < 7 && y < 7) return true
        // Top-Right corner (7x7)
        if (x >= matrixWidth - 7 && y < 7) return true
        // Bottom-Left corner (7x7)
        if (x < 7 && y >= matrixHeight - 7) return true
        return false
    }
}
