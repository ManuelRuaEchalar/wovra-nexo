package com.redcentinela.escudo.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.redcentinela.escudo.data.ble.TagLink
import com.redcentinela.escudo.ui.EscudoViewModel
import com.redcentinela.escudo.ui.component.Avatar
import com.redcentinela.escudo.ui.component.CabeceraSimple
import com.redcentinela.escudo.ui.component.Enlace
import com.redcentinela.escudo.ui.component.Etiqueta
import com.redcentinela.escudo.ui.component.FilaItem
import com.redcentinela.escudo.ui.component.Halo
import com.redcentinela.escudo.ui.component.Iconos
import com.redcentinela.escudo.ui.component.Interruptor
import com.redcentinela.escudo.ui.component.Separador
import com.redcentinela.escudo.ui.component.Tarjeta
import com.redcentinela.escudo.ui.theme.AmbarTexto
import com.redcentinela.escudo.ui.theme.Aurora
import com.redcentinela.escudo.ui.theme.Destello
import com.redcentinela.escudo.ui.theme.Medio
import com.redcentinela.escudo.ui.theme.Noche
import com.redcentinela.escudo.ui.theme.Tipo

/**
 * A4 · Vincular guardián. El guardián es algo que se queda contigo: un
 * llavero Bluetooth tipo iTag o el celular de alguien con quien vas.
 */
@Composable
fun GuardianContenido(vm: EscudoViewModel, modifier: Modifier = Modifier) {
    val members by vm.members.collectAsStateWithLifecycle()
    val tag by vm.tagState.collectAsStateWithLifecycle()
    val enlazado = vm.config.tagAddress != null && tag != TagLink.State.SIN_LLAVERO
    // Solo se buscan llaveros mientras no haya uno enlazado.
    val cercanos by remember(enlazado) { if (enlazado) kotlinx.coroutines.flow.flowOf(emptyList()) else vm.discoverTags() }
        .collectAsState(initial = emptyList())

    Column(modifier.verticalScroll(rememberScrollState()).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Vincula tu guardián", style = Tipo.Titulo)
            Text("Se queda contigo. Si tu celular se aleja, avisa al instante, con el ladrón todavía cerca.", style = Tipo.Cuerpo)
        }

        Box(Modifier.fillMaxWidth().height(170.dp), contentAlignment = Alignment.Center) {
            Halo(Modifier.fillMaxSize(), punteado = !enlazado, centro = Color.Transparent)
            Box(
                Modifier.size(width = 50.dp, height = 62.dp).background(Color.White, RoundedCornerShape(18.dp))
                    .border(1.6.dp, Noche, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(Iconos.Guardian, null, tint = Noche, modifier = Modifier.size(24.dp)) }
        }

        if (enlazado) {
            Tarjeta(borde = Aurora, relleno = 16.dp) {
                FilaItem(
                    vm.config.tagName ?: "Llavero",
                    when (tag) {
                        TagLink.State.CERCA -> "Enlazado · cerca"
                        TagLink.State.LEJOS -> "Enlazado · fuera de alcance"
                        else -> "Enlazado · buscando…"
                    },
                    colorNota = if (tag == TagLink.State.LEJOS) AmbarTexto else Medio,
                    inicio = { IconoLlavero() }
                ) { Enlace("Quitar", Modifier.fillMaxWidth(0.3f), color = AmbarTexto) { vm.unlinkTag() } }
            }
        } else {
            Column {
                Etiqueta("Llaveros cerca", Modifier.padding(start = 4.dp, bottom = 8.dp))
                if (cercanos.isEmpty()) {
                    Text(
                        "Buscando… Enciende tu llavero (iTag) y acércalo. Si no tienes uno, usa el celular de alguien de tu círculo.",
                        style = Tipo.Nota, modifier = Modifier.padding(horizontal = 4.dp)
                    )
                } else {
                    Tarjeta(relleno = 16.dp) {
                        cercanos.take(5).forEachIndexed { i, found ->
                            FilaItem(
                                found.name,
                                "Encontrado · señal ${if (found.rssi > -60) "fuerte" else if (found.rssi > -80) "media" else "débil"}",
                                inicio = { IconoLlavero() },
                                onClick = { vm.linkTag(found) }
                            ) { Text("Vincular", style = Tipo.Fila.copy(fontWeight = FontWeight(600)), color = Medio) }
                            if (i < minOf(cercanos.size, 5) - 1) Separador()
                        }
                    }
                }
            }
        }

        Column {
            Etiqueta("O usa como guardián", Modifier.padding(start = 4.dp, bottom = 8.dp))
            if (members.isEmpty()) {
                Text(
                    "El celular de alguien con quien vas: agrégalo primero a tu círculo.",
                    style = Tipo.Nota, modifier = Modifier.padding(horizontal = 4.dp)
                )
            } else {
                Tarjeta(relleno = 16.dp) {
                    members.forEachIndexed { i, member ->
                        FilaItem(
                            "El celular de ${member.name}",
                            if (member.isGuardian) "Es tu guardián" else "Avisa si tu celular se aleja del suyo",
                            inicio = { Avatar(member.initial, tamano = 40.dp) }
                        ) { Interruptor(member.isGuardian) { vm.setGuardian(member, it) } }
                        if (i < members.lastIndex) Separador()
                    }
                }
            }
        }
    }
}

@Composable
private fun IconoLlavero() {
    Box(Modifier.size(44.dp).background(Noche, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
        Icon(Iconos.Guardian, null, tint = Destello, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun GuardianScreen(vm: EscudoViewModel, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 12.dp)) {
        CabeceraSimple("Guardián", onBack = onBack)
        Spacer(Modifier.height(20.dp))
        GuardianContenido(vm)
    }
}
