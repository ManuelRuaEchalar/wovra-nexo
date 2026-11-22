package com.dendra.app.data.comm

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import com.dendra.app.data.circle.CircleStore
import com.dendra.app.domain.model.CommChannel
import com.dendra.app.domain.model.Outbound
import com.dendra.app.domain.port.CommLink
import com.dendra.app.service.EscudoConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Segundo eslabón: SMS. Sin datos, pero con chip. Manda la última ubicación
 * directo a los celulares del círculo (y las órdenes al celular reportado).
 *
 * Los SMS cuestan y Android frena a las apps que mandan muchos seguidos, así
 * que la ubicación sale espaciada y a un máximo de [MAX_RECIPIENTS] personas.
 * El permiso SEND_SMS es de los más controlados por Google Play; en el
 * prototipo se instala el APK directamente, como anota la propuesta.
 */
class SmsCommLink(
    context: Context,
    private val config: EscudoConfig,
    private val circle: CircleStore
) : CommLink {

    private val appContext = context.applicationContext
    override val channel = CommChannel.SMS

    private var lastPositionAt = 0L

    override fun isAvailable(): Boolean {
        val pm = appContext.packageManager
        if (!pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) return false
        if (appContext.checkSelfPermission(android.Manifest.permission.SEND_SMS) !=
            PackageManager.PERMISSION_GRANTED
        ) return false
        // En modo avión el chip sigue "listo", pero el SMS no sale.
        val airplane = android.provider.Settings.Global.getInt(
            appContext.contentResolver, android.provider.Settings.Global.AIRPLANE_MODE_ON, 0
        ) != 0
        if (airplane) return false
        val tm = appContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        return tm?.simState == TelephonyManager.SIM_STATE_READY
    }

    override suspend fun send(message: Outbound): Boolean = when (message) {
        is Outbound.Report -> sendPosition(message)
        is Outbound.Command -> {
            val phone = message.target.phone
            phone != null && sendText(
                phone,
                SmsCodec.order(
                    message.target.id, message.target.secret, message.command,
                    System.currentTimeMillis(), config.deviceId
                )
            )
        }
        is Outbound.Found -> sendToCircle(
            SmsCodec.found(
                config.userName, config.deviceId, config.secret,
                message.point?.latitude, message.point?.longitude, message.at
            )
        )
        is Outbound.Event -> false
    }

    private suspend fun sendPosition(report: Outbound.Report): Boolean {
        val point = report.point ?: return false
        if (!report.inAlert) return false
        val now = System.currentTimeMillis()
        val sinceAlert = now - (report.alertSince ?: now)
        val gap = if (sinceAlert < FIRST_PHASE_MS) GAP_FIRST_MS else GAP_LATER_MS
        if (now - lastPositionAt < gap) return false
        val text = SmsCodec.position(
            config.userName, config.deviceId, config.secret,
            point.latitude, point.longitude, point.accuracyMeters.toInt().coerceIn(0, 9999),
            point.capturedAt, stolen = report.status == com.dendra.app.domain.model.ShieldStatus.ALERTA_ROBO,
            batteryPct = report.batteryPct
        )
        return sendToCircle(text).also { if (it) lastPositionAt = now }
    }

    private suspend fun sendToCircle(text: String): Boolean {
        val recipients = circle.members.value
            .filter { !it.phone.isNullOrBlank() }
            .sortedByDescending { it.isGuardian }
            .take(MAX_RECIPIENTS)
        if (recipients.isEmpty()) return false
        return recipients.map { sendText(it.phone!!, text) }.any { it }
    }

    @SuppressLint("MissingPermission")
    private suspend fun sendText(number: String, body: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val manager = smsManager()
            val parts = manager.divideMessage(body)
            if (parts.size <= 1) {
                manager.sendTextMessage(number, null, body, null, null)
            } else {
                manager.sendMultipartTextMessage(number, null, parts, null, null)
            }
            true
        }.getOrDefault(false)
    }

    @Suppress("DEPRECATION")
    private fun smsManager(): SmsManager =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            appContext.getSystemService(SmsManager::class.java)
        } else {
            SmsManager.getDefault()
        }

    private companion object {
        const val MAX_RECIPIENTS = 2
        const val FIRST_PHASE_MS = 15 * 60_000L
        const val GAP_FIRST_MS = 3 * 60_000L
        const val GAP_LATER_MS = 10 * 60_000L
    }
}
