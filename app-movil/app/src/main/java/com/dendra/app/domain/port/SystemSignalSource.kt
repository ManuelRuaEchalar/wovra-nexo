package com.dendra.app.domain.port

import com.dendra.app.domain.model.SystemSignal
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
