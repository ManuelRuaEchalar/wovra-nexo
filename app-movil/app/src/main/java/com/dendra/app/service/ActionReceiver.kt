package com.dendra.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dendra.app.data.alarm.Alarm
import com.dendra.app.domain.model.RemoteCommand
import kotlinx.coroutines.launch

/** Botones de las notificaciones (reportar desde el aviso del guardián, silenciar). */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val locator = ServiceLocator.from(context)
        val member = intent.getStringExtra(EXTRA_MEMBER)?.let(locator.circle::byId)
        when (intent.action) {
            ACTION_REPORT -> {
                Alarm.stop(context)
                member ?: return
                locator.notifier.cancelGuardian(member)
                val pending = goAsync()
                locator.appScope.launch {
                    try {
                        locator.sendCommand(member, RemoteCommand.ROBO)
                    } finally {
                        pending.finish()
                    }
                }
            }
            ACTION_QUIET -> {
                Alarm.stop(context)
                member?.let(locator.notifier::cancelGuardian)
            }
            ACTION_STOP_RING -> {
                Alarm.stop(context)
                locator.notifier.cancelRing()
            }
        }
    }

    companion object {
        const val ACTION_REPORT = "com.dendra.app.REPORT"
        const val ACTION_QUIET = "com.dendra.app.QUIET"
        const val ACTION_STOP_RING = "com.dendra.app.STOP_RING"
        const val EXTRA_MEMBER = "member"
    }
}
