package com.scanflow.qr.core.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfDocumentExporter {

    // Standard A4 dimensions in PostScript points (72 points per inch)
    private const val A4_WIDTH = 595
    private const val A4_HEIGHT = 842

    /**
     * Renders a complete, professional A4 printable document containing the QR code,
     * decorative borders, scanning instructions, and metadata.
     */
    fun createPdfDocument(
        title: String,
        type: String,
        content: String,
        bitmap: Bitmap?
    ): PdfDocument {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // 1. Background Paper
        canvas.drawColor(Color.WHITE)

        // 2. Outer Decorative Frame / Border
        val framePaint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        val innerFrame = RectF(28f, 28f, (A4_WIDTH - 28).toFloat(), (A4_HEIGHT - 28).toFloat())
        canvas.drawRoundRect(innerFrame, 16f, 16f, framePaint)

        // Header Top Accent Band
        val bandPaint = Paint().apply {
            color = Color.parseColor("#0066FF")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val bandRect = RectF(28f, 28f, (A4_WIDTH - 28).toFloat(), 38f)
        canvas.drawRect(bandRect, bandPaint)

        // 3. Header Text
        val headerTitlePaint = Paint().apply {
            color = Color.parseColor("#0A0E1A")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("ScanFlow QR · Lembar Cetak Resmi", (A4_WIDTH / 2).toFloat(), 75f, headerTitlePaint)

        val headerSubPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Dokumen ini siap dicetak langsung dan dipajang untuk pemindaian massal.", (A4_WIDTH / 2).toFloat(), 95f, headerSubPaint)

        // Divider
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(50f, 115f, (A4_WIDTH - 50).toFloat(), 115f, dividerPaint)

        // 4. Document Item Title & Category Badge
        val itemTitlePaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val safeTitle = if (title.isNotBlank()) title else "Kode QR Resmi"
        canvas.drawText(safeTitle, (A4_WIDTH / 2).toFloat(), 150f, itemTitlePaint)

        // Badge pill
        val badgeText = "KATEGORI: ${type.uppercase()}"
        val badgeTextPaint = Paint().apply {
            color = Color.parseColor("#0066FF")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val badgeWidth = badgeTextPaint.measureText(badgeText) + 20f
        val badgeRect = RectF(
            (A4_WIDTH / 2) - (badgeWidth / 2),
            165f,
            (A4_WIDTH / 2) + (badgeWidth / 2),
            185f
        )
        val badgeBgPaint = Paint().apply {
            color = Color.parseColor("#E0E7FF")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(badgeRect, 10f, 10f, badgeBgPaint)
        canvas.drawText(badgeText, (A4_WIDTH / 2).toFloat(), 179f, badgeTextPaint)

        // 5. QR Code Graphic Box with Cutting / Scanning Guide Frame
        val qrBoxSize = 300f
        val qrLeft = (A4_WIDTH - qrBoxSize) / 2
        val qrTop = 210f
        val qrRight = qrLeft + qrBoxSize
        val qrBottom = qrTop + qrBoxSize

        // Subtle shadow/frame behind QR
        val qrFramePaint = Paint().apply {
            color = Color.parseColor("#F8FAFC")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val qrBorderPaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
            isAntiAlias = true
        }
        val qrBoxRect = RectF(qrLeft - 16, qrTop - 16, qrRight + 16, qrBottom + 16)
        canvas.drawRoundRect(qrBoxRect, 12f, 12f, qrFramePaint)
        canvas.drawRoundRect(qrBoxRect, 12f, 12f, qrBorderPaint)

        // Draw the QR Bitmap inside the box if present
        if (bitmap != null) {
            val destRect = Rect(qrLeft.toInt(), qrTop.toInt(), qrRight.toInt(), qrBottom.toInt())
            val bitmapPaint = Paint().apply {
                isFilterBitmap = true
                isAntiAlias = true
            }
            canvas.drawBitmap(bitmap, null, destRect, bitmapPaint)
        }

        // 6. Scan Instruction Box
        val instrBoxTop = qrBottom + 36f
        val instrBoxRect = RectF(50f, instrBoxTop, (A4_WIDTH - 50).toFloat(), instrBoxTop + 65f)
        val instrBgPaint = Paint().apply {
            color = Color.parseColor("#F1F5F9")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(instrBoxRect, 10f, 10f, instrBgPaint)

        val instrTitlePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("📱 CARA PEMINDAIAN", (A4_WIDTH / 2).toFloat(), instrBoxTop + 24f, instrTitlePaint)

        val instrSubPaint = Paint().apply {
            color = Color.parseColor("#475569")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "Buka aplikasi Kamera HP atau pemindai ScanFlow QR, lalu arahkan ke kode di atas.",
            (A4_WIDTH / 2).toFloat(),
            instrBoxTop + 45f,
            instrSubPaint
        )

        // 7. Metadata / Payload details box
        val metaTop = instrBoxTop + 85f
        val metaBoxRect = RectF(50f, metaTop, (A4_WIDTH - 50).toFloat(), metaTop + 65f)
        val metaBorderPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        canvas.drawRoundRect(metaBoxRect, 8f, 8f, metaBorderPaint)

        val metaHeaderPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("KONTEN DATA TERSIMPAN:", 64f, metaTop + 22f, metaHeaderPaint)

        val metaContentPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 10f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            isAntiAlias = true
        }
        val safeContent = if (content.length > 70) content.take(67) + "..." else content
        canvas.drawText(safeContent, 64f, metaTop + 42f, metaContentPaint)

        // 8. Footer
        val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())
        val footerDateStr = "Dicetak pada: ${dateFormat.format(Date())}"
        val footerPaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText(footerDateStr, 50f, (A4_HEIGHT - 45).toFloat(), footerPaint)

        val watermarkPaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("ScanFlow QR Studio · Industri & Pendidikan", (A4_WIDTH - 50).toFloat(), (A4_HEIGHT - 45).toFloat(), watermarkPaint)

        pdfDocument.finishPage(page)
        return pdfDocument
    }

    /**
     * Saves the printable PDF document to device storage (Downloads or Documents).
     */
    fun savePdfToStorage(
        context: Context,
        pdfDocument: PdfDocument,
        filename: String = "ScanFlow_Document_${System.currentTimeMillis()}"
    ): Uri? {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$filename.pdf")
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/ScanFlowQR")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val documentUri = resolver.insert(collectionUri, contentValues) ?: return null

        return try {
            val outputStream: OutputStream? = resolver.openOutputStream(documentUri)
            if (outputStream != null) {
                pdfDocument.writeTo(outputStream)
                outputStream.flush()
                outputStream.close()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(documentUri, contentValues, null, null)
            }

            documentUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            pdfDocument.close()
        }
    }

    /**
     * Saves the printable PDF to the internal cache directory for sharing via FileProvider.
     */
    fun savePdfToCache(
        context: Context,
        pdfDocument: PdfDocument,
        filename: String = "ScanFlow_Doc_${System.currentTimeMillis()}.pdf"
    ): Uri? {
        return try {
            val docsFolder = File(context.cacheDir, "documents")
            docsFolder.mkdirs()
            val file = File(docsFolder, filename)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
