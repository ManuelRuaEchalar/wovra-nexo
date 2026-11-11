package com.redcentinela.escudo.domain.service

import com.redcentinela.escudo.data.storage.EventLog
import com.redcentinela.escudo.data.storage.ShieldStore
import com.redcentinela.escudo.domain.model.AlertReason
import com.redcentinela.escudo.domain.model.CommChannel
import com.redcentinela.escudo.domain.model.EventEntry
import com.redcentinela.escudo.domain.model.GeoPoint
import com.redcentinela.escudo.domain.model.Outbound
import com.redcentinela.escudo.domain.model.ShieldState
import com.redcentinela.escudo.domain.model.ShieldStatus
import com.redcentinela.escudo.domain.model.SystemSignal
import com.redcentinela.escudo.domain.port.BatteryGauge
import com.redcentinela.escudo.domain.port.LocationLog
import com.redcentinela.escudo.domain.port.LocationTracker
import com.redcentinela.escudo.domain.port.SystemSignalSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.coroutineContext

/**
 * Cerebro del Escudo. Coordina el registro de ubicación, la cascada de
 * comunicación y la reacción a señales del sistema.
 *
 * El servicio de primer plano le inyecta las implementaciones concretas y le
 * presta un CoroutineScope. Toda la lógica de decisión queda en un punto.
 */
class EscudoCoordinator(
    private val tracker: LocationTracker,
    private val log: LocationLog,
    private val cascade: CommCascade,
    private val signals: SystemSignalSource,
    private val battery: BatteryGauge,
    private val store: ShieldStore,
    private val events: EventLog,
    /** ¿El equipo está bloqueado ahora? Lo que pasa sin desbloquear es sospechoso. */
    private val isLocked: () -> Boolean,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val _state = MutableStateFlow(ShieldState())
    val state: StateFlow<ShieldState> = _state.asStateFlow()

    private var scope: CoroutineScope? = null
    private var locationJob: Job? = null
    private var signalJob: Job? = null

    /** Despierta el bucle de ubicación cuando cambia el estado. */
    private val kick = MutableStateFlow(0)
    private val sendLock = Mutex()

    /** Arma el Escudo. Si el equipo estaba en alerta (p. ej. lo reiniciaron), la retoma. */
    @Synchronized
    fun arm(scope: CoroutineScope) {
        if (locationJob?.isActive == true) return
        this.scope = scope
        val saved = store.load()
        val resumed = saved.status.isAlert
        _state.value = ShieldState(
            status = if (resumed) saved.status else ShieldStatus.ARMADO,
            reason = if (resumed) saved.reason else null,
            alertSince = if (resumed) saved.alertSince else null,
            reportedBy = if (resumed) saved.reportedBy else null
        )
        persist()
        locationJob = scope.launch { locationLoop() }
        signalJob = scope.launch { signals.signals().collect { onSignal(it) } }
        scope.launch {
            _state.update { it.copy(pendingToSend = log.pendingCount(), lastPoint = it.lastPoint ?: log.recent(1).firstOrNull()) }
            announce()
        }
    }

    /** Reporta robo: escala a alerta intensiva y activa la cascada. */
    fun reportStolen(reason: AlertReason = AlertReason.REPORTE_MANUAL, by: String? = null) {
        val current = _state.value.status
        if (current == ShieldStatus.INACTIVO || current == ShieldStatus.ALERTA_ROBO) return
        enterAlert(ShieldStatus.ALERTA_ROBO, reason, by)
        note(
            "Reportado como robado",
            by?.let { "Desde el celular de $it" } ?: reasonText(reason),
            alert = true
        )
    }

    /** Reporta pérdida: modo encontrado, sin acciones agresivas. */
    fun reportLost(by: String? = null) {
        val current = _state.value.status
        if (current == ShieldStatus.INACTIVO || current == ShieldStatus.ALERTA_PERDIDA) return
        enterAlert(ShieldStatus.ALERTA_PERDIDA, AlertReason.PERDIDA, by)
        note("Marcado como perdido", by?.let { "Desde el celular de $it" } ?: "Modo encontrado activo")
    }

    /** Vuelve a la vigilancia normal tras resolver una alerta. */
    fun standDown(by: String? = null) {
        if (!_state.value.status.isAlert) return
        _state.update { it.copy(status = ShieldStatus.ARMADO, reason = null, alertSince = null, reportedBy = null) }
        persist()
        note("Recuperado", by?.let { "Lo confirmó $it" } ?: "Vuelve a la vigilancia normal")
        kick.update { it + 1 }
        scope?.launch { announce() }
    }

    /** Desarma el Escudo por completo. */
    @Synchronized
    fun disarm() {
        val wasArmed = _state.value.status != ShieldStatus.INACTIVO
        locationJob?.cancel()
        signalJob?.cancel()
        locationJob = null
        signalJob = null
        _state.value = ShieldState(status = ShieldStatus.INACTIVO)
        persist()
        if (wasArmed) scope?.launch { announce() }
    }

    /** Quien encontró el celular perdido tocó "avisar al dueño". */
    fun notifyFound() {
        note("Alguien avisó que lo encontró", "Desde la pantalla de celular perdido")
        scope?.launch { sendLock.withLock { cascade.dispatch(Outbound.Found(_state.value.lastPoint, clock())) } }
    }

    /** Anota algo en el registro; en alerta, también lo comparte con el círculo. */
    fun note(title: String, detail: String, alert: Boolean = false) {
        val entry = EventEntry(clock(), title, detail, alert)
        events.add(entry)
        if (_state.value.status.isAlert) {
            scope?.launch { cascade.dispatch(Outbound.Event(entry)) }
        }
    }

    // --- Bucles internos ---------------------------------------------------

    private suspend fun locationLoop() {
        while (coroutineContext.isActive) {
            val current = _state.value
            if (current.status == ShieldStatus.INACTIVO) break
            val seen = kick.value
            val interval = ReportingPolicy.intervalMillis(
                status = current.status,
                millisSinceAlert = current.alertSince?.let { clock() - it } ?: 0L,
                batteryPct = battery.levelPct()
            )
            captureOnce(interval)
            // Espera el intervalo, salvo que el estado cambie antes.
            withTimeoutOrNull(interval) { kick.first { it != seen } }
        }
    }

    /** Pide un punto fresco (o usa el último conocido) y lo procesa. */
    private suspend fun captureOnce(interval: Long) {
        val point: GeoPoint? = withTimeoutOrNull(interval.coerceAtMost(MAX_FIX_WAIT)) {
            runCatching { tracker.stream(interval.coerceAtMost(MAX_FIX_WAIT)).first() }.getOrNull()
        } ?: runCatching { tracker.lastKnown() }.getOrNull()
        if (point != null) process(point)
    }

    private suspend fun process(point: GeoPoint) {
        log.append(point)
        _state.update { it.copy(lastPoint = point, pendingToSend = it.pendingToSend + 1) }
        // En vigilancia normal la ubicación no sale del teléfono.
        if (_state.value.status.isAlert) announce()
    }

    /** Saca por la cascada el estado actual y, en alerta, ubicación + bitácora. */
    private suspend fun announce() = sendLock.withLock {
        val current = _state.value
        val backlog = if (current.status.isAlert) log.pending().takeLast(MAX_BACKLOG) else emptyList()
        val result = cascade.dispatch(
            Outbound.Report(
                status = current.status,
                reason = current.reason,
                alertSince = current.alertSince,
                point = current.lastPoint,
                batteryPct = battery.levelPct(),
                backlog = backlog
            )
        )
        if (!current.status.isAlert) return@withLock
        if (result.channel == CommChannel.INTERNET) {
            // Internet pudo enviar todo: la bitácora queda entregada.
            backlog.lastOrNull()?.let { log.markSentUpTo(it.capturedAt) }
        }
        _state.update {
            it.copy(
                lastChannel = result.channel,
                lastSentAt = if (result.delivered) clock() else it.lastSentAt,
                pendingToSend = log.pendingCount()
            )
        }
    }

    /** Traduce una señal del sistema a una reacción del Escudo. */
    private fun onSignal(signal: SystemSignal) {
        if (_state.value.status == ShieldStatus.INACTIVO) return
        val locked = isLocked()
        when (signal) {
            SystemSignal.APAGADO_FALSO -> {
                note("Intento de apagado", "La pantalla simuló apagarse; sigue registrando", alert = true)
                reportStolen(AlertReason.INTENTO_APAGADO)
            }
            SystemSignal.INTENTO_APAGADO ->
                if (locked) {
                    // "Antes de que lo apaguen, la ubicación ya salió".
                    note("Apagado con el equipo bloqueado", "Última ubicación enviada antes de apagarse", alert = true)
                    reportStolen(AlertReason.INTENTO_APAGADO)
                } else {
                    note("Equipo apagado", "Apagado normal, con el equipo desbloqueado")
                }
            SystemSignal.REINICIO -> {
                note("El equipo se encendió", "El Escudo retomó el registro", alert = _state.value.status.isAlert)
                kick.update { it + 1 }
            }
            SystemSignal.CHIP_RETIRADO, SystemSignal.CHIP_CAMBIADO -> {
                note(
                    if (signal == SystemSignal.CHIP_RETIRADO) "Chip retirado" else "Chip cambiado",
                    if (locked) "Sin desbloquear · señal sospechosa" else "Con el equipo desbloqueado",
                    alert = locked
                )
                if (locked) reportStolen(AlertReason.CHIP_RETIRADO)
            }
            SystemSignal.MODO_AVION -> note(
                "Modo avión activado",
                if (locked) "Sin desbloquear · señal sospechosa" else "Con el equipo desbloqueado",
                alert = locked
            )
            SystemSignal.BATERIA_CRITICA -> note("Batería baja", "El registro pasa a modo mínimo")
            SystemSignal.DESBLOQUEO_FALLIDO -> note("Desbloqueo fallido", "Señal sospechosa", alert = true)
        }
    }

    // --- Utilidades --------------------------------------------------------

    private fun enterAlert(status: ShieldStatus, reason: AlertReason, by: String?) {
        _state.update { it.copy(status = status, reason = reason, alertSince = clock(), reportedBy = by) }
        persist()
        kick.update { it + 1 }
        scope?.launch {
            announce()
            // Lo ocurrido justo antes del reporte ayuda a reconstruir el robo.
            val since = clock() - RECENT_EVENTS_MS
            events.entries.value.filter { it.at >= since }.takeLast(6).forEach { cascade.dispatch(Outbound.Event(it)) }
        }
    }

    private fun persist() {
        val s = _state.value
        store.save(s.status, s.reason, s.alertSince, s.reportedBy)
    }

    private fun reasonText(reason: AlertReason): String = when (reason) {
        AlertReason.INTENTO_APAGADO -> "Intentaron apagarlo"
        AlertReason.CHIP_RETIRADO -> "Retiraron el chip"
        AlertReason.ARREBATO, AlertReason.ROBO_DISCRETO -> "Se separó del guardián"
        AlertReason.AISLADO -> "Quedó aislado: bloqueado, sin red y lejos de su guardián"
        AlertReason.PERDIDA -> "Pérdida"
        AlertReason.REPORTE_MANUAL -> "Reporte manual"
    }

    companion object {
        private const val MAX_FIX_WAIT = 30_000L
        private const val MAX_BACKLOG = 240
        private const val RECENT_EVENTS_MS = 2 * 3_600_000L
    }
}
