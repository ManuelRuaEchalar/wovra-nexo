package com.redcentinela.escudo.data.alarm

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Sirena del equipo: suena por el canal de alarma a todo volumen, aunque el
 * teléfono esté en silencio. La usan "hacer sonar" (para hallarlo de cerca)
 * y el aviso del guardián cuando el celular protegido se aleja. Con [flash]
 * además parpadea la linterna, para delatar al equipo en un lugar oscuro o
 * dentro de un bolso.
 */
object Alarm {
    private var player: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var previousVolume: Int? = null

    @Synchronized
    fun ring(context: Context, seconds: Int, flash: Boolean = false) {
        stop(context)
        val app = context.applicationContext
        val audio = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        runCatching {
            previousVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM)
            audio.setStreamVolume(AudioManager.STREAM_ALARM, audio.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)
        }
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        player = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(app, uri)
                isLooping = true
                prepare()
                start()
            }
        }.getOrNull()
        vibrate(app, seconds * 1000L)
        if (flash) strobe(app)
        handler.postDelayed({ stop(app) }, seconds * 1000L)
    }

    @Synchronized
    fun stop(context: Context) {
        handler.removeCallbacksAndMessages(null)
        player?.let { runCatching { it.stop() }; runCatching { it.release() } }
        player = null
        val audio = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        previousVolume?.let { runCatching { audio.setStreamVolume(AudioManager.STREAM_ALARM, it, 0) } }
        previousVolume = null
        runCatching { vibrator(context.applicationContext).cancel() }
        torch(context.applicationContext, false)
        torchCamera = null
    }

    // --- Linterna --------------------------------------------------------------

    private var torchCamera: String? = null
    private var torchOn = false

    /** Parpadeo de la linterna; lo corta [stop] al limpiar el handler. */
    private fun strobe(context: Context) {
        val cameras = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
        torchCamera = runCatching {
            cameras.cameraIdList.firstOrNull {
                cameras.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull() ?: return
        val blink = object : Runnable {
            override fun run() {
                torch(context, !torchOn)
                handler.postDelayed(this, STROBE_MS)
            }
        }
        handler.post(blink)
    }

    private fun torch(context: Context, on: Boolean) {
        val id = torchCamera ?: return
        val cameras = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
        // No pide permiso de cámara; falla si otra app la está usando.
        runCatching { cameras.setTorchMode(id, on) }
        torchOn = on
    }

    private const val STROBE_MS = 180L

    private fun vibrate(context: Context, millis: Long) = runCatching {
        val pattern = longArrayOf(0, 400, 250)
        vibrator(context).vibrate(VibrationEffect.createWaveform(pattern, 0))
        handler.postDelayed({ runCatching { vibrator(context).cancel() } }, millis)
    }

    @Suppress("DEPRECATION")
    private fun vibrator(context: Context): Vibrator =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
}
