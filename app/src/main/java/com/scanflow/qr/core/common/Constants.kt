package com.scanflow.qr.core.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Constants {
    const val DATABASE_NAME = "scanflow_qr_database"
    const val PREFERENCES_NAME = "scanflow_preferences"
    const val FILE_PROVIDER_AUTHORITY = "com.scanflow.qr.fileprovider"
    
    const val DEFAULT_QR_SIZE = 512
    const val HIGH_RES_QR_SIZE = 1024
    
    // Stitch Design System Colors
    const val COLOR_PRIMARY = 0xFF0052FF.toInt()
    const val COLOR_ACCENT_CYAN = 0xFF00E5FF.toInt()
    const val COLOR_BG_DARK = 0xFF0B1020.toInt()
    const val COLOR_SURFACE_DARK = 0xFF131B2E.toInt()
    const val COLOR_BG_LIGHT = 0xFFF7F9FC.toInt()
    const val COLOR_SURFACE_LIGHT = 0xFFFFFFFF.toInt()
}

fun Long.toFormattedDateString(): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault())
    return formatter.format(Date(this))
}

fun Long.toFormattedDateOnlyString(): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return formatter.format(Date(this))
}
