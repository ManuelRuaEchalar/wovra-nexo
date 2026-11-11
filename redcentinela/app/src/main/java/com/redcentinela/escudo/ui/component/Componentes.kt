package com.redcentinela.escudo.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.redcentinela.escudo.ui.theme.Ambar
import com.redcentinela.escudo.ui.theme.AmbarOscuro
import com.redcentinela.escudo.ui.theme.AmbarSuave
import com.redcentinela.escudo.ui.theme.AmbarTexto
import com.redcentinela.escudo.ui.theme.AmbarTinta
import com.redcentinela.escudo.ui.theme.Aurora
import com.redcentinela.escudo.ui.theme.BrumaSuave
import com.redcentinela.escudo.ui.theme.Destello
import com.redcentinela.escudo.ui.theme.Fondo
import com.redcentinela.escudo.ui.theme.Linea
import com.redcentinela.escudo.ui.theme.Medio
import com.redcentinela.escudo.ui.theme.Noche
import com.redcentinela.escudo.ui.theme.Sora
import com.redcentinela.escudo.ui.theme.Superficie
import com.redcentinela.escudo.ui.theme.Texto2
import com.redcentinela.escudo.ui.theme.Tinta
import com.redcentinela.escudo.ui.theme.Tipo

private val Pastilla = RoundedCornerShape(50)

/** Toque sin la onda de Material: el diseño es plano. */
@Composable
fun Modifier.toque(enabled: Boolean = true, onClick: () -> Unit): Modifier =
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        enabled = enabled,
        onClick = onClick
    )

// --- Botones -----------------------------------------------------------------

/** Botón principal: noche, con un filo de aurora. */
@Composable
fun BotonPrincipal(texto: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(Pastilla)
            .background(Noche)
            .border(1.dp, Aurora.copy(alpha = 0.55f), Pastilla)
            .toque(enabled, onClick),
        contentAlignment = Alignment.Center
    ) { Text(texto, style = Tipo.Boton, color = Color.White, textAlign = TextAlign.Center) }
}

@Composable
fun BotonSecundario(texto: String, modifier: Modifier = Modifier, alto: Dp = 48.dp, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(alto)
            .clip(Pastilla)
            .border(1.dp, Tinta, Pastilla)
            .toque(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(texto, style = Tipo.Boton.copy(fontSize = 15.sp, fontWeight = FontWeight(500)), color = Tinta) }
}

/** Botón ámbar: solo para alertas (robo). */
@Composable
fun BotonAlerta(texto: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(Pastilla)
            .background(Ambar)
            .toque(enabled, onClick),
        contentAlignment = Alignment.Center
    ) { Text(texto, style = Tipo.Boton, color = AmbarOscuro, textAlign = TextAlign.Center) }
}

/** Botón claro para usar sobre tarjetas nocturnas. */
@Composable
fun BotonDestello(texto: String, modifier: Modifier = Modifier, alto: Dp = 48.dp, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(alto)
            .clip(Pastilla)
            .background(Destello)
            .toque(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(texto, style = Tipo.Boton.copy(fontSize = 15.sp), color = Tinta) }
}

/** Botón de borde punteado (acciones opcionales, como invitar). */
@Composable
fun BotonPunteado(texto: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(Pastilla)
            .toque(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawRoundRect(
                color = Texto2,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2),
                style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
            )
        }
        Text(texto, style = Tipo.Boton.copy(fontSize = 15.sp, fontWeight = FontWeight(500)), color = Tinta)
    }
}

@Composable
fun Enlace(texto: String, modifier: Modifier = Modifier, color: Color = Tinta, onClick: () -> Unit) {
    Box(modifier.fillMaxWidth().heightIn(min = 44.dp).toque(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(texto, style = Tipo.Boton.copy(fontSize = 15.sp, fontWeight = FontWeight(500)), color = color, textAlign = TextAlign.Center)
    }
}

/** Botón redondo de 44 px con borde fino (volver, agregar). */
@Composable
fun BotonRedondo(icono: ImageVector, descripcion: String, modifier: Modifier = Modifier, sobreNoche: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .then(
                if (sobreNoche) Modifier.background(Color.White.copy(alpha = 0.1f))
                else Modifier.border(1.dp, Linea, CircleShape)
            )
            .toque(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icono, descripcion, tint = if (sobreNoche) Color.White else Tinta, modifier = Modifier.size(20.dp)) }
}

// --- Texto ---------------------------------------------------------------------

@Composable
fun Etiqueta(texto: String, modifier: Modifier = Modifier, color: Color = Texto2) {
    Text(texto.uppercase(), style = Tipo.Etiqueta, color = color, modifier = modifier)
}

// --- Tarjetas y filas ------------------------------------------------------------

/** Tarjeta blanca con borde fino; sus filas se separan con [Separador]. */
@Composable
fun Tarjeta(
    modifier: Modifier = Modifier,
    radio: Dp = 22.dp,
    borde: Color = Linea,
    relleno: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(radio))
            .background(Superficie)
            .border(1.dp, borde, RoundedCornerShape(radio))
            .padding(horizontal = relleno, vertical = 2.dp),
        content = content
    )
}

@Composable
fun Separador() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Linea))
}

/** Fila "etiqueta ........ valor" de las tarjetas de datos. */
@Composable
fun FilaDato(etiqueta: String, valor: String, punto: Color? = null, colorValor: Color = Tinta, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.toque(onClick = onClick) else Modifier)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(etiqueta, style = Tipo.Fila, color = Texto2)
        Spacer(Modifier.weight(1f).width(12.dp))
        if (punto != null) {
            Box(Modifier.size(7.dp).background(punto, CircleShape))
            Spacer(Modifier.width(6.dp))
        }
        Text(valor, style = Tipo.Fila.copy(fontWeight = FontWeight(600)), color = colorValor, textAlign = TextAlign.End)
        if (onClick != null) {
            Spacer(Modifier.width(4.dp))
            Icon(Iconos.Chevron, null, tint = Texto2, modifier = Modifier.size(16.dp))
        }
    }
}

/** Fila con icono o avatar a la izquierda, título + nota, y algo al final. */
@Composable
fun FilaItem(
    titulo: String,
    nota: String?,
    modifier: Modifier = Modifier,
    colorNota: Color = Texto2,
    inicio: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    fin: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().then(if (onClick != null) Modifier.toque(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        inicio?.invoke()
        Column(Modifier.weight(1f)) {
            Text(titulo, style = Tipo.Fuerte, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!nota.isNullOrBlank()) Text(nota, style = Tipo.Nota, color = colorNota)
        }
        fin?.invoke(this)
    }
}

@Composable
fun IconoRedondo(icono: ImageVector, tamano: Dp = 40.dp, color: Color = Medio, fondo: Color? = null, borde: Color? = Linea) {
    Box(
        modifier = Modifier.size(tamano).clip(CircleShape)
            .then(if (fondo != null) Modifier.background(fondo) else Modifier)
            .then(if (borde != null && fondo == null) Modifier.border(1.dp, borde, CircleShape) else Modifier),
        contentAlignment = Alignment.Center
    ) { Icon(icono, null, tint = color, modifier = Modifier.size(20.dp)) }
}

/** Inicial de la persona; el anillo de aurora indica "está cerca". */
@Composable
fun Avatar(inicial: String, tamano: Dp = 44.dp, anillo: Boolean = false) {
    Box(contentAlignment = Alignment.Center) {
        if (anillo) Box(Modifier.size(tamano + 7.dp).border(1.5.dp, Aurora, CircleShape))
        Box(Modifier.size(tamano).background(BrumaSuave, CircleShape), contentAlignment = Alignment.Center) {
            Text(inicial, fontFamily = Sora, fontWeight = FontWeight(300), fontSize = (tamano.value * 0.39f).sp, color = Tinta)
        }
    }
}

@Composable
fun Chip(texto: String, modifier: Modifier = Modifier, fondo: Color, color: Color, borde: Color? = null, punto: Color? = null) {
    Row(
        modifier = modifier.height(32.dp).clip(Pastilla).background(fondo)
            .then(if (borde != null) Modifier.border(1.dp, borde, Pastilla) else Modifier)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (punto != null) Box(Modifier.size(8.dp).border(2.dp, punto, CircleShape))
        Text(texto, style = Tipo.Nota.copy(fontWeight = FontWeight(600)), color = color)
    }
}

/** Interruptor del sistema: pista noche, perilla destello. */
@Composable
fun Interruptor(activo: Boolean, onChange: (Boolean) -> Unit) {
    Box(
        modifier = Modifier.size(width = 50.dp, height = 30.dp).clip(Pastilla)
            .background(if (activo) Noche else Linea)
            .toque { onChange(!activo) }
            .padding(3.dp),
        contentAlignment = if (activo) Alignment.CenterEnd else Alignment.CenterStart
    ) { Box(Modifier.size(24.dp).background(if (activo) Destello else Color.White, CircleShape)) }
}

/** Aviso ámbar suave con una acción opcional. */
@Composable
fun AvisoAmbar(texto: String, modifier: Modifier = Modifier, accion: String? = null, onAccion: (() -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(AmbarSuave)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(texto, style = Tipo.Fila, color = AmbarTinta, modifier = Modifier.weight(1f))
        if (accion != null && onAccion != null) {
            Text(
                accion, style = Tipo.Fila.copy(fontWeight = FontWeight(600)), color = AmbarTexto,
                modifier = Modifier.toque(onClick = onAccion).padding(vertical = 6.dp)
            )
        }
    }
}

@Composable
fun Vineta(texto: String, color: Color = Aurora) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.padding(top = 7.dp).size(6.dp).background(color, CircleShape))
        Text(texto, style = Tipo.Fila, modifier = Modifier.weight(1f))
    }
}

/** Campo de texto del sistema: 50 px, radio 16, borde fino. */
@Composable
fun Campo(
    etiqueta: String,
    valor: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    ayuda: String = "",
    teclado: KeyboardType = KeyboardType.Text
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(etiqueta, style = Tipo.Nota.copy(fontWeight = FontWeight(600)))
        BasicTextField(
            value = valor,
            onValueChange = onChange,
            singleLine = true,
            textStyle = Tipo.Cuerpo.copy(color = Tinta),
            cursorBrush = SolidColor(Medio),
            keyboardOptions = KeyboardOptions(keyboardType = teclado),
            decorationBox = { inner ->
                Box(
                    Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(16.dp)).background(Superficie)
                        .border(1.dp, Linea, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (valor.isEmpty()) Text(ayuda, style = Tipo.Cuerpo.copy(color = Texto2.copy(alpha = 0.6f)))
                    inner()
                }
            }
        )
    }
}

// --- Navegación -------------------------------------------------------------------

/** Cabecera de los pasos del inicio: volver, barra de progreso y "n de 4". */
@Composable
fun CabeceraPaso(paso: Int, total: Int, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        BotonRedondo(Iconos.Atras, "Volver", onClick = onBack)
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(total) { i ->
                Box(Modifier.weight(1f).height(3.dp).background(if (i < paso) Noche else Linea, RoundedCornerShape(2.dp)))
            }
        }
        Text("$paso de $total", style = Tipo.Nota)
    }
}

@Composable
fun CabeceraSimple(titulo: String, subtitulo: String? = null, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        BotonRedondo(Iconos.Atras, "Volver", onClick = onBack)
        Column {
            Text(titulo, style = Tipo.Fuerte)
            if (subtitulo != null) Text(subtitulo, style = Tipo.Nota)
        }
    }
}

enum class Pestana(val titulo: String, val icono: ImageVector) {
    ESCUDO("Escudo", Iconos.Escudo),
    CIUDAD("Ciudad", Iconos.Ciudad),
    CIRCULO("Círculo", Iconos.Circulo),
    AJUSTES("Ajustes", Iconos.Ajustes)
}

@Composable
fun BarraInferior(actual: Pestana, modifier: Modifier = Modifier, onSelect: (Pestana) -> Unit) {
    Column(modifier.fillMaxWidth().background(Fondo)) {
        Separador()
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 10.dp)) {
            Pestana.entries.forEach { p ->
                val activo = p == actual
                Column(
                    modifier = Modifier.weight(1f).toque { onSelect(p) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(p.icono, null, tint = if (activo) Tinta else Texto2, modifier = Modifier.size(22.dp))
                    Text(
                        p.titulo, fontSize = 11.sp, fontFamily = Tipo.Nota.fontFamily,
                        fontWeight = FontWeight(if (activo) 600 else 400), color = if (activo) Tinta else Texto2
                    )
                    Box(Modifier.size(4.dp).background(if (activo) Aurora else Color.Transparent, CircleShape))
                }
            }
        }
    }
}

@Composable
fun BoxScope.Centrado(content: @Composable () -> Unit) {
    Box(Modifier.align(Alignment.Center)) { content() }
}

/** Punto para la leyenda del mapa: lleno, hueco o punteado según el canal. */
@Composable
fun PuntoLeyenda(color: Color, lleno: Boolean, punteado: Boolean = false) {
    Canvas(Modifier.size(8.dp)) {
        val r = size.minDimension / 2
        if (lleno) drawCircle(color, r)
        else drawCircle(
            color, r - 1f, center = Offset(r, r),
            style = Stroke(1.5.dp.toPx(), pathEffect = if (punteado) PathEffect.dashPathEffect(floatArrayOf(3f, 3f)) else null)
        )
    }
}
