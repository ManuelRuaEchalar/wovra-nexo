package com.dendra.app.domain.model

/**
 * Persona del círculo. Al vincularse, cada teléfono entrega su [id] público
 * y su [secret]: con el secreto, el círculo descifra la ubicación de ese
 * equipo y puede darle órdenes (reportarlo, hacerlo sonar). Nadie más puede.
 */
data class CircleMember(
    val id: String,
    val name: String,
    val secret: String,
    val phone: String? = null,
    /** Esta persona va conmigo: su celular es MI guardián. */
    val isGuardian: Boolean = false,
    /** Yo soy guardián de su celular: aviso si se aleja del mío. */
    val iWatch: Boolean = false
) {
    val initial: String get() = name.trim().take(1).uppercase().ifEmpty { "?" }
}

/** Órdenes que el círculo puede dar a un equipo. */
enum class RemoteCommand(val code: String) {
    ROBO("ROBO"),
    PERDIDA("PERD"),
    RECUPERADO("OK"),
    SONAR("RING"),
    GUARDIAN_ON("GON"),
    GUARDIAN_OFF("GOFF"),
    /** Quien busca de cerca pide que el aviso Bluetooth salga a máxima potencia y cadencia. */
    BUSCAR("SEEK");

    companion object {
        fun fromCode(code: String) = entries.firstOrNull { it.code == code }
    }
}

/** Un punto de la ruta de otro equipo, con el canal por el que llegó. */
data class TrackPoint(
    val point: GeoPoint,
    val via: CommChannel,
    /** Si un teléfono de la red lo guardó y lo entregó tarde: cuándo llegó. */
    val deliveredAt: Long? = null
)

/** Entrada del registro (bitácora de eventos del Escudo). */
data class EventEntry(
    val at: Long,
    val title: String,
    val detail: String,
    /** true = señal sospechosa (se pinta en ámbar). */
    val alert: Boolean = false
)

/** Lo que este teléfono sabe del equipo de alguien de su círculo. */
data class RemoteState(
    val memberId: String,
    val status: ShieldStatus? = null,
    val reason: AlertReason? = null,
    val alertSince: Long? = null,
    val statusAt: Long = 0,
    val batteryPct: Int? = null,
    val points: List<TrackPoint> = emptyList(),
    val events: List<EventEntry> = emptyList(),
    /** Momento en que alguien avisó que lo encontró (modo encontrado). */
    val foundAt: Long? = null
) {
    val last: TrackPoint? get() = points.lastOrNull()
    val inAlert: Boolean get() = status?.isAlert == true
}
