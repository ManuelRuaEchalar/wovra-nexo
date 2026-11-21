package com.redcentinela.escudo.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.redcentinela.escudo.data.ble.TagLink
import com.redcentinela.escudo.data.circle.Card
import com.redcentinela.escudo.data.reward.Reward
import com.redcentinela.escudo.data.shutdown.FakeShutdownService
import com.redcentinela.escudo.data.shutdown.SignalBus
import com.redcentinela.escudo.domain.model.AlertReason
import com.redcentinela.escudo.domain.model.CircleMember
import com.redcentinela.escudo.domain.model.CommChannel
import com.redcentinela.escudo.domain.model.EventEntry
import com.redcentinela.escudo.domain.model.GeoPoint
import com.redcentinela.escudo.domain.model.Presence
import com.redcentinela.escudo.domain.model.ProximityReading
import com.redcentinela.escudo.domain.model.RemoteCommand
import com.redcentinela.escudo.domain.model.RemoteState
import com.redcentinela.escudo.domain.model.ShieldState
import com.redcentinela.escudo.domain.model.SystemSignal
import com.redcentinela.escudo.domain.service.DirectionFinder
import com.redcentinela.escudo.domain.service.ProximityEstimator
import com.redcentinela.escudo.service.EscudoService
import com.redcentinela.escudo.service.ServiceLocator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * ViewModel de la app. Expone a la UI el estado del Escudo, el círculo y lo
 * que se sabe de cada equipo, y traduce los gestos del usuario en llamadas a
 * los servicios compartidos. No contiene lógica de decisión: solo adapta.
 */
class EscudoViewModel(app: Application) : AndroidViewModel(app) {

    private val locator = ServiceLocator.from(app)
    private val coordinator = locator.coordinator
    val config = locator.config

    val shield: StateFlow<ShieldState> = coordinator.state
    val members: StateFlow<List<CircleMember>> = locator.circle.members
    val remote: StateFlow<Map<String, RemoteState>> = locator.sync.remote
    val presence: StateFlow<Map<String, Presence>> = locator.guardian.presence
    val tagState: StateFlow<TagLink.State> = locator.tag.state
    val events: StateFlow<List<EventEntry>> = locator.events.entries

    private val _points = MutableStateFlow<List<GeoPoint>>(emptyList())
    val points: StateFlow<List<GeoPoint>> = _points.asStateFlow()

    private val _proximity = MutableStateFlow<ProximityReading?>(null)
    val proximity: StateFlow<ProximityReading?> = _proximity.asStateFlow()

    // --- Recompensa garantizada (Stellar) ---
    val reward: StateFlow<Reward?> = locator.rewards.reward
    val rewardWorking: StateFlow<Boolean> = locator.rewards.working
    val rewardProblem: StateFlow<String?> = locator.rewards.problem
    fun confirmReward() = locator.rewards.confirm()
    fun rejectRewardClaim() = locator.rewards.reject()
    fun cancelReward() = locator.rewards.cancel()
    fun dismissReward() = locator.rewards.dismiss()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    /** Mensajes breves para el usuario (resultado de una acción). */
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        locator.sync.start()
        if (config.shieldEnabled && config.onboarded) EscudoService.start(app)
        // Refresca la bitácora propia para la pantalla de registro.
        viewModelScope.launch {
            while (isActive) {
                _points.value = locator.locationLog.recent(300)
                delay(4_000)
            }
        }
    }

    fun say(text: String) {
        _messages.tryEmit(text)
    }

    // --- Ayudar a la red (micropagos) ------------------------------------------

    private val _nodeStats = MutableStateFlow<org.json.JSONObject?>(null)
    val nodeStats: StateFlow<org.json.JSONObject?> = _nodeStats.asStateFlow()

    fun refreshNodeStats() {
        viewModelScope.launch { _nodeStats.value = locator.nodeStats() }
    }

    // --- Panel web (cuenta de rescate) ---------------------------------------

    private val _rescueEmail = MutableStateFlow(locator.rescue.email)
    val rescueEmail: StateFlow<String?> = _rescueEmail.asStateFlow()
    private val _rescueWorking = MutableStateFlow(false)
    val rescueWorking: StateFlow<Boolean> = _rescueWorking.asStateFlow()

    fun setUpRescue(email: String, password: String, done: () -> Unit) {
        if (_rescueWorking.value) return
        _rescueWorking.value = true
        viewModelScope.launch {
            runCatching { locator.rescue.setUp(email, password) }
                .onSuccess {
                    _rescueEmail.value = locator.rescue.email
                    say("Listo: entra al panel web con ${locator.rescue.email}.")
                    done()
                }
                .onFailure { say(it.message ?: "No se pudo guardar la cuenta del panel") }
            _rescueWorking.value = false
        }
    }

    fun removeRescue() {
        if (_rescueWorking.value) return
        _rescueWorking.value = true
        viewModelScope.launch {
            runCatching { locator.rescue.remove() }
                .onSuccess { _rescueEmail.value = null; say("Cuenta del panel web eliminada.") }
                .onFailure { say(it.message ?: "No se pudo eliminar la cuenta") }
            _rescueWorking.value = false
        }
    }

    // --- Escudo ------------------------------------------------------------

    fun startShield() {
        config.shieldEnabled = true
        EscudoService.start(getApplication())
    }

    fun stopShield() {
        config.shieldEnabled = false
        EscudoService.stop(getApplication())
    }

    fun standDown() = coordinator.standDown()

    /** Canal que usaría la cascada ahora mismo. */
    fun currentChannel(): CommChannel = when {
        locator.relay.hasInternet() -> CommChannel.INTERNET
        locator.smsAvailable() && members.value.any { !it.phone.isNullOrBlank() } -> CommChannel.SMS
        locator.ble.advertising -> CommChannel.BLUETOOTH
        else -> CommChannel.LOCAL
    }

    // --- Pruebas (Ajustes) ---------------------------------------------------

    fun simulateTheft() {
        if (requireShield()) coordinator.reportStolen(AlertReason.REPORTE_MANUAL)
    }

    fun simulateLost() {
        if (requireShield()) coordinator.reportLost()
    }

    fun simulateSignal(signal: SystemSignal) {
        if (requireShield()) SignalBus.publish(signal)
    }

    /** Muestra el apagado simulado ahora, sin esperar al botón de encendido. */
    fun testFakeShutdown() {
        val service = FakeShutdownService.instance
        if (service != null) {
            service.show()
        } else {
            getApplication<Application>().startActivity(
                Intent(getApplication(), FakeShutdownActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun fakeShutdownReady(): Boolean = FakeShutdownService.isEnabled(getApplication())

    private fun requireShield(): Boolean {
        val armed = shield.value.status != com.redcentinela.escudo.domain.model.ShieldStatus.INACTIVO
        if (!armed) say("Primero activa el Escudo")
        return armed
    }

    // --- Círculo -------------------------------------------------------------

    /** Agrega a alguien a partir del texto de su QR o de su invitación. */
    fun addFromText(text: String): CircleMember? {
        val member = Card.decode(text)
        when {
            member == null -> say("No encontré una invitación válida")
            member.id == config.deviceId -> say("Ese es tu propio código")
            !locator.circle.upsert(member) -> say("Tu círculo ya tiene 5 personas")
            else -> {
                say("${member.name} ya está en tu círculo")
                // Con internet, el otro teléfono me agrega solo.
                viewModelScope.launch {
                    if (!locator.sync.sendHello(member)) {
                        say("Sin internet: pídele a ${member.name} que escanee también tu código")
                    }
                }
                return member
            }
        }
        return null
    }

    fun removeMember(id: String) {
        locator.circle.remove(id)
        locator.sync.forget(id)
    }

    fun setPhone(id: String, phone: String) =
        locator.circle.update(id) { it.copy(phone = phone.trim().ifBlank { null }) }

    /** Elige (o quita) a alguien del círculo como mi guardián. */
    fun setGuardian(member: CircleMember, on: Boolean) {
        members.value.filter { it.isGuardian && it.id != member.id }.forEach { other ->
            locator.circle.update(other.id) { it.copy(isGuardian = false) }
            viewModelScope.launch { locator.sync.sendCommand(other, RemoteCommand.GUARDIAN_OFF) }
        }
        locator.circle.update(member.id) { it.copy(isGuardian = on) }
        viewModelScope.launch {
            val told = locator.sync.sendCommand(member, if (on) RemoteCommand.GUARDIAN_ON else RemoteCommand.GUARDIAN_OFF)
            if (on && !told) say("Sin internet: pídele a ${member.name} que active \"Soy su guardián\" en su app")
        }
    }

    fun setWatch(member: CircleMember, on: Boolean) =
        locator.circle.update(member.id) { it.copy(iWatch = on) }

    /** Reporta el celular de alguien del círculo (robo o pérdida). */
    fun report(member: CircleMember, stolen: Boolean) = command(
        member, if (stolen) RemoteCommand.ROBO else RemoteCommand.PERDIDA
    )

    fun command(member: CircleMember, command: RemoteCommand) {
        viewModelScope.launch {
            val result = locator.sendCommand(member, command)
            say(
                when (result.channel) {
                    CommChannel.INTERNET -> "Enviado por internet al celular de ${member.name}"
                    CommChannel.SMS -> "Enviado por SMS al celular de ${member.name}"
                    CommChannel.BLUETOOTH -> "Recibido por Bluetooth en el celular de ${member.name}"
                    else -> if (member.phone.isNullOrBlank()) {
                        "No se pudo enviar: sin internet y ${member.name} no tiene número guardado"
                    } else {
                        "No se pudo enviar: sin internet ni SMS disponibles"
                    }
                }
            )
        }
    }

    // --- Guardián: llavero -----------------------------------------------------

    fun discoverTags(): Flow<List<TagLink.Found>> = locator.tag.discover()

    fun linkTag(found: TagLink.Found) {
        config.tagAddress = found.address
        config.tagName = found.name
        locator.tag.connect(found.address)
        say("Llavero vinculado")
    }

    fun unlinkTag() {
        config.tagAddress = null
        config.tagName = null
        locator.tag.disconnect()
    }

    // --- Búsqueda cercana por Bluetooth ----------------------------------------

    private var seekJob: Job? = null

    private val _heading = MutableStateFlow<Float?>(null)
    /** Rumbo hacia el que mira quien busca (grados; null = sin brújula). */
    val heading: StateFlow<Float?> = _heading.asStateFlow()

    private val _sweep = MutableStateFlow<DirectionFinder.Sweep?>(null)
    /** Fuerza de la señal por dirección, mientras se gira buscando. */
    val sweep: StateFlow<DirectionFinder.Sweep?> = _sweep.asStateFlow()

    /** Empieza a medir qué tan cerca, y hacia dónde, está el celular de [memberId]. */
    fun startSeeking(memberId: String) {
        stopSeeking()
        val estimator = ProximityEstimator()
        val finder = DirectionFinder()
        locator.ble.setBoost(true)
        seekJob = viewModelScope.launch {
            var lastAt = 0L
            launch { locator.compass.headings().collect { _heading.value = it } }
            launch {
                locator.ble.sightings.collect { sighting ->
                    if (sighting.deviceId != memberId) return@collect
                    val reading = estimator.onRssi(sighting.rssi, sighting.at)
                    lastAt = sighting.at
                    _proximity.value = reading
                    _heading.value?.let { _sweep.value = finder.onReading(it, sighting.rssi, sighting.at) }
                    val (interval, solid) = ProximityEstimator.feedbackFor(reading.rssiDbm)
                    locator.feedback.pulse(interval, solid)
                }
            }
            // Le pide al otro equipo que emita al máximo mientras dure la búsqueda.
            launch {
                while (isActive) {
                    val member = members.value.firstOrNull { it.id == memberId } ?: break
                    delay(if (locator.requestSeek(member)) SEEK_RENEW_MS else SEEK_RETRY_MS)
                }
            }
            // Sin avisos por unos segundos = fuera de alcance.
            while (isActive) {
                delay(2_000)
                if (_proximity.value != null && System.currentTimeMillis() - lastAt > SIGNAL_LOST_MS) {
                    _proximity.value = null
                    _sweep.value = null
                    estimator.reset()
                    finder.reset()
                    locator.feedback.stop()
                }
            }
        }
    }

    fun stopSeeking() {
        seekJob?.cancel()
        seekJob = null
        locator.ble.setBoost(false)
        locator.feedback.stop()
        _proximity.value = null
        _sweep.value = null
        _heading.value = null
    }

    fun bluetoothReady(): Boolean = locator.ble.canScan()

    override fun onCleared() {
        stopSeeking()
        super.onCleared()
    }

    private companion object {
        const val SIGNAL_LOST_MS = 7_000L
        const val SEEK_RENEW_MS = 150_000L
        const val SEEK_RETRY_MS = 5_000L
    }
}
