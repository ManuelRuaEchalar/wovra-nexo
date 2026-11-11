package com.redcentinela.escudo.data.comm

import com.redcentinela.escudo.data.circle.CircleSync
import com.redcentinela.escudo.data.net.RelayClient
import com.redcentinela.escudo.domain.model.CommChannel
import com.redcentinela.escudo.domain.model.Outbound
import com.redcentinela.escudo.domain.port.CommLink
import org.json.JSONObject

/**
 * Primer eslabón de la cascada: internet (datos o cualquier WiFi). Es el
 * único canal que lleva todo: estado, registro del robo y la bitácora de
 * ubicaciones acumulada mientras no hubo conexión.
 */
class RelayCommLink(
    private val relay: RelayClient,
    private val sync: CircleSync
) : CommLink {

    override val channel = CommChannel.INTERNET

    override fun isAvailable(): Boolean = relay.hasInternet()

    override suspend fun send(message: Outbound): Boolean = when (message) {
        is Outbound.Report -> sendReport(message)
        is Outbound.Command -> sync.sendCommand(message.target, message.command)
        is Outbound.Event -> sync.publishFeed(
            JSONObject().put("t", "ev").put("at", message.entry.at).put("ti", message.entry.title)
                .put("d", message.entry.detail).put("a", message.entry.alert)
        )
        is Outbound.Found -> sync.publishFeed(
            JSONObject().put("t", "found").put("at", message.at).apply {
                message.point?.let { put("lat", it.latitude).put("lng", it.longitude) }
            }
        )
    }

    private suspend fun sendReport(report: Outbound.Report): Boolean {
        val status = JSONObject().put("t", "st").put("s", report.status.name)
            .put("at", System.currentTimeMillis()).put("bat", report.batteryPct)
        report.reason?.let { status.put("r", it.name) }
        report.alertSince?.let { status.put("since", it) }
        if (!sync.publishFeed(status)) return false
        if (!report.inAlert) return true
        // La ubicación solo se comparte en alerta. Cada mensaje del relevo
        // admite pocos KB: la ruta sale en tandas.
        val points = (report.backlog + listOfNotNull(report.point)).distinctBy { it.capturedAt }
        for (chunk in points.chunked(POINTS_PER_MESSAGE)) {
            if (!sync.publishFeed(CircleSync.pointsJson(chunk))) return false
        }
        return true
    }

    private companion object {
        const val POINTS_PER_MESSAGE = 40
    }
}
