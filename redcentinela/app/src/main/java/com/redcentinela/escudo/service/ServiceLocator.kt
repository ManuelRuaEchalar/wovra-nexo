package com.redcentinela.escudo.service

import android.app.KeyguardManager
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.redcentinela.escudo.data.alarm.Alarm
import com.redcentinela.escudo.data.ble.BleCodec
import com.redcentinela.escudo.data.ble.BleHub
import com.redcentinela.escudo.data.ble.TagLink
import com.redcentinela.escudo.data.circle.CircleStore
import com.redcentinela.escudo.data.circle.CircleSync
import com.redcentinela.escudo.data.comm.BleCommLink
import com.redcentinela.escudo.data.comm.LocalCommLink
import com.redcentinela.escudo.data.comm.RelayCommLink
import com.redcentinela.escudo.data.comm.SmsCommLink
import com.redcentinela.escudo.data.finder.ToneFeedbackEmitter
import com.redcentinela.escudo.data.location.FusedLocationTracker
import com.redcentinela.escudo.data.net.RelayClient
import com.redcentinela.escudo.data.reward.RewardClient
import com.redcentinela.escudo.data.reward.RewardManager
import com.redcentinela.escudo.data.reward.RewardStore
import com.redcentinela.escudo.data.shutdown.AndroidBatteryGauge
import com.redcentinela.escudo.data.shutdown.AndroidSystemSignalSource
import com.redcentinela.escudo.data.storage.EncryptedLocationLog
import com.redcentinela.escudo.data.storage.EventLog
import com.redcentinela.escudo.data.storage.ShieldStore
import com.redcentinela.escudo.domain.model.CircleMember
import com.redcentinela.escudo.domain.model.CommChannel
import com.redcentinela.escudo.domain.model.CommResult
import com.redcentinela.escudo.domain.model.Outbound
import com.redcentinela.escudo.domain.model.RemoteCommand
import com.redcentinela.escudo.domain.model.ShieldStatus
import com.redcentinela.escudo.domain.port.BatteryGauge
import com.redcentinela.escudo.domain.port.FeedbackEmitter
import com.redcentinela.escudo.domain.port.LocationLog
import com.redcentinela.escudo.domain.service.CommCascade
import com.redcentinela.escudo.domain.service.EscudoCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

/**
 * Contenedor de dependencias hecho a mano (inyección manual). Es un singleton
 * para que la UI, el servicio en primer plano y los receivers compartan el
 * MISMO coordinador y el mismo estado observable.
 */
class ServiceLocator private constructor(context: Context) {

    private val appContext = context.applicationContext

    /** Vive lo que viva el proceso: sincronización con el círculo y envíos. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val config = EscudoConfig(appContext)
    val notifier = Notifier(appContext)
    val events = EventLog(appContext)
    val battery: BatteryGauge = AndroidBatteryGauge(appContext)
    val locationLog: LocationLog = EncryptedLocationLog(appContext)
    val circle = CircleStore(securePrefs("escudo_circulo"))
    val relay = RelayClient(appContext) { config.relayUrl }
    val sync = CircleSync(config, circle, relay, appScope, notifier)
    val ble = BleHub(appContext, appScope)
    val tag = TagLink(appContext)
    val guardian = GuardianWatch(ble, circle)
    val feedback: FeedbackEmitter by lazy { ToneFeedbackEmitter(appContext) }

    val compass by lazy { com.redcentinela.escudo.data.finder.Compass(appContext) }

    private val sms = SmsCommLink(appContext, config, circle)

    /** Último contacto real con el círculo (envío entregado, orden recibida, Bluetooth cercano). */
    @Volatile
    var lastContactAt = System.currentTimeMillis()
        private set

    fun touchContact() {
        lastContactAt = System.currentTimeMillis()
    }

    /** Hasta cuándo emitir el aviso Bluetooth a máxima potencia porque alguien busca este equipo. */
    val seekUntil = MutableStateFlow(0L)

    /** La pantalla "Este celular está perdido" debe estar visible. */
    val lostScreen = MutableStateFlow(false)

    /** Reportes que no se pudieron confirmar: se entregan por Bluetooth si el equipo aparece cerca. */
    private val pendingOrders = ConcurrentHashMap<String, Pair<RemoteCommand, Long>>()
    private val pendingTriedAt = ConcurrentHashMap<String, Long>()
    private var lastBleOrderAt = 0L

    private val cascade = CommCascade(
        links = listOf(RelayCommLink(relay, sync), sms, BleCommLink(ble), LocalCommLink())
    )

    private val smsOnly = CommCascade(listOf(sms))

    fun smsAvailable(): Boolean = sms.isAvailable()

    /** ¿Puede este equipo recibir una orden a distancia ahora mismo? */
    fun reachable(): Boolean = relay.hasInternet() || sms.isAvailable()

    val coordinator = EscudoCoordinator(
        tracker = FusedLocationTracker(appContext),
        log = locationLog,
        cascade = cascade,
        signals = AndroidSystemSignalSource(appContext),
        battery = battery,
        store = ShieldStore(appContext),
        events = events,
        isLocked = {
            (appContext.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked
        }
    )

    /** Recompensa garantizada en Stellar si este celular se pierde. */
    val rewards = RewardManager(
        config = config,
        client = RewardClient { config.rewardServerUrl },
        store = RewardStore(securePrefs("escudo_recompensa")),
        relay = relay,
        scope = appScope,
        shield = coordinator.state
    )

    init {
        sync.onCommand = ::execute
    }

    /** Ejecuta en ESTE equipo una orden que mandó alguien del círculo. */
    private suspend fun execute(command: RemoteCommand, from: CircleMember) {
        touchContact()
        when (command) {
            RemoteCommand.ROBO, RemoteCommand.PERDIDA -> {
                if (!config.shieldEnabled) return
                ensureArmed()
                if (command == RemoteCommand.ROBO) coordinator.reportStolen(by = from.name)
                else coordinator.reportLost(by = from.name)
            }
            RemoteCommand.RECUPERADO -> coordinator.standDown(by = from.name)
            RemoteCommand.SONAR -> {
                Alarm.ring(appContext, RING_SECONDS, flash = true)
                notifier.ringing(from.name)
            }
            RemoteCommand.BUSCAR -> seekUntil.value = System.currentTimeMillis() + SEEK_MS
            RemoteCommand.GUARDIAN_ON -> circle.update(from.id) { it.copy(iWatch = true) }
            RemoteCommand.GUARDIAN_OFF -> circle.update(from.id) { it.copy(iWatch = false) }
        }
    }

    /** La orden puede llegar (por SMS) con la app cerrada: levanta el Escudo. */
    private suspend fun ensureArmed() {
        if (coordinator.state.value.status != ShieldStatus.INACTIVO) return
        EscudoService.start(appContext)
        withTimeoutOrNull(5_000) { coordinator.state.first { it.status != ShieldStatus.INACTIVO } }
    }

    /** Una orden que llegó por Bluetooth directo: se valida la firma y sigue el camino normal. */
    fun onBleOrder(bytes: ByteArray) {
        val order = BleCodec.readOrder(bytes, config.secret) ?: return
        appScope.launch { sync.acceptCommand(order.command, order.fromId, order.at) }
    }

    /**
     * Manda una orden al celular de alguien del círculo por el mejor canal.
     *
     * - Si ese celular está a la vista por Bluetooth, va directo: no depende
     *   de que ninguno de los dos tenga internet ni saldo, y queda confirmada.
     * - Si no, sale por la cascada (internet, luego SMS). Un reporte que salió
     *   por internet pero no se confirma (ese celular puede estar sin datos)
     *   se repite por SMS, y queda pendiente para entregarlo por Bluetooth si
     *   el equipo aparece cerca.
     */
    suspend fun sendCommand(target: CircleMember, command: RemoteCommand): CommResult {
        if (bleOrder(target, command)) return CommResult(delivered = true, channel = CommChannel.BLUETOOTH)
        val result = cascade.dispatch(Outbound.Command(target, command))
        when (command) {
            RemoteCommand.ROBO, RemoteCommand.PERDIDA ->
                pendingOrders[target.id] = command to System.currentTimeMillis() + PENDING_MS
            RemoteCommand.RECUPERADO -> pendingOrders.remove(target.id)
            else -> Unit
        }
        val expectsAlert = command == RemoteCommand.ROBO || command == RemoteCommand.PERDIDA
        if (result.channel == CommChannel.INTERNET && expectsAlert && !target.phone.isNullOrBlank()) {
            appScope.launch {
                delay(SMS_FALLBACK_AFTER_MS)
                if (sync.remote.value[target.id]?.inAlert != true) {
                    smsOnly.dispatch(Outbound.Command(target, command))
                }
            }
        }
        return result
    }

    /**
     * Pide a ese equipo que emita su aviso Bluetooth al máximo un rato, para
     * buscarlo de cerca. Nunca por SMS: no vale lo que cuesta.
     */
    suspend fun requestSeek(target: CircleMember): Boolean =
        bleOrder(target, RemoteCommand.BUSCAR) ||
            (relay.hasInternet() && sync.sendCommand(target, RemoteCommand.BUSCAR))

    private suspend fun bleOrder(target: CircleMember, command: RemoteCommand): Boolean {
        if (!ble.isNear(target.id)) return false
        // La hora viaja en segundos y el otro equipo descarta órdenes que no sean
        // más nuevas que la última: dos seguidas nunca llevan la misma marca.
        val at = synchronized(this) {
            maxOf(System.currentTimeMillis(), lastBleOrderAt + 1_000).also { lastBleOrderAt = it }
        }
        val bytes = BleCodec.order(command, at, config.deviceId, target.secret)
        return ble.sendOrder(target.id, bytes)
    }

    /**
     * Un equipo del círculo apareció por Bluetooth. Si hay un reporte suyo sin
     * confirmar y su aviso todavía no dice "en alerta", se le entrega ahora:
     * así se entera de que está robado aunque no tenga internet ni chip.
     */
    fun onMemberSighted(memberId: String, inAlert: Boolean) {
        val (command, expires) = pendingOrders[memberId] ?: return
        val now = System.currentTimeMillis()
        if (inAlert || now > expires) {
            pendingOrders.remove(memberId)
            return
        }
        if (now - (pendingTriedAt[memberId] ?: 0) < PENDING_RETRY_MS) return
        pendingTriedAt[memberId] = now
        val target = circle.byId(memberId) ?: return
        appScope.launch { if (bleOrder(target, command)) pendingOrders.remove(memberId) }
    }

    private fun securePrefs(name: String): SharedPreferences {
        val masterKey = MasterKey.Builder(appContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        return EncryptedSharedPreferences.create(
            appContext, name, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private const val RING_SECONDS = 40
        private const val SMS_FALLBACK_AFTER_MS = 25_000L
        private const val SEEK_MS = 3 * 60_000L
        private const val PENDING_MS = 12 * 3_600_000L
        private const val PENDING_RETRY_MS = 20_000L

        @Volatile
        private var instance: ServiceLocator? = null

        fun from(context: Context): ServiceLocator =
            instance ?: synchronized(this) {
                instance ?: ServiceLocator(context).also { instance = it }
            }
    }
}
