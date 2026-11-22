package com.dendra.app.domain.model

/**
 * Canal físico por el que puede salir un mensaje, en orden de preferencia
 * de la cascada de la Capa 1 (propuesta).
 */
enum class CommChannel(val label: String) {
    INTERNET("Internet"),
    SMS("SMS"),
    BLUETOOTH("Bluetooth"),
    LOCAL("Memoria local")
}

/** Lo que el Escudo puede necesitar sacar por la cascada. */
sealed interface Outbound {
    /**
     * Estado del equipo y, en alerta, su ubicación. [backlog] es la bitácora
     * pendiente: solo internet puede llevarla completa ("la ruta entera").
     */
    data class Report(
        val status: ShieldStatus,
        val reason: AlertReason?,
        val alertSince: Long?,
        val point: GeoPoint?,
        val batteryPct: Int,
        val backlog: List<GeoPoint> = emptyList()
    ) : Outbound {
        val inAlert get() = status.isAlert
    }

    /** Orden para el celular de alguien de mi círculo (reportar, sonar…). */
    data class Command(val target: CircleMember, val command: RemoteCommand) : Outbound

    /** Entrada del registro del robo (señal sospechosa, separación…). */
    data class Event(val entry: EventEntry) : Outbound

    /** Quien encontró el celular perdido tocó "avisar al dueño". */
    data class Found(val point: GeoPoint?, val at: Long) : Outbound
}

/** Resultado de un intento de envío por la cascada. */
data class CommResult(val delivered: Boolean, val channel: CommChannel?)
