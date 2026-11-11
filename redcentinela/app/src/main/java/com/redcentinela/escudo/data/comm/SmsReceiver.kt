package com.redcentinela.escudo.data.comm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.redcentinela.escudo.service.ServiceLocator
import kotlinx.coroutines.launch

/**
 * Recibe los SMS de la cascada: la ubicación de un equipo del círculo, o una
 * orden para ESTE equipo ("repórtalo como robado") enviada desde el celular
 * de alguien del círculo cuando no hay internet. Ignora cualquier otro SMS.
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val text = runCatching {
            Telephony.Sms.Intents.getMessagesFromIntent(intent).joinToString("") { it.messageBody ?: "" }
        }.getOrNull() ?: return
        val parsed = SmsCodec.parse(text) ?: return
        val locator = ServiceLocator.from(context)
        val pending = goAsync()
        locator.appScope.launch {
            try {
                locator.sync.onSms(parsed)
            } finally {
                pending.finish()
            }
        }
    }
}
