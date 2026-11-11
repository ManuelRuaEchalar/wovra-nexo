package com.redcentinela.escudo.data.circle

import android.content.SharedPreferences
import com.redcentinela.escudo.domain.model.CircleMember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * El círculo, guardado cifrado en el teléfono (contiene los secretos de los
 * demás equipos). De 2 a 5 personas, como indica la propuesta.
 */
class CircleStore(private val prefs: SharedPreferences) {

    private val _members = MutableStateFlow(load())
    val members: StateFlow<List<CircleMember>> = _members.asStateFlow()

    fun byId(id: String): CircleMember? = _members.value.firstOrNull { it.id == id }

    /** Agrega o actualiza. @return false si el círculo ya está lleno. */
    @Synchronized
    fun upsert(member: CircleMember): Boolean {
        val current = _members.value
        val existing = current.firstOrNull { it.id == member.id }
        if (existing == null && current.size >= MAX) return false
        val merged = existing?.copy(
            name = member.name,
            secret = member.secret,
            phone = member.phone ?: existing.phone
        ) ?: member
        save(current.filterNot { it.id == member.id } + merged)
        return true
    }

    @Synchronized
    fun update(id: String, change: (CircleMember) -> CircleMember) {
        save(_members.value.map { if (it.id == id) change(it) else it })
    }

    @Synchronized
    fun remove(id: String) = save(_members.value.filterNot { it.id == id })

    private fun save(list: List<CircleMember>) {
        val arr = JSONArray()
        list.forEach { m ->
            arr.put(
                JSONObject().put("i", m.id).put("n", m.name).put("s", m.secret)
                    .put("p", m.phone ?: "").put("g", m.isGuardian).put("w", m.iWatch)
            )
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
        _members.value = list
    }

    private fun load(): List<CircleMember> = runCatching {
        val arr = JSONArray(prefs.getString(KEY, "[]"))
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            CircleMember(
                id = o.getString("i"),
                name = o.getString("n"),
                secret = o.getString("s"),
                phone = o.optString("p").ifBlank { null },
                isGuardian = o.optBoolean("g"),
                iWatch = o.optBoolean("w")
            )
        }
    }.getOrDefault(emptyList())

    companion object {
        const val MAX = 5
        private const val KEY = "circle"
    }
}
