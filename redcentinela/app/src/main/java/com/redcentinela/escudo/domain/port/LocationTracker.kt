package com.redcentinela.escudo.domain.port

import com.redcentinela.escudo.domain.model.GeoPoint
import kotlinx.coroutines.flow.Flow

/**
 * Fuente de ubicaciones del equipo. Abstrae de dónde vienen los puntos
 * (GPS, fused provider, etc.) para que el dominio no dependa de las APIs
 * de Android.
 *
 * El intervalo es un pedido, no una garantía: el proveedor puede entregar
 * puntos con mayor o menor frecuencia según disponibilidad.
 */
interface LocationTracker {
    /**
     * Emite ubicaciones mientras se colecte el flujo, pidiendo una cada
     * [intervalMillis] aprox. Cerrar la colección detiene las actualizaciones.
     */
    fun stream(intervalMillis: Long): Flow<GeoPoint>

    /** Intenta obtener la última ubicación conocida al instante (puede ser nula). */
    suspend fun lastKnown(): GeoPoint?
}
