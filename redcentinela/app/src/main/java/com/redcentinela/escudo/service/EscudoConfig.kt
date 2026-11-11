package com.redcentinela.escudo.service

import android.content.Context
import com.redcentinela.escudo.data.circle.Card
import com.redcentinela.escudo.data.crypto.Crypto

/**
 * Identidad y ajustes del equipo. El identificador y el secreto se generan
 * una sola vez; son los que se comparten con el círculo al vincularse.
 */
class EscudoConfig(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("escudo_config", Context.MODE_PRIVATE)

    /** Identificador público corto (viaja en el aviso Bluetooth). */
    val deviceId: String by lazy { stored(KEY_ID) { Crypto.randomHex(4) } }

    /** Secreto del equipo: solo lo recibe el círculo. */
    val secret: String by lazy { stored(KEY_SECRET) { Crypto.randomHex(16) } }

    var userName: String
        get() = prefs.getString(KEY_NAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_NAME, value.trim()).apply()

    /** Número de este celular, para que el círculo pueda mandarle órdenes por SMS. */
    var phone: String?
        get() = prefs.getString(KEY_PHONE, null)
        set(value) = prefs.edit().putString(KEY_PHONE, value?.trim()?.ifBlank { null }).apply()

    var onboarded: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDED, value).apply()

    /** El usuario quiere el Escudo activo (se respeta al reiniciar). */
    var shieldEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_ENABLED, value).apply()

    var fakeShutdown: Boolean
        get() = prefs.getBoolean(KEY_FAKE, true)
        set(value) = prefs.edit().putBoolean(KEY_FAKE, value).apply()

    var beepOnSeparation: Boolean
        get() = prefs.getBoolean(KEY_BEEP, true)
        set(value) = prefs.edit().putBoolean(KEY_BEEP, value).apply()

    /**
     * Minutos bloqueado, sin ningún canal y lejos del guardián tras los que el
     * equipo entra en alerta solo. 0 = desactivado.
     */
    var selfAlertMinutes: Int
        get() = prefs.getInt(KEY_SELF_ALERT, 10)
        set(value) = prefs.edit().putInt(KEY_SELF_ALERT, value).apply()

    /**
     * Servidor de relevo para el canal de internet. Habla el protocolo de
     * ntfy (publicar/suscribir por HTTP); el servidor propio del equipo puede
     * reemplazarlo cambiando esta URL.
     */
    var relayUrl: String
        get() = prefs.getString(KEY_RELAY, null)?.ifBlank { null } ?: DEFAULT_RELAY
        set(value) = prefs.edit().putString(KEY_RELAY, value.trim().trimEnd('/')).apply()

    /** Dirección Bluetooth del llavero guardián enlazado (iTag). */
    var tagAddress: String?
        get() = prefs.getString(KEY_TAG, null)
        set(value) = prefs.edit().putString(KEY_TAG, value).apply()

    var tagName: String?
        get() = prefs.getString(KEY_TAG_NAME, null)
        set(value) = prefs.edit().putString(KEY_TAG_NAME, value).apply()

    /** Hora (seg) del último mensaje de la bandeja ya procesado. */
    var inboxSince: Long
        get() = prefs.getLong(KEY_INBOX, 0)
        set(value) = prefs.edit().putLong(KEY_INBOX, value).apply()

    /** Marca de la última orden aplicada: evita repetir órdenes viejas. */
    var lastCommandAt: Long
        get() = prefs.getLong(KEY_CMD, 0)
        set(value) = prefs.edit().putLong(KEY_CMD, value).apply()

    fun myCard(): String = Card.encode(userName.ifBlank { "Sin nombre" }, deviceId, secret, phone)

    private fun stored(key: String, create: () -> String): String =
        prefs.getString(key, null) ?: create().also { prefs.edit().putString(key, it).apply() }

    companion object {
        const val DEFAULT_RELAY = "https://ntfy.sh"
        private const val KEY_ID = "device_id"
        private const val KEY_SECRET = "secret"
        private const val KEY_NAME = "name"
        private const val KEY_PHONE = "phone"
        private const val KEY_ONBOARDED = "onboarded"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_FAKE = "fake_shutdown"
        private const val KEY_BEEP = "beep"
        private const val KEY_SELF_ALERT = "self_alert_min"
        private const val KEY_RELAY = "relay"
        private const val KEY_TAG = "tag"
        private const val KEY_TAG_NAME = "tag_name"
        private const val KEY_INBOX = "inbox_since"
        private const val KEY_CMD = "last_cmd"
    }
}
