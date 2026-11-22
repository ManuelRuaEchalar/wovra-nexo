package com.dendra.app.domain.service

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * "¿Hacia dónde?" con un solo teléfono. Bluetooth no da dirección, pero el
 * cuerpo de quien busca sí: tapa la señal cuando le da la espalda al equipo.
 * Al girar sobre uno mismo con el celular al pecho, la señal es más fuerte
 * mirando hacia el equipo buscado.
 *
 * Reparte las lecturas de señal en sectores según el rumbo de la brújula y,
 * cuando ya se barrió buena parte de la vuelta y hay contraste suficiente,
 * estima el rumbo más fuerte. Independiente de Android.
 */
class DirectionFinder(private val sectors: Int = 12) {

    /**
     * @param strength fuerza relativa por sector (0-1; null = aún sin medir),
     *   empezando en el norte y en sentido horario.
     * @param bearing rumbo estimado del equipo en grados (0 = norte), o null
     *   si falta barrer o la señal es pareja en todas las direcciones.
     */
    class Sweep(val strength: List<Float?>, val bearing: Float?, val covered: Int)

    private val level = arrayOfNulls<Double>(sectors)
    private val seenAt = LongArray(sectors)

    fun reset() {
        level.fill(null)
    }

    fun onReading(azimuthDeg: Float, rssiDbm: Int, now: Long): Sweep {
        val width = 360f / sectors
        val index = (((azimuthDeg % 360 + 360) % 360 + width / 2) / width).toInt() % sectors
        level[index] = level[index]?.let { 0.6 * it + 0.4 * rssiDbm } ?: rssiDbm.toDouble()
        seenAt[index] = now
        // Quien busca camina: lo medido hace rato ya no vale para esta posición.
        for (i in 0 until sectors) if (level[i] != null && now - seenAt[i] > STALE_MS) level[i] = null

        val known = level.filterNotNull()
        val low = known.minOrNull() ?: return Sweep(List(sectors) { null }, null, 0)
        val high = known.max()
        val span = high - low
        val strength = level.map { v -> v?.let { if (span < 1) 0.5f else ((it - low) / span).toFloat() } }

        var bearing: Float? = null
        if (known.size >= sectors * 2 / 3 && span >= MIN_CONTRAST_DB) {
            // Media circular ponderada: pesan mucho más los sectores fuertes.
            var x = 0.0
            var y = 0.0
            for (i in 0 until sectors) {
                val v = level[i] ?: continue
                val weight = ((v - low) / span).pow(3)
                val angle = Math.toRadians((i * width).toDouble())
                x += weight * cos(angle)
                y += weight * sin(angle)
            }
            bearing = ((Math.toDegrees(atan2(y, x)) + 360) % 360).toFloat()
        }
        return Sweep(strength, bearing, known.size)
    }

    private companion object {
        const val STALE_MS = 25_000L
        const val MIN_CONTRAST_DB = 6.0
    }
}
