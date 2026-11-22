package com.dendra.app.data.comm

import com.dendra.app.data.crypto.Crypto
import com.dendra.app.domain.model.RemoteCommand
import java.text.Normalizer
import java.util.Locale

/**
 * Formato de los SMS de la cascada. Cada SMS lleva un texto que cualquier
 * persona entiende (con enlace al mapa) y, al final, una cola `#RC1,...`
 * firmada que la app del destinatario interpreta sola.
 *
 *   Ubicación:  #RC1,P,<id>,<lat>,<lng>,<precisión>,<hora>,<R|L>,<batería>,<firma>
 *   Orden:      #RC1,C,<id destino>,<orden>,<hora>,<id origen>,<firma>
 *   Encontrado: #RC1,F,<id>,<lat>,<lng>,<hora>,<firma>
 *
 * La firma usa el secreto del equipo, así nadie ajeno al círculo puede
 * falsificar una ubicación ni una orden.
 */
object SmsCodec {
    private val TAIL = Regex("#RC1,([A-Za-z0-9,.\\-]+)")

    sealed interface Parsed {
        val deviceId: String
    }

    data class Position(
        override val deviceId: String,
        val latitude: Double,
        val longitude: Double,
        val accuracy: Int,
        val at: Long,
        val stolen: Boolean,
        val batteryPct: Int,
        private val signed: String,
        private val tag: String
    ) : Parsed {
        fun verify(secret: String) = Crypto.tag(secret, signed) == tag
    }

    data class Order(
        override val deviceId: String,
        val command: RemoteCommand,
        val at: Long,
        val fromId: String,
        private val signed: String,
        private val tag: String
    ) : Parsed {
        fun verify(secret: String) = Crypto.tag(secret, signed) == tag
    }

    data class FoundNotice(
        override val deviceId: String,
        val latitude: Double?,
        val longitude: Double?,
        val at: Long,
        private val signed: String,
        private val tag: String
    ) : Parsed {
        fun verify(secret: String) = Crypto.tag(secret, signed) == tag
    }

    fun position(
        name: String, id: String, secret: String,
        latitude: Double, longitude: Double, accuracy: Int,
        at: Long, stolen: Boolean, batteryPct: Int
    ): String {
        val lat = coord(latitude)
        val lng = coord(longitude)
        val body = "P,$id,$lat,$lng,$accuracy,${stamp(at)},${if (stolen) "R" else "L"},$batteryPct"
        val state = if (stolen) "ROBADO" else "PERDIDO"
        return "Dendra: celular de ${plain(name)} $state. " +
            "https://maps.google.com/?q=$lat,$lng #RC1,$body,${Crypto.tag(secret, body)}"
    }

    fun order(targetId: String, targetSecret: String, command: RemoteCommand, at: Long, fromId: String): String {
        val body = "C,$targetId,${command.code},${stamp(at)},$fromId"
        return "Dendra #RC1,$body,${Crypto.tag(targetSecret, body)}"
    }

    fun found(name: String, id: String, secret: String, latitude: Double?, longitude: Double?, at: Long): String {
        val lat = latitude?.let(::coord) ?: "x"
        val lng = longitude?.let(::coord) ?: "x"
        val body = "F,$id,$lat,$lng,${stamp(at)}"
        val where = if (latitude != null) " https://maps.google.com/?q=$lat,$lng" else ""
        return "Dendra: alguien encontro el celular de ${plain(name)}.$where " +
            "#RC1,$body,${Crypto.tag(secret, body)}"
    }

    fun parse(text: String): Parsed? = runCatching {
        val f = (TAIL.find(text) ?: return null).groupValues[1].split(',')
        val signed = f.dropLast(1).joinToString(",")
        val tag = f.last()
        when (f[0]) {
            "P" -> Position(
                deviceId = f[1], latitude = f[2].toDouble(), longitude = f[3].toDouble(),
                accuracy = f[4].toInt(), at = unstamp(f[5]), stolen = f[6] == "R",
                batteryPct = f[7].toInt(), signed = signed, tag = tag
            )
            "C" -> Order(
                deviceId = f[1], command = RemoteCommand.fromCode(f[2]) ?: return null,
                at = unstamp(f[3]), fromId = f[4], signed = signed, tag = tag
            )
            "F" -> FoundNotice(
                deviceId = f[1], latitude = f[2].toDoubleOrNull(), longitude = f[3].toDoubleOrNull(),
                at = unstamp(f[4]), signed = signed, tag = tag
            )
            else -> null
        }
    }.getOrNull()

    private fun coord(value: Double) = String.format(Locale.US, "%.5f", value)
    private fun stamp(millis: Long) = (millis / 1000).toString(36)
    private fun unstamp(text: String) = text.toLong(36) * 1000

    /** Sin tildes ni símbolos: mantiene el SMS en 160 caracteres. */
    private fun plain(name: String): String =
        Normalizer.normalize(name, Normalizer.Form.NFD)
            .replace(Regex("[^A-Za-z0-9 ]"), "").trim().take(14).ifEmpty { "tu contacto" }
}
