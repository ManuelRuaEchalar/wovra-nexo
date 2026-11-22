package com.dendra.app.domain.model

/**
 * Qué tan cerca estamos del equipo buscado, derivado del RSSI.
 * Es la base de la búsqueda por Bluetooth: la banda indica "caliente/frío".
 */
enum class ProximityBand { MUY_CERCA, CERCA, MEDIO, LEJOS, SIN_SENAL }

/**
 * Lectura de proximidad ya interpretada (no el RSSI crudo).
 */
data class ProximityReading(
    /** RSSI suavizado en dBm (valores típicos -30 muy cerca, -100 lejos). */
    val rssiDbm: Int,
    /** Distancia aproximada en metros (estimación, no exacta). */
    val estimatedMeters: Double,
    val band: ProximityBand,
    /** Tendencia respecto a la lectura anterior: nos acercamos o alejamos. */
    val trend: ProximityTrend,
    val capturedAt: Long
)

enum class ProximityTrend { ACERCANDOSE, ALEJANDOSE, ESTABLE }

/** Presencia Bluetooth de un equipo del círculo (guardián). */
data class Presence(
    val near: Boolean,
    val meters: Double?,
    val lastSeen: Long
)
