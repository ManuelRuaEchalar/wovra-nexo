package com.redcentinela.escudo.domain.service

import com.redcentinela.escudo.domain.model.Outbound
import com.redcentinela.escudo.domain.model.CommResult
import com.redcentinela.escudo.domain.port.CommLink

/**
 * Cascada de comunicación de la Capa 1: intenta cada canal en orden de
 * preferencia y baja al siguiente si falla (internet -> SMS -> Bluetooth
 * -> memoria local).
 *
 * Implementa el patrón Chain of Responsibility sobre una lista ordenada de
 * [CommLink]. No conoce canales concretos: recibe los eslabones inyectados,
 * por lo que agregar o reordenar canales no toca esta clase (abierto/cerrado,
 * inversión de dependencias).
 */
class CommCascade(
    /** Eslabones en orden de preferencia. El último debería ser LOCAL
     *  (siempre disponible) para garantizar "guardar y reenviar". */
    private val links: List<CommLink>
) {
    /**
     * Envía el mensaje por el primer canal disponible que lo acepte.
     * @return resultado con el canal que tuvo éxito, o no entregado.
     */
    suspend fun dispatch(message: Outbound): CommResult {
        for (link in links) {
            if (!link.isAvailable()) continue
            val ok = runCatching { link.send(message) }.getOrDefault(false)
            if (ok) {
                return CommResult(delivered = true, channel = link.channel)
            }
        }
        return CommResult(delivered = false, channel = null)
    }
}
