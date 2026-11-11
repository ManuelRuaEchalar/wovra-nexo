package com.redcentinela.escudo.domain.model

/**
 * Estado del Escudo. Modela el ciclo de vida descrito en la propuesta:
 * el equipo pasa de reposo a vigilancia y, ante un reporte, a alerta.
 */
enum class ShieldStatus {
    /** Escudo apagado: no registra ni vigila. */
    INACTIVO,

    /** Vigilando en segundo plano: registra ubicación con baja frecuencia
     *  y escucha señales sospechosas. Es el estado normal del día a día. */
    ARMADO,

    /** El equipo fue reportado como robado: registro intensivo y cascada
     *  de comunicación activa. */
    ALERTA_ROBO,

    /** El equipo fue reportado como perdido: modo encontrado, sin cámara
     *  ni acciones agresivas. */
    ALERTA_PERDIDA;

    val isAlert: Boolean get() = this == ALERTA_ROBO || this == ALERTA_PERDIDA
}

/**
 * Motivo por el que el Escudo entró en alerta. Permite diferenciar robo de
 * pérdida y registrar qué disparó la reacción.
 */
enum class AlertReason {
    ARREBATO,
    ROBO_DISCRETO,
    PERDIDA,
    INTENTO_APAGADO,
    CHIP_RETIRADO,
    /** Bloqueado, sin ningún canal y lejos del guardián: entró en alerta solo. */
    AISLADO,
    REPORTE_MANUAL
}

/**
 * Instantánea del estado del Escudo para presentar a la UI.
 */
data class ShieldState(
    val status: ShieldStatus = ShieldStatus.INACTIVO,
    val reason: AlertReason? = null,
    val lastPoint: GeoPoint? = null,
    val pendingToSend: Int = 0,
    /** Momento (epoch millis) en que entró en alerta. */
    val alertSince: Long? = null,
    /** Canal por el que salió el último envío de la cascada. */
    val lastChannel: CommChannel? = null,
    val lastSentAt: Long? = null,
    /** Quién lo reportó (nombre de alguien del círculo), si aplica. */
    val reportedBy: String? = null
)
