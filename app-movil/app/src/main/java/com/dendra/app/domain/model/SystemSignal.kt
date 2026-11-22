package com.dendra.app.domain.model

/**
 * Señal del sistema que el Escudo vigila. La propuesta lista varias como
 * "sospechosas" cuando ocurren con el equipo bloqueado: apagado, modo avión,
 * retiro de chip, batería crítica.
 */
enum class SystemSignal {
    INTENTO_APAGADO,
    REINICIO,
    MODO_AVION,
    CHIP_RETIRADO,
    CHIP_CAMBIADO,
    BATERIA_CRITICA,
    DESBLOQUEO_FALLIDO,
    /** Mantuvieron el botón de encendido con el equipo bloqueado. */
    APAGADO_FALSO
}
