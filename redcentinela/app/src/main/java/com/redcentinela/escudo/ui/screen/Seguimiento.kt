package com.redcentinela.escudo.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.redcentinela.escudo.domain.model.CircleMember
import com.redcentinela.escudo.domain.model.CommChannel
import com.redcentinela.escudo.domain.model.EventEntry
import com.redcentinela.escudo.domain.model.GeoPoint
import com.redcentinela.escudo.domain.model.ProximityBand
import com.redcentinela.escudo.domain.model.ProximityTrend
import com.redcentinela.escudo.domain.model.RemoteCommand
import com.redcentinela.escudo.domain.model.RemoteState
import com.redcentinela.escudo.domain.model.ShieldStatus
import com.redcentinela.escudo.domain.service.DirectionFinder
import com.redcentinela.escudo.ui.EscudoViewModel
import com.redcentinela.escudo.ui.component.AvisoAmbar
import com.redcentinela.escudo.ui.component.BotonAlerta
import com.redcentinela.escudo.ui.component.BotonDestello
import com.redcentinela.escudo.ui.component.BotonPrincipal
import com.redcentinela.escudo.ui.component.BotonRedondo
import com.redcentinela.escudo.ui.component.BotonSecundario
import com.redcentinela.escudo.ui.component.CabeceraSimple
import com.redcentinela.escudo.ui.component.Enlace
import com.redcentinela.escudo.ui.component.Etiqueta
import com.redcentinela.escudo.ui.component.FilaDato
import com.redcentinela.escudo.ui.component.Halo
import com.redcentinela.escudo.ui.component.Iconos
import com.redcentinela.escudo.ui.component.MapaRuta
import com.redcentinela.escudo.ui.component.PuntoLeyenda
import com.redcentinela.escudo.ui.component.Separador
import com.redcentinela.escudo.ui.component.Tarjeta
import com.redcentinela.escudo.ui.theme.Ambar
import com.redcentinela.escudo.ui.theme.AmbarNoche
import com.redcentinela.escudo.ui.theme.AmbarTexto
import com.redcentinela.escudo.ui.theme.Aurora
import com.redcentinela.escudo.ui.theme.Bruma
import com.redcentinela.escudo.ui.theme.Destello
import com.redcentinela.escudo.ui.theme.Medio
import com.redcentinela.escudo.ui.theme.Noche
import com.redcentinela.escudo.ui.theme.Sora
import com.redcentinela.escudo.ui.theme.Texto2
import com.redcentinela.escudo.ui.theme.TextoNoche
import com.redcentinela.escudo.ui.theme.Tinta
import com.redcentinela.escudo.ui.theme.Tipo
import com.redcentinela.escudo.util.Format

// --- B3 · Seguimiento en vivo -------------------------------------------------------------

@Composable
fun SeguimientoScreen(
    vm: EscudoViewModel,
    memberId: String,
    onBack: () -> Unit,
    onRegistro: () -> Unit,
    onBuscar: () -> Unit,
    onReport: (CircleMember) -> Unit
) {
    val context = LocalContext.current
    val members by vm.members.collectAsStateWithLifecycle()
    val remote by vm.remote.collectAsStateWithLifecycle()
    val presence by vm.presence.collectAsStateWithLifecycle()
    val member = members.firstOrNull { it.id == memberId }
    if (member == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val state = remote[member.id] ?: RemoteState(member.id)
    val cerca = presence[member.id]?.takeIf { it.near }
    val now = ahora(3_000)
    val last = state.last

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Box(
            Modifier.padding(horizontal = 12.dp).padding(top = 4.dp).fillMaxWidth().height(400.dp)
                .clip(RoundedCornerShape(36.dp)).background(Noche)
        ) {
            MapaRuta(state.points, Modifier.fillMaxSize())
            BotonRedondo(Iconos.Atras, "Volver", Modifier.padding(16.dp).align(Alignment.TopStart), sobreNoche = true, onClick = onBack)
            if (state.inAlert) {
                Row(
                    Modifier.padding(16.dp).align(Alignment.TopEnd).height(30.dp).clip(RoundedCornerShape(50))
                        .background(Color(0xE60E3B36)).padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(Modifier.size(6.dp).background(Ambar, CircleShape))
                    Text(
                        if (state.status == ShieldStatus.ALERTA_ROBO) "En vivo · cada 20 s" else "Modo encontrado",
                        fontSize = 12.sp, fontWeight = FontWeight(600), color = AmbarNoche
                    )
                }
            }
            Row(
                Modifier.padding(14.dp).align(Alignment.BottomStart).clip(RoundedCornerShape(50))
                    .background(Color(0xCC0E3B36)).padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                Leyenda("Internet") { PuntoLeyenda(Destello, lleno = true) }
                Leyenda("SMS") { PuntoLeyenda(Destello, lleno = false) }
                Leyenda("Bluetooth") { PuntoLeyenda(Destello, lleno = false, punteado = true) }
            }
        }

        Column(Modifier.padding(horizontal = 24.dp).padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    when (state.status) {
                        ShieldStatus.ALERTA_ROBO -> "Reportado como robado ${Format.ago(state.alertSince ?: state.statusAt, now)}"
                        ShieldStatus.ALERTA_PERDIDA -> "Reportado como perdido ${Format.ago(state.alertSince ?: state.statusAt, now)}"
                        ShieldStatus.ARMADO -> "Escudo activo · sin alerta"
                        ShieldStatus.INACTIVO -> "Escudo apagado en ese celular"
                        null -> "Sin noticias de ese celular todavía"
                    },
                    style = Tipo.Nota.copy(fontWeight = FontWeight(600)),
                    color = if (state.inAlert) AmbarTexto else Texto2
                )
                Text("Celular de ${member.name}", style = Tipo.Titulo.copy(fontSize = 28.sp))
            }

            state.foundAt?.let {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Bruma).padding(horizontal = 16.dp, vertical = 13.dp)
                ) { Text("Alguien avisó que lo encontró a las ${Format.clock(it)}.", style = Tipo.Fila, color = Medio) }
            }

            Tarjeta {
                FilaDato(
                    "Última ubicación",
                    last?.let { "${Format.ago(it.point.capturedAt, now)} · ${Format.accuracy(it.point)}" } ?: "aún no llega"
                )
                Separador()
                FilaDato(
                    "Llegó por",
                    when (last?.via) {
                        CommChannel.INTERNET -> "Internet"
                        CommChannel.SMS -> "SMS · sin datos"
                        CommChannel.BLUETOOTH -> if (last.deliveredAt != null) "Bluetooth · lo guardó un teléfono de la red y llegó tarde"
                            else "Bluetooth · teléfono cercano"
                        else -> "—"
                    }
                )
                Separador()
                FilaDato("Puntos de la ruta", state.points.size.toString())
                if (state.batteryPct != null) {
                    Separador()
                    FilaDato("Batería", "${state.batteryPct} %")
                }
                if (cerca != null) {
                    Separador()
                    FilaDato("Bluetooth", "cerca · a ${Format.meters(cerca.meters ?: 0.0)}", punto = Aurora)
                }
            }

            if (!state.inAlert && state.status != null) {
                Text(
                    "La ubicación solo se comparte cuando el celular está reportado. Si lo robaron o se perdió, repórtalo.",
                    style = Tipo.Nota
                )
                BotonAlerta("Reportar el celular de ${member.name}") { onReport(member) }
            }
            if (state.status == null) {
                AvisoAmbar(
                    "Si ya lo reportaste, la orden está en camino: llegará por internet cuando ese celular tenga conexión" +
                        if (member.phone != null) ", o por SMS." else ". Guarda su número para insistir por SMS."
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BotonSecundario("Registro", Modifier.weight(1f), alto = 52.dp, onClick = onRegistro)
                Box(Modifier.weight(1f)) { BotonPrincipal("Buscar de cerca", Modifier.height(52.dp), onClick = onBuscar) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BotonSecundario("Hacer sonar", Modifier.weight(1f)) { vm.command(member, RemoteCommand.SONAR) }
                BotonSecundario("Abrir en Maps", Modifier.weight(1f)) {
                    if (last == null) vm.say("Todavía no hay una ubicación") else abrirMapa(context, last.point, member.name)
                }
            }
            if (state.inAlert) {
                Enlace("Ya lo recuperó · terminar la alerta") { vm.command(member, RemoteCommand.RECUPERADO) }
            }
        }
    }
}

@Composable
private fun Leyenda(texto: String, punto: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        punto()
        Text(texto, fontSize = 12.sp, color = TextoNoche)
    }
}

private fun abrirMapa(context: Context, point: GeoPoint, nombre: String) {
    val uri = Uri.parse("geo:${point.latitude},${point.longitude}?q=${point.latitude},${point.longitude}(Celular de $nombre)")
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        .onFailure {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com/?q=${point.latitude},${point.longitude}"))
                )
            }
        }
}

// --- B4 · Registro -------------------------------------------------------------------------

/**
 * Registro del robo de alguien del círculo ([memberId]) o el propio (null):
 * línea de tiempo de lo que pasó y la ruta, con un informe para la denuncia.
 */
@Composable
fun RegistroScreen(vm: EscudoViewModel, memberId: String?, onBack: () -> Unit) {
    val context = LocalContext.current
    val members by vm.members.collectAsStateWithLifecycle()
    val remote by vm.remote.collectAsStateWithLifecycle()
    val ownEvents by vm.events.collectAsStateWithLifecycle()
    val ownPoints by vm.points.collectAsStateWithLifecycle()

    val member = members.firstOrNull { it.id == memberId }
    val events: List<EventEntry> = if (member != null) remote[member.id]?.events.orEmpty() else ownEvents
    val points: List<GeoPoint> =
        if (member != null) remote[member.id]?.points.orEmpty().map { it.point } else ownPoints.sortedBy { it.capturedAt }
    val titulo = member?.let { "Celular de ${it.name}" } ?: "Este celular"

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 18.dp)) {
        CabeceraSimple(if (member != null) "Registro del robo" else "Registro", "$titulo · ${points.size} ubicaciones", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 18.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Etiqueta("Qué pasó")
                if (events.isEmpty()) {
                    Text("Todavía no hay eventos registrados.", style = Tipo.Nota)
                } else {
                    Tarjeta(radio = 24.dp) {
                        Column(Modifier.padding(vertical = 6.dp)) {
                            events.takeLast(40).forEach { e ->
                                Row(Modifier.padding(vertical = 9.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Text(
                                        Format.clock(e.at), fontFamily = Sora, fontWeight = FontWeight(300),
                                        fontSize = 14.sp, color = Texto2, modifier = Modifier.width(44.dp)
                                    )
                                    Box(
                                        Modifier.padding(top = 4.dp).size(10.dp)
                                            .background(if (e.alert) Ambar else Aurora, CircleShape)
                                    )
                                    Column(Modifier.weight(1f)) {
                                        Text(e.title, style = Tipo.Fila.copy(fontWeight = FontWeight(600)), color = if (e.alert) AmbarTexto else Tinta)
                                        if (e.detail.isNotBlank()) Text(e.detail, fontSize = 12.sp, color = Texto2)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Etiqueta("Últimas ubicaciones")
                if (points.isEmpty()) {
                    Text(
                        if (member != null) "Aún no llegó ninguna ubicación de este celular." else "Activa el Escudo para empezar a registrar tu ruta.",
                        style = Tipo.Nota
                    )
                } else {
                    Tarjeta {
                        val ultimos = points.takeLast(8).reversed()
                        ultimos.forEachIndexed { i, p ->
                            FilaDato(Format.clockSeconds(p.capturedAt), "${Format.coords(p)} · ${Format.accuracy(p)}")
                            if (i < ultimos.lastIndex) Separador()
                        }
                    }
                }
            }
        }
        BotonPrincipal("Compartir informe para la Policía", enabled = points.isNotEmpty() || events.isNotEmpty()) {
            val texto = informe(titulo, events, points)
            context.startActivity(
                Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto), "Compartir informe")
            )
        }
    }
}

private fun informe(titulo: String, events: List<EventEntry>, points: List<GeoPoint>): String = buildString {
    appendLine("RED CENTINELA · Informe del Escudo")
    appendLine("$titulo · generado el ${Format.day(System.currentTimeMillis())} a las ${Format.clock(System.currentTimeMillis())}")
    appendLine()
    appendLine("EVENTOS")
    if (events.isEmpty()) appendLine("(sin eventos)")
    events.forEach { appendLine("${Format.clockSeconds(it.at)}  ${it.title}${if (it.detail.isBlank()) "" else " — ${it.detail}"}") }
    appendLine()
    appendLine("RUTA (${points.size} ubicaciones; se listan las últimas ${minOf(points.size, 60)})")
    points.takeLast(60).forEach {
        appendLine("${Format.clockSeconds(it.capturedAt)}  ${Format.coords(it)}  ${Format.accuracy(it)}")
    }
    points.lastOrNull()?.let {
        appendLine()
        appendLine("Última ubicación: https://maps.google.com/?q=${it.latitude},${it.longitude}")
    }
}

// --- Búsqueda cercana por Bluetooth -----------------------------------------------------------

/**
 * Cuando el mapa ya te llevó cerca: mide la señal Bluetooth del celular y
 * guía con pitidos más seguidos a medida que te acercas. Sin internet.
 */
@Composable
fun BuscarScreen(vm: EscudoViewModel, memberId: String, onBack: () -> Unit) {
    val members by vm.members.collectAsStateWithLifecycle()
    val reading by vm.proximity.collectAsStateWithLifecycle()
    val member = members.firstOrNull { it.id == memberId }
    if (member == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    DisposableEffect(memberId) {
        vm.startSeeking(memberId)
        onDispose { vm.stopSeeking() }
    }
    val heading by vm.heading.collectAsStateWithLifecycle()
    val sweep by vm.sweep.collectAsStateWithLifecycle()
    val band = reading?.band ?: ProximityBand.SIN_SENAL
    val color = when (band) {
        ProximityBand.MUY_CERCA, ProximityBand.CERCA -> Ambar
        ProximityBand.SIN_SENAL -> TextoNoche
        else -> Aurora
    }
    val rumbo = heading
    val barrido = sweep

    Column(Modifier.fillMaxSize().background(Noche).padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            BotonRedondo(Iconos.Atras, "Volver", sobreNoche = true, onClick = onBack)
            Column {
                Text("Buscar de cerca", style = Tipo.Fuerte, color = Color.White)
                Text("Celular de ${member.name}", style = Tipo.Nota, color = TextoNoche)
            }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (rumbo != null && reading != null) {
                Radar(Modifier.size(236.dp), rumbo, barrido, color)
            } else {
                Halo(
                    Modifier.fillMaxWidth().height(220.dp), color = color, centro = color, fondo = Noche,
                    punteado = band == ProximityBand.SIN_SENAL
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                reading?.let { "≈ ${Format.meters(it.estimatedMeters)}" } ?: "—",
                style = Tipo.Cifra, color = Color.White
            )
            Text(
                when (band) {
                    ProximityBand.MUY_CERCA -> "¡Está aquí!"
                    ProximityBand.CERCA -> "Muy cerca"
                    ProximityBand.MEDIO -> "Vas bien"
                    ProximityBand.LEJOS -> "Señal débil"
                    ProximityBand.SIN_SENAL -> "Sin señal todavía"
                },
                fontFamily = Sora, fontWeight = FontWeight(300), fontSize = 26.sp, color = color
            )
            Spacer(Modifier.height(8.dp))
            Text(
                when {
                    !vm.bluetoothReady() -> "Enciende el Bluetooth y concede el permiso para buscar."
                    reading == null -> "Camina por la zona que marca el mapa. La señal aparece a unos 20–30 m, si ese celular tiene el Escudo activo."
                    band == ProximityBand.MUY_CERCA -> "Hazlo sonar: debería estar al alcance de la mano."
                    rumbo != null && barrido?.bearing != null -> "Sigue la flecha unos pasos y vuelve a girar para afinar."
                    rumbo != null -> "Gira despacio una vuelta completa, con el celular frente al pecho, para saber hacia dónde ir."
                    reading?.trend == ProximityTrend.ACERCANDOSE -> "Te estás acercando"
                    reading?.trend == ProximityTrend.ALEJANDOSE -> "Te estás alejando"
                    else -> "Sigue moviéndote despacio"
                },
                style = Tipo.Cuerpo, color = TextoNoche, textAlign = TextAlign.Center
            )
            if (reading != null && rumbo != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    when (reading?.trend) {
                        ProximityTrend.ACERCANDOSE -> "Te estás acercando"
                        ProximityTrend.ALEJANDOSE -> "Te estás alejando"
                        else -> " "
                    },
                    style = Tipo.Nota, color = color
                )
            }
        }
        BotonDestello("Hacer sonar ese celular", alto = 56.dp) { vm.command(member, RemoteCommand.SONAR) }
    }
}

/**
 * Brújula de señal: el anillo muestra, para cada dirección ya barrida, qué tan
 * fuerte llegó la señal (arriba = hacia donde miras ahora). Cuando hay
 * contraste suficiente aparece la flecha hacia la dirección más fuerte.
 */
@Composable
private fun Radar(modifier: Modifier, rumbo: Float, barrido: DirectionFinder.Sweep?, color: Color) {
    Canvas(modifier) {
        val centro = Offset(size.width / 2, size.height / 2)
        val radio = size.minDimension / 2 - 10.dp.toPx()
        val sectores = barrido?.strength ?: List(12) { null }
        val ancho = 360f / sectores.size
        sectores.forEachIndexed { i, fuerza ->
            // 0° del lienzo apunta a la derecha; la brújula, hacia arriba.
            val inicio = i * ancho - rumbo - 90f - ancho / 2 + 3f
            drawArc(
                color = if (fuerza == null) TextoNoche.copy(alpha = 0.22f) else color.copy(alpha = 0.18f + 0.82f * fuerza),
                startAngle = inicio, sweepAngle = ancho - 6f, useCenter = false,
                topLeft = Offset(centro.x - radio, centro.y - radio), size = Size(radio * 2, radio * 2),
                style = Stroke(
                    width = if (fuerza == null) 2.dp.toPx() else (4 + 10 * fuerza).dp.toPx(), cap = StrokeCap.Round
                )
            )
        }
        // Marca fija: hacia donde miras.
        drawLine(
            Color.White.copy(alpha = 0.7f), Offset(centro.x, centro.y - radio - 9.dp.toPx()),
            Offset(centro.x, centro.y - radio + 9.dp.toPx()), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round
        )
        val destino = barrido?.bearing
        if (destino != null) {
            rotate(destino - rumbo, centro) {
                val punta = Offset(centro.x, centro.y - radio + 26.dp.toPx())
                drawLine(color, centro, punta, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                val flecha = Path().apply {
                    moveTo(punta.x, punta.y - 14.dp.toPx())
                    lineTo(punta.x - 11.dp.toPx(), punta.y + 8.dp.toPx())
                    lineTo(punta.x + 11.dp.toPx(), punta.y + 8.dp.toPx())
                    close()
                }
                drawPath(flecha, color)
            }
        }
        drawCircle(Noche, 11.dp.toPx(), centro)
        drawCircle(color, 7.dp.toPx(), centro)
    }
}
