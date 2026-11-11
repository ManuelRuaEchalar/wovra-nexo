package com.redcentinela.escudo.ui.screen

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.redcentinela.escudo.data.circle.CircleStore
import com.redcentinela.escudo.domain.model.CircleMember
import com.redcentinela.escudo.domain.model.Presence
import com.redcentinela.escudo.domain.model.RemoteState
import com.redcentinela.escudo.domain.model.ShieldStatus
import com.redcentinela.escudo.ui.EscudoViewModel
import com.redcentinela.escudo.ui.component.Avatar
import com.redcentinela.escudo.ui.component.BotonDestello
import com.redcentinela.escudo.ui.component.BotonPrincipal
import com.redcentinela.escudo.ui.component.BotonPunteado
import com.redcentinela.escudo.ui.component.BotonRedondo
import com.redcentinela.escudo.ui.component.BotonSecundario
import com.redcentinela.escudo.ui.component.CabeceraSimple
import com.redcentinela.escudo.ui.component.Campo
import com.redcentinela.escudo.ui.component.Enlace
import com.redcentinela.escudo.ui.component.Etiqueta
import com.redcentinela.escudo.ui.component.FilaItem
import com.redcentinela.escudo.ui.component.Iconos
import com.redcentinela.escudo.ui.component.Interruptor
import com.redcentinela.escudo.ui.component.Separador
import com.redcentinela.escudo.ui.component.Tarjeta
import com.redcentinela.escudo.ui.component.TarjetaNoche
import com.redcentinela.escudo.ui.component.toque
import com.redcentinela.escudo.ui.theme.AmbarNoche
import com.redcentinela.escudo.ui.theme.AmbarTexto
import com.redcentinela.escudo.ui.theme.Medio
import com.redcentinela.escudo.ui.theme.Noche
import com.redcentinela.escudo.ui.theme.Sora
import com.redcentinela.escudo.ui.theme.Texto2
import com.redcentinela.escudo.ui.theme.TextoNoche
import com.redcentinela.escudo.ui.theme.Tinta
import com.redcentinela.escudo.ui.theme.Tipo
import com.redcentinela.escudo.util.Format

/** Resumen de una persona del círculo: qué se sabe de su celular ahora. */
fun estadoDe(member: CircleMember, remote: RemoteState?, presence: Presence?): Pair<String, Color> {
    val rol = when {
        member.isGuardian -> " · tu guardián"
        member.iWatch -> " · lo cuidas"
        else -> ""
    }
    return when {
        remote?.status == ShieldStatus.ALERTA_ROBO -> "Reportado como robado" to AmbarTexto
        remote?.status == ShieldStatus.ALERTA_PERDIDA -> "Reportado como perdido" to AmbarTexto
        presence?.near == true -> "Cerca · a ${Format.meters(presence.meters ?: 0.0)}$rol" to Medio
        remote?.status == ShieldStatus.ARMADO -> "Escudo activo$rol" to Medio
        remote?.status == ShieldStatus.INACTIVO -> "Escudo apagado$rol" to Texto2
        else -> "Sin datos todavía$rol" to Texto2
    }
}

// --- D1 · Círculo -----------------------------------------------------------------

@Composable
fun CirculoScreen(
    vm: EscudoViewModel,
    onAdd: () -> Unit,
    onMember: (CircleMember) -> Unit,
    onReport: (CircleMember) -> Unit,
    onTrack: (CircleMember) -> Unit
) {
    val members by vm.members.collectAsStateWithLifecycle()
    val remote by vm.remote.collectAsStateWithLifecycle()
    val presence by vm.presence.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
            .padding(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tu círculo", style = Tipo.Titulo, modifier = Modifier.weight(1f))
            BotonRedondo(Iconos.Mas, "Agregar a alguien", Modifier.background(Color.White, CircleShape), onClick = onAdd)
        }

        // Tarjeta nocturna solo en el momento clave: hay un robo o pérdida en curso.
        members.filter { remote[it.id]?.inAlert == true }.forEach { member ->
            val state = remote.getValue(member.id)
            TarjetaNoche {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column {
                        Text(
                            if (state.status == ShieldStatus.ALERTA_ROBO) "Robo en curso" else "Celular perdido",
                            style = Tipo.Nota.copy(fontWeight = FontWeight(500)), color = AmbarNoche
                        )
                        Spacer(Modifier.height(60.dp))
                        Text("Celular de ${member.name}", fontFamily = Sora, fontWeight = FontWeight(300), fontSize = 24.sp, color = Color.White)
                        Text(
                            state.last?.let { "Última ubicación ${Format.ago(it.point.capturedAt)} · por ${it.via.label}" }
                                ?: "Esperando su primera ubicación…",
                            style = Tipo.Fila, color = TextoNoche
                        )
                    }
                    BotonDestello("Ver seguimiento") { onTrack(member) }
                }
            }
        }

        if (members.isEmpty()) {
            Tarjeta(radio = 24.dp) {
                Column(Modifier.padding(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Todavía no hay nadie", style = Tipo.Subtitulo)
                    Text(
                        "Agrega de 2 a 5 personas de confianza. Podrán reportar tu celular desde el suyo, " +
                            "ver dónde está si te lo roban y ser tu guardián cuando van contigo.",
                        style = Tipo.Cuerpo
                    )
                }
            }
            BotonPrincipal("Agregar a alguien", onClick = onAdd)
        } else {
            Tarjeta(radio = 24.dp, relleno = 16.dp) {
                members.forEachIndexed { i, member ->
                    val (texto, color) = estadoDe(member, remote[member.id], presence[member.id])
                    FilaItem(
                        member.name, texto, colorNota = color,
                        inicio = { Avatar(member.initial, anillo = presence[member.id]?.near == true) },
                        onClick = { onMember(member) }
                    ) {
                        if (remote[member.id]?.inAlert == true) {
                            Text("Ver", style = Tipo.Nota.copy(fontWeight = FontWeight(600)), color = AmbarTexto,
                                modifier = Modifier.toque { onTrack(member) }.padding(8.dp))
                        } else {
                            Text("Reportar", style = Tipo.Nota.copy(fontWeight = FontWeight(600)), color = AmbarTexto,
                                modifier = Modifier.toque { onReport(member) }.padding(8.dp))
                        }
                    }
                    if (i < members.lastIndex) Separador()
                }
            }
            Text(
                "${members.size} de ${CircleStore.MAX} personas. El anillo verde indica que su celular está cerca del tuyo (Bluetooth).",
                style = Tipo.Nota
            )
        }
    }
}

// --- A5 · Arma tu círculo / agregar ---------------------------------------------------

/** Contenido para vincular personas: mi QR, escanear, invitar y pegar. */
@Composable
fun CirculoArmar(vm: EscudoViewModel, modifier: Modifier = Modifier, onAdded: (CircleMember) -> Unit = {}) {
    val context = LocalContext.current
    val members by vm.members.collectAsStateWithLifecycle()
    val escaner = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { text -> vm.addFromText(text)?.let(onAdded) }
    }
    val card = remember(vm.config.userName, vm.config.phone) { vm.config.myCard() }
    val qr = remember(card) { qrBitmap(card) }

    Column(modifier.verticalScroll(rememberScrollState()).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Arma tu círculo", style = Tipo.Titulo)
            Text(
                "De 2 a 5 personas. Pueden reportar tu celular desde el suyo y ver dónde está si te lo roban.",
                style = Tipo.Cuerpo
            )
        }

        Tarjeta(radio = 24.dp) {
            Column(
                Modifier.fillMaxWidth().padding(vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Etiqueta("Tu código")
                if (qr != null) Image(qr.asImageBitmap(), "Tu código QR", Modifier.size(200.dp))
                Text(
                    "Que la otra persona lo escanee desde su app:\nCírculo → + → Escanear su código.",
                    style = Tipo.Nota, textAlign = TextAlign.Center
                )
            }
        }

        BotonPrincipal("Escanear su código") {
            escaner.launch(
                ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE).setBeepEnabled(false)
                    .setOrientationLocked(true).setPrompt("Apunta al código QR de tu contacto")
            )
        }
        BotonPunteado("Invitar por enlace") {
            val texto = "Te invito a mi círculo de Red Centinela. Copia este mensaje completo y, en la app, " +
                "entra a Círculo → + → Pegar invitación.\n\n$card"
            context.startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto), "Invitar"
                )
            )
        }
        Enlace("Pegar invitación") {
            val clip = (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                .primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
            if (clip.isNullOrBlank()) vm.say("Copia primero el mensaje de invitación")
            else vm.addFromText(clip)?.let(onAdded)
        }

        Text(
            "Con internet basta que uno escanee: el otro queda vinculado solo. Sin internet, escanéense mutuamente.",
            style = Tipo.Nota
        )

        if (members.isNotEmpty()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.Bottom) {
                Etiqueta("Tus contactos", Modifier.weight(1f))
                Text("${members.size}", fontFamily = Sora, fontWeight = FontWeight(300), fontSize = 18.sp, color = Tinta)
                Text(" / ${CircleStore.MAX}", fontFamily = Sora, fontWeight = FontWeight(300), fontSize = 18.sp, color = Texto2)
            }
            Tarjeta(radio = 24.dp, relleno = 16.dp) {
                members.forEachIndexed { i, member ->
                    FilaItem(
                        member.name, if (member.phone != null) "Tiene la app · ${member.phone}" else "Tiene la app · sin número para SMS",
                        inicio = { Avatar(member.initial) }
                    ) {
                        Box(Modifier.size(26.dp).background(Noche, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Iconos.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                    if (i < members.lastIndex) Separador()
                }
            }
        }
    }
}

@Composable
fun AgregarScreen(vm: EscudoViewModel, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 12.dp)) {
        CabeceraSimple("Agregar al círculo", onBack = onBack)
        Spacer(Modifier.height(20.dp))
        CirculoArmar(vm, onAdded = { onBack() })
    }
}

// --- Ficha de una persona -----------------------------------------------------------

@Composable
fun MiembroScreen(
    vm: EscudoViewModel,
    memberId: String,
    onBack: () -> Unit,
    onReport: (CircleMember) -> Unit,
    onTrack: (CircleMember) -> Unit
) {
    val members by vm.members.collectAsStateWithLifecycle()
    val remote by vm.remote.collectAsStateWithLifecycle()
    val presence by vm.presence.collectAsStateWithLifecycle()
    val member = members.firstOrNull { it.id == memberId }
    if (member == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    var numero by rememberSaveable(member.id) { mutableStateOf(member.phone ?: "") }
    val (texto, color) = estadoDe(member, remote[member.id], presence[member.id])

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
            .padding(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CabeceraSimple("Tu círculo", onBack = onBack)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Avatar(member.initial, tamano = 64.dp, anillo = presence[member.id]?.near == true)
            Column {
                Text(member.name, style = Tipo.Titulo)
                Text(texto, style = Tipo.Fila, color = color)
            }
        }

        Column {
            Etiqueta("Guardián", Modifier.padding(start = 4.dp, bottom = 8.dp))
            Tarjeta(relleno = 16.dp) {
                FilaItem("Es mi guardián", "Va conmigo: su celular avisa si el mío se aleja") {
                    Interruptor(member.isGuardian) { vm.setGuardian(member, it) }
                }
                Separador()
                FilaItem("Soy su guardián", "Mi celular pita si el suyo se aleja de mí") {
                    Interruptor(member.iWatch) { vm.setWatch(member, it) }
                }
            }
            Text(
                "Funciona por Bluetooth, sin internet, cuando los dos tienen el Escudo activo.",
                style = Tipo.Nota, modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
        }

        Campo("Su número (para SMS sin internet)", numero, { numero = it.take(16) }, ayuda = "+591 7XXXXXXX", teclado = KeyboardType.Phone)
        if (numero.trim() != (member.phone ?: "")) {
            BotonSecundario("Guardar número") {
                vm.setPhone(member.id, numero)
                vm.say("Número guardado")
            }
        }

        BotonPrincipal("Ver su celular en el mapa") { onTrack(member) }
        BotonSecundario("Reportar su celular") { onReport(member) }
        Enlace("Quitar del círculo", color = AmbarTexto) {
            vm.removeMember(member.id)
            onBack()
        }
    }
}

/** Genera el QR con los colores del sistema (tinta sobre blanco). */
private fun qrBitmap(text: String, size: Int = 600): Bitmap? = runCatching {
    val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, mapOf(EncodeHintType.MARGIN to 1))
    val dark = Tinta.toArgb()
    val light = Color.White.toArgb()
    val pixels = IntArray(size * size) { i -> if (matrix[i % size, i / size]) dark else light }
    Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
}.getOrNull()
