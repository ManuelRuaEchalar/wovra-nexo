package com.redcentinela.escudo.data.circle

import com.redcentinela.escudo.data.ble.BleCodec
import com.redcentinela.escudo.data.ble.Sighting
import com.redcentinela.escudo.data.comm.SmsCodec
import com.redcentinela.escudo.data.crypto.Crypto
import com.redcentinela.escudo.data.net.RelayClient
import com.redcentinela.escudo.data.net.RelayMessage
import com.redcentinela.escudo.domain.model.AlertReason
import com.redcentinela.escudo.domain.model.CircleMember
import com.redcentinela.escudo.domain.model.CommChannel
import com.redcentinela.escudo.domain.model.EventEntry
import com.redcentinela.escudo.domain.model.GeoPoint
import com.redcentinela.escudo.domain.model.LocationSource
import com.redcentinela.escudo.domain.model.RemoteCommand
import com.redcentinela.escudo.domain.model.RemoteState
import com.redcentinela.escudo.domain.model.ShieldStatus
import com.redcentinela.escudo.domain.model.TrackPoint
import com.redcentinela.escudo.domain.service.EscudoCoordinator
import com.redcentinela.escudo.service.EscudoConfig
import com.redcentinela.escudo.service.Notifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Sincroniza este teléfono con su círculo. Reúne en un solo estado lo que
 * llega de cada equipo por los tres canales de la cascada:
 *
 *  - internet: temas cifrados del servidor de relevo (estado, ruta, registro),
 *  - SMS: última ubicación firmada,
 *  - Bluetooth: avisos captados de cerca, propios o reenviados por la red.
 *
 * También recibe las órdenes que el círculo le da a ESTE equipo.
 */
class CircleSync(
    private val config: EscudoConfig,
    private val circle: CircleStore,
    private val relay: RelayClient,
    private val scope: CoroutineScope,
    private val notifier: Notifier,
    /** Avisos ajenos captados sin internet, a entregar después. */
    private val forwardQueue: ForwardQueue,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val _remote = MutableStateFlow<Map<String, RemoteState>>(emptyMap())
    val remote: StateFlow<Map<String, RemoteState>> = _remote.asStateFlow()

    /**
     * Lo conecta el contenedor: un teléfono de la red (nodo) entregó un aviso
     * válido y nuevo de alguien de mi círculo; se confirma para que cobre.
     */
    var onUsefulRelay: ((deviceId: String, nodeId: String, at: Long) -> Unit)? = null

    /** Lo conecta el contenedor: ejecuta una orden recibida del círculo. */
    var onCommand: (suspend (RemoteCommand, CircleMember) -> Unit)? = null

    private var job: Job? = null
    private val seen = ConcurrentHashMap.newKeySet<String>()
    private val lastSighting = ConcurrentHashMap<String, Long>()
    private val flushing = Mutex()

    private val feedTopic get() = Crypto.topic(config.secret, "feed")
    private val inboxTopic get() = Crypto.topic(config.secret, "inbox")

    @Synchronized
    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            launch {
                while (true) {
                    if (forwardQueue.size() > 0 && relay.hasInternet()) flushForwarded()
                    delay(FLUSH_EVERY_MS)
                }
            }
            circle.members
                .map { list -> list.map { it.id to it.secret } }
                .distinctUntilChanged()
                .collectLatest {
                    coroutineScope {
                        launch { listenInbox() }
                        launch { listenFeeds(circle.members.value) }
                    }
                }
        }
    }

    // --- Salida ------------------------------------------------------------

    /** Publica en el tema propio; solo el círculo puede descifrarlo. */
    suspend fun publishFeed(json: JSONObject): Boolean =
        relay.publish(feedTopic, Crypto.seal(config.secret, json.toString()))

    suspend fun sendCommand(target: CircleMember, command: RemoteCommand): Boolean {
        val json = JSONObject().put("t", "cmd").put("c", command.code)
            .put("from", config.deviceId).put("at", clock())
        return relay.publish(Crypto.topic(target.secret, "inbox"), Crypto.seal(target.secret, json.toString()))
    }

    /** Tras leer el QR de alguien, le manda mi tarjeta: el vínculo queda en ambos. */
    suspend fun sendHello(target: CircleMember): Boolean {
        val json = JSONObject().put("t", "hello").put("card", config.myCard())
        return relay.publish(Crypto.topic(target.secret, "inbox"), Crypto.seal(target.secret, json.toString()))
    }

    // --- Entrada: internet ---------------------------------------------------

    private suspend fun listenInbox() {
        relay.stream(listOf(inboxTopic)) {
            val since = config.inboxSince
            (if (since > 0) since else clock() / 1000 - FRESH_INBOX_SEC).toString()
        }.collect { msg ->
            if (!seen.add(msg.id)) return@collect
            config.inboxSince = msg.timeSec
            val json = open(config.secret, msg.body) ?: return@collect
            when (json.optString("t")) {
                "hello" -> onHello(json.optString("card"))
                "cmd" -> {
                    val command = RemoteCommand.fromCode(json.optString("c")) ?: return@collect
                    acceptCommand(command, json.optString("from"), json.optLong("at"))
                }
            }
        }
    }

    private fun onHello(card: String) {
        val member = Card.decode(card) ?: return
        if (member.id == config.deviceId) return
        val isNew = circle.byId(member.id) == null
        if (circle.upsert(member) && isNew) notifier.joined(member.name)
    }

    /** Valida y ejecuta una orden (llegue por internet o por SMS). */
    suspend fun acceptCommand(command: RemoteCommand, fromId: String, at: Long) {
        // El panel web firma como este mismo equipo: es el dueño desde un navegador.
        val from = if (fromId == config.deviceId) {
            if (command !in WEB_COMMANDS) return
            CircleMember(config.deviceId, EscudoCoordinator.WEB_PANEL, config.secret)
        } else {
            circle.byId(fromId) ?: return
        }
        val now = clock()
        if (at <= config.lastCommandAt || at < now - MAX_COMMAND_AGE || at > now + CLOCK_SKEW) return
        config.lastCommandAt = at
        onCommand?.invoke(command, from)
    }

    private suspend fun listenFeeds(members: List<CircleMember>) {
        if (members.isEmpty()) return
        val byTopic = HashMap<String, CircleMember>()
        members.forEach { m ->
            byTopic[Crypto.topic(m.secret, "feed")] = m
            byTopic[sightTopic(m.id)] = m
        }
        var lastId: String? = null
        relay.stream(byTopic.keys.toList()) { lastId ?: HISTORY }.collect { msg ->
            lastId = msg.id
            if (!seen.add(msg.id)) return@collect
            val member = byTopic[msg.topic] ?: return@collect
            if (msg.topic.startsWith(SIGHT_PREFIX)) onForwarded(member, msg) else onFeed(member, msg)
        }
    }

    private fun onFeed(member: CircleMember, msg: RelayMessage) {
        val json = open(member.secret, msg.body) ?: return
        when (json.optString("t")) {
            "st" -> applyStatus(
                member,
                status = runCatching { ShieldStatus.valueOf(json.getString("s")) }.getOrNull() ?: return,
                reason = runCatching { AlertReason.valueOf(json.optString("r")) }.getOrNull(),
                alertSince = json.optLong("since").takeIf { it > 0 },
                at = json.optLong("at"),
                battery = json.optInt("bat", -1).takeIf { it >= 0 }
            )
            "pts" -> {
                val arr = json.optJSONArray("p") ?: return
                val points = List(arr.length()) { i ->
                    val p = arr.getJSONArray(i)
                    TrackPoint(
                        GeoPoint(
                            latitude = p.getDouble(0), longitude = p.getDouble(1),
                            accuracyMeters = p.getDouble(2).toFloat(),
                            source = runCatching { LocationSource.valueOf(p.getString(4)) }
                                .getOrDefault(LocationSource.DESCONOCIDA),
                            capturedAt = p.getLong(3)
                        ),
                        CommChannel.INTERNET
                    )
                }
                addPoints(member.id, points)
            }
            "ev" -> edit(member.id) { state ->
                val entry = EventEntry(json.optLong("at"), json.optString("ti"), json.optString("d"), json.optBoolean("a"))
                if (state.events.any { it.at == entry.at && it.title == entry.title }) state
                else state.copy(events = (state.events + entry).sortedBy { it.at }.takeLast(MAX_EVENTS))
            }
            "found" -> onFound(
                member, json.optLong("at"),
                json.optDouble("lat").takeIf { !it.isNaN() }, json.optDouble("lng").takeIf { !it.isNaN() }
            )
        }
    }

    private fun onForwarded(member: CircleMember, msg: RelayMessage) {
        val json = runCatching { JSONObject(msg.body) }.getOrNull() ?: return
        val raw = Crypto.unb64(json.optString("a")) ?: return
        val sighting = BleCodec.parse(raw, 0, json.optLong("at", msg.timeSec * 1000)) ?: return
        // Si lo guardó un teléfono sin internet, llega tarde: se anota cuándo.
        val delivered = msg.timeSec * 1000
        // Útil = trae una ubicación válida que no llegó ya por internet en ese rato.
        val node = json.optString("n").takeIf { Regex("^[0-9a-f]{16}$").matches(it) }
        val useful = node != null && BleCodec.locate(sighting, member.secret) != null &&
            _remote.value[member.id]?.points.orEmpty().none {
                it.via == CommChannel.INTERNET && kotlin.math.abs(it.point.capturedAt - sighting.at) < USEFUL_GAP_MS
            }
        applySighting(member, sighting, deliveredAt = delivered.takeIf { it - sighting.at > LATE_MS })
        if (useful) onUsefulRelay?.invoke(member.id, node!!, sighting.at)
    }

    // --- Entrada: SMS --------------------------------------------------------

    suspend fun onSms(parsed: SmsCodec.Parsed) {
        when (parsed) {
            is SmsCodec.Order -> {
                if (parsed.deviceId != config.deviceId || !parsed.verify(config.secret)) return
                acceptCommand(parsed.command, parsed.fromId, parsed.at)
            }
            is SmsCodec.Position -> {
                val member = circle.byId(parsed.deviceId) ?: return
                if (!parsed.verify(member.secret)) return
                val status = if (parsed.stolen) ShieldStatus.ALERTA_ROBO else ShieldStatus.ALERTA_PERDIDA
                applyStatus(member, status, null, null, parsed.at, parsed.batteryPct, weak = true)
                addPoints(
                    member.id,
                    listOf(
                        TrackPoint(
                            GeoPoint(
                                parsed.latitude, parsed.longitude, parsed.accuracy.toFloat(),
                                LocationSource.FUSED, capturedAt = parsed.at
                            ),
                            CommChannel.SMS
                        )
                    )
                )
            }
            is SmsCodec.FoundNotice -> {
                val member = circle.byId(parsed.deviceId) ?: return
                if (parsed.verify(member.secret)) onFound(member, parsed.at, parsed.latitude, parsed.longitude)
            }
        }
    }

    // --- Entrada: Bluetooth ----------------------------------------------------

    /** Un aviso captado por este teléfono. */
    fun onSighting(sighting: Sighting) {
        val key = "${sighting.deviceId}-${sighting.counter}"
        val member = circle.byId(sighting.deviceId)
        if (member != null) {
            val last = lastSighting[sighting.deviceId] ?: 0
            if (!seen.add("ble-$key") && sighting.at - last < SIGHTING_EVERY_MS) return
            lastSighting[sighting.deviceId] = sighting.at
            applySighting(member, sighting)
        } else if (sighting.inAlert && sighting.deviceId != config.deviceId) {
            // Equipo ajeno en alerta: se guarda tal cual y se entrega a su
            // círculo en cuanto haya internet (ahora o más tarde). Este
            // teléfono no puede leer su ubicación.
            if (forwardQueue.add(sighting.deviceId, Crypto.b64(sighting.raw), sighting.at) && relay.hasInternet()) {
                scope.launch { flushForwarded() }
            }
        }
    }

    /** Entrega al relevo los avisos ajenos guardados, del más antiguo al más nuevo. */
    suspend fun flushForwarded() {
        if (!flushing.tryLock()) return
        try {
            forwardQueue.flush { e ->
                val body = JSONObject().put("a", e.raw).put("at", e.at).put("n", config.nodeId)
                relay.publish(sightTopic(e.deviceId), body.toString())
            }
        } finally {
            flushing.unlock()
        }
    }

    private fun applySighting(member: CircleMember, sighting: Sighting, deliveredAt: Long? = null) {
        val status = when (sighting.status) {
            BleCodec.STATUS_STOLEN -> ShieldStatus.ALERTA_ROBO
            BleCodec.STATUS_LOST -> ShieldStatus.ALERTA_PERDIDA
            else -> ShieldStatus.ARMADO
        }
        applyStatus(member, status, null, null, sighting.at, null, weak = true)
        val where = BleCodec.locate(sighting, member.secret) ?: return
        addPoints(
            member.id,
            listOf(
                TrackPoint(
                    GeoPoint(where.latitude, where.longitude, 30f, LocationSource.FUSED, capturedAt = sighting.at),
                    CommChannel.BLUETOOTH,
                    deliveredAt = deliveredAt
                )
            )
        )
    }

    // --- Estado --------------------------------------------------------------

    /**
     * @param weak el dato viene de SMS/Bluetooth (sin motivo ni hora de inicio):
     *        cambia el estado solo si difiere, sin pisar el detalle de internet.
     */
    private fun applyStatus(
        member: CircleMember, status: ShieldStatus, reason: AlertReason?,
        alertSince: Long?, at: Long, battery: Int?, weak: Boolean = false
    ) {
        var becameAlert = false
        edit(member.id) { state ->
            if (at < state.statusAt) return@edit state
            if (weak && state.status == status) {
                return@edit state.copy(statusAt = at, batteryPct = battery ?: state.batteryPct)
            }
            becameAlert = status.isAlert && state.status?.isAlert != true
            val since = if (status.isAlert) alertSince ?: state.alertSince ?: at else null
            val fresh = since != null && state.alertSince != null && since != state.alertSince
            state.copy(
                status = status,
                reason = if (status.isAlert) reason ?: state.reason else null,
                alertSince = since,
                statusAt = at,
                batteryPct = battery ?: state.batteryPct,
                points = if (fresh) state.points.filter { it.point.capturedAt > since!! - KEEP_BEFORE_ALERT } else state.points,
                events = if (fresh) state.events.filter { it.at > since!! - KEEP_BEFORE_ALERT } else state.events,
                foundAt = if (status.isAlert) state.foundAt else null
            )
        }
        if (becameAlert && at > clock() - NOTIFY_WINDOW) {
            notifier.memberAlert(member, stolen = status == ShieldStatus.ALERTA_ROBO)
        }
    }

    private fun addPoints(memberId: String, incoming: List<TrackPoint>) = edit(memberId) { state ->
        val known = state.points.associateBy { it.point.capturedAt / 1000 }.toMutableMap()
        // Si el mismo punto llega por dos canales, se conserva el primero.
        incoming.forEach { known.putIfAbsent(it.point.capturedAt / 1000, it) }
        state.copy(points = known.values.sortedBy { it.point.capturedAt }.takeLast(MAX_POINTS))
    }

    private fun onFound(member: CircleMember, at: Long, latitude: Double?, longitude: Double?) {
        var isNew = false
        edit(member.id) { state ->
            isNew = state.foundAt != at
            state.copy(foundAt = at)
        }
        if (latitude != null && longitude != null) {
            addPoints(
                member.id,
                listOf(TrackPoint(GeoPoint(latitude, longitude, 30f, LocationSource.FUSED, capturedAt = at), CommChannel.INTERNET))
            )
        }
        if (isNew && at > clock() - NOTIFY_WINDOW) notifier.found(member)
    }

    private fun edit(memberId: String, change: (RemoteState) -> RemoteState) {
        _remote.update { map -> map + (memberId to change(map[memberId] ?: RemoteState(memberId))) }
    }

    fun forget(memberId: String) = _remote.update { it - memberId }

    private fun open(secret: String, body: String): JSONObject? =
        Crypto.open(secret, body)?.let { runCatching { JSONObject(it) }.getOrNull() }

    companion object {
        private const val SIGHT_PREFIX = "rcs"
        fun sightTopic(deviceId: String) = SIGHT_PREFIX + deviceId
        private const val HISTORY = "12h"
        private const val FRESH_INBOX_SEC = 600
        private const val MAX_COMMAND_AGE = 12 * 3_600_000L
        private const val CLOCK_SKEW = 10 * 60_000L
        private const val NOTIFY_WINDOW = 15 * 60_000L
        private const val KEEP_BEFORE_ALERT = 2 * 3_600_000L
        private const val SIGHTING_EVERY_MS = 8_000L
        private val WEB_COMMANDS = setOf(
            RemoteCommand.ROBO, RemoteCommand.PERDIDA, RemoteCommand.RECUPERADO, RemoteCommand.SONAR, RemoteCommand.BUSCAR
        )
        private const val FLUSH_EVERY_MS = 30_000L
        private const val USEFUL_GAP_MS = 2 * 60_000L
        /** Un aviso reenviado que llega más tarde que esto se marca "entregado tarde". */
        private const val LATE_MS = 5 * 60_000L
        private const val MAX_POINTS = 600
        private const val MAX_EVENTS = 80

        /** Empaqueta puntos para el tema propio. */
        fun pointsJson(points: List<GeoPoint>): JSONObject {
            val arr = JSONArray()
            points.forEach { p ->
                arr.put(
                    JSONArray().put(round5(p.latitude)).put(round5(p.longitude))
                        .put(p.accuracyMeters.toInt().coerceAtMost(9999)).put(p.capturedAt).put(p.source.name)
                )
            }
            return JSONObject().put("t", "pts").put("p", arr)
        }

        private fun round5(v: Double) = Math.round(v * 100_000) / 100_000.0
    }
}
