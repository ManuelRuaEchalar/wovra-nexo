package com.redcentinela.escudo.data.shutdown

import android.content.Context
import android.os.BatteryManager
import com.redcentinela.escudo.domain.port.BatteryGauge

/** Lee el nivel de batería del sistema para la política de reporte. */
class AndroidBatteryGauge(context: Context) : BatteryGauge {
    private val manager =
        context.applicationContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    override fun levelPct(): Int =
        manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            .coerceIn(0, 100)
}
