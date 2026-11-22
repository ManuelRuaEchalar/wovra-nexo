package com.dendra.app.data.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicReference

data class RelayMessage(val id: String, val timeSec: Long, val topic: String, val body: String)

/**
 * Cliente del servidor de relevo (canal de internet de la cascada). Usa el
 * protocolo de ntfy: `POST /tema` publica y `GET /tema1,tema2/json` mantiene
 * abierta una conexión por la que llegan los mensajes. El servidor guarda
 * los mensajes unas horas, así un teléfono que estuvo sin conexión recibe lo
 * que se perdió ("guardar y reenviar"). Solo ve texto cifrado.
 */
class RelayClient(context: Context, private val baseUrl: () -> String) {

    private val appContext = context.applicationContext

    fun hasInternet(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    suspend fun publish(topic: String, body: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL("${baseUrl()}/$topic").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8_000
                readTimeout = 8_000
                doOutput = true
                setRequestProperty("Content-Type", "text/plain; charset=utf-8")
            }
            try {
                conn.outputStream.use { it.write(body.toByteArray()) }
                conn.responseCode in 200..299
            } finally {
                conn.disconnect()
            }
        }.getOrDefault(false)
    }

    /**
     * Escucha [topics] sin fin, reconectando sola. [since] se consulta en cada
     * (re)conexión: una hora en segundos, un id de mensaje o "12h".
     */
    fun stream(topics: List<String>, since: () -> String): Flow<RelayMessage> = channelFlow {
        val open = AtomicReference<HttpURLConnection?>()
        launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val url = "${baseUrl()}/${topics.joinToString(",")}/json?since=${since()}"
                    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 10_000
                        // El servidor manda un "keepalive" cada ~45 s.
                        readTimeout = 100_000
                    }
                    open.set(conn)
                    conn.inputStream.bufferedReader().use { reader ->
                        while (isActive) {
                            val line = reader.readLine() ?: break
                            parse(line)?.let { send(it) }
                        }
                    }
                } catch (_: Exception) {
                    // Sin red o conexión cortada: se reintenta abajo.
                }
                open.getAndSet(null)?.disconnect()
                delay(RETRY_MS)
            }
        }
        // Cerrar el socket desbloquea la lectura al cancelar el flujo.
        awaitClose {
            val conn = open.getAndSet(null)
            Thread { runCatching { conn?.disconnect() } }.start()
        }
    }

    private fun parse(line: String): RelayMessage? = runCatching {
        val json = JSONObject(line)
        if (json.optString("event") != "message") return null
        RelayMessage(
            id = json.getString("id"),
            timeSec = json.getLong("time"),
            topic = json.getString("topic"),
            body = json.optString("message")
        )
    }.getOrNull()

    private companion object {
        const val RETRY_MS = 4_000L
    }
}
