package com.scanflow.qr.core.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
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

            // Center Logo Overlay
            config.logoBitmap?.let { logo ->
                val logoSize = (width * 0.22f).toInt()
                val logoLeft = (width - logoSize) / 2f
                val logoTop = (height - logoSize) / 2f
                val logoRect = RectF(logoLeft, logoTop, logoLeft + logoSize, logoTop + logoSize)

                // 1. Draw rounded background badge behind the logo to clear QR modules
                val badgePadding = (logoSize * 0.08f)
                val badgeRect = RectF(
                    logoRect.left - badgePadding,
                    logoRect.top - badgePadding,
                    logoRect.right + badgePadding,
                    logoRect.bottom + badgePadding
                )
                val badgePaint = Paint().apply {
                    color = config.backgroundColor
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                val cornerRadius = badgePadding * 2.5f
                canvas.drawRoundRect(badgeRect, cornerRadius, cornerRadius, badgePaint)

                // 2. Draw subtle border around badge
                val borderPaint = Paint().apply {
                    color = config.foregroundColor
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                    isAntiAlias = true
                }
                canvas.drawRoundRect(badgeRect, cornerRadius, cornerRadius, borderPaint)

                // 3. Draw scaled logo bitmap
                val scaledLogo = Bitmap.createScaledBitmap(logo, logoSize, logoSize, true)
                canvas.drawBitmap(scaledLogo, logoLeft, logoTop, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
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

            // Center Logo Overlay in SVG
            config.logoBitmap?.let { logo ->
                val logoModuleSize = width * 0.22
                val logoX = (width - logoModuleSize) / 2.0
                val logoY = (height - logoModuleSize) / 2.0
                val pad = logoModuleSize * 0.08
                sb.append("""  <rect x="${logoX - pad}" y="${logoY - pad}" width="${logoModuleSize + pad * 2}" height="${logoModuleSize + pad * 2}" rx="${pad * 2}" ry="${pad * 2}" fill="$bgHex" stroke="$fgHex" stroke-width="0.3"/>""").append("\n")
                try {
                    val stream = java.io.ByteArrayOutputStream()
                    logo.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    val base64 = android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP)
                    sb.append("""  <image x="$logoX" y="$logoY" width="$logoModuleSize" height="$logoModuleSize" href="data:image/png;base64,$base64"/>""").append("\n")
                } catch (_: Exception) {}
            }

            sb.append("""</svg>""").append("\n")
            sb.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun resolveBarcodeFormat(formatName: String?): BarcodeFormat {
        return when (formatName?.uppercase()?.trim()) {
            "EAN_13", "EAN-13" -> BarcodeFormat.EAN_13
            "EAN_8", "EAN-8" -> BarcodeFormat.EAN_8
            "UPC_A", "UPC-A" -> BarcodeFormat.UPC_A
            "UPC_E", "UPC-E" -> BarcodeFormat.UPC_E
            "CODE_39", "CODE-39" -> BarcodeFormat.CODE_39
            "ITF" -> BarcodeFormat.ITF
            "PDF_417", "PDF-417" -> BarcodeFormat.PDF_417
            "DATA_MATRIX" -> BarcodeFormat.DATA_MATRIX
            "AZTEC" -> BarcodeFormat.AZTEC
            else -> BarcodeFormat.CODE_128
        }
    }

    fun sanitizeBarcodeContent(content: String, format: BarcodeFormat): String {
        return when (format) {
            BarcodeFormat.EAN_13 -> {
                val digits = content.filter { it.isDigit() }
                if (digits.length == 12) {
                    digits
                } else if (digits.length >= 13) {
                    val base12 = digits.take(12)
                    var sum = 0
                    for (i in 0 until 12) {
                        val d = base12[i] - '0'
                        sum += if (i % 2 == 0) d else d * 3
                    }
                    val rem = sum % 10
                    val checkDigit = if (rem == 0) 0 else 10 - rem
                    base12 + checkDigit
                } else {
                    digits
                }
            }
            BarcodeFormat.EAN_8 -> {
                val digits = content.filter { it.isDigit() }
                if (digits.length == 7) {
                    digits
                } else if (digits.length >= 8) {
                    val base7 = digits.take(7)
                    var sum = 0
                    for (i in 0 until 7) {
                        val d = base7[i] - '0'
                        sum += if (i % 2 == 0) d * 3 else d
                    }
                    val rem = sum % 10
                    val checkDigit = if (rem == 0) 0 else 10 - rem
                    base7 + checkDigit
                } else {
                    digits
                }
            }
            BarcodeFormat.UPC_A -> {
                val digits = content.filter { it.isDigit() }
                if (digits.length == 11) {
                    digits
                } else if (digits.length >= 12) {
                    val base11 = digits.take(11)
                    var sum = 0
                    for (i in 0 until 11) {
                        val d = base11[i] - '0'
                        sum += if (i % 2 == 0) d * 3 else d
                    }
                    val rem = sum % 10
                    val checkDigit = if (rem == 0) 0 else 10 - rem
                    base11 + checkDigit
                } else {
                    digits
                }
            }
            BarcodeFormat.CODE_39 -> content.uppercase()
            else -> content
        }
    }

    /**
     * Generates a 1D linear barcode bitmap (e.g. Code 128, EAN-13, UPC-A, Code 39).
     */
    fun generateBarcodeBitmap(
        content: String,
        formatName: String = "CODE_128",
        width: Int = 800,
        height: Int = 300,
        foregroundColor: Int = 0xFF000000.toInt(),
        backgroundColor: Int = 0xFFFFFFFF.toInt(),
        margin: Int = 2
    ): Bitmap? {
        if (content.isEmpty()) return null
        return try {
            val format = resolveBarcodeFormat(formatName)
            val cleanContent = sanitizeBarcodeContent(content, format)
            val hints = hashMapOf<EncodeHintType, Any>(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to margin
            )
            val writer = MultiFormatWriter()
            val bitMatrix = writer.encode(cleanContent, format, width, height, hints)

            val realWidth = bitMatrix.width
            val realHeight = bitMatrix.height
            val bitmap = Bitmap.createBitmap(realWidth, realHeight, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(realWidth * realHeight)

            for (y in 0 until realHeight) {
                val offset = y * realWidth
                for (x in 0 until realWidth) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) foregroundColor else backgroundColor
                }
            }
            bitmap.setPixels(pixels, 0, realWidth, 0, 0, realWidth, realHeight)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates a pure SVG 1.1 vector string for 1D Barcode with grouped consecutive bars for optimal file size.
     */
    fun generateBarcodeSvg(
        content: String,
        formatName: String = "CODE_128",
        foregroundColor: Int = 0xFF000000.toInt(),
        backgroundColor: Int = 0xFFFFFFFF.toInt(),
        width: Int = 800,
        height: Int = 300,
        margin: Int = 2
    ): String? {
        if (content.isEmpty()) return null
        return try {
            val format = resolveBarcodeFormat(formatName)
            val cleanContent = sanitizeBarcodeContent(content, format)
            val hints = hashMapOf<EncodeHintType, Any>(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to margin
            )
            val writer = MultiFormatWriter()
            val bitMatrix = writer.encode(cleanContent, format, width, height, hints)
            val realWidth = bitMatrix.width
            val realHeight = bitMatrix.height

            val fgHex = String.format("#%06X", 0xFFFFFF and foregroundColor)
            val bgHex = String.format("#%06X", 0xFFFFFF and backgroundColor)

            val sb = StringBuilder()
            sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append("\n")
            sb.append("""<svg xmlns="http://www.w3.org/2000/svg" version="1.1" viewBox="0 0 $realWidth $realHeight" width="$realWidth" height="$realHeight" shape-rendering="crispEdges">""").append("\n")
            sb.append("""  <rect width="100%" height="100%" fill="$bgHex"/>""").append("\n")
            sb.append("""  <g fill="$fgHex">""").append("\n")

            // Scan columns at the vertical midpoint and group consecutive black modules into rectangles
            var inBar = false
            var barStart = 0
            val midY = realHeight / 2

            for (x in 0 until realWidth) {
                val isBlack = bitMatrix.get(x, midY)
                if (isBlack && !inBar) {
                    inBar = true
                    barStart = x
                } else if (!isBlack && inBar) {
                    inBar = false
                    val barWidth = x - barStart
                    sb.append("""    <rect x="$barStart" y="0" width="$barWidth" height="$realHeight"/>""").append("\n")
                }
            }
            if (inBar) {
                val barWidth = realWidth - barStart
                sb.append("""    <rect x="$barStart" y="0" width="$barWidth" height="$realHeight"/>""").append("\n")
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
