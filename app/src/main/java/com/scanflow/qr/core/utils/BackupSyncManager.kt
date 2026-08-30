package com.scanflow.qr.core.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.scanflow.qr.core.database.ScanFlowDatabase
import com.scanflow.qr.data.local.entity.QrCodeEntity
import com.scanflow.qr.data.local.entity.ScanHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupSyncManager {

    suspend fun exportBackup(
        context: Context,
        database: ScanFlowDatabase
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val allScans = database.scanHistoryDao().getAllHistory().first()
            val allQrs = database.qrCodeDao().getAllUserQrs().first()

            val rootJson = JSONObject()
            rootJson.put("app", "ScanFlow QR")
            rootJson.put("version", 1)
            rootJson.put("exportedAt", System.currentTimeMillis())

            // Scans Array
            val scansArray = JSONArray()
            for (scan in allScans) {
                val scanObj = JSONObject().apply {
                    put("type", scan.type)
                    put("format", scan.format)
                    put("title", scan.title)
                    put("content", scan.content)
                    put("isFavorite", scan.isFavorite)
                    put("isSecure", scan.isSecure)
                    put("safetyWarning", scan.safetyWarning ?: "")
                    put("createdAt", scan.createdAt)
                }
                scansArray.put(scanObj)
            }
            rootJson.put("scans", scansArray)

            // User QRs Array
            val qrsArray = JSONArray()
            for (qr in allQrs) {
                val qrObj = JSONObject().apply {
                    put("type", qr.type)
                    put("title", qr.title)
                    put("content", qr.content)
                    put("foregroundColor", qr.foregroundColor)
                    put("backgroundColor", qr.backgroundColor)
                    put("patternStyle", qr.patternStyle)
                    put("eyeStyle", qr.eyeStyle)
                    put("logoPath", qr.logoPath ?: "")
                    put("isFavorite", qr.isFavorite)
                    put("scanCount", qr.scanCount)
                    put("shareCount", qr.shareCount)
                    put("downloadCount", qr.downloadCount)
                    put("createdAt", qr.createdAt)
                    put("updatedAt", qr.updatedAt)
                }
                qrsArray.put(qrObj)
            }
            rootJson.put("userQrs", qrsArray)

            // Write to Cache File
            val backupDir = File(context.cacheDir, "backups").apply { if (!exists()) mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(backupDir, "scanflow_backup_$timeStamp.json")
            backupFile.writeText(rootJson.toString(2))

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, backupFile)

            // Launch Android Share / Save to Drive Intent
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "ScanFlow QR Backup ($timeStamp)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, "Save Backup to Cloud / Drive / Files").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)

            uri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun importBackup(
        context: Context,
        uri: Uri,
        database: ScanFlowDatabase
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        var importedScans = 0
        var importedQrs = 0

        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext 0 to 0
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonText = reader.use { it.readText() }

            val rootJson = JSONObject(jsonText)
            if (!rootJson.has("scans") && !rootJson.has("userQrs")) {
                return@withContext 0 to 0
            }

            // Restore Scans
            if (rootJson.has("scans")) {
                val scansArray = rootJson.getJSONArray("scans")
                for (i in 0 until scansArray.length()) {
                    val obj = scansArray.getJSONObject(i)
                    val scan = ScanHistoryEntity(
                        id = 0, // Auto-generate new primary key
                        type = obj.optString("type", "TEXT"),
                        format = obj.optString("format", "QR_CODE"),
                        title = obj.optString("title", "Scan Item"),
                        content = obj.optString("content", ""),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        isFavorite = obj.optBoolean("isFavorite", false),
                        isSecure = obj.optBoolean("isSecure", true),
                        safetyWarning = if (obj.has("safetyWarning") && obj.getString("safetyWarning").isNotEmpty()) obj.getString("safetyWarning") else null
                    )
                    database.scanHistoryDao().insertScan(scan)
                    importedScans++
                }
            }

            // Restore User QRs
            if (rootJson.has("userQrs")) {
                val qrsArray = rootJson.getJSONArray("userQrs")
                for (i in 0 until qrsArray.length()) {
                    val obj = qrsArray.getJSONObject(i)
                    val qr = QrCodeEntity(
                        id = 0, // Auto-generate new primary key
                        type = obj.optString("type", "TEXT"),
                        title = obj.optString("title", "My QR"),
                        content = obj.optString("content", ""),
                        foregroundColor = obj.optInt("foregroundColor", -16777216),
                        backgroundColor = obj.optInt("backgroundColor", -1),
                        patternStyle = obj.optString("patternStyle", "SQUARE"),
                        eyeStyle = obj.optString("eyeStyle", "SQUARE"),
                        logoPath = if (obj.has("logoPath") && obj.getString("logoPath").isNotEmpty()) obj.getString("logoPath") else null,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                        isFavorite = obj.optBoolean("isFavorite", false),
                        scanCount = obj.optInt("scanCount", 0),
                        shareCount = obj.optInt("shareCount", 0),
                        downloadCount = obj.optInt("downloadCount", 0)
                    )
                    database.qrCodeDao().insertQr(qr)
                    importedQrs++
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        importedScans to importedQrs
    }
}
