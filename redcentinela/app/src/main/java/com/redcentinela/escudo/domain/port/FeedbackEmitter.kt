package com.redcentinela.escudo.domain.port

/**
 * Emite la señal sensorial que guía al usuario en la búsqueda cercana
 * (pitido y/o vibración). La cadencia la decide el dominio según la banda.
 */
interface FeedbackEmitter {
    /**
     * @param intervalMillis período entre pitidos; menor = más cerca.
     * @param solid true para una señal continua (muy cerca).
     */
    fun pulse(intervalMillis: Long, solid: Boolean)
    fun stop()
}
