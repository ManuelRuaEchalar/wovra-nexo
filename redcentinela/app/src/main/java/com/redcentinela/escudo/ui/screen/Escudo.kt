package com.redcentinela.escudo.ui.screen

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.redcentinela.escudo.data.ble.TagLink
import com.redcentinela.escudo.domain.model.CommChannel
import com.redcentinela.escudo.domain.model.ShieldStatus
import com.redcentinela.escudo.ui.EscudoViewModel
import com.redcentinela.escudo.ui.component.BotonDestello
import com.redcentinela.escudo.ui.component.BotonPrincipal
import com.redcentinela.escudo.ui.component.Chip
import com.redcentinela.escudo.ui.component.FilaDato
import com.redcentinela.escudo.ui.component.Halo
import com.redcentinela.escudo.ui.component.Iconos
import com.redcentinela.escudo.ui.component.Separador
import com.redcentinela.escudo.ui.component.Tarjeta
import com.redcentinela.escudo.ui.component.TarjetaNoche
import com.redcentinela.escudo.ui.component.toque
import com.redcentinela.escudo.ui.theme.Ambar
import com.redcentinela.escudo.ui.theme.AmbarNoche
import com.redcentinela.escudo.ui.theme.AmbarOscuro
import com.redcentinela.escudo.ui.theme.AmbarSuave
import com.redcentinela.escudo.ui.theme.AmbarTexto
import com.redcentinela.escudo.ui.theme.AmbarTinta
import com.redcentinela.escudo.ui.theme.Aurora
import com.redcentinela.escudo.ui.theme.Linea
import com.redcentinela.escudo.ui.theme.Medio
import com.redcentinela.escudo.ui.theme.Sora
import com.redcentinela.escudo.ui.theme.Superficie
import com.redcentinela.escudo.ui.theme.Texto2
import com.redcentinela.escudo.ui.theme.TextoNoche
import com.redcentinela.escudo.ui.theme.Tinta
import com.redcentinela.escudo.ui.theme.Tipo
import com.redcentinela.escudo.util.Format
import kotlinx.coroutines.delay

/** Reloj que refresca los textos "hace 2 min" sin recomponer toda la app. */
@Composable
fun ahora(cadaMs: Long = 5_000): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(cadaMs) {
        while (true) {
            delay(cadaMs)
            now = System.currentTimeMillis()
        }
    }
    return now
}

// --- B1 · Inicio · Escudo -------------------------------------------------------------

@Composable
fun EscudoScreen(
    vm: EscudoViewModel,
    onReport: () -> Unit,
    onGuardian: () -> Unit,
    onRegistro: () -> Unit,
    onAjustes: () -> Unit,
    onCiudad: () -> Unit
) {
    val state by vm.shield.collectAsStateWithLifecycle()
    val members by vm.members.collectAsStateWithLifecycle()
    val presence by vm.presence.collectAsStateWithLifecycle()
    val tag by vm.tagState.collectAsStateWithLifecycle()
    val now = ahora()
    val activo = state.status != ShieldStatus.INACTIVO

    // Guardián: llavero o celular de alguien del círculo.
    val guardianMember = members.firstOrNull { it.isGuardian }
    val guardianPresence = guardianMember?.let { presence[it.id] }
    val (guardianTexto, guardianCerca) = when {
        tag == TagLink.State.CERCA -> "Llavero guardián cerca" to true
        tag == TagLink.State.LEJOS -> "Llavero guardián fuera de alcance" to false
        tag == TagLink.State.BUSCANDO -> "Buscando el llavero guardián…" to null
        guardianMember != null && guardianPresence?.near == true ->
            "Guardián: ${guardianMember.name} a ${Format.meters(guardianPresence.meters ?: 0.0)}" to true
        guardianMember != null && guardianPresence != null -> "Guardián: ${guardianMember.name} fuera de alcance" to false
        guardianMember != null -> "Guardián: ${guardianMember.name} · buscando su señal…" to null
        else -> "Sin guardián · toca para vincular uno" to null
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
            .padding(top = 12.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(Format.greeting(now), style = Tipo.Nota)
                Text(vm.config.userName.ifBlank { "Tu Escudo" }, style = Tipo.Fuerte.copy(fontSize = 18.sp))
            }
            Box(
                Modifier.size(44.dp).border(1.dp, Linea, CircleShape).toque(onClick = onAjustes),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    vm.config.userName.trim().take(1).uppercase().ifEmpty { "·" },
                    fontFamily = Sora, fontWeight = FontWeight(300), fontSize = 17.sp, color = Tinta
                )
            }
        }

        if (state.status.isAlert) {
            TarjetaEnAlerta(vm, now)
        } else {
            val pulso by rememberInfiniteTransition(label = "halo").animateFloat(
                0f, 1f, infiniteRepeatable(tween(2600), RepeatMode.Reverse), label = "pulso"
            )
            Halo(
                Modifier.fillMaxWidth().height(196.dp),
                color = if (!activo) Texto2 else if (guardianCerca == false) Ambar else Aurora,
                punteado = !activo,
                guardian = activo && guardianCerca == true,
                pulso = if (activo) pulso else 0f
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (activo) "Escudo activo" else "Escudo apagado", style = Tipo.Display)
                Text(
                    if (activo) guardianTexto else "No registra tu ruta ni vigila el equipo.",
                    style = Tipo.Cuerpo,
                    color = if (activo && guardianCerca == false) AmbarTexto else Texto2,
                    modifier = if (activo) Modifier.toque(onClick = onGuardian) else Modifier
                )
            }
        }

        if (!activo) {
            BotonPrincipal("Activar Escudo") { vm.startShield() }
        } else {
            val canal = if (state.status.isAlert) state.lastChannel ?: vm.currentChannel() else vm.currentChannel()
            Tarjeta {
                FilaDato(
                    "Último registro",
                    state.lastPoint?.let { "${Format.ago(it.capturedAt, now)} · ${Format.accuracy(it)}" }
                        ?: "buscando ubicación…"
                )
                Separador()
                FilaDato(
                    "Canal de envío", canal.label,
                    punto = if (canal == CommChannel.INTERNET) Aurora else if (canal == CommChannel.LOCAL) Texto2 else Ambar
                )
                Separador()
                FilaDato(
                    "Ruta guardada",
                    if (state.pendingToSend > 0) "${state.pendingToSend} puntos sin enviar" else "al día",
                    onClick = onRegistro
                )
            }
        }

        // Módulos de la red. Hoy está construido el Escudo; el resto llega después.
        val filas = listOf(
            listOf(Triple("Fila", Iconos.Fila, false), Triple("Micros", Iconos.Micros, false), Triple("Señal", Iconos.Senal, false)),
            listOf(Triple("Llegué", Iconos.Llegue, false), Triple("Huellita", Iconos.Huellita, false), Triple("Reportar", Iconos.Reportar, true))
        )
        filas.forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                fila.forEach { (titulo, icono, alerta) ->
                    Modulo(titulo, icono, alerta, Modifier.weight(1f)) { if (alerta) onReport() else onCiudad() }
                }
            }
        }
    }
}

@Composable
private fun Modulo(titulo: String, icono: ImageVector, alerta: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val forma = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier.clip(forma)
            .background(if (alerta) AmbarSuave else Superficie)
            .then(if (alerta) Modifier else Modifier.border(1.dp, Linea, forma))
            .toque(onClick = onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier.size(40.dp)
                .then(if (alerta) Modifier.background(Ambar, CircleShape) else Modifier.border(1.dp, Linea, CircleShape))
                .alpha(if (alerta) 1f else 0.6f),
            contentAlignment = Alignment.Center
        ) { Icon(icono, null, tint = if (alerta) AmbarOscuro else Medio, modifier = Modifier.size(20.dp)) }
        Text(
            titulo, style = Tipo.Nota.copy(fontWeight = FontWeight(if (alerta) 600 else 500)),
            color = if (alerta) AmbarTinta else Texto2
        )
    }
}

/** Este mismo celular está reportado: tarjeta nocturna con lo que está haciendo. */
@Composable
private fun TarjetaEnAlerta(vm: EscudoViewModel, now: Long) {
    val state by vm.shield.collectAsStateWithLifecycle()
    val robo = state.status == ShieldStatus.ALERTA_ROBO
    TarjetaNoche {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Chip(
                if (robo) "En vivo · ubicación cada 20 s" else "Modo encontrado activo",
                fondo = Ambar.copy(alpha = 0.18f), color = AmbarNoche
            )
            Spacer(Modifier.height(36.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (robo) "Reportado como robado" else "Reportado como perdido",
                    fontFamily = Sora, fontWeight = FontWeight(300), fontSize = 28.sp, color = Color.White
                )
                Text(
                    buildString {
                        append(state.alertSince?.let { Format.ago(it, now) } ?: "")
                        state.reportedBy?.let { append(" · desde el celular de $it") }
                        append(". Tu círculo recibe la ubicación por ${(state.lastChannel ?: vm.currentChannel()).label}.")
                    }.replaceFirstChar { it.uppercase() },
                    style = Tipo.Fila, color = TextoNoche
                )
            }
            BotonDestello("Lo recuperé") { vm.standDown() }
        }
    }
}

// --- Ciudad (aún no construido) -----------------------------------------------------------

@Composable
fun CiudadScreen() {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
            .padding(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Ciudad", style = Tipo.Titulo)
        Text(
            "La misma red que cuida tu celular medirá la fila del surtidor, mostrará dónde viene tu micro y " +
                "qué operadora tiene mejor señal. Esta versión construye primero el Escudo.",
            style = Tipo.Cuerpo
        )
        Tarjeta(radio = 24.dp, relleno = 16.dp) {
            val modulos = listOf(
                Triple("Fila", "Espera real en cada surtidor", Iconos.Fila),
                Triple("Micros", "Dónde viene y qué tan lleno", Iconos.Micros),
                Triple("Señal", "Cobertura real por operadora", Iconos.Senal)
            )
            modulos.forEachIndexed { i, (titulo, nota, icono) ->
                com.redcentinela.escudo.ui.component.FilaItem(
                    titulo, nota, inicio = { com.redcentinela.escudo.ui.component.IconoRedondo(icono) }
                ) { Chip("Próximamente", fondo = Color.Transparent, color = Texto2, borde = Linea) }
                if (i < modulos.lastIndex) Separador()
            }
        }
    }
}
