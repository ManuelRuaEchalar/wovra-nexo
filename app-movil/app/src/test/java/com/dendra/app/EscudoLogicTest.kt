package com.dendra.app

import com.dendra.app.data.ble.BleCodec
import com.dendra.app.domain.model.ProximityTrend
import com.dendra.app.domain.model.RemoteCommand
import com.dendra.app.domain.model.ShieldStatus
import com.dendra.app.domain.service.DirectionFinder
import com.dendra.app.domain.service.IsolationPolicy
import com.dendra.app.domain.service.IsolationPolicy.Screen
import com.dendra.app.domain.service.ProximityEstimator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos

class EscudoLogicTest {

    private val secret = "00112233445566778899aabbccddeeff"
    private val min = 60_000L

    // --- Orden por Bluetooth ---------------------------------------------------

    @Test
    fun `la orden por bluetooth cabe en una escritura y vuelve intacta`() {
        val bytes = BleCodec.order(RemoteCommand.SONAR, 1_760_000_000_000, "a1b2c3d4", secret)
        assertTrue(bytes.size <= 20)
        val order = BleCodec.readOrder(bytes, secret)!!
        assertEquals(RemoteCommand.SONAR, order.command)
        assertEquals("a1b2c3d4", order.fromId)
        assertEquals(1_760_000_001_000, order.at)
    }

    @Test
    fun `una orden firmada con otro secreto o alterada se rechaza`() {
        val bytes = BleCodec.order(RemoteCommand.ROBO, 1_760_000_000_000, "a1b2c3d4", secret)
        assertNull(BleCodec.readOrder(bytes, secret.reversed()))
        bytes[0] = RemoteCommand.SONAR.ordinal.toByte()
        assertNull(BleCodec.readOrder(bytes, secret))
        assertNull(BleCodec.readOrder(ByteArray(5), secret))
    }

    // --- Cercanía ----------------------------------------------------------------

    @Test
    fun `las caidas sueltas de senal no mueven la lectura`() {
        val estimator = ProximityEstimator()
        var last = 0
        for (i in 0 until 40) {
            val rssi = if (i % 4 == 3) -85 else -60   // una de cada cuatro lecturas, tapada
            last = estimator.onRssi(rssi, i * 150L).rssiDbm
        }
        assertTrue("lectura $last", last >= -62)
    }

    @Test
    fun `la tendencia sigue al movimiento y el pitido se acelera`() {
        val estimator = ProximityEstimator()
        var trend = ProximityTrend.ESTABLE
        for (i in 0 until 40) trend = estimator.onRssi(-85 + i, i * 150L).trend
        assertEquals(ProximityTrend.ACERCANDOSE, trend)
        val far = ProximityEstimator.feedbackFor(-88).first
        val near = ProximityEstimator.feedbackFor(-60).first
        assertTrue(far > near * 3)
        assertTrue(ProximityEstimator.feedbackFor(-48).second)
    }

    // --- Dirección -----------------------------------------------------------------

    @Test
    fun `girando una vuelta encuentra hacia donde la senal es mas fuerte`() {
        val finder = DirectionFinder()
        var sweep: DirectionFinder.Sweep? = null
        // El equipo está al este (90°): el cuerpo quita hasta 12 dB mirando al oeste.
        for (step in 0 until 72) {
            val azimuth = step * 5f
            val rssi = -70 + 6 * cos(Math.toRadians(azimuth - 90.0)) + (if (step % 3 == 0) -2 else 1)
            sweep = finder.onReading(azimuth, rssi.toInt(), step * 200L)
        }
        val bearing = sweep!!.bearing
        assertNotNull(bearing)
        assertTrue("rumbo $bearing", abs(bearing!! - 90f) < 25f)
    }

    @Test
    fun `sin vuelta completa o con senal pareja no inventa direccion`() {
        val half = DirectionFinder()
        var sweep = half.onReading(0f, -60, 0)
        for (step in 1 until 20) sweep = half.onReading(step * 5f, -60 - step, step * 200L)
        assertNull(sweep.bearing)

        val flat = DirectionFinder()
        for (step in 0 until 72) sweep = flat.onReading(step * 5f, -70 + step % 2, step * 200L)
        assertNull(sweep.bearing)
    }

    // --- Aislamiento -----------------------------------------------------------------

    private fun facts(
        status: ShieldStatus = ShieldStatus.ARMADO, locked: Boolean = true, reachable: Boolean = false,
        hasGuardian: Boolean = true, guardianNear: Boolean = false, aloneSince: Long? = 0,
        lastContactAt: Long = 0, alertSince: Long? = null, fake: Boolean = false, revealed: Boolean = false
    ) = IsolationPolicy.Facts(
        status, locked, reachable, hasGuardian, guardianNear, aloneSince, lastContactAt, alertSince, fake, revealed
    )

    @Test
    fun `alerta sola solo si esta bloqueado sin canal y lejos del guardian el tiempo fijado`() {
        assertTrue(IsolationPolicy.shouldSelfAlert(facts(), 10 * min, 10 * min))
        assertFalse(IsolationPolicy.shouldSelfAlert(facts(), 9 * min, 10 * min))
        assertFalse(IsolationPolicy.shouldSelfAlert(facts(locked = false), 20 * min, 10 * min))
        assertFalse(IsolationPolicy.shouldSelfAlert(facts(reachable = true), 20 * min, 10 * min))
        assertFalse(IsolationPolicy.shouldSelfAlert(facts(guardianNear = true), 20 * min, 10 * min))
        assertFalse(IsolationPolicy.shouldSelfAlert(facts(hasGuardian = false), 20 * min, 10 * min))
        assertFalse(IsolationPolicy.shouldSelfAlert(facts(status = ShieldStatus.ALERTA_ROBO), 20 * min, 10 * min))
        assertFalse(IsolationPolicy.shouldSelfAlert(facts(), 20 * min, 0))
    }

    @Test
    fun `con comunicacion el apagado falso manda sobre la pantalla de perdido`() {
        val stolen = ShieldStatus.ALERTA_ROBO
        val lost = ShieldStatus.ALERTA_PERDIDA
        // Reportado como perdido mientras finge estar apagado, con internet: sigue a oscuras.
        assertEquals(Screen.APAGADO_FALSO, IsolationPolicy.screen(facts(lost, reachable = true, alertSince = 0, fake = true), 60 * min, 10 * min))
        // Sin internet ni SMS, pero un Bluetooth del círculo lo vio hace 2 min: sigue a oscuras.
        assertEquals(Screen.APAGADO_FALSO, IsolationPolicy.screen(facts(stolen, lastContactAt = 58 * min, alertSince = 0, fake = true), 60 * min, 10 * min))
        // Ya no queda ningún canal desde hace 10 min: se muestra cómo devolverlo.
        assertEquals(Screen.PERDIDO, IsolationPolicy.screen(facts(stolen, lastContactAt = 50 * min, alertSince = 0, fake = true), 60 * min, 10 * min))
        // Una vez mostrada, se queda aunque vuelva la señal.
        assertEquals(Screen.PERDIDO, IsolationPolicy.screen(facts(stolen, reachable = true, alertSince = 0, revealed = true), 61 * min, 10 * min))
        // Sin apagado falso: perdido muestra la pantalla, robado no.
        assertEquals(Screen.PERDIDO, IsolationPolicy.screen(facts(lost, alertSince = 0), min, 10 * min))
        assertEquals(Screen.NORMAL, IsolationPolicy.screen(facts(stolen, alertSince = 0), min, 10 * min))
        assertEquals(Screen.NORMAL, IsolationPolicy.screen(facts(fake = true), min, 10 * min))
    }
}
