package com.dendra.app.data.ble

import com.dendra.app.data.crypto.Crypto
import com.dendra.app.domain.model.RemoteCommand
import java.nio.ByteBuffer

/** Lo que otro teléfono de la red capta de un equipo con Escudo. */
class Sighting(
    val deviceId: String,
    /** 0 = Escudo activo, 1 = robado, 2 = perdido. */
    val status: Int,
    val counter: Int,
    val raw: ByteArray,
    val rssi: Int,
    val at: Long
) {
    val inAlert get() = status != BleCodec.STATUS_ARMED
}

class BleLocation(val latitude: Double, val longitude: Double, val batteryPct: Int)

/**
 * Formato del aviso Bluetooth (BLE advertising). Un aviso admite ~24 bytes
 * útiles, así que va todo en binario:
 *
 *   [0] marca  [1] estado  [2..5] id del equipo  [6..7] contador
 *   [8..16] latitud, longitud y batería cifradas  [17] verificación
 *
 * El id identifica al equipo para su círculo; la ubicación solo se incluye
 * en alerta y va cifrada con el secreto del equipo, de modo que un teléfono
 * ajeno puede reenviar el aviso ("guardar y reenviar") sin poder leerlo.
 */
object BleCodec {
    /** ID de fabricante del aviso (valor de prototipo). */
    const val MANUFACTURER_ID = 0x05C1
    const val MAGIC: Byte = 0xC7.toByte()
    const val STATUS_ARMED = 0
    const val STATUS_STOLEN = 1
    const val STATUS_LOST = 2
    private const val SIZE = 18

    fun build(
        deviceId: String,
        secret: String,
        status: Int,
        counter: Int,
        latitude: Double?,
        longitude: Double?,
        batteryPct: Int
    ): ByteArray {
        val buf = ByteBuffer.allocate(SIZE)
        buf.put(MAGIC).put(status.toByte()).put(Crypto.unhex(deviceId)).putShort(counter.toShort())
        val plain = ByteBuffer.allocate(9)
        if (latitude != null && longitude != null && status != STATUS_ARMED) {
            plain.putInt((latitude * 1_000_000).toInt()).putInt((longitude * 1_000_000).toInt())
            plain.put(batteryPct.coerceIn(0, 100).toByte())
        } else {
            plain.putInt(NO_FIX).putInt(NO_FIX).put(batteryPct.coerceIn(0, 100).toByte())
        }
        val mask = mask(secret, counter)
        val body = plain.array()
        for (i in body.indices) buf.put((body[i].toInt() xor mask[i].toInt()).toByte())
        buf.put(check(secret, counter, body))
        return buf.array()
    }

    fun parse(data: ByteArray?, rssi: Int, at: Long): Sighting? {
        if (data == null || data.size < SIZE || data[0] != MAGIC) return null
        return Sighting(
            deviceId = Crypto.hex(data.copyOfRange(2, 6)),
            status = data[1].toInt(),
            counter = ByteBuffer.wrap(data, 6, 2).short.toInt() and 0xFFFF,
            raw = data.copyOf(SIZE),
            rssi = rssi,
            at = at
        )
    }

    /** Descifra la ubicación de un aviso con el secreto de su dueño. */
    fun locate(sighting: Sighting, secret: String): BleLocation? {
        val mask = mask(secret, sighting.counter)
        val body = ByteArray(9) { (sighting.raw[8 + it].toInt() xor mask[it].toInt()).toByte() }
        if (sighting.raw[17] != check(secret, sighting.counter, body)) return null
        val buf = ByteBuffer.wrap(body)
        val lat = buf.int
        val lng = buf.int
        if (lat == NO_FIX) return null
        return BleLocation(lat / 1_000_000.0, lng / 1_000_000.0, buf.get().toInt())
    }

    /**
     * Orden directa por Bluetooth (conexión entre los dos teléfonos), en los
     * 20 bytes que admite una escritura sin negociar nada:
     *
     *   [0] orden  [1..4] hora (s)  [5..8] id de quien la da  [9..16] firma
     *
     * La firma usa el secreto del equipo que la recibe: solo su círculo puede darla.
     */
    fun order(command: RemoteCommand, atMillis: Long, fromId: String, targetSecret: String): ByteArray {
        val head = ByteBuffer.allocate(ORDER_HEAD)
            .put(command.ordinal.toByte()).putInt((atMillis / 1000 + 1).toInt()).put(Crypto.unhex(fromId)).array()
        return head + Crypto.hmac(targetSecret, ORDER_SALT + head).copyOf(ORDER_TAG)
    }

    class Order(val command: RemoteCommand, val at: Long, val fromId: String)

    /** Inverso de [order]; null si no viene firmada con [mySecret]. */
    fun readOrder(data: ByteArray?, mySecret: String): Order? {
        if (data == null || data.size != ORDER_HEAD + ORDER_TAG) return null
        val head = data.copyOf(ORDER_HEAD)
        val tag = Crypto.hmac(mySecret, ORDER_SALT + head).copyOf(ORDER_TAG)
        if (!tag.contentEquals(data.copyOfRange(ORDER_HEAD, data.size))) return null
        val buf = ByteBuffer.wrap(head)
        val command = RemoteCommand.entries.getOrNull(buf.get().toInt()) ?: return null
        val at = (buf.int.toLong() and 0xFFFFFFFFL) * 1000
        return Order(command, at, Crypto.hex(head.copyOfRange(5, 9)))
    }

    private const val ORDER_HEAD = 9
    private const val ORDER_TAG = 8
    private val ORDER_SALT = "orden-ble".toByteArray()

    private fun mask(secret: String, counter: Int): ByteArray =
        Crypto.hmac(secret, "ble-$counter".toByteArray())

    private fun check(secret: String, counter: Int, plain: ByteArray): Byte =
        Crypto.hmac(secret, "chk-$counter".toByteArray() + plain)[0]

    private const val NO_FIX = Int.MIN_VALUE
}
