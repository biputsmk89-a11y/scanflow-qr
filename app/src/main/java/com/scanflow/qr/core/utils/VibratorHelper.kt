package com.scanflow.qr.core.utils

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object VibratorHelper {

    fun vibrateSuccess(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(70)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun vibrateWarning(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 60, 50), -1))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 60, 50), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 50, 60, 50), -1)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

object SoundHelper {
    private val lock = Any()

    @Volatile
    private var toneGenerator: ToneGenerator? = null

    @Volatile
    var isEnabled: Boolean = true

    /**
     * Memutar nada bip pemindaian menggunakan ToneGenerator singleton terkelola.
     * Mencegah kebocoran AudioTrack native dan memory leak dengan mereuse instance yang sama.
     */
    fun playBeep() {
        if (!isEnabled) return
        synchronized(lock) {
            try {
                var generator = toneGenerator
                if (generator == null) {
                    generator = createToneGenerator()
                    toneGenerator = generator
                }
                generator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
            } catch (e: Exception) {
                // Self-healing jika native audio server mereset atau track invalid
                try {
                    toneGenerator?.release()
                } catch (_: Exception) {}
                try {
                    val fallbackGen = createToneGenerator()
                    toneGenerator = fallbackGen
                    fallbackGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                } catch (retryEx: Exception) {
                    retryEx.printStackTrace()
                    toneGenerator = null
                }
            }
        }
    }

    /**
     * Melepaskan resource ToneGenerator secara tuntas saat aplikasi di latar belakang
     * atau saat memori rendah.
     */
    fun release() {
        synchronized(lock) {
            try {
                toneGenerator?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                toneGenerator = null
            }
        }
    }

    fun hasActiveInstance(): Boolean = toneGenerator != null

    private fun createToneGenerator(): ToneGenerator? {
        return try {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

