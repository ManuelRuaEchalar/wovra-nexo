package com.redcentinela.escudo.data.reward

import android.content.SharedPreferences
import com.redcentinela.escudo.data.net.RelayClient
import com.redcentinela.escudo.domain.model.ShieldState
import com.redcentinela.escudo.domain.model.ShieldStatus
import com.redcentinela.escudo.service.EscudoConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Recompensa garantizada por devolver el celular perdido (Stellar).
 *
 * El dueño elige de antemano un monto en Ajustes. Cuando ESTE celular pasa a
 * "perdido", pide al servidor de recompensas que bloquee ese monto en un escrow
 * de Trustless Work (USDC en Stellar testnet). La pantalla de perdido muestra el
 * monto y un QR: quien lo encuentra reclama desde su propio teléfono. Cuando el
 * dueño recupera el celular y lo desbloquea, confirma desde aquí y el pago sale
 * a quien lo devolvió.
 *
 * Solo para pérdidas, nunca para robos: pagar por un celular robado incentivaría
 * robar para "devolverlo".
 */
data class Reward(
    val id: String,
    val code: String,
    val token: String,
    val claimUrl: String,
    val amountBs: Double,
    val amountUsdc: Double,
    val status: String,
    val claimerName: String? = null,
    val claimerContact: String? = null,
    val escrowUrl: String? = null,
    val lastTxUrl: String? = null,
    val error: String? = null,
    /** Inicio de la pérdida para la que se creó (alertSince): una pérdida = una recompensa. */
    val episode: Long = 0
) {
    val creating get() = status == "creando"
    val active get() = status == "activa"
    val claimed get() = status == "reclamada"
    val busy get() = status == "pagando" || status == "devolviendo"
    val paid get() = status == "pagada"
    val refunded get() = status == "devuelta"
    val failed get() = status == "error"
    val finished get() = paid || refunded

    /** Falló antes de bloquear el dinero: se puede descartar y volver a crear. */
    val replaceable get() = finished || (failed && escrowUrl == null)

    /** Se muestra en la pantalla de perdido: alguien todavía puede reclamarla. */
    val showOnLostScreen get() = creating || active || claimed

    fun toJson(): String = JSONObject()
        .put("id", id).put("code", code).put("token", token).put("claimUrl", claimUrl)
        .put("amountBs", amountBs).put("amountUsdc", amountUsdc).put("status", status)
        .putOpt("claimerName", claimerName).putOpt("claimerContact", claimerContact)
        .putOpt("escrowUrl", escrowUrl).putOpt("lastTxUrl", lastTxUrl).putOpt("error", error)
        .put("episode", episode)
        .toString()

    /** Mezcla la respuesta del servidor (vista del dueño) con lo guardado. */
    fun merge(j: JSONObject): Reward {
        val claim = j.optJSONObject("reclamo")
        val moves = j.optJSONArray("movimientos")
        val lastTx = moves?.takeIf { it.length() > 0 }?.optJSONObject(moves.length() - 1)?.optString("enlace")
        return copy(
            status = j.optString("estado", status),
            claimerName = claim?.optString("nombre") ?: j.optJSONObject("reclamante")?.optString("nombre"),
            claimerContact = claim?.optString("contacto"),
            escrowUrl = j.optJSONObject("enlaces")?.optString("escrow") ?: escrowUrl,
            lastTxUrl = lastTx?.ifBlank { null } ?: lastTxUrl,
            error = j.optString("error").ifBlank { null },
            code = j.optString("codigo").ifBlank { code },
            claimUrl = j.optString("urlReclamo").ifBlank { claimUrl }
        )
    }

    companion object {
        fun fromJson(s: String): Reward? = runCatching {
            val j = JSONObject(s)
            Reward(
                id = j.getString("id"), code = j.getString("code"), token = j.getString("token"),
                claimUrl = j.getString("claimUrl"), amountBs = j.getDouble("amountBs"),
                amountUsdc = j.getDouble("amountUsdc"), status = j.getString("status"),
                claimerName = j.optString("claimerName").ifBlank { null },
                claimerContact = j.optString("claimerContact").ifBlank { null },
                escrowUrl = j.optString("escrowUrl").ifBlank { null },
                lastTxUrl = j.optString("lastTxUrl").ifBlank { null },
                error = j.optString("error").ifBlank { null },
                episode = j.optLong("episode", 0)
            )
        }.getOrNull()
    }
}

class RewardException(message: String) : IOException(message)

/** Cliente HTTP del servidor de recompensas (recompensas/server). */
class RewardClient(private val baseUrl: () -> String) {

    suspend fun create(
        deviceId: String, ownerName: String, amountBs: Int, message: String, sharePct: Int = 0
    ): Pair<JSONObject, String> {
        val body = JSONObject()
            .put("dispositivoId", deviceId).put("nombreDueno", ownerName)
            .put("montoBs", amountBs).put("mensaje", message).put("porcentajeRed", sharePct)
        val r = call("POST", "/api/recompensas", body, null)
        return r.getJSONObject("recompensa") to r.getString("token")
    }

    suspend fun get(id: String, token: String) = call("GET", "/api/recompensas/$id", null, token)
    suspend fun confirm(id: String, token: String) = call("POST", "/api/recompensas/$id/confirmar", JSONObject(), token)
    suspend fun reject(id: String, token: String) = call("POST", "/api/recompensas/$id/rechazar", JSONObject(), token)
    suspend fun cancel(id: String, token: String) = call("POST", "/api/recompensas/$id/cancelar", JSONObject(), token)

    suspend fun call(method: String, path: String, body: JSONObject?, token: String? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val base = baseUrl().trimEnd('/')
            if (base.isBlank()) throw RewardException("Falta configurar el servidor de recompensas")
            val conn = (URL(base + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 10_000
                // Confirmar y cancelar esperan varias transacciones en la red.
                readTimeout = 90_000
                setRequestProperty("Accept", "application/json")
                if (token != null) setRequestProperty("Authorization", "Bearer $token")
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }
            }
            try {
                if (body != null) conn.outputStream.use { it.write(body.toString().toByteArray()) }
                val code = conn.responseCode
                val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = runCatching { JSONObject(text) }.getOrElse { JSONObject() }
                if (code !in 200..299) throw RewardException(json.optString("error").ifBlank { "Error $code del servidor" })
                json
            } finally {
                conn.disconnect()
            }
        }
}

/** Guarda la recompensa en curso (incluye el token del dueño: van en preferencias cifradas). */
class RewardStore(private val prefs: SharedPreferences) {
    fun load(): Reward? = prefs.getString(KEY, null)?.let(Reward::fromJson)
    fun save(r: Reward?) {
        prefs.edit().apply { if (r == null) remove(KEY) else putString(KEY, r.toJson()) }.apply()
    }

    private companion object {
        const val KEY = "reward"
    }
}

/**
 * Crea la recompensa al entrar en "perdido", la mantiene al día y expone las
 * acciones del dueño (confirmar, rechazar el reclamo, cancelar).
 */
class RewardManager(
    private val config: EscudoConfig,
    private val client: RewardClient,
    private val store: RewardStore,
    private val relay: RelayClient,
    private val scope: CoroutineScope,
    shield: StateFlow<ShieldState>
) {
    private val _reward = MutableStateFlow(store.load())
    val reward: StateFlow<Reward?> = _reward.asStateFlow()

    private val _working = MutableStateFlow(false)
    val working: StateFlow<Boolean> = _working.asStateFlow()

    /** Último error de creación (sin internet, servidor caído…), para mostrarlo en la app. */
    private val _problem = MutableStateFlow<String?>(null)
    val problem: StateFlow<String?> = _problem.asStateFlow()

    private val mutex = Mutex()

    /** Inicio de la pérdida en curso (alertSince), o null si el celular no está perdido. */
    @Volatile private var lostSince: Long? = null

    /**
     * Falta publicar la recompensa de ESTA pérdida: no hay ninguna creada para
     * ella, o la que hubo falló antes de bloquear el dinero. Una pérdida = una
     * recompensa, aunque después se pague o se cancele (y aunque la app se reinicie).
     */
    private val wantsReward: Boolean
        get() {
            val since = lostSince ?: return false
            val r = _reward.value ?: return true
            return r.episode != since || (r.failed && r.escrowUrl == null)
        }

    /** Reintentos espaciados (1, 2, 4… hasta 15 min) si el servidor o la red fallan. */
    @Volatile private var failures = 0
    @Volatile private var nextAttemptAt = 0L

    private fun backoff() {
        failures++
        nextAttemptAt = System.currentTimeMillis() + minOf(60_000L shl (failures - 1).coerceAtMost(4), 15 * 60_000L)
    }

    init {
        scope.launch {
            shield.map { if (it.status == ShieldStatus.ALERTA_PERDIDA) it.alertSince ?: 0L else null }
                .distinctUntilChanged().collect { since ->
                lostSince = since
                if (wantsReward) {
                    failures = 0
                    nextAttemptAt = 0L
                    ensureCreated()
                }
            }
        }
        scope.launch { poll() }
    }

    val enabled get() = config.rewardBs > 0

    private suspend fun ensureCreated() = mutex.withLock {
        val current = _reward.value
        val since = lostSince ?: return@withLock
        if (!enabled || !wantsReward || (current != null && !current.replaceable)) return@withLock
        if (System.currentTimeMillis() < nextAttemptAt) return@withLock
        if (!relay.hasInternet()) {
            _problem.value = "Sin internet: la recompensa se publicará al recuperar conexión."
            return@withLock
        }
        runCatching {
            val (json, token) = client.create(
                deviceId = config.deviceId,
                ownerName = config.userName.ifBlank { "su dueño" },
                amountBs = config.rewardBs,
                message = config.rewardMessage,
                sharePct = config.rewardSharePct
            )
            Reward(
                id = json.getString("id"), code = json.getString("codigo"), token = token,
                claimUrl = json.getString("urlReclamo"), amountBs = json.getDouble("montoBs"),
                amountUsdc = json.getDouble("montoUsdc"), status = json.getString("estado"),
                episode = since
            )
        }.onSuccess {
            _problem.value = null
            update(it)
        }.onFailure {
            backoff()
            _problem.value = it.message ?: "No se pudo publicar la recompensa"
        }
    }

    /** Mientras haya una recompensa abierta, la refresca; si falta crearla, reintenta. */
    private suspend fun poll() {
        while (true) {
            val r = _reward.value
            when {
                r == null || r.replaceable -> if (wantsReward && enabled) ensureCreated()
                relay.hasInternet() -> refresh()
            }
            delay(if (r?.creating == true || r?.busy == true) 4_000 else 15_000)
        }
    }

    suspend fun refresh() {
        val r = _reward.value ?: return
        runCatching { client.get(r.id, r.token) }.onSuccess { json ->
            val updated = r.merge(json)
            if (updated.failed && !r.failed) backoff()
            if (updated.active || updated.claimed) failures = 0
            update(updated)
        }
    }

    fun confirm() = act { client.confirm(it.id, it.token) }
    fun reject() = act { client.reject(it.id, it.token) }
    fun cancel() = act { client.cancel(it.id, it.token) }

    /** Olvida una recompensa terminada (pagada, devuelta o que falló sin bloquear dinero). */
    fun dismiss() {
        if (_reward.value?.replaceable == true) update(null)
    }

    private fun act(call: suspend (Reward) -> JSONObject) {
        val r = _reward.value ?: return
        if (_working.value) return
        _working.value = true
        scope.launch {
            runCatching { call(r) }
                .onSuccess { update(r.merge(it)); _problem.value = null }
                .onFailure { _problem.value = it.message; refresh() }
            _working.value = false
        }
    }

    private fun update(r: Reward?) {
        _reward.value = r
        store.save(r)
    }
}
