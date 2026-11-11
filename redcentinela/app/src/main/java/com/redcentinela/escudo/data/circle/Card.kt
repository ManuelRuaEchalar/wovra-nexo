package com.redcentinela.escudo.data.circle

import com.redcentinela.escudo.data.crypto.Crypto
import com.redcentinela.escudo.domain.model.CircleMember
import org.json.JSONObject

/**
 * "Tarjeta" con la que dos teléfonos se vinculan al círculo: viaja en el
 * código QR o en el texto de la invitación. Contiene nombre, identificador,
 * secreto y (si lo puso) número de celular para el respaldo por SMS.
 */
object Card {
    private const val PREFIX = "RC1:"
    private val PATTERN = Regex("RC1:([A-Za-z0-9_-]{20,})")

    fun encode(name: String, id: String, secret: String, phone: String?): String {
        val json = JSONObject().put("n", name).put("i", id).put("s", secret)
        if (!phone.isNullOrBlank()) json.put("p", phone)
        return PREFIX + Crypto.b64(json.toString().toByteArray())
    }

    /** Busca una tarjeta dentro de cualquier texto (QR o mensaje pegado). */
    fun decode(text: String): CircleMember? = runCatching {
        val match = PATTERN.find(text) ?: return null
        val json = JSONObject(String(Crypto.unb64(match.groupValues[1]) ?: return null))
        val id = json.getString("i")
        val secret = json.getString("s")
        if (!id.matches(Regex("[0-9a-f]{8}")) || !secret.matches(Regex("[0-9a-f]{32}"))) return null
        CircleMember(
            id = id,
            name = json.getString("n").take(30).ifBlank { "Sin nombre" },
            secret = secret,
            phone = json.optString("p").ifBlank { null }
        )
    }.getOrNull()
}
