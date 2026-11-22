package com.dendra.app.data.comm

import com.dendra.app.domain.model.CommChannel
import com.dendra.app.domain.model.Outbound
import com.dendra.app.domain.port.CommLink

/**
 * Último eslabón de la cascada: memoria local. El coordinador ya guardó el
 * punto cifrado en la bitácora antes de intentar la cascada, de modo que este
 * eslabón confirma la retención: el registro completo sale apenas haya
 * conexión (guardar y reenviar). Las órdenes a otros equipos no se pueden
 * "guardar": si ningún canal las llevó, no salieron.
 */
class LocalCommLink : CommLink {
    override val channel = CommChannel.LOCAL
    override fun isAvailable(): Boolean = true
    override suspend fun send(message: Outbound): Boolean =
        message is Outbound.Report || message is Outbound.Event
}
