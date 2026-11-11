package com.redcentinela.escudo.domain.model

/**
 * Fuente de la que proviene una ubicación. La propuesta prioriza GPS y
 * recurre a WiFi/antena cuando el GPS no está disponible (interiores,
 * arranque en frío sin datos).
 */
enum class LocationSource { GPS, WIFI, CELL, FUSED, DESCONOCIDA }

/**
 * Un punto de ubicación con la metadata mínima necesaria para reconstruir
 * la ruta de un equipo, con o sin internet.
 *
 * Es un dato inmutable (value object): no contiene lógica de Android, por lo
 * que el dominio puede razonar sobre ubicaciones sin depender del framework.
 */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    /** Precisión estimada en metros (menor es mejor). */
    val accuracyMeters: Float,
    val source: LocationSource,
    /** Velocidad en m/s, si el proveedor la entrega. */
    val speedMps: Float? = null,
    /** Rumbo en grados (0-360), si el proveedor lo entrega. */
    val bearingDeg: Float? = null,
    /** Momento de captura, epoch millis. */
    val capturedAt: Long
)
