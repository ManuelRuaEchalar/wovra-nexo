package com.redcentinela.escudo.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.redcentinela.escudo.domain.model.CircleMember
import com.redcentinela.escudo.ui.EscudoViewModel
import com.redcentinela.escudo.ui.component.BotonAlerta
import com.redcentinela.escudo.ui.component.BotonPrincipal
import com.redcentinela.escudo.ui.component.CabeceraSimple
import com.redcentinela.escudo.ui.component.Etiqueta
import com.redcentinela.escudo.ui.component.Iconos
import com.redcentinela.escudo.ui.component.Separador
import com.redcentinela.escudo.ui.component.Vineta
import com.redcentinela.escudo.ui.component.toque
import com.redcentinela.escudo.ui.theme.Ambar
import com.redcentinela.escudo.ui.theme.AmbarOscuro
import com.redcentinela.escudo.ui.theme.Linea
import com.redcentinela.escudo.ui.theme.Medio
import com.redcentinela.escudo.ui.theme.Noche
import com.redcentinela.escudo.ui.theme.NocheClara
import com.redcentinela.escudo.ui.theme.Superficie
import com.redcentinela.escudo.ui.theme.Tinta
import com.redcentinela.escudo.ui.theme.Tipo

/**
 * B2 · Reportar. Se reporta el celular de OTRA persona del círculo desde el
 * propio: quien perdió su celular no lo tiene en la mano.
 */
@Composable
fun ReportarScreen(
    vm: EscudoViewModel,
    inicial: String?,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onReported: (CircleMember) -> Unit
) {
    val members by vm.members.collectAsStateWithLifecycle()
    var elegido by rememberSaveable { mutableStateOf(inicial) }
    var robo by rememberSaveable { mutableStateOf(true) }
    val member = members.firstOrNull { it.id == elegido } ?: members.firstOrNull()

    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 18.dp)) {
        CabeceraSimple("Reportar un celular", onBack = onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 18.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (member == null) {
                Text("Aún no tienes a nadie en tu círculo", style = Tipo.Titulo)
                Text(
                    "Solo puedes reportar celulares de personas de tu círculo, y ellas el tuyo. " +
                        "Vincúlense una vez (con el código QR) y quedará listo para cuando haga falta.",
                    style = Tipo.Cuerpo
                )
                return@Column
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Etiqueta("¿De quién es?")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    members.forEach { m ->
                        val activo = m.id == member.id
                        Row(
                            Modifier.height(40.dp).clip(RoundedCornerShape(50))
                                .background(if (activo) Noche else Superficie)
                                .then(if (activo) Modifier else Modifier.border(1.dp, Linea, RoundedCornerShape(50)))
                                .toque { elegido = m.id }
                                .padding(start = if (activo) 8.dp else 16.dp, end = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (activo) Box(Modifier.size(24.dp).background(NocheClara, CircleShape), contentAlignment = Alignment.Center) {
                                Text(m.initial, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight(600))
                            }
                            Text(
                                m.name, style = Tipo.Fila.copy(fontWeight = FontWeight(if (activo) 600 else 400)),
                                color = if (activo) Color.White else Tinta
                            )
                        }
                    }
                }
            }

            Text("¿Qué pasó?", style = Tipo.Titulo)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Opcion(
                    "Se lo robaron", "Ubicación cada 20 s y aviso a todo el círculo. Si intentan apagarlo, simula apagarse y sigue registrando.",
                    Iconos.Reportar, activo = robo, alerta = true
                ) { robo = true }
                Opcion(
                    "Lo perdió", "Mensaje en pantalla para quien lo encuentre, que puede avisar sin desbloquearlo.",
                    Iconos.Buscar, activo = !robo, alerta = false
                ) { robo = false }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Separador()
                Spacer(Modifier.height(8.dp))
                Etiqueta("Cómo llega la orden")
                Spacer(Modifier.height(2.dp))
                Vineta("Por internet, si los dos celulares tienen conexión.")
                Vineta(
                    if (member.phone != null) "Por SMS al ${member.phone}, si alguno no tiene datos."
                    else "Por SMS no: falta guardar el número de ${member.name} (en su ficha del círculo)."
                )
                Vineta("Con llavero guardián: apretando su botón, sin celular.")
            }
        }

        when {
            member == null -> BotonPrincipal("Agregar a alguien", onClick = onAdd)
            robo -> BotonAlerta("Reportar robo del celular de ${member.name}") {
                vm.report(member, stolen = true)
                onReported(member)
            }
            else -> BotonPrincipal("Marcar como perdido") {
                vm.report(member, stolen = false)
                onReported(member)
            }
        }
    }
}

@Composable
private fun Opcion(titulo: String, nota: String, icono: ImageVector, activo: Boolean, alerta: Boolean, onClick: () -> Unit) {
    val forma = RoundedCornerShape(24.dp)
    val borde = if (!activo) Linea else if (alerta) Ambar else Medio
    Row(
        Modifier.fillMaxWidth().clip(forma).background(Superficie)
            .border(if (activo) 2.dp else 1.dp, borde, forma)
            .toque(onClick = onClick).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier.size(44.dp)
                .then(if (alerta) Modifier.background(Ambar, CircleShape) else Modifier.border(1.dp, Linea, CircleShape)),
            contentAlignment = Alignment.Center
        ) { Icon(icono, null, tint = if (alerta) AmbarOscuro else Medio, modifier = Modifier.size(20.dp)) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(titulo, style = Tipo.Fuerte.copy(fontSize = 17.sp))
            Text(nota, style = Tipo.Nota)
        }
    }
}

