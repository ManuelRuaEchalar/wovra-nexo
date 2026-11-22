package com.dendra.app.data.rescue

import com.dendra.app.data.crypto.Crypto
import com.dendra.app.data.reward.RewardClient
import com.dendra.app.service.EscudoConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Cuenta de rescate para el panel web: permite seguir y controlar este
 * celular desde cualquier navegador, sin depender de alguien del círculo.
 *
 * La contraseña nunca sale del teléfono. Con PBKDF2 se derivan 64 bytes:
 * los primeros 32 cifran (AES-GCM) la identidad del equipo; los otros 32 son
 * el "verificador" que prueba al servidor que quien entra conoce la
 * contraseña. El servidor guarda el bloque cifrado y no puede abrirlo.
 * El navegador (frontend, /panel) hace la misma derivación.
 */
class RescueManager(private val config: EscudoConfig, private val client: RewardClient) {

    val email: String? get() = config.rescueEmail

    /** Crea o actualiza la cuenta. Lanza una excepción con un mensaje para el usuario. */
    suspend fun setUp(email: String, password: String) {
        val correo = email.trim().lowercase()
        require(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(correo)) { "Escribe un correo válido." }
        require(password.length >= MIN_PASSWORD) { "La contraseña debe tener al menos $MIN_PASSWORD caracteres." }
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val derived = withContext(Dispatchers.Default) { derive(password, salt, ITERATIONS) }
        val plain = JSONObject()
            .put("v", 1).put("id", config.deviceId).put("s", config.secret)
            .put("n", config.userName).put("relay", config.relayUrl)
        val body = JSONObject()
            .put("correo", correo).put("idEquipo", config.deviceId)
            .put("sal", Crypto.hex(salt)).put("iteraciones", ITERATIONS)
            .put("verificador", Crypto.hex(derived.copyOfRange(32, 64)))
            .put("tokenEquipo", config.rescueToken)
            .put("bloque", seal(derived.copyOfRange(0, 32), plain.toString()))
        client.call("POST", "/api/rescate", body)
        config.rescueEmail = correo
    }

    suspend fun remove() {
        client.call("POST", "/api/rescate/borrar", JSONObject().put("tokenEquipo", config.rescueToken))
        config.rescueEmail = null
    }

    companion object {
        const val ITERATIONS = 210_000
        const val MIN_PASSWORD = 8

        fun derive(password: String, salt: ByteArray, iterations: Int): ByteArray {
            val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 512)
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        }

        private fun seal(key: ByteArray, plain: String): String {
            val nonce = ByteArray(12).also(SecureRandom()::nextBytes)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
            return Crypto.b64(nonce + cipher.doFinal(plain.toByteArray()))
        }
    }
}
