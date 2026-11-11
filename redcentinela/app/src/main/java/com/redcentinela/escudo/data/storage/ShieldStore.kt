package com.redcentinela.escudo.data.storage

import android.content.Context
import com.redcentinela.escudo.domain.model.AlertReason
import com.redcentinela.escudo.domain.model.ShieldStatus

/**
 * Recuerda si el equipo estaba en alerta. Si lo apagan de verdad, al
 * encenderlo (hay que encenderlo para flashearlo) el Escudo retoma la alerta
 * y vuelve a reportar.
 */
class ShieldStore(context: Context) {

    class Saved(val status: ShieldStatus, val reason: AlertReason?, val alertSince: Long?, val reportedBy: String?)

    private val prefs = context.applicationContext
        .getSharedPreferences("escudo_estado", Context.MODE_PRIVATE)

    fun save(status: ShieldStatus, reason: AlertReason?, alertSince: Long?, reportedBy: String?) {
        prefs.edit()
            .putString("status", status.name)
            .putString("reason", reason?.name)
            .putLong("since", alertSince ?: 0)
            .putString("by", reportedBy)
            .apply()
    }

    fun load(): Saved = Saved(
        status = runCatching { ShieldStatus.valueOf(prefs.getString("status", null)!!) }
            .getOrDefault(ShieldStatus.INACTIVO),
        reason = runCatching { AlertReason.valueOf(prefs.getString("reason", null)!!) }.getOrNull(),
        alertSince = prefs.getLong("since", 0).takeIf { it > 0 },
        reportedBy = prefs.getString("by", null)
    )
}
