package com.dendra.app.data.shutdown

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dendra.app.service.EscudoService
import com.dendra.app.service.ServiceLocator

/**
 * Receiver declarado en el manifest. Al encender el equipo vuelve a levantar
 * el Escudo: "para flashearlo hay que encenderlo", y al encender vuelve a
 * reportar. (El apagado lo capta el servicio mientras está vivo.)
 */
class SystemEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!ServiceLocator.from(context).config.shieldEnabled) return
        EscudoService.start(context, fromBoot = true)
    }
}
