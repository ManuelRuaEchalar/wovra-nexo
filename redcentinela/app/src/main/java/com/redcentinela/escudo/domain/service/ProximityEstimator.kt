package com.redcentinela.escudo.domain.service

import com.redcentinela.escudo.domain.model.ProximityBand
import com.redcentinela.escudo.domain.model.ProximityReading
import com.redcentinela.escudo.domain.model.ProximityTrend
import kotlin.math.pow

/**
 * Convierte RSSI crudo en una lectura de proximidad interpretable, y decide
 * la cadencia de la señal sonora de la búsqueda cercana.
 *
 * El RSSI es ruidoso, y casi todo su ruido es hacia abajo: el cuerpo, una
 * pared o un rebote le quitan señal, nada se la suma. Por eso no se promedia
 * sin más: de las lecturas del último segundo y medio se toma una alta (el
 * percentil 75), que descarta las caídas, y recién eso se suaviza. La
 * tendencia compara contra el valor de hace un par de segundos, no contra la
 * lectura anterior, para no confundir ruido con movimiento.
 *
 * Es independiente de Android: trabaja solo con enteros y el reloj se inyecta.
 */
class ProximityEstimator(
    /** RSSI esperado a 1 metro (calibración típica de un teléfono). */
    private val rssiAtOneMeter: Int = -59,
    /** Exponente de pérdida de trayectoria (2 = espacio libre, 2.7-3 interiores). */
    private val pathLossExponent: Double = 2.5,
    /** Peso del suavizado exponencial (0-1); menor = más suave. */
    private val smoothing: Double = 0.45
) {
    private val window = ArrayDeque<Pair<Long, Int>>()
    private val history = ArrayDeque<Pair<Long, Double>>()
    private var smoothedRssi: Double? = null

    /** Reinicia el estado (al empezar una nueva búsqueda). */
    fun reset() {
        window.clear()
        history.clear()
        smoothedRssi = null
    }

    fun onRssi(rssiDbm: Int, now: Long): ProximityReading {
        window.addLast(now to rssiDbm)
        while (window.size > 1 && now - window.first().first > WINDOW_MS) window.removeFirst()
        val sorted = window.map { it.second }.sorted()
        val robust = sorted[((sorted.size - 1) * 0.75).toInt()].toDouble()

        val previous = smoothedRssi
        val smoothed = if (previous == null) robust else smoothing * robust + (1 - smoothing) * previous
        smoothedRssi = smoothed

        history.addLast(now to smoothed)
        while (history.size > 1 && now - history.first().first > TREND_MS) history.removeFirst()
        val before = history.first().takeIf { now - it.first >= TREND_MS / 2 }?.second

        return ProximityReading(
            rssiDbm = smoothed.toInt(),
            estimatedMeters = metersFor(smoothed, rssiAtOneMeter, pathLossExponent),
            band = bandFor(smoothed),
            trend = trendFor(before, smoothed),
            capturedAt = now
        )
    }

    private fun bandFor(rssi: Double): ProximityBand = when {
        rssi >= -55 -> ProximityBand.MUY_CERCA
        rssi >= -67 -> ProximityBand.CERCA
        rssi >= -80 -> ProximityBand.MEDIO
        else -> ProximityBand.LEJOS
    }

    private fun trendFor(before: Double?, current: Double): ProximityTrend {
        if (before == null) return ProximityTrend.ESTABLE
        val delta = current - before
        return when {
            delta > 3.0 -> ProximityTrend.ACERCANDOSE   // RSSI sube = más cerca
            delta < -3.0 -> ProximityTrend.ALEJANDOSE
            else -> ProximityTrend.ESTABLE
        }
    }

    companion object {
        private const val WINDOW_MS = 1_500L
        private const val TREND_MS = 2_500L

        /** Modelo log-distance: d = 10 ^ ((RSSI@1m - RSSI) / (10 * n)). */
        fun metersFor(rssi: Double, rssiAtOneMeter: Int = -59, pathLossExponent: Double = 2.5): Double {
            val exponent = (rssiAtOneMeter - rssi) / (10.0 * pathLossExponent)
            return 10.0.pow(exponent).coerceIn(0.1, 100.0)
        }

        /**
         * Cadencia del pitido para una señal dada, como un contador Geiger:
         * cambia de forma continua, así cada paso se nota. Devuelve
         * (intervaloMs, continuo); muy cerca = tono continuo.
         */
        fun feedbackFor(rssiDbm: Int): Pair<Long, Boolean> {
            if (rssiDbm >= -52) return 0L to true
            val t = ((rssiDbm + 92) / 40.0).coerceIn(0.0, 1.0)   // -92 dBm = 0, -52 dBm = 1
            return (1_400 - 1_260 * t).toLong() to false
        }
    }
}
