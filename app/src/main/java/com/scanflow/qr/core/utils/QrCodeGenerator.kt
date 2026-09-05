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

    /**
     * Generates a pure W3C SVG 1.1 vector string from the QR code data and styling configuration.
     * SVG can be scaled infinitely without rasterization loss, suitable for print banners and CAD/vector software.
     */
    fun generateQrSvg(
        content: String,
        config: QrStyleConfig = QrStyleConfig()
    ): String? {
        if (content.isEmpty()) return null

        return try {
            val hints = hashMapOf<EncodeHintType, Any>(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
                EncodeHintType.MARGIN to config.margin
            )

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)

            val width = bitMatrix.width
            val height = bitMatrix.height

            val fgHex = String.format("#%06X", 0xFFFFFF and config.foregroundColor)
            val bgHex = String.format("#%06X", 0xFFFFFF and config.backgroundColor)

            val sb = StringBuilder()
            sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append("\n")
            sb.append("""<svg xmlns="http://www.w3.org/2000/svg" version="1.1" viewBox="0 0 $width $height" width="1000" height="1000" shape-rendering="crispEdges">""").append("\n")
            sb.append("""  <rect width="100%" height="100%" fill="$bgHex"/>""").append("\n")
            sb.append("""  <g fill="$fgHex">""").append("\n")

            for (x in 0 until width) {
                for (y in 0 until height) {
                    if (bitMatrix.get(x, y)) {
                        val isCorner = isCornerEyeModule(x, y, width, height)
                        if (isCorner) {
                            when (config.cornerEyeStyle) {
                                QrCornerStyle.ROUNDED -> {
                                    sb.append("""    <rect x="$x" y="$y" width="1" height="1" rx="0.35" ry="0.35"/>""").append("\n")
                                }
                                QrCornerStyle.CIRCLE -> {
                                    sb.append("""    <circle cx="${x + 0.5}" cy="${y + 0.5}" r="0.5"/>""").append("\n")
                                }
                                QrCornerStyle.SQUARE -> {
                                    sb.append("""    <rect x="$x" y="$y" width="1" height="1"/>""").append("\n")
                                }
                            }
                        } else {
                            when (config.patternStyle) {
                                QrPatternStyle.ROUNDED -> {
                                    sb.append("""    <rect x="$x" y="$y" width="1" height="1" rx="0.35" ry="0.35"/>""").append("\n")
                                }
                                QrPatternStyle.DOTS -> {
                                    sb.append("""    <circle cx="${x + 0.5}" cy="${y + 0.5}" r="0.4"/>""").append("\n")
                                }
                                QrPatternStyle.DIAMOND -> {
                                    val topX = x + 0.5
                                    val topY = y.toDouble()
                                    val rightX = x + 1.0
                                    val rightY = y + 0.5
                                    val botX = x + 0.5
                                    val botY = y + 1.0
                                    val leftX = x.toDouble()
                                    val leftY = y + 0.5
                                    sb.append("""    <polygon points="$topX,$topY $rightX,$rightY $botX,$botY $leftX,$leftY"/>""").append("\n")
                                }
                                QrPatternStyle.SQUARE -> {
                                    sb.append("""    <rect x="$x" y="$y" width="1" height="1"/>""").append("\n")
                                }
                            }
                        }
                    }
                }
            }

            sb.append("""  </g>""").append("\n")
            sb.append("""</svg>""").append("\n")
            sb.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
