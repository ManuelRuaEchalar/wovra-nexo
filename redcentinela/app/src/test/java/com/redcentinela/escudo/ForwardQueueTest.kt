package com.redcentinela.escudo

import com.redcentinela.escudo.data.circle.ForwardQueue
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ForwardQueueTest {

    private val min = 60_000L
    private var now = 1_760_000_000_000L
    private var stored: String? = null

    private fun queue() = ForwardQueue(load = { stored }, save = { stored = it }, clock = { now })

    @Test
    fun `sin internet guarda el aviso y lo entrega despues, con la hora en que se capto`() = runBlocking {
        val q = queue()
        val seenAt = now
        assertTrue(q.add("aa", "x1", seenAt))
        now += 90 * min // una hora y media sin conexión
        val sent = mutableListOf<ForwardQueue.Entry>()
        assertEquals(1, q.flush { sent += it; true })
        assertEquals(seenAt, sent.single().at)
        assertEquals(0, q.size())
    }

    @Test
    fun `la cola sobrevive a que se cierre la app`() {
        queue().add("aa", "x1", now)
        assertEquals(1, queue().size())
    }

    @Test
    fun `cerca por mucho rato ocupa un lugar cada 5 min y gana el mas reciente`() {
        val q = queue()
        q.add("aa", "x1", now)
        q.add("aa", "x2", now + 1 * min)
        q.add("aa", "x3", now + 3 * min)
        assertEquals(listOf("x3"), q.snapshot().map { it.raw })
        q.add("aa", "x4", now + 6 * min)
        assertEquals(listOf("x3", "x4"), q.snapshot().map { it.raw })
    }

    @Test
    fun `si falla el envio conserva el aviso para el siguiente intento`() = runBlocking {
        val q = queue()
        q.add("aa", "x1", now)
        q.add("bb", "y1", now + 1000)
        assertEquals(0, q.flush { false })
        assertEquals(2, q.size())
        assertEquals(2, q.flush { true })
    }

    @Test
    fun `descarta lo que tiene mas de 12 horas`() {
        val q = queue()
        q.add("aa", "x1", now)
        now += 13 * 60 * min
        q.add("bb", "y1", now)
        assertEquals(listOf("bb"), q.snapshot().map { it.deviceId })
    }

    @Test
    fun `con internet no reenvia el mismo equipo a cada segundo`() = runBlocking {
        val q = queue()
        q.add("aa", "x1", now)
        q.flush { true }
        assertFalse(q.add("aa", "x2", now + 5_000))
        now += 30_000
        assertTrue(q.add("aa", "x3", now))
    }
}
