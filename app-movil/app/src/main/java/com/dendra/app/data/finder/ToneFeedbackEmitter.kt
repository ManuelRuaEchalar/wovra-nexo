package com.dendra.app.data.finder

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.dendra.app.domain.port.FeedbackEmitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Señal sensorial de la búsqueda cercana: pitidos (y vibración) cuya cadencia la fija
 * el dominio. Más cerca = pitidos más seguidos; muy cerca = tono continuo.
 *
 * No decide la cadencia: solo la ejecuta (responsabilidad única). Usa un
 * scope propio para el bucle de pitido y lo cancela al parar.
 */
class ToneFeedbackEmitter(context: Context) : FeedbackEmitter {

    private val appContext = context.applicationContext
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 90)
    private val scope = CoroutineScope(Dispatchers.Default)
    private var pulseJob: Job? = null

    @Volatile private var interval = 1_000L
    @Volatile private var continuous = false

    /** Cambia la cadencia sin cortar el ritmo: el bucle la lee en cada vuelta. */
    override fun pulse(intervalMillis: Long, solid: Boolean) {
        interval = intervalMillis.coerceAtLeast(120)
        continuous = solid
        if (pulseJob?.isActive == true) return
        pulseJob = scope.launch {
            while (isActive) {
                if (continuous) {
                    tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 500)
                    vibrate(200)
                    delay(500)
                } else {
                    tone.startTone(ToneGenerator.TONE_PROP_BEEP, 90)
                    vibrate(60)
                    delay(interval)
                }
            }
        }
    }

    override fun stop() {
        pulseJob?.cancel()
        pulseJob = null
        runCatching { tone.stopTone() }
    }

    @Suppress("DEPRECATION")
    private fun vibrate(ms: Long) {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            (appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)
                as VibratorManager).defaultVibrator
        } else {
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(ms)
        }
    }
}
