package com.redcentinela.escudo.ui.screen

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.redcentinela.escudo.ui.EscudoViewModel
import com.redcentinela.escudo.ui.Permisos
import com.redcentinela.escudo.ui.component.AvisoAmbar
import com.redcentinela.escudo.ui.component.BotonPrincipal
import com.redcentinela.escudo.ui.component.CabeceraPaso
import com.redcentinela.escudo.ui.component.Campo
import com.redcentinela.escudo.ui.component.Chip
import com.redcentinela.escudo.ui.component.FilaItem
import com.redcentinela.escudo.ui.component.Halo
import com.redcentinela.escudo.ui.component.Iconos
import com.redcentinela.escudo.ui.component.IconoRedondo
import com.redcentinela.escudo.ui.component.Separador
import com.redcentinela.escudo.ui.component.Tarjeta
import com.redcentinela.escudo.ui.component.TarjetaNoche
import com.redcentinela.escudo.ui.component.toque
import com.redcentinela.escudo.ui.theme.Aurora
import com.redcentinela.escudo.ui.theme.Bruma
import com.redcentinela.escudo.ui.theme.Destello
import com.redcentinela.escudo.ui.theme.Medio
import com.redcentinela.escudo.ui.theme.Noche
import com.redcentinela.escudo.ui.theme.Sora
import com.redcentinela.escudo.ui.theme.Texto2
import com.redcentinela.escudo.ui.theme.Tinta
import com.redcentinela.escudo.ui.theme.Tipo

/**
 * Primer uso (pantallas A1 a A5 del diseño): bienvenida, perfil, bloqueo de
 * pantalla, permisos, guardián y círculo. Termina activando el Escudo.
 */
@Composable
fun Inicio(vm: EscudoViewModel, refresco: Int, onDone: () -> Unit) {
    var paso by rememberSaveable { mutableIntStateOf(0) }
    BackHandler(enabled = paso > 0) { paso-- }
    when (paso) {
        0 -> Bienvenida { paso = 1 }
        1 -> Perfil(vm, onBack = { paso = 0 }) { paso = 2 }
        2 -> Cuenta(vm, onBack = { paso = 1 }) { paso = 3 }
        3 -> Bloqueo(refresco, onBack = { paso = 2 }) { paso = 4 }
        4 -> PermisosPaso(refresco, onBack = { paso = 3 }) { paso = 5 }
        5 -> Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 18.dp)) {
            CabeceraPaso(3, 4) { paso = 4 }
            Spacer(Modifier.height(20.dp))
            GuardianContenido(vm, Modifier.weight(1f))
            BotonPrincipal("Continuar") { paso = 6 }
        }
        else -> Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 18.dp)) {
            CabeceraPaso(4, 4) { paso = 5 }
            Spacer(Modifier.height(20.dp))
            CirculoArmar(vm, Modifier.weight(1f))
            BotonPrincipal("Activar Escudo", enabled = Permisos.ubicacion(LocalContext.current)) {
                vm.config.onboarded = true
                vm.startShield()
                onDone()
            }
            if (!Permisos.ubicacion(LocalContext.current)) {
                Text("Falta el permiso de ubicación (paso 2).", style = Tipo.Nota, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

// --- A1 · Bienvenida -----------------------------------------------------------

@Composable
private fun Bienvenida(onStart: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 18.dp)) {
        TarjetaNoche(Modifier.weight(1f), radio = 36.dp) {
            Halo(
                Modifier.fillMaxWidth().height(260.dp).align(Alignment.Center).padding(top = 60.dp),
                color = Aurora, centro = Destello, fondo = Noche, guardian = true
            )
            Row(
                Modifier.padding(22.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Halo(Modifier.size(width = 34.dp, height = 24.dp), color = Destello, centro = Destello)
                Text("Dendra", fontFamily = Sora, fontSize = 16.sp, color = Color.White)
            }
        }
        Spacer(Modifier.height(28.dp))
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Una red que se cuida a sí misma", style = Tipo.Display)
            Text("Tu celular protege y es protegido. Funciona con o sin internet.", style = Tipo.Cuerpo.copy(fontSize = 16.sp))
        }
        Spacer(Modifier.height(28.dp))
        BotonPrincipal("Empezar", Modifier.padding(horizontal = 16.dp), onClick = onStart)
    }
}

// --- Perfil ---------------------------------------------------------------------

@Composable
private fun Perfil(vm: EscudoViewModel, onBack: () -> Unit, onNext: () -> Unit) {
    var nombre by rememberSaveable { mutableStateOf(vm.config.userName) }
    var numero by rememberSaveable { mutableStateOf(vm.config.phone ?: "") }
    Paso(
        cabecera = { com.redcentinela.escudo.ui.component.CabeceraSimple("Tu perfil", onBack = onBack) },
        titulo = "¿Cómo te llamas?",
        texto = "Así te verá tu círculo. Tu número permite que reporten tu celular por SMS cuando no hay internet.",
        boton = "Continuar",
        habilitado = nombre.isNotBlank(),
        onBoton = {
            vm.config.userName = nombre
            vm.config.phone = numero
            onNext()
        }
    ) {
        Campo("Tu nombre", nombre, { nombre = it.take(30) }, ayuda = "Juan")
        Campo(
            "Número de este celular (opcional)", numero, { numero = it.take(16) },
            ayuda = "+591 7XXXXXXX", teclado = KeyboardType.Phone
        )
    }
}

// --- Cuenta (panel web) ----------------------------------------------------------

/**
 * La cuenta se crea al configurar la app: con ella el dueño entra al panel
 * web desde cualquier computadora si pierde el celular. La contraseña no
 * sale del teléfono (ver data/rescue/Rescue.kt).
 */
@Composable
private fun Cuenta(vm: EscudoViewModel, onBack: () -> Unit, onNext: () -> Unit) {
    val creada by vm.rescueEmail.collectAsStateWithLifecycle()
    val trabajando by vm.rescueWorking.collectAsStateWithLifecycle()
    var correo by rememberSaveable { mutableStateOf(creada ?: "") }
    var clave by remember { mutableStateOf("") }
    var clave2 by remember { mutableStateOf("") }
    var servidor by rememberSaveable { mutableStateOf(vm.config.rewardServerUrl) }
    val sinServidor = vm.config.rewardServerUrl.isBlank()
    val valida = creada != null || (correo.contains('@') && clave.length >= 8 && clave == clave2)

    Paso(
        cabecera = { com.redcentinela.escudo.ui.component.CabeceraSimple("Tu cuenta", onBack = onBack) },
        titulo = if (creada != null) "Tu cuenta está lista" else "Crea tu cuenta",
        texto = if (creada != null) {
            "Si pierdes el celular, entra al panel web con $creada desde cualquier computadora: ves dónde está, " +
                "lo reportas o lo haces sonar, aunque no tengas a nadie en tu círculo."
        } else {
            "Con tu correo y una contraseña podrás entrar al panel web desde cualquier computadora si pierdes " +
                "el celular: ver dónde está, reportarlo o hacerlo sonar, aunque no tengas a nadie en tu círculo."
        },
        boton = when {
            creada != null -> "Continuar"
            trabajando -> "Creando tu cuenta…"
            else -> "Crear cuenta"
        },
        habilitado = valida && !trabajando,
        onBoton = {
            when {
                creada != null -> onNext()
                else -> {
                    if (sinServidor && servidor.isNotBlank()) vm.config.rewardServerUrl = servidor
                    vm.setUpRescue(correo, clave) { clave = ""; clave2 = ""; onNext() }
                }
            }
        }
    ) {
        if (creada == null) {
            Campo("Correo", correo, { correo = it.trim() }, ayuda = "tu@correo.com", teclado = KeyboardType.Email)
            Campo("Contraseña (mínimo 8 caracteres)", clave, { clave = it }, oculto = true)
            Campo("Repite la contraseña", clave2, { clave2 = it }, oculto = true)
            if (clave2.isNotEmpty() && clave != clave2) {
                Text("Las contraseñas no coinciden.", style = Tipo.Nota, color = com.redcentinela.escudo.ui.theme.AmbarTexto)
            }
            if (sinServidor) {
                // Solo mientras el servidor no tenga dirección fija (versión de desarrollo).
                Campo("Servidor (versión de prueba)", servidor, { servidor = it.trim() }, ayuda = "https://…", teclado = KeyboardType.Uri)
            }
            Tarjeta(relleno = 16.dp) {
                Column(Modifier.padding(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Tu contraseña no sale de este celular.", style = Tipo.Nota)
                    Text("Nuestro servidor guarda tu cuenta cifrada y no puede leerla.", style = Tipo.Nota)
                    Text("Anótala: si la olvidas, se crea una nueva desde Ajustes.", style = Tipo.Nota)
                }
            }
            com.redcentinela.escudo.ui.component.Enlace("Sin internet ahora: crearla después en Ajustes", color = Texto2) { onNext() }
        }
    }
}

// --- A2 · Bloqueo de pantalla ---------------------------------------------------

@Composable
private fun Bloqueo(refresco: Int, onBack: () -> Unit, onNext: () -> Unit) {
    val context = LocalContext.current
    val seguro = refresco.let { Permisos.bloqueoSeguro(context) }
    Paso(
        cabecera = { CabeceraPaso(1, 4, onBack) },
        ilustracion = {
            Box(Modifier.fillMaxWidth().height(190.dp), contentAlignment = Alignment.Center) {
                Halo(Modifier.fillMaxSize(), centro = Color.Transparent)
                Box(Modifier.size(60.dp).background(Noche, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Iconos.Candado, null, tint = Destello, modifier = Modifier.size(26.dp))
                }
            }
        },
        titulo = "Primero, bloquea tu celular",
        texto = "Con PIN, patrón o huella, quien lo tome no entra a tus apps. El Escudo se encarga de ubicarlo y registrar quién lo tiene.",
        boton = "Continuar",
        habilitado = seguro,
        onBoton = onNext
    ) {
        Tarjeta {
            FilaItem(
                "Bloqueo de pantalla", null,
                inicio = {
                    if (seguro) IconoRedondo(Iconos.Check, tamano = 28.dp, fondo = Bruma)
                    else IconoRedondo(Iconos.Candado, tamano = 28.dp, color = Texto2)
                },
                onClick = if (seguro) null else ({ Permisos.abrirSeguridad(context) })
            ) {
                Text(
                    if (seguro) "Activado" else "Configurar",
                    style = Tipo.Fila.copy(fontWeight = FontWeight(600)),
                    color = if (seguro) Medio else com.redcentinela.escudo.ui.theme.AmbarTexto
                )
            }
        }
        if (!seguro) {
            AvisoAmbar("El Escudo solo se activa si el celular tiene PIN, patrón o huella.", accion = "Abrir ajustes") {
                Permisos.abrirSeguridad(context)
            }
        } else {
            AvisoAmbar("Recomendado: oculta los ajustes rápidos en la pantalla de bloqueo, así nadie activa el modo avión sin desbloquear.")
        }
    }
}

// --- A3 · Permisos --------------------------------------------------------------

private class Permiso(
    val titulo: String, val motivo: String, val icono: ImageVector,
    val listo: Boolean, val pedir: () -> Unit
)

@Composable
private fun PermisosPaso(refresco: Int, onBack: () -> Unit, onNext: () -> Unit) {
    val context = LocalContext.current
    var vuelta by rememberSaveable { mutableIntStateOf(0) }
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { vuelta++ }
    // El estado se vuelve a leer al volver de un diálogo o de los ajustes.
    val permisos = (refresco + vuelta).let {
        listOfNotNull(
            Permiso(
                "Ubicación todo el tiempo", "Registra la ruta aunque la app esté cerrada.",
                Iconos.Ubicacion, Permisos.ubicacionSiempre(context)
            ) {
                when {
                    !Permisos.ubicacion(context) -> pedir.launch(Permisos.UBICACION)
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
                        pedir.launch(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
                }
            },
            Permiso(
                "Bluetooth", "Enlaza tu guardián y pasa avisos entre teléfonos.",
                Iconos.Bluetooth, Permisos.tiene(context, Permisos.BLUETOOTH)
            ) { pedir.launch(Permisos.BLUETOOTH) },
            Permiso(
                "Enviar y recibir SMS", "Avisa dónde está tu celular cuando no hay datos.",
                Iconos.Sms, Permisos.tiene(context, Permisos.SMS)
            ) { pedir.launch(Permisos.SMS) },
            if (Permisos.NOTIFICACIONES.isEmpty()) null else Permiso(
                "Notificaciones", "Te avisa si reportan un celular de tu círculo.",
                Iconos.Campana, Permisos.tiene(context, Permisos.NOTIFICACIONES)
            ) { pedir.launch(Permisos.NOTIFICACIONES) },
            Permiso(
                "Sin ahorro de batería", "Algunas marcas cierran apps en segundo plano.",
                Iconos.Bateria, Permisos.sinAhorroBateria(context)
            ) { Permisos.pedirSinAhorro(context) },
            Permiso(
                "Apagado falso", "Accesibilidad: detecta el menú de apagado con el celular bloqueado.",
                Iconos.Apagado, Permisos.apagadoFalso(context)
            ) { Permisos.abrirAccesibilidad(context) }
        )
    }
    Paso(
        cabecera = { CabeceraPaso(2, 4, onBack) },
        titulo = "Lo que necesita el Escudo",
        texto = "Cada permiso tiene un motivo. Nada se comparte fuera de tu círculo.",
        boton = "Continuar",
        habilitado = Permisos.ubicacion(context),
        onBoton = onNext
    ) {
        Tarjeta(radio = 24.dp, relleno = 16.dp) {
            permisos.forEachIndexed { i, p ->
                FilaItem(p.titulo, p.motivo, inicio = { IconoRedondo(p.icono) }) {
                    if (p.listo) Chip("Listo", fondo = Bruma, color = Medio)
                    else Text(
                        "Permitir", style = Tipo.Fila.copy(fontWeight = FontWeight(500)),
                        modifier = Modifier.clip(RoundedCornerShape(50)).border(1.dp, Tinta, RoundedCornerShape(50))
                            .toque(onClick = p.pedir).padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
                if (i < permisos.lastIndex) Separador()
            }
        }
        if (!Permisos.ubicacionSiempre(context) && Permisos.ubicacion(context)) {
            Text(
                "Para \"todo el tiempo\": toca Permitir y elige \"Permitir todo el tiempo\" en la pantalla de ubicación.",
                style = Tipo.Nota
            )
        }
    }
}

// --- Andamiaje de un paso -------------------------------------------------------

@Composable
private fun Paso(
    cabecera: @Composable () -> Unit,
    titulo: String,
    texto: String,
    boton: String,
    habilitado: Boolean = true,
    onBoton: () -> Unit,
    ilustracion: (@Composable () -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 18.dp)) {
        cabecera()
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 22.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ilustracion?.invoke()
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(titulo, style = Tipo.Titulo)
                Text(texto, style = Tipo.Cuerpo)
            }
            contenido()
        }
        BotonPrincipal(boton, enabled = habilitado, onClick = onBoton)
    }
}
