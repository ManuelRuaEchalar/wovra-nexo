package com.redcentinela.escudo.domain.port

import com.redcentinela.escudo.domain.model.CommChannel
import com.redcentinela.escudo.domain.model.Outbound

/**
 * Un eslabón de la cascada de comunicación (Capa 1 de la propuesta).
 *
 * Cada implementación resuelve UN canal: internet, SMS, Bluetooth o memoria
 * local. La cascada se arma componiendo varios eslabones en orden de
 * preferencia (patrón Chain of Responsibility), sin que ninguno conozca a
 * los demás. Agregar un canal nuevo = agregar un CommLink (abierto/cerrado).
 */
interface CommLink {
    val channel: CommChannel

    /** ¿Este canal está disponible ahora mismo? (p. ej. ¿hay internet?). */
    fun isAvailable(): Boolean

    /**
     * Intenta enviar el mensaje por este canal.
     * @return true si el mensaje salió por este canal. Un canal puede
     * rechazar mensajes que no le corresponden (p. ej. SMS no lleva eventos).
     */
    suspend fun send(message: Outbound): Boolean
}
