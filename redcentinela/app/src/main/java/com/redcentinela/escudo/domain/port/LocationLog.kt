package com.redcentinela.escudo.domain.port

import com.redcentinela.escudo.domain.model.GeoPoint

/**
 * Bitácora local de ubicaciones, cifrada en el dispositivo. Permite
 * reconstruir la ruta completa aunque el reporte llegue horas después
 * (propuesta: "guardar y reenviar").
 */
interface LocationLog {
    /** Agrega un punto a la bitácora, marcado como pendiente de envío. */
    suspend fun append(point: GeoPoint)

    /** Puntos aún no confirmados como enviados por internet. */
    suspend fun pending(): List<GeoPoint>

    /** Marca como enviados los puntos capturados hasta [capturedAtInclusive]. */
    suspend fun markSentUpTo(capturedAtInclusive: Long)

    /** Toda la bitácora, más reciente primero (para la UI de prueba). */
    suspend fun recent(limit: Int = 100): List<GeoPoint>

    suspend fun pendingCount(): Int

    /** Borra la bitácora (p. ej. al desactivar el Escudo). */
    suspend fun clear()
}
