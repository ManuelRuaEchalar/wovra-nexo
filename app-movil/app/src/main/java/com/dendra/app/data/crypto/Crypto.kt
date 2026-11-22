package com.dendra.app.data.crypto

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Criptografía del círculo. Todo lo que sale del teléfono (servidor de
 * relevo, SMS, Bluetooth) va cifrado o firmado con el secreto del equipo,
 * que solo conoce su círculo: "tu ubicación solo la ven tú y tu círculo".
 */
object Crypto {
    private val random = SecureRandom()
    private const val B64 = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING

    fun randomHex(bytes: Int): String = hex(ByteArray(bytes).also(random::nextBytes))

    fun hex(bytes: ByteArray): String = bytes.joinToString("") { "%02x".format(it) }

    fun unhex(hex: String): ByteArray =
        ByteArray(hex.length / 2) { hex.substring(it * 2, it * 2 + 2).toInt(16).toByte() }

    private fun sha256(data: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(data)

    private fun derive(secretHex: String, purpose: String): ByteArray =
        sha256(unhex(secretHex) + purpose.toByteArray())

    /** Nombre del tema en el servidor de relevo: imposible de adivinar sin el secreto. */
    fun topic(secretHex: String, kind: String): String =
        "rc" + hex(derive(secretHex, "topic-$kind")).take(24)

    /** Cifra [plain] con AES-256-GCM. Devuelve base64url(nonce | cifrado). */
    fun seal(secretHex: String, plain: String): String {
        val nonce = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(derive(secretHex, "key"), "AES"), GCMParameterSpec(128, nonce))
        return Base64.encodeToString(nonce + cipher.doFinal(plain.toByteArray()), B64)
    }

    /** Inverso de [seal]; null si no se pudo abrir (otro secreto o alterado). */
    fun open(secretHex: String, sealed: String): String? = runCatching {
        val raw = Base64.decode(sealed.trim(), B64)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(derive(secretHex, "key"), "AES"),
            GCMParameterSpec(128, raw, 0, 12)
        )
        String(cipher.doFinal(raw, 12, raw.size - 12))
    }.getOrNull()

    fun hmac(secretHex: String, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(derive(secretHex, "mac"), "HmacSHA256"))
        return mac.doFinal(data)
    }

    /** Firma corta (para SMS, donde cada carácter cuenta). */
    fun tag(secretHex: String, data: String, hexChars: Int = 8): String =
        hex(hmac(secretHex, data.toByteArray())).take(hexChars)

    fun b64(bytes: ByteArray): String = Base64.encodeToString(bytes, B64)
    fun unb64(text: String): ByteArray? = runCatching { Base64.decode(text.trim(), B64) }.getOrNull()
}
