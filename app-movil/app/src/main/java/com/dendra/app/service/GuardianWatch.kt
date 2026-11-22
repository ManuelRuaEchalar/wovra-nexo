package com.dendra.app.service

import com.dendra.app.data.ble.BleHub
import com.dendra.app.data.circle.CircleStore
import com.dendra.app.domain.model.CircleMember
import com.dendra.app.domain.model.Presence
import com.dendra.app.domain.service.ProximityEstimator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * El guardián: vigila por Bluetooth que el celular protegido y el de su
 * acompañante sigan juntos. Si el aviso del otro deja de llegar, se separaron:
 * así se detecta el robo discreto o el olvido "en el momento".
 *
 * Corre en los dos teléfonos: en el del guardián dispara la alarma
 * ([onWatchedAway]); en el protegido deja constancia ([onGuardianAway]).
 */
class GuardianWatch(
    private val hub: BleHub,
    private val circle: CircleStore,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private val _presence = MutableStateFlow<Map<String, Presence>>(emptyMap())
    val presence: StateFlow<Map<String, Presence>> = _presence.asStateFlow()

    var onWatchedAway: (CircleMember) -> Unit = {}
    var onWatchedBack: (CircleMember) -> Unit = {}
    var onGuardianAway: (CircleMember) -> Unit = {}
    var onGuardianBack: (CircleMember) -> Unit = {}

    private val smoothed = HashMap<String, Double>()

    fun run(scope: CoroutineScope) {
        scope.launch {
            hub.sightings.collect { sighting ->
                val member = circle.byId(sighting.deviceId) ?: return@collect
                val rssi = smoothed[member.id]?.let { 0.3 * sighting.rssi + 0.7 * it } ?: sighting.rssi.toDouble()
                smoothed[member.id] = rssi
                val wasNear = _presence.value[member.id]?.near == true
                _presence.update {
                    it + (member.id to Presence(true, ProximityEstimator.metersFor(rssi), sighting.at))
                }
                if (!wasNear) {
                    if (member.iWatch) onWatchedBack(member)
                    if (member.isGuardian) onGuardianBack(member)
                }
            }
        }
        scope.launch {
            while (isActive) {
                delay(CHECK_MS)
                val now = clock()
                _presence.value.forEach { (id, presence) ->
                    if (!presence.near || now - presence.lastSeen < LOST_MS) return@forEach
                    _presence.update { it + (id to presence.copy(near = false, meters = null)) }
                    smoothed.remove(id)
                    val member = circle.byId(id) ?: return@forEach
                    if (member.iWatch) onWatchedAway(member)
                    if (member.isGuardian) onGuardianAway(member)
                }
            }
        }
    }

    private companion object {
        const val CHECK_MS = 4_000L
        /** Sin aviso durante este tiempo = fuera de alcance (unos 10-30 m). */
        const val LOST_MS = 25_000L
    }
}
