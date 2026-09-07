package com.scanflow.qr.core.security

import com.scanflow.qr.domain.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLockManager @Inject constructor() {

    private val _isUnlocked = MutableStateFlow<Boolean?>(null)
    val isUnlocked: StateFlow<Boolean?> = _isUnlocked.asStateFlow()

    private var backgroundTimestamp: Long = 0L
    private var isTemporarilyBypassed: Boolean = false

    /**
     * Inisialisasi awal saat aplikasi mulai berjalan.
     * Jika status belum pernah diset, tentukan apakah terkunci berdasarkan konfigurasi settings.
     */
    fun init(settings: AppSettings) {
        if (_isUnlocked.value == null) {
            val isLockConfigured = settings.isAppLockEnabled || settings.isBiometricEnabled
            _isUnlocked.value = !isLockConfigured
        }
    }

    /**
     * Membuka kunci aplikasi setelah otentikasi berhasil (PIN atau Biometrik).
     */
    fun unlock() {
        _isUnlocked.value = true
        backgroundTimestamp = 0L
        isTemporarilyBypassed = false
    }

    /**
     * Mengunci aplikasi secara eksplisit (misalnya saat tombol Test App Lock atau timeout terjadi).
     */
    fun lock() {
        _isUnlocked.value = false
        backgroundTimestamp = 0L
    }

    /**
     * Memperbarui status kunci jika pengguna menonaktifkan App Lock di pengaturan.
     */
    fun onSettingsChanged(settings: AppSettings) {
        val isLockConfigured = settings.isAppLockEnabled || settings.isBiometricEnabled
        if (!isLockConfigured) {
            _isUnlocked.value = true
        }
    }

    /**
     * Menandai bahwa aplikasi meluncurkan intent eksternal (misal pemilih berkas/kamera/share)
     * agar aplikasi tidak terkunci seketika saat kembali.
     */
    fun setTemporarilyBypassed(bypassed: Boolean) {
        isTemporarilyBypassed = bypassed
    }

    /**
     * Dipanggil saat aplikasi berpindah ke background (onStop pada lifecycle).
     */
    fun onAppBackgrounded() {
        if (!isTemporarilyBypassed) {
            backgroundTimestamp = System.currentTimeMillis()
        }
    }

    /**
     * Dipanggil saat aplikasi kembali ke foreground (onStart pada lifecycle).
     * Memeriksa durasi timeout yang telah dikonfigurasi.
     */
    fun onAppForegrounded(settings: AppSettings) {
        val isLockConfigured = settings.isAppLockEnabled || settings.isBiometricEnabled
        if (isTemporarilyBypassed) {
            isTemporarilyBypassed = false
            return
        }

        if (!isLockConfigured) {
            _isUnlocked.value = true
            return
        }

        // Jika sebelumnya sudah unlocked dan ada timestamp saat backgrounded
        if (_isUnlocked.value == true && backgroundTimestamp > 0L) {
            val elapsedSeconds = (System.currentTimeMillis() - backgroundTimestamp) / 1000L
            if (elapsedSeconds >= settings.lockTimeoutSeconds) {
                _isUnlocked.value = false // Re-lock aplikasi
            }
        }
        backgroundTimestamp = 0L
    }
}
