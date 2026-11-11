package com.redcentinela.escudo.domain.port

import com.redcentinela.escudo.domain.model.SystemSignal
import kotlinx.coroutines.flow.Flow

/** Emite señales del sistema que el Escudo vigila (apagado, chip, batería). */
interface SystemSignalSource {
    fun signals(): Flow<SystemSignal>
}

/** Informa el nivel de batería actual, para la política de reporte. */
interface BatteryGauge {
    /** 0-100. */
    fun levelPct(): Int
}
