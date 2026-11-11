package com.redcentinela.escudo.data.storage

import android.content.Context
import com.redcentinela.escudo.domain.model.EventEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Registro de lo que le pasó al equipo: base del "Registro del robo". */
class EventLog(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("escudo_eventos", Context.MODE_PRIVATE)

    private val _entries = MutableStateFlow(load())
    val entries: StateFlow<List<EventEntry>> = _entries.asStateFlow()

    @Synchronized
    fun add(entry: EventEntry) {
        val list = (_entries.value + entry).takeLast(MAX)
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("at", it.at).put("ti", it.title).put("d", it.detail).put("a", it.alert))
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
        _entries.value = list
    }

    private fun load(): List<EventEntry> = runCatching {
        val arr = JSONArray(prefs.getString(KEY, "[]"))
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            EventEntry(o.getLong("at"), o.getString("ti"), o.optString("d"), o.optBoolean("a"))
        }
    }.getOrDefault(emptyList())

    private companion object {
        const val KEY = "events"
        const val MAX = 150
    }
}
