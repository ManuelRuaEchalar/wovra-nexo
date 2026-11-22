package com.dendra.app.service

import android.app.KeyguardManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.dendra.app.data.ble.BleCodec
import com.dendra.app.data.ble.BleHub
import com.dendra.app.data.ble.TagLink
import com.dendra.app.data.shutdown.FakeShutdownService
import com.dendra.app.domain.model.AlertReason
import com.dendra.app.domain.model.CommChannel
import com.dendra.app.domain.service.IsolationPolicy
import com.dendra.app.domain.model.ShieldState
import com.dendra.app.domain.model.ShieldStatus
import com.dendra.app.ui.LostModeActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano que mantiene vivo el Escudo con una notificación
 * fija, como exige Android para la ubicación en segundo plano.
 *
 * Solo gestiona el ciclo de vida: arma el coordinador, mantiene al día el
 * aviso Bluetooth del equipo, y conecta el guardián (celular acompañante o
 * llavero) con el coordinador. La lógica de decisión vive en
 * [com.dendra.app.domain.service.EscudoCoordinator].
 */
class EscudoService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var locator: ServiceLocator
    private var started = false
    private var bleCounter = (System.currentTimeMillis() / 1000).toInt() and 0xFFFF

    override fun onCreate() {
        super.onCreate()
        locator = ServiceLocator.from(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            shutDown()
            return START_NOT_STICKY
        }
        if (!hasLocationPermission() || !enterForeground()) {
            // Sin permiso de ubicación Android no permite este servicio.
            stopSelf()
            return START_NOT_STICKY
        }
        if (!started) {
            started = true
            running = true
            wire()
        }
        if (intent?.getBooleanExtra(EXTRA_BOOT, false) == true) {
            locator.coordinator.note("El equipo se encendió", "El Escudo retomó el registro")
        }
        return START_STICKY
    }

    private fun wire() {
        val coordinator = locator.coordinator
        coordinator.arm(locator.appScope)
        locator.sync.start()
        // Órdenes por Bluetooth directo (antes del aviso: lo vuelve conectable).
        locator.ble.startServer(locator::onBleOrder)

        // Estado -> notificación y aviso Bluetooth. Si alguien está buscando
        // este equipo de cerca, el aviso sale a máxima potencia y cadencia.
        scope.launch {
            combine(coordinator.state, locator.seekUntil) { state, seekUntil -> state to seekUntil }
                .collectLatest { (state, seekUntil) ->
                    if (state.status == ShieldStatus.INACTIVO) return@collectLatest
                    locator.notifier.updateService(state.status)
                    val left = seekUntil - System.currentTimeMillis()
                    advertise(state, sought = left > 0)
                    if (left > 0) {
                        delay(left)
                        advertise(state, sought = false)
                    }
                }
        }

        // Un envío entregado por internet o SMS cuenta como contacto con el círculo.
        scope.launch {
            coordinator.state.map { it.lastSentAt to it.lastChannel }.distinctUntilChanged().collect { (at, channel) ->
                if (at != null && (channel == CommChannel.INTERNET || channel == CommChannel.SMS)) locator.touchContact()
            }
        }

        // Pantalla del equipo y alerta sola: se revisan con cada cambio y cada pocos segundos.
        scope.launch {
            combine(coordinator.state.map { it.status }.distinctUntilChanged(), FakeShutdownService.active) { _, fake -> fake }
                .collect { fake ->
                    if (fake && !fakeWasActive) {
                        // Apagado falso recién puesto: el disimulo empieza de cero.
                        revealed = false
                        fakeSince = System.currentTimeMillis()
                    }
                    fakeWasActive = fake
                    evaluate()
                }
        }
        scope.launch {
            while (isActive) {
                delay(EVALUATE_MS)
                evaluate()
            }
        }

        // Bluetooth: escucha los avisos del resto de la red.
        scope.launch {
            locator.ble.sightings.collect { sighting ->
                locator.sync.onSighting(sighting)
                if (locator.circle.byId(sighting.deviceId) != null) {
                    locator.touchContact()
                    locator.onMemberSighted(sighting.deviceId, sighting.inAlert)
                }
            }
        }
        scope.launch {
            locator.circle.members
                .map { list -> list.any { it.iWatch || it.isGuardian } }
                .distinctUntilChanged()
                .collect { guarding ->
                    locator.ble.setLevel(if (guarding) BleHub.ScanLevel.GUARDIAN else BleHub.ScanLevel.FONDO)
                }
        }

        // Guardián: el celular de quien va conmigo.
        locator.guardian.apply {
            onWatchedAway = { member ->
                locator.notifier.guardianAway(member)
                if (locator.config.beepOnSeparation) {
                    com.dendra.app.data.alarm.Alarm.ring(this@EscudoService, GUARDIAN_BEEP_SECONDS)
                }
            }
            onWatchedBack = { member ->
                locator.notifier.cancelGuardian(member)
                com.dendra.app.data.alarm.Alarm.stop(this@EscudoService)
            }
            onGuardianAway = { member ->
                coordinator.note("Se separó del guardián", "El celular de ${member.name} quedó fuera de alcance", alert = isLocked())
            }
            onGuardianBack = { member ->
                coordinator.note("Guardián de vuelta", "El celular de ${member.name} está cerca")
            }
            run(scope)
        }

        // Guardián: llavero Bluetooth.
        locator.tag.apply {
            onAway = { coordinator.note("Se separó del guardián", "El llavero quedó fuera de alcance", alert = isLocked()) }
            onBack = { coordinator.note("Guardián de vuelta", "El llavero está cerca") }
            onButton = { coordinator.reportStolen(AlertReason.ARREBATO, by = null) }
        }
        locator.config.tagAddress?.let { address ->
            if (locator.tag.state.value == TagLink.State.SIN_LLAVERO) locator.tag.connect(address)
        }
    }

    /** Mantiene el aviso BLE al día: estado y, en alerta, ubicación cifrada. */
    private fun advertise(state: ShieldState, sought: Boolean) {
        bleCounter = (bleCounter + 1) and 0xFFFF
        val payload = BleCodec.build(
            deviceId = locator.config.deviceId,
            secret = locator.config.secret,
            status = when (state.status) {
                ShieldStatus.ALERTA_ROBO -> BleCodec.STATUS_STOLEN
                ShieldStatus.ALERTA_PERDIDA -> BleCodec.STATUS_LOST
                else -> BleCodec.STATUS_ARMED
            },
            counter = bleCounter,
            latitude = state.lastPoint?.latitude,
            longitude = state.lastPoint?.longitude,
            batteryPct = locator.battery.levelPct()
        )
        locator.ble.advertise(payload, urgent = state.status.isAlert || sought)
    }

    // --- Protocolo de aislamiento (ver IsolationPolicy) ---------------------------

    private var aloneSince: Long? = null
    private var revealed = false
    private var fakeWasActive = false
    private var fakeSince = 0L

    @Synchronized
    private fun evaluate() {
        val coordinator = locator.coordinator
        val state = coordinator.state.value
        if (state.status == ShieldStatus.INACTIVO) return
        val now = System.currentTimeMillis()
        if (!state.status.isAlert) revealed = false

        val guardianMember = locator.circle.members.value.firstOrNull { it.isGuardian }
        val tagState = locator.tag.state.value
        val facts = IsolationPolicy.Facts(
            status = state.status,
            locked = isLocked(),
            reachable = locator.reachable(),
            hasGuardian = guardianMember != null || tagState != TagLink.State.SIN_LLAVERO,
            guardianNear = tagState == TagLink.State.CERCA ||
                (guardianMember != null && locator.guardian.presence.value[guardianMember.id]?.near == true),
            aloneSince = aloneSince,
            lastContactAt = maxOf(locator.lastContactAt, fakeSince),
            alertSince = state.alertSince,
            fakeShutdownActive = FakeShutdownService.active.value,
            revealed = revealed
        )
        val minutes = locator.config.selfAlertMinutes

        // 1. Alerta sola.
        aloneSince = if (IsolationPolicy.alone(facts)) aloneSince ?: now else null
        if (IsolationPolicy.shouldSelfAlert(facts, now, minutes * 60_000L)) {
            aloneSince = null
            coordinator.note(
                "Quedó aislado", "$minutes min bloqueado, sin red y lejos de su guardián", alert = true
            )
            coordinator.reportStolen(AlertReason.AISLADO)
            return
        }

        // 2. Qué se ve en la pantalla: el apagado falso manda mientras haya comunicación.
        val revealAfter = (if (minutes > 0) minutes else DEFAULT_REVEAL_MIN) * 60_000L
        when (IsolationPolicy.screen(facts, now, revealAfter)) {
            IsolationPolicy.Screen.NORMAL -> {
                // Recuperado: se levanta el apagado simulado si estaba puesto.
                if (!state.status.isAlert) FakeShutdownService.instance?.hide()
                hideLostScreen()
            }
            IsolationPolicy.Screen.APAGADO_FALSO -> hideLostScreen()
            IsolationPolicy.Screen.PERDIDO -> {
                if (facts.fakeShutdownActive) {
                    revealed = true
                    coordinator.note(
                        "Sin comunicación", "Se quitó el apagado falso y se muestra cómo devolver el equipo", alert = true
                    )
                    FakeShutdownService.instance?.hide()
                }
                showLostScreen()
            }
        }
    }

    private fun showLostScreen() {
        if (locator.lostScreen.value) return
        locator.lostScreen.value = true
        locator.notifier.showLostScreen()
        // Con el servicio de accesibilidad activo se puede abrir directo.
        FakeShutdownService.instance?.launch(Intent(this, LostModeActivity::class.java))
    }

    private fun hideLostScreen() {
        if (!locator.lostScreen.value) return
        locator.lostScreen.value = false
        locator.notifier.cancelLostScreen()
    }

    private fun isLocked(): Boolean =
        (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isKeyguardLocked

    private fun enterForeground(): Boolean = runCatching {
        val notification = locator.notifier.service(ShieldStatus.ARMADO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(Notifier.ID_SERVICE, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(Notifier.ID_SERVICE, notification)
        }
        true
    }.getOrDefault(false)

    private fun hasLocationPermission(): Boolean =
        checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun shutDown() {
        locator.coordinator.disarm()
        locator.ble.stopAdvertising()
        locator.ble.setLevel(BleHub.ScanLevel.OFF)
        locator.ble.stopServer()
        locator.tag.disconnect()
        hideLostScreen()
        locator.notifier.cancelLostScreen()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        running = false
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "com.dendra.app.STOP"
        private const val EXTRA_BOOT = "boot"
        private const val GUARDIAN_BEEP_SECONDS = 20
        private const val EVALUATE_MS = 15_000L
        private const val DEFAULT_REVEAL_MIN = 10

        @Volatile
        var running = false
            private set

        fun start(context: Context, fromBoot: Boolean = false) {
            val intent = Intent(context, EscudoService::class.java).putExtra(EXTRA_BOOT, fromBoot)
            runCatching { context.startForegroundService(intent) }
        }

        fun stop(context: Context) {
            if (!running) return
            runCatching {
                context.startService(Intent(context, EscudoService::class.java).setAction(ACTION_STOP))
            }
        }
    }
}
