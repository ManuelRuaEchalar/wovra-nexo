package com.redcentinela.escudo.data.shutdown

import com.redcentinela.escudo.domain.model.SystemSignal
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Canal interno por el que distintos componentes de Android (el receiver del
 * manifest, pruebas de la UI) publican señales del sistema para que la fuente
 * de señales las entregue al dominio.
 *
 * Es un único punto de publicación (DRY): tanto el BroadcastReceiver estático
 * como la UI de demo empujan aquí, y [AndroidSystemSignalSource] las fusiona
 * con las que captura en tiempo de ejecución.
 */
object SignalBus {
    private val _signals = MutableSharedFlow<SystemSignal>(
        replay = 0,
        extraBufferCapacity = 16
    )
    val signals: SharedFlow<SystemSignal> = _signals.asSharedFlow()

    fun publish(signal: SystemSignal) {
        _signals.tryEmit(signal)
    }
}
