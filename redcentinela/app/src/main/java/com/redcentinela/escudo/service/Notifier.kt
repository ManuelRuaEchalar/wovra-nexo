package com.redcentinela.escudo.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.redcentinela.escudo.R
import com.redcentinela.escudo.domain.model.CircleMember
import com.redcentinela.escudo.domain.model.ShieldStatus
import com.redcentinela.escudo.ui.LostModeActivity
import com.redcentinela.escudo.ui.MainActivity

/** Todas las notificaciones de la app, en un solo lugar. */
@SuppressLint("MissingPermission")
class Notifier(context: Context) {

    private val app = context.applicationContext
    private val manager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        manager.createNotificationChannel(
            NotificationChannel(CH_SERVICE, "Escudo activo", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "Mantiene activa la protección del equipo." }
        )
        manager.createNotificationChannel(
            NotificationChannel(CH_ALERT, "Alertas del círculo", NotificationManager.IMPORTANCE_HIGH)
                .apply { description = "Robos, pérdidas y avisos del guardián." }
        )
    }

    fun service(status: ShieldStatus): Notification {
        val text = when (status) {
            ShieldStatus.INACTIVO -> "Escudo apagado"
            ShieldStatus.ARMADO -> "Escudo activo · registrando tu ruta"
            ShieldStatus.ALERTA_ROBO -> "Reportado como robado · enviando ubicación"
            ShieldStatus.ALERTA_PERDIDA -> "Reportado como perdido · enviando ubicación"
        }
        return NotificationCompat.Builder(app, CH_SERVICE)
            .setContentTitle("Red Centinela")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_shield)
            .setOngoing(true)
            .setContentIntent(openApp(null))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun updateService(status: ShieldStatus) = manager.notify(ID_SERVICE, service(status))

    /** El celular de alguien del círculo entró en alerta. */
    fun memberAlert(member: CircleMember, stolen: Boolean) = alert(
        id = member.id.hashCode(),
        title = "Celular de ${member.name}: ${if (stolen) "reportado como robado" else "perdido"}",
        text = "Toca para ver dónde está.",
        intent = openApp(member.id)
    )

    /** Soy guardián de [member] y su celular se alejó del mío. */
    fun guardianAway(member: CircleMember) {
        val report = PendingIntent.getBroadcast(
            app, member.id.hashCode(),
            Intent(app, ActionReceiver::class.java).setAction(ActionReceiver.ACTION_REPORT)
                .putExtra(ActionReceiver.EXTRA_MEMBER, member.id),
            FLAGS
        )
        val dismiss = PendingIntent.getBroadcast(
            app, member.id.hashCode() + 1,
            Intent(app, ActionReceiver::class.java).setAction(ActionReceiver.ACTION_QUIET)
                .putExtra(ActionReceiver.EXTRA_MEMBER, member.id),
            FLAGS
        )
        manager.notify(
            guardId(member),
            NotificationCompat.Builder(app, CH_ALERT)
                .setContentTitle("El celular de ${member.name} se alejó")
                .setContentText("Dejó de estar a tu alcance. ¿Lo tiene con él o ella?")
                .setSmallIcon(R.drawable.ic_shield)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(openApp(member.id))
                .setDeleteIntent(dismiss)
                .addAction(0, "Reportar robo", report)
                .addAction(0, "Está con ${member.name}", dismiss)
                .setAutoCancel(true)
                .build()
        )
    }

    fun cancelGuardian(member: CircleMember) = manager.cancel(guardId(member))

    fun joined(name: String) = alert(
        id = ("join" + name).hashCode(),
        title = "$name se unió a tu círculo",
        text = "Ya pueden reportar y ubicar el celular del otro.",
        intent = openApp(null)
    )

    fun found(member: CircleMember) = alert(
        id = member.id.hashCode() + 7,
        title = "Alguien encontró el celular de ${member.name}",
        text = "Toca para ver la ubicación y coordinar la devolución.",
        intent = openApp(member.id)
    )

    /** El equipo está sonando por orden del círculo. */
    fun ringing(by: String) {
        val stop = PendingIntent.getBroadcast(
            app, 3, Intent(app, ActionReceiver::class.java).setAction(ActionReceiver.ACTION_STOP_RING), FLAGS
        )
        manager.notify(
            ID_RING,
            NotificationCompat.Builder(app, CH_ALERT)
                .setContentTitle("$by está haciendo sonar este celular")
                .setContentText("Toca para silenciar.")
                .setSmallIcon(R.drawable.ic_shield)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(stop)
                .setDeleteIntent(stop)
                .setAutoCancel(true)
                .build()
        )
    }

    fun cancelRing() = manager.cancel(ID_RING)

    /**
     * Modo encontrado: muestra la pantalla "Este celular está perdido" encima
     * del bloqueo, sin necesidad de desbloquear.
     */
    fun showLostScreen() {
        val intent = PendingIntent.getActivity(
            app, 5,
            Intent(app, LostModeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            FLAGS
        )
        manager.notify(
            ID_LOST,
            NotificationCompat.Builder(app, CH_ALERT)
                .setContentTitle("Este celular está perdido")
                .setContentText("Toca aquí para avisar a su dueño.")
                .setSmallIcon(R.drawable.ic_shield)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setContentIntent(intent)
                .setFullScreenIntent(intent, true)
                .build()
        )
    }

    fun cancelLostScreen() = manager.cancel(ID_LOST)

    private fun alert(id: Int, title: String, text: String, intent: PendingIntent) {
        manager.notify(
            id,
            NotificationCompat.Builder(app, CH_ALERT)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_shield)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(intent)
                .setAutoCancel(true)
                .build()
        )
    }

    private fun openApp(trackMemberId: String?): PendingIntent = PendingIntent.getActivity(
        app, trackMemberId?.hashCode() ?: 0,
        Intent(app, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(MainActivity.EXTRA_TRACK, trackMemberId),
        FLAGS
    )

    private fun guardId(member: CircleMember) = member.id.hashCode() + 3

    companion object {
        const val ID_SERVICE = 1001
        private const val ID_RING = 1002
        private const val ID_LOST = 1003
        private const val CH_SERVICE = "escudo"
        private const val CH_ALERT = "alertas"
        private const val FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    }
}
