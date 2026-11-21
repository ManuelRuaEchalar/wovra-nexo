package com.redcentinela.escudo.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.redcentinela.escudo.data.ble.TagLink
import com.redcentinela.escudo.domain.model.ShieldStatus
import com.redcentinela.escudo.domain.model.SystemSignal
import com.redcentinela.escudo.service.EscudoConfig
import com.redcentinela.escudo.ui.EscudoViewModel
import com.redcentinela.escudo.ui.Permisos
import com.redcentinela.escudo.ui.component.AvisoAmbar
import com.redcentinela.escudo.ui.component.BotonSecundario
import com.redcentinela.escudo.ui.component.Campo
import com.redcentinela.escudo.ui.component.Enlace
import com.redcentinela.escudo.ui.component.Etiqueta
import com.redcentinela.escudo.ui.component.FilaItem
import com.redcentinela.escudo.ui.component.Halo
import com.redcentinela.escudo.ui.component.Iconos
import com.redcentinela.escudo.ui.component.Interruptor
import com.redcentinela.escudo.ui.component.Separador
import com.redcentinela.escudo.ui.component.Tarjeta
import com.redcentinela.escudo.ui.component.Vineta
import com.redcentinela.escudo.ui.theme.AmbarTexto
import com.redcentinela.escudo.ui.theme.Medio
import com.redcentinela.escudo.ui.theme.Texto2
import com.redcentinela.escudo.ui.theme.Tipo

/** D5 · Ajustes y privacidad. Incluye las pruebas del Escudo para ensayar la demo. */
@Composable
fun AjustesScreen(vm: EscudoViewModel, refresco: Int, onGuardian: () -> Unit) {
    val context = LocalContext.current
    val state by vm.shield.collectAsStateWithLifecycle()
    val members by vm.members.collectAsStateWithLifecycle()
    val tag by vm.tagState.collectAsStateWithLifecycle()
    val activo = state.status != ShieldStatus.INACTIVO

    var apagadoFalso by rememberSaveable { mutableStateOf(vm.config.fakeShutdown) }
    var pitido by rememberSaveable { mutableStateOf(vm.config.beepOnSeparation) }
    var aislado by rememberSaveable { mutableStateOf(vm.config.selfAlertMinutes) }
    var nombre by rememberSaveable { mutableStateOf(vm.config.userName) }
    var numero by rememberSaveable { mutableStateOf(vm.config.phone ?: "") }
    var relevo by rememberSaveable { mutableStateOf(vm.config.relayUrl) }
    var recompensa by rememberSaveable { mutableStateOf(vm.config.rewardBs.takeIf { it > 0 }?.toString() ?: "") }
    var mensajeRecompensa by rememberSaveable { mutableStateOf(vm.config.rewardMessage) }
    var servidorRecompensas by rememberSaveable { mutableStateOf(vm.config.rewardServerUrl) }
    // Lo guardado, como estado: las preferencias no avisan a la pantalla cuando cambian.
    var recompensaGuardada by rememberSaveable {
        mutableStateOf(Triple(vm.config.rewardBs, vm.config.rewardMessage, vm.config.rewardServerUrl))
    }
    val accesibilidad = refresco.let { Permisos.apagadoFalso(context) }
    val sinAhorro = refresco.let { Permisos.sinAhorroBateria(context) }

    val guardianMember = members.firstOrNull { it.isGuardian }
    val (guardianTitulo, guardianNota) = when {
        tag != TagLink.State.SIN_LLAVERO -> "Guardián · ${vm.config.tagName ?: "Llavero"}" to when (tag) {
            TagLink.State.CERCA -> "Enlazado · cerca"
            TagLink.State.LEJOS -> "Enlazado · fuera de alcance"
            else -> "Enlazado · buscando…"
        }
        guardianMember != null -> "Guardián · ${guardianMember.name}" to "El celular de quien va contigo"
        else -> "Sin guardián" to "Vincula un llavero o el celular de alguien"
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
            .padding(top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Ajustes", style = Tipo.Titulo)

        Tarjeta(relleno = 16.dp) {
            FilaItem(
                guardianTitulo, guardianNota,
                colorNota = if (tag == TagLink.State.LEJOS) AmbarTexto else if (guardianMember != null || tag != TagLink.State.SIN_LLAVERO) Medio else Texto2,
                inicio = { Halo(Modifier.size(width = 54.dp, height = 36.dp), punteado = guardianMember == null && tag == TagLink.State.SIN_LLAVERO) },
                onClick = onGuardian
            ) { Icon(Iconos.Chevron, null, tint = Texto2, modifier = Modifier.size(18.dp)) }
        }

        Seccion("Escudo") {
            Tarjeta(relleno = 16.dp) {
                FilaItem("Escudo activo", "Registra tu ruta y vigila el equipo en segundo plano") {
                    Interruptor(activo) { if (it) vm.startShield() else vm.stopShield() }
                }
                Separador()
                FilaItem("Apagado falso", "La pantalla simula apagarse y sigue registrando") {
                    Interruptor(apagadoFalso) {
                        apagadoFalso = it
                        vm.config.fakeShutdown = it
                    }
                }
                Separador()
                FilaItem(
                    "Alerta sola si queda aislado",
                    if (aislado > 0) "Bloqueado, sin internet ni SMS y lejos de tu guardián por $aislado min: se reporta solo. Toca para cambiar."
                    else "Desactivada. Toca para cambiar.",
                    onClick = {
                        aislado = when (aislado) { 10 -> 0; 0 -> 2; 2 -> 5; else -> 10 }
                        vm.config.selfAlertMinutes = aislado
                    }
                ) { Text(if (aislado > 0) "$aislado min" else "No", style = Tipo.Fila, color = Medio) }
                Separador()
                FilaItem("Pitido al alejarte", "Suena si se aleja el celular que cuidas como guardián") {
                    Interruptor(pitido) {
                        pitido = it
                        vm.config.beepOnSeparation = it
                    }
                }
            }
        }
        if (apagadoFalso && !accesibilidad) {
            AvisoAmbar(
                "Para el apagado falso falta activar \"Dendra · Apagado falso\" en Accesibilidad.",
                accion = "Activar"
            ) { Permisos.abrirAccesibilidad(context) }
        }
        if (!sinAhorro) {
            AvisoAmbar("Tu marca puede cerrar la app para ahorrar batería.", accion = "Revisar") { Permisos.pedirSinAhorro(context) }
        }

        Seccion("Tu perfil") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Campo("Tu nombre", nombre, { nombre = it.take(30) })
                Campo("Número de este celular", numero, { numero = it.take(16) }, ayuda = "+591 7XXXXXXX", teclado = KeyboardType.Phone)
                if (nombre.trim() != vm.config.userName || numero.trim() != (vm.config.phone ?: "")) {
                    BotonSecundario("Guardar perfil") {
                        vm.config.userName = nombre
                        vm.config.phone = numero
                        vm.say("Perfil guardado. Si ya tenías círculo, vuelve a compartir tu código para actualizarlo.")
                    }
                }
            }
        }

        Seccion("Recompensa si lo pierdes") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Si reportan este celular como perdido, el monto se bloquea en un escrow de Stellar y la pantalla " +
                        "muestra un código para que quien lo encuentre lo cobre al devolverlo. Tú confirmas el pago. " +
                        "Nunca se ofrece en un robo.",
                    style = Tipo.Nota
                )
                Campo(
                    "Monto en bolivianos (vacío = sin recompensa)", recompensa,
                    { recompensa = it.filter(Char::isDigit).take(3) },
                    ayuda = "50", teclado = KeyboardType.Number
                )
                Campo("Mensaje para quien lo encuentre", mensajeRecompensa, { mensajeRecompensa = it.take(120) }, ayuda = "Gracias por devolverlo")
                var compartir by rememberSaveable { mutableStateOf(vm.config.rewardSharePct > 0) }
                Tarjeta(relleno = 16.dp) {
                    FilaItem(
                        "Compartir 10 % con la red",
                        "Si te lo devuelven, una parte va a los teléfonos que lo vieron y avisaron dónde estaba"
                    ) {
                        Interruptor(compartir) {
                            compartir = it
                            vm.config.rewardSharePct = if (it) 10 else 0
                        }
                    }
                }
                Campo("Servidor de recompensas", servidorRecompensas, { servidorRecompensas = it.trim() }, ayuda = "https://…", teclado = KeyboardType.Uri)
                val monto = recompensa.toIntOrNull() ?: 0
                if (Triple(monto, mensajeRecompensa.trim(), servidorRecompensas.trimEnd('/')) != recompensaGuardada) {
                    BotonSecundario("Guardar recompensa") {
                        vm.config.rewardBs = monto
                        vm.config.rewardMessage = mensajeRecompensa
                        vm.config.rewardServerUrl = servidorRecompensas
                        recompensa = vm.config.rewardBs.takeIf { it > 0 }?.toString() ?: ""
                        servidorRecompensas = vm.config.rewardServerUrl
                        recompensaGuardada = Triple(vm.config.rewardBs, vm.config.rewardMessage, vm.config.rewardServerUrl)
                        vm.say(
                            if (vm.config.rewardBs > 0) "Listo: ${vm.config.rewardBs} Bs si se pierde (máximo ${EscudoConfig.MAX_REWARD_BS})."
                            else "Recompensa desactivada."
                        )
                    }
                }
            }
        }

        Seccion("Ayudas a la red") {
            AyudasRed(vm)
        }

        Seccion("Panel web") {
            PanelWeb(vm)
        }

        Seccion("Privacidad") {
            Tarjeta(relleno = 16.dp) {
                Vineta("Tu ubicación solo sale del teléfono cuando está reportado como robado o perdido.")
                Separador()
                Vineta("Por internet y por Bluetooth viaja cifrada: solo tu círculo puede leerla.")
                Separador()
                Vineta("Tu ruta diaria se guarda cifrada en este teléfono y no se comparte.")
            }
        }

        Seccion("Probar el Escudo") {
            Tarjeta(relleno = 16.dp) {
                Prueba("Probar apagado falso", "Muestra el apagado simulado ahora. Para salir: toca la pantalla 5 veces seguidas.") { vm.testFakeShutdown() }
                Separador()
                Prueba("Simular robo en este celular", "Entra en alerta: envía tu ubicación a tu círculo por la cascada.") { vm.simulateTheft() }
                Separador()
                Prueba("Simular pérdida", "Muestra la pantalla \"Este celular está perdido\" sobre el bloqueo.") { vm.simulateLost() }
                Separador()
                Prueba("Simular retiro de chip", "Con el celular desbloqueado solo queda en el registro.") { vm.simulateSignal(SystemSignal.CHIP_RETIRADO) }
                if (state.status.isAlert) {
                    Separador()
                    Prueba("Terminar la alerta", "Vuelve a la vigilancia normal.") { vm.standDown() }
                }
            }
        }

        Seccion("Conexión") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Campo("Servidor de relevo (internet)", relevo, { relevo = it.trim() }, ayuda = EscudoConfig.DEFAULT_RELAY, teclado = KeyboardType.Uri)
                Text(
                    "Por aquí pasan los mensajes cifrados entre los teléfonos del círculo. Todos deben usar el mismo servidor.",
                    style = Tipo.Nota
                )
                if (relevo != vm.config.relayUrl) {
                    BotonSecundario("Guardar servidor") {
                        vm.config.relayUrl = relevo
                        relevo = vm.config.relayUrl
                        vm.say("Servidor guardado. Cierra y abre la app para aplicarlo.")
                    }
                }
            }
        }
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    Column {
        Etiqueta(titulo, Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp))
        contenido()
    }
}

@Composable
private fun Prueba(titulo: String, nota: String, onClick: () -> Unit) {
    FilaItem(titulo, nota, onClick = onClick) { Icon(Iconos.Chevron, null, tint = Texto2, modifier = Modifier.size(18.dp)) }
}

/**
 * Cuenta de rescate: con un correo y una contraseña, el dueño puede seguir y
 * controlar este celular desde cualquier navegador (sin necesitar a nadie del
 * círculo). La contraseña no sale del teléfono.
 */
@Composable
private fun PanelWeb(vm: EscudoViewModel) {
    val correoGuardado by vm.rescueEmail.collectAsStateWithLifecycle()
    val trabajando by vm.rescueWorking.collectAsStateWithLifecycle()
    var editando by rememberSaveable { mutableStateOf(false) }
    var correo by rememberSaveable { mutableStateOf(correoGuardado ?: "") }
    var clave by remember { mutableStateOf("") }
    var clave2 by remember { mutableStateOf("") }
    val servidor = vm.config.rewardServerUrl

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Si no tienes a nadie en tu círculo, o no puedes comunicarte con esa persona, entra desde cualquier " +
                "computadora para ver dónde está tu celular, reportarlo, hacerlo sonar o terminar la alerta.",
            style = Tipo.Nota
        )
        if (correoGuardado != null && !editando) {
            Tarjeta(relleno = 16.dp) {
                Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Etiqueta("Cuenta del panel", color = Medio)
                    Text(correoGuardado!!, style = Tipo.Fila)
                    if (servidor.isNotBlank()) Text("Entra en $servidor/panel", style = Tipo.Nota)
                    if (trabajando) Text("Guardando…", style = Tipo.Nota, color = Medio)
                    else androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Enlace("Cambiar contraseña", Modifier.weight(1f)) { editando = true }
                        Enlace("Eliminar cuenta", Modifier.weight(1f), color = AmbarTexto) { vm.removeRescue() }
                    }
                }
            }
        } else {
            Campo("Correo", correo, { correo = it.trim() }, ayuda = "tu@correo.com", teclado = KeyboardType.Email)
            Campo("Contraseña (mínimo 8 caracteres)", clave, { clave = it }, oculto = true)
            Campo("Repite la contraseña", clave2, { clave2 = it }, oculto = true)
            Text(
                "Anótala en un lugar seguro: no se puede recuperar. Nadie más la conoce, ni siquiera nuestro servidor.",
                style = Tipo.Nota, color = Texto2
            )
            when {
                servidor.isBlank() -> AvisoAmbar("Primero configura el servidor en \"Recompensa si lo pierdes\".")
                trabajando -> Text("Cifrando y guardando…", style = Tipo.Nota, color = Medio)
                else -> BotonSecundario(if (correoGuardado == null) "Crear cuenta del panel" else "Guardar") {
                    if (clave != clave2) vm.say("Las contraseñas no coinciden.")
                    else vm.setUpRescue(correo, clave) {
                        clave = ""; clave2 = ""; editando = false
                    }
                }
            }
            if (editando) Enlace("Cancelar") { editando = false; clave = ""; clave2 = "" }
        }
    }
}

/**
 * Lo que este teléfono ganó como nodo: cada aviso útil que entregó de un
 * celular ajeno en alerta se paga en USDC desde el fondo de la red.
 */
@Composable
private fun AyudasRed(vm: EscudoViewModel) {
    val stats by vm.nodeStats.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(Unit) { vm.refreshNodeStats() }
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Con el Escudo activo, tu celular escucha por Bluetooth a los celulares perdidos o robados que pasan cerca " +
                "y entrega su aviso cifrado (sin poder leerlo). Cada aviso útil se paga en USDC en la red Stellar, " +
                "desde un fondo que se alimenta con las suscripciones.",
            style = Tipo.Nota
        )
        val s = stats
        Tarjeta(relleno = 16.dp) {
            if (s == null) {
                Column(Modifier.padding(vertical = 14.dp)) {
                    Text("Todavía no hay datos de la red.", style = Tipo.Nota)
                }
            } else {
                FilaItem("Avisos útiles entregados", "Celulares ajenos que ayudaste a ubicar") {
                    Text(s.optInt("avisos").toString(), style = Tipo.Fila, color = Medio)
                }
                Separador()
                FilaItem("Ganado por avisos", "Pagado ${usdc(s.optDouble("pagadoUsdc"))} · por pagar ${usdc(s.optDouble("pendienteUsdc"))}") {
                    Text(usdc(s.optDouble("ganadoUsdc")), style = Tipo.Fila, color = Medio)
                }
                if (s.optDouble("recompensasUsdc") > 0) {
                    Separador()
                    FilaItem("De recompensas compartidas", "Dueños que recuperaron su celular gracias a tu aviso") {
                        Text(usdc(s.optDouble("recompensasUsdc")), style = Tipo.Fila, color = Medio)
                    }
                }
            }
        }
        s?.optString("enlaceCuenta")?.takeIf { it.isNotBlank() }?.let { enlace ->
            Enlace("Ver tu cuenta en la red Stellar", color = Medio) { runCatching { uri.openUri(enlace) } }
        }
    }
}

private fun usdc(v: Double) = if (v.isNaN()) "0 USDC" else "%.2f USDC".format(v).replace('.', ',')
