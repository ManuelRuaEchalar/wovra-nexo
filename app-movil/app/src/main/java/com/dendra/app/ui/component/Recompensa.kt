package com.dendra.app.ui.component

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.dendra.app.data.reward.Reward
import com.dendra.app.ui.EscudoViewModel
import com.dendra.app.ui.theme.AmbarTexto
import com.dendra.app.ui.theme.Aurora
import com.dendra.app.ui.theme.Destello
import com.dendra.app.ui.theme.Medio
import com.dendra.app.ui.theme.Sora
import com.dendra.app.ui.theme.Texto2
import com.dendra.app.ui.theme.TextoNoche
import com.dendra.app.ui.theme.Tinta
import com.dendra.app.ui.theme.Tipo

/** Formato corto de bolivianos: "50" o "12,5". */
fun bs(monto: Double): String =
    if (monto % 1.0 == 0.0) monto.toLong().toString() else "%.2f".format(monto).trimEnd('0').replace('.', ',')

fun qrRecompensa(text: String, size: Int = 480): Bitmap? = runCatching {
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 1))
    val dark = Tinta.toArgb()
    val light = Color.White.toArgb()
    val pixels = IntArray(size * size) { i -> if (matrix[i % size, i / size]) dark else light }
    Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
}.getOrNull()

/**
 * Bloque de la pantalla "Este celular está perdido": monto garantizado y QR
 * para que quien lo encuentra reclame desde su propio teléfono.
 */
@Composable
fun RecompensaEnPantallaPerdido(r: Reward) {
    val qr = remember(r.claimUrl) { qrRecompensa(r.claimUrl) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.08f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (qr != null) {
            Box(Modifier.size(118.dp).clip(RoundedCornerShape(14.dp)).background(Color.White).padding(6.dp)) {
                Image(qr.asImageBitmap(), contentDescription = "Código QR para reclamar la recompensa")
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Etiqueta("Recompensa garantizada", color = Destello)
            Text(
                "${bs(r.amountBs)} Bs", fontFamily = Sora, fontWeight = FontWeight(600), fontSize = 30.sp,
                color = Color.White
            )
            Text(
                if (r.claimed) "Ya fue reclamada. Devuélvelo y se paga al confirmar."
                else "Escanea el código con tu celular. Se paga en Stellar cuando el dueño lo recupere.",
                style = Tipo.Nota, color = TextoNoche
            )
            Text("Código ${r.code}", style = Tipo.Nota.copy(fontWeight = FontWeight(600)), color = Destello)
        }
    }
}

/** Tarjeta para el dueño en la pantalla principal: estado y acciones de la recompensa. */
@Composable
fun TarjetaRecompensa(vm: EscudoViewModel) {
    val reward by vm.reward.collectAsStateWithLifecycle()
    val working by vm.rewardWorking.collectAsStateWithLifecycle()
    val problem by vm.rewardProblem.collectAsStateWithLifecycle()
    val r = reward
    val uri = LocalUriHandler.current

    if (r == null) {
        // Se intentó publicar y no se pudo (sin internet, sin servidor…).
        if (problem != null && vm.config.rewardBs > 0) AvisoAmbar("Recompensa: $problem")
        return
    }

    Tarjeta(relleno = 16.dp) {
        Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Etiqueta("Recompensa en Stellar", Modifier.weight(1f), color = Medio)
                Text("${bs(r.amountBs)} Bs", style = Tipo.Fuerte, color = Tinta)
            }
            val (texto, color) = when {
                r.creating -> "Bloqueando la recompensa en un escrow de Stellar…" to Texto2
                r.active -> "Bloqueada y visible en la pantalla de tu celular. Nadie la reclamó todavía." to Texto2
                r.claimed -> "${r.claimerName ?: "Alguien"} dice que encontró tu celular" +
                    (r.claimerContact?.let { ". Contacto: $it" } ?: "") to Tinta
                r.status == "pagando" -> "Pagando a ${r.claimerName ?: "quien lo devolvió"}…" to Texto2
                r.status == "devolviendo" -> "Devolviendo el dinero a tu cuenta…" to Texto2
                r.paid -> "Pagada a ${r.claimerName ?: "quien lo devolvió"}. ¡Gracias por cerrar el círculo!" to Medio
                r.refunded -> "Cancelada: el dinero volvió a tu cuenta." to Texto2
                else -> "No se pudo completar: ${r.error ?: "error desconocido"}" to AmbarTexto
            }
            Text(texto, style = Tipo.Fila, color = color)
            problem?.let { Text(it, style = Tipo.Nota, color = AmbarTexto) }

            when {
                working || r.busy || r.creating -> Text("Esperando a la red…", style = Tipo.Nota, color = Aurora)
                r.claimed -> {
                    BotonPrincipal("Me lo devolvió: pagar ${bs(r.amountBs)} Bs") { vm.confirmReward() }
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Enlace("No fue esa persona", Modifier.weight(1f)) { vm.rejectRewardClaim() }
                        Enlace("Cancelar", Modifier.weight(1f), color = AmbarTexto) { vm.cancelReward() }
                    }
                }
                r.active -> Enlace("Cancelar y recuperar el dinero", color = AmbarTexto) { vm.cancelReward() }
                r.finished || r.replaceable -> Enlace("Listo") { vm.dismissReward() }
                r.failed -> Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    if (r.claimerName != null) Enlace("Reintentar el pago", Modifier.weight(1f)) { vm.confirmReward() }
                    Enlace("Devolverme el dinero", Modifier.weight(1f), color = AmbarTexto) { vm.cancelReward() }
                }
            }
            val enlace = r.lastTxUrl ?: r.escrowUrl
            if (enlace != null && !r.creating) {
                Enlace("Ver en la red Stellar", color = Medio) { runCatching { uri.openUri(enlace) } }
            }
        }
    }
}
