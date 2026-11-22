package com.dendra.app.data.circle

/**
 * "Guardar y reenviar": avisos Bluetooth de equipos AJENOS en alerta que este
 * teléfono captó. Este teléfono no puede leerlos (van cifrados para el
 * círculo del dueño); solo los guarda y los entrega al relevo cuando tiene
 * internet, aunque sea horas después y lejos de donde los vio.
 *
 * Reglas:
 *  - Cada aviso conserva la hora en que se CAPTÓ. El círculo del dueño ordena
 *    por esa hora, así un aviso entregado tarde entra en la ruta en su lugar
 *    y nunca pisa una posición más nueva que haya llegado por otro teléfono.
 *  - Mientras no se envía, un equipo ocupa a lo sumo un lugar cada
 *    [BUCKET_MS]: un aviso nuevo dentro de ese lapso reemplaza al anterior
 *    (gana el más reciente) para no llenar la memoria si el equipo queda cerca.
 *  - Con internet, entre envíos del mismo equipo pasan al menos [MIN_GAP_MS].
 *  - Los avisos de más de [TTL_MS] se descartan: ya no ayudan a encontrarlo.
 *
 * La lógica no depende de Android: [load]/[save] guardan el texto de la cola.
 */
class ForwardQueue(
    private val load: () -> String?,
    private val save: (String) -> Unit,
    private val clock: () -> Long = System::currentTimeMillis
) {
    data class Entry(
        val deviceId: String,
        /** Aviso tal como se captó, en base64. */
        val raw: String,
        /** Hora en que se captó (epoch ms). */
        val at: Long,
        /** Hora del primer aviso que ocupó este lugar. */
        val since: Long = at
    )

    private val entries: MutableList<Entry> = decode(load()).toMutableList()
    private val lastSentAt = HashMap<String, Long>()

    @Synchronized
    fun size(): Int = entries.size

    @Synchronized
    fun snapshot(): List<Entry> = entries.toList()

    /** Guarda un aviso captado. Devuelve false si no hacía falta guardarlo. */
    @Synchronized
    fun add(deviceId: String, raw: String, at: Long): Boolean {
        prune()
        val i = entries.indexOfLast { it.deviceId == deviceId }
        val last = entries.getOrNull(i)
        when {
            last != null && at <= last.at -> return false
            last != null && at - last.since < BUCKET_MS -> entries[i] = last.copy(raw = raw, at = at)
            last == null && at - (lastSentAt[deviceId] ?: 0) < MIN_GAP_MS -> return false
            else -> entries.add(Entry(deviceId, raw, at))
        }
        trim(deviceId)
        persist()
        return true
    }

    /**
     * Entrega los avisos más antiguos primero con [send]; se detiene en el
     * primer fallo (seguramente se cortó la conexión) y conserva el resto.
     */
    suspend fun flush(send: suspend (Entry) -> Boolean): Int {
        var sent = 0
        while (true) {
            val next = synchronized(this) { prune(); entries.firstOrNull() } ?: break
            if (!send(next)) break
            synchronized(this) {
                entries.remove(next)
                lastSentAt[next.deviceId] = clock()
                persist()
            }
            sent++
        }
        return sent
    }

    private fun prune() {
        val limit = clock() - TTL_MS
        if (entries.removeAll { it.at < limit }) persist()
    }

    private fun trim(deviceId: String) {
        val mine = entries.filter { it.deviceId == deviceId }
        if (mine.size > MAX_PER_DEVICE) entries.removeAll(mine.take(mine.size - MAX_PER_DEVICE).toSet())
        while (entries.size > MAX_TOTAL) entries.removeAt(0)
    }

    private fun persist() = save(entries.joinToString("\n") { "${it.deviceId};${it.raw};${it.at};${it.since}" })

    companion object {
        const val BUCKET_MS = 5 * 60_000L
        const val MIN_GAP_MS = 20_000L
        const val TTL_MS = 12 * 3_600_000L
        const val MAX_PER_DEVICE = 150
        const val MAX_TOTAL = 500

        private fun decode(text: String?): List<Entry> =
            text.orEmpty().lineSequence().mapNotNull { line ->
                val p = line.split(';')
                if (p.size != 4) return@mapNotNull null
                val at = p[2].toLongOrNull() ?: return@mapNotNull null
                Entry(p[0], p[1], at, p[3].toLongOrNull() ?: at)
            }.sortedBy { it.at }.toList()
    }
}
