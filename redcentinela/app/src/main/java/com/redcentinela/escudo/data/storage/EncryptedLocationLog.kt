package com.redcentinela.escudo.data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.redcentinela.escudo.domain.model.GeoPoint
import com.redcentinela.escudo.domain.model.LocationSource
import com.redcentinela.escudo.domain.port.LocationLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Bitácora de ubicaciones cifrada en el dispositivo, usando
 * EncryptedSharedPreferences (clave respaldada por el keystore de Android).
 *
 * Cumple el requisito de la propuesta: "Todo se guarda cifrado en el
 * teléfono y sale por la cascada". Cada punto lleva una marca de enviado
 * para el esquema guardar-y-reenviar.
 *
 * Para el prototipo guarda la bitácora como un arreglo JSON. El acceso se
 * serializa con un Mutex y se ejecuta fuera del hilo principal.
 */
class EncryptedLocationLog(context: Context) : LocationLog {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            "escudo_bitacora",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val mutex = Mutex()

    override suspend fun append(point: GeoPoint) = withContext(Dispatchers.IO) {
        mutex.withLock {
            var arr = readArray()
            arr.put(point.toJson(sent = false))
            // Tope de la bitácora: se descartan los puntos más viejos.
            if (arr.length() > MAX_POINTS) {
                val trimmed = JSONArray()
                for (i in arr.length() - MAX_POINTS until arr.length()) trimmed.put(arr.get(i))
                arr = trimmed
            }
            writeArray(arr)
        }
    }

    override suspend fun pending(): List<GeoPoint> = withContext(Dispatchers.IO) {
        mutex.withLock { readArray().toPoints { !it.getBoolean(KEY_SENT) } }
    }

    override suspend fun pendingCount(): Int = pending().size

    override suspend fun markSentUpTo(capturedAtInclusive: Long) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val arr = readArray()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.getLong(KEY_AT) <= capturedAtInclusive) {
                    obj.put(KEY_SENT, true)
                }
            }
            writeArray(arr)
        }
    }

    override suspend fun recent(limit: Int): List<GeoPoint> = withContext(Dispatchers.IO) {
        mutex.withLock {
            readArray().toPoints().sortedByDescending { it.capturedAt }.take(limit)
        }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        mutex.withLock { prefs.edit().remove(KEY_LOG).apply() }
    }

    // --- JSON helpers ------------------------------------------------------

    private fun readArray(): JSONArray =
        prefs.getString(KEY_LOG, null)?.let { JSONArray(it) } ?: JSONArray()

    private fun writeArray(arr: JSONArray) {
        prefs.edit().putString(KEY_LOG, arr.toString()).apply()
    }

    private inline fun JSONArray.toPoints(
        predicate: (JSONObject) -> Boolean = { true }
    ): List<GeoPoint> {
        val out = ArrayList<GeoPoint>(length())
        for (i in 0 until length()) {
            val obj = getJSONObject(i)
            if (predicate(obj)) out.add(obj.toPoint())
        }
        return out
    }

    private fun GeoPoint.toJson(sent: Boolean): JSONObject = JSONObject().apply {
        put(KEY_LAT, latitude)
        put(KEY_LNG, longitude)
        put(KEY_ACC, accuracyMeters.toDouble())
        put(KEY_SRC, source.name)
        put(KEY_SPD, speedMps?.toDouble() ?: JSONObject.NULL)
        put(KEY_BRG, bearingDeg?.toDouble() ?: JSONObject.NULL)
        put(KEY_AT, capturedAt)
        put(KEY_SENT, sent)
    }

    private fun JSONObject.toPoint(): GeoPoint = GeoPoint(
        latitude = getDouble(KEY_LAT),
        longitude = getDouble(KEY_LNG),
        accuracyMeters = getDouble(KEY_ACC).toFloat(),
        source = runCatching { LocationSource.valueOf(getString(KEY_SRC)) }
            .getOrDefault(LocationSource.DESCONOCIDA),
        speedMps = if (isNull(KEY_SPD)) null else getDouble(KEY_SPD).toFloat(),
        bearingDeg = if (isNull(KEY_BRG)) null else getDouble(KEY_BRG).toFloat(),
        capturedAt = getLong(KEY_AT)
    )

    private companion object {
        const val KEY_LOG = "log"
        const val KEY_LAT = "lat"
        const val KEY_LNG = "lng"
        const val KEY_ACC = "acc"
        const val KEY_SRC = "src"
        const val KEY_SPD = "spd"
        const val KEY_BRG = "brg"
        const val KEY_AT = "at"
        const val KEY_SENT = "sent"
        const val MAX_POINTS = 1500
    }
}
