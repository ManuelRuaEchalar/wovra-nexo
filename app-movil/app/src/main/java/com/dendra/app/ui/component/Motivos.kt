package com.dendra.app.ui.component

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dendra.app.ui.theme.Aurora
import com.dendra.app.ui.theme.Destello
import com.dendra.app.ui.theme.Fondo
import com.dendra.app.ui.theme.Medio
import com.dendra.app.ui.theme.Noche

/** Trazo con resplandor (el "blur" de las líneas de diseño). */
private fun DrawScope.resplandor(color: Color, ancho: Float, desenfoque: Float, dibujar: (android.graphics.Canvas, android.graphics.Paint) -> Unit) {
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            style = PaintingStyle.Stroke
            strokeWidth = ancho
        }.asFrameworkPaint().apply {
            this.color = color.toArgb()
            maskFilter = BlurMaskFilter(desenfoque, BlurMaskFilter.Blur.NORMAL)
        }
        dibujar(canvas.nativeCanvas, paint)
    }
}

/**
 * Motivo "anillo": alcance del guardián y estado del Escudo. Siempre
 * inclinado -14°, trazo de 1 a 2 px. El punto sobre el anillo es el guardián.
 *
 * @param punteado anillos exteriores punteados (buscando / Escudo apagado).
 * @param guardian dibuja el punto del guardián dentro del alcance.
 */
@Composable
fun Halo(
    modifier: Modifier = Modifier,
    color: Color = Aurora,
    centro: Color = Noche,
    fondo: Color = Fondo,
    punteado: Boolean = false,
    guardian: Boolean = false,
    pulso: Float = 0f
) {
    Canvas(modifier) {
        // Proporciones del diseño: lienzo 342 x 196.
        val u = minOf(size.width / 342f, size.height / 196f)
        val c = Offset(size.width / 2, size.height / 2)
        fun ovalo(rx: Float, ry: Float, ancho: Float, alpha: Float, dash: Boolean = false) {
            drawOval(
                color = color.copy(alpha = alpha),
                topLeft = Offset(c.x - rx * u, c.y - ry * u),
                size = Size(2 * rx * u, 2 * ry * u),
                style = Stroke(ancho * u, pathEffect = if (dash) PathEffect.dashPathEffect(floatArrayOf(3 * u, 5 * u)) else null)
            )
        }
        rotate(-14f, c) {
            ovalo(160f, 64f, 1f, 0.16f + 0.2f * pulso, punteado)
            ovalo(116f, 47f, 1f, 0.38f, punteado)
            resplandor(color.copy(alpha = 0.45f), 7f * u, 5f * u) { canvas, paint ->
                canvas.drawOval(c.x - 72 * u, c.y - 29 * u, c.x + 72 * u, c.y + 29 * u, paint)
            }
            ovalo(72f, 29f, 2f, 1f)
            if (guardian) {
                val p = Offset(c.x + 100 * u, c.y + 23 * u)
                drawCircle(fondo, 6.5f * u, p)
                drawCircle(Medio, 6.5f * u, p, style = Stroke(2f * u))
            }
        }
        drawCircle(centro, 8f * u, c)
    }
}

/** Arco de aurora de las tarjetas nocturnas. */
@Composable
private fun AuroraArco(modifier: Modifier) {
    Canvas(modifier) {
        val sx = size.width / 404f
        val sy = minOf(size.height / 200f, sx * 1.4f)
        val path = Path().apply {
            moveTo(-20f * sx, 130f * sy)
            cubicTo(70f * sx, 20f * sy, 260f * sx, 0f, 430f * sx, 90f * sy)
        }
        resplandor(Aurora.copy(alpha = 0.32f), 24f * sx, 14f * sx) { canvas, paint ->
            canvas.drawPath(path.asAndroidPath(), paint)
        }
        drawPath(path, Destello, style = Stroke(1.5.dp.toPx()))
        drawCircle(Color.White.copy(alpha = 0.7f), 1.3.dp.toPx(), Offset(300f * sx, 40f * sy))
        drawCircle(Color.White.copy(alpha = 0.5f), 1.dp.toPx(), Offset(60f * sx, 40f * sy))
        drawCircle(Color.White.copy(alpha = 0.5f), 1.2.dp.toPx(), Offset(370f * sx, 150f * sy))
    }
}

/**
 * Tarjeta nocturna: solo en momentos clave (bienvenida, robo en curso,
 * celular perdido).
 */
@Composable
fun TarjetaNoche(modifier: Modifier = Modifier, radio: Dp = 28.dp, aurora: Boolean = true, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(radio)).background(Noche)) {
        if (aurora) AuroraArco(Modifier.matchParentSize())
        content()
    }
}
