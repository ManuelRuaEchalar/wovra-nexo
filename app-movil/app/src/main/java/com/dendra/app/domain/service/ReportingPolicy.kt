package com.dendra.app.domain.service

import com.dendra.app.domain.model.ShieldStatus

/**
 * Decide con qué frecuencia el Escudo registra ubicación. Codifica la tabla
 * de la propuesta: muy frecuente al inicio de una alerta, más espaciado con
 * el tiempo, y mínimo cuando la batería está crítica.
 *
 * Es una función pura (sin estado ni dependencias de Android): recibe el
 * contexto y devuelve un intervalo. Fácil de probar y de ajustar.
 */
object ReportingPolicy {

    const val BATERIA_CRITICA_PCT = 10

    // Intervalos en milisegundos (ver tabla "Registro de ubicación").
    private const val ARMADO = 10 * 60_000L          // vigilancia diaria: 10 min
    private const val ALERTA_PRIMEROS_15MIN = 20_000L // cada 15-30 s -> usamos 20 s
    private const val ALERTA_PRIMERA_HORA = 2 * 60_000L
    private const val ALERTA_DESPUES = 12 * 60_000L   // 10-15 min
    private const val BATERIA_BAJA = 15 * 60_000L     // modo mínimo

    private const val QUINCE_MIN = 15 * 60_000L
    private const val UNA_HORA = 60 * 60_000L

    /**
     * @param status estado actual del Escudo.
     * @param millisSinceAlert tiempo transcurrido desde que entró en alerta
     *        (ignorado si no está en alerta).
     * @param batteryPct nivel de batería 0-100.
     */
    fun intervalMillis(
        status: ShieldStatus,
        millisSinceAlert: Long,
        batteryPct: Int
    ): Long {
        if (status == ShieldStatus.INACTIVO) return Long.MAX_VALUE
        if (batteryPct in 0..BATERIA_CRITICA_PCT) return BATERIA_BAJA

        val enAlerta = status == ShieldStatus.ALERTA_ROBO || status == ShieldStatus.ALERTA_PERDIDA
        if (!enAlerta) return ARMADO

        return when {
            millisSinceAlert <= QUINCE_MIN -> ALERTA_PRIMEROS_15MIN
            millisSinceAlert <= UNA_HORA -> ALERTA_PRIMERA_HORA
            else -> ALERTA_DESPUES
        }
    }

    fun esBateriaCritica(batteryPct: Int): Boolean = batteryPct in 0..BATERIA_CRITICA_PCT
}
