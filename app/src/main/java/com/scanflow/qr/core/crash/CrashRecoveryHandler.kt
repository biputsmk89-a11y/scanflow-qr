package com.scanflow.qr.core.crash

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.util.Log
import com.scanflow.qr.BuildConfig
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

class CrashRecoveryHandler(
    private val applicationContext: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val stringWriter = StringWriter()
            throwable.printStackTrace(PrintWriter(stringWriter))
            val stackTrace = stringWriter.toString()

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val crashReport = buildString {
                appendLine("=== SCANFLOW QR CRASH REPORT ===")
                appendLine("Waktu: $timestamp")
                appendLine("Aplikasi: ScanFlow QR v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                appendLine("Perangkat: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
                appendLine("Versi Android: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                appendLine("Thread: ${thread.name} (id=${thread.id})")
                appendLine("Tipe Error: ${throwable.javaClass.name}")
                appendLine("Pesan: ${throwable.localizedMessage ?: throwable.message ?: "Tidak ada pesan"}")
                appendLine()
                appendLine("--- STACK TRACE ---")
                appendLine(stackTrace)
            }

            Log.e("CrashRecoveryHandler", "Intercepted uncaught exception:\n$crashReport")

            val intent = Intent(applicationContext, CrashRecoveryActivity::class.java).apply {
                putExtra(CrashRecoveryActivity.EXTRA_CRASH_REPORT, crashReport)
                putExtra(CrashRecoveryActivity.EXTRA_ERROR_MESSAGE, throwable.localizedMessage ?: throwable.message ?: "Kesalahan tak terduga")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            applicationContext.startActivity(intent)

            Process.killProcess(Process.myPid())
            exitProcess(10)
        } catch (e: Exception) {
            Log.e("CrashRecoveryHandler", "Failed to launch crash recovery activity", e)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        fun install(context: Context) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(
                CrashRecoveryHandler(context.applicationContext, defaultHandler)
            )
        }
    }
}
