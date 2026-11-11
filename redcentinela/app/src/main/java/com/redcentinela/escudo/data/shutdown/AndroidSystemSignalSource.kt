package com.redcentinela.escudo.data.shutdown

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.TelephonyManager
import com.redcentinela.escudo.domain.model.SystemSignal
import com.redcentinela.escudo.domain.port.SystemSignalSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.merge

/**
 * Fuente de señales del sistema sobre Android. Captura en tiempo de ejecución
 * los eventos "sospechosos" que lista la propuesta (apagado, modo avión,
 * batería crítica, cambios de chip) y los fusiona con los que publica el
 * [SignalBus] (p. ej. el receiver estático o la UI de demo).
 */
class AndroidSystemSignalSource(context: Context) : SystemSignalSource {

    private val appContext = context.applicationContext

    override fun signals(): Flow<SystemSignal> =
        merge(SignalBus.signals, runtimeReceivers())

    private fun runtimeReceivers(): Flow<SystemSignal> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val signal = intent?.toSignal() ?: return
                trySend(signal)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SHUTDOWN)
            addAction("android.intent.action.QUICKBOOT_POWEROFF")
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(ACTION_SIM_CARD_STATE_CHANGED)
            addAction("android.intent.action.SIM_STATE_CHANGED")
        }
        // registerReceiver con flag NOT_EXPORTED en API 33+; las acciones son
        // broadcasts del sistema, no necesitan exportarse.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            appContext.registerReceiver(receiver, filter)
        }
        awaitClose { runCatching { appContext.unregisterReceiver(receiver) } }
    }

    private fun Intent.toSignal(): SystemSignal? = when (action) {
        Intent.ACTION_SHUTDOWN,
        "android.intent.action.QUICKBOOT_POWEROFF" -> SystemSignal.INTENTO_APAGADO

        Intent.ACTION_BATTERY_LOW -> SystemSignal.BATERIA_CRITICA

        Intent.ACTION_AIRPLANE_MODE_CHANGED ->
            if (getBooleanExtra("state", false)) SystemSignal.MODO_AVION else null

        ACTION_SIM_CARD_STATE_CHANGED,
        "android.intent.action.SIM_STATE_CHANGED" -> simSignal()

        else -> null
    }

    /**
     * La acción nueva trae el estado como entero; la antigua
     * (SIM_STATE_CHANGED), como texto en el extra "ss".
     */
    private fun Intent.simSignal(): SystemSignal? {
        val absent = getIntExtra(EXTRA_SIM_STATE, TelephonyManager.SIM_STATE_UNKNOWN) ==
            TelephonyManager.SIM_STATE_ABSENT || getStringExtra("ss") == "ABSENT"
        if (!absent) return null
        // Las dos acciones avisan del mismo retiro: se reporta una vez.
        val now = System.currentTimeMillis()
        if (now - lastSimSignalAt < 5_000) return null
        lastSimSignalAt = now
        return SystemSignal.CHIP_RETIRADO
    }

    private var lastSimSignalAt = 0L

    // Constantes @SystemApi de TelephonyManager: no están en el SDK público,
    // así que se declaran con su valor literal.
    private companion object {
        const val ACTION_SIM_CARD_STATE_CHANGED =
            "android.telephony.action.SIM_CARD_STATE_CHANGED"
        const val EXTRA_SIM_STATE = "android.telephony.extra.SIM_STATE"
    }
}
