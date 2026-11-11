package com.redcentinela.escudo.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * Iconos de trazo fino del sistema Halo (24 px, 1,5 px de trazo, puntas
 * redondeadas), tomados de las líneas de diseño.
 */
object Iconos {
    private fun trazo(vararg paths: String, ancho: Float = 1.5f): ImageVector =
        ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .apply {
                paths.forEach { path ->
                    addPath(
                        pathData = addPathNodes(path),
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = ancho,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round
                    )
                }
            }.build()

    private fun circulo(cx: Float, cy: Float, r: Float) =
        "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

    val Escudo = trazo("M12 3l7 3v5c0 4.5-3 8.2-7 10-4-1.8-7-5.5-7-10V6l7-3z")
    val Reportar = trazo("M12 3l7 3v5c0 4.5-3 8.2-7 10-4-1.8-7-5.5-7-10V6l7-3z", "M12 8v5M12 16v.5", ancho = 1.7f)
    val Ciudad = trazo("M9 4L3 6v14l6-2 6 2 6-2V4l-6 2-6-2zM9 4v14M15 6v14")
    val Circulo = trazo(circulo(9f, 8f, 3f), "M3 19c0-3 2.7-5 6-5s6 2 6 5M16 5a3 3 0 0 1 0 6M21 19c0-2.4-1.6-4.2-4-4.8")
    val Ajustes = trazo("M4 7h10M18 7h2M4 17h4M12 17h8", circulo(16f, 7f, 2f), circulo(10f, 17f, 2f))
    val Atras = trazo("M15 6l-6 6 6 6", ancho = 1.6f)
    val Chevron = trazo("M9 6l6 6-6 6", ancho = 1.6f)
    val Mas = trazo("M12 5v14M5 12h14", ancho = 1.6f)
    val Check = trazo("M5 12.5l4.5 4.5L19 7.5", ancho = 1.8f)
    val Buscar = trazo(circulo(11f, 11f, 6f), "M20 20l-4.5-4.5", ancho = 1.6f)
    val Fila = trazo("M5 20V5a1 1 0 0 1 1-1h7a1 1 0 0 1 1 1v15M4 20h11M14 9h2a2 2 0 0 1 2 2v5a1.5 1.5 0 0 0 3 0V8l-3-3M7 8h5")
    val Micros = trazo("M7 3h10a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z", "M5 11h14M8 18v2M16 18v2")
    val Senal = trazo("M4 20v-3M9 20v-7M14 20V9M19 20V4")
    val Llegue = trazo("M4 11l8-6 8 6v8a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1v-8z", "M9 14l2 2 4-4")
    val Huellita = trazo(
        circulo(8f, 7f, 1.6f), circulo(16f, 7f, 1.6f), circulo(5f, 12f, 1.6f), circulo(19f, 12f, 1.6f),
        "M12 11c-2.5 0-5 3.5-5 6 0 1.7 1.5 2.5 3 2l2-.6 2 .6c1.5.5 3-.3 3-2 0-2.5-2.5-6-5-6z"
    )
    val Guardian = trazo(circulo(12f, 12f, 3f), "M6.3 6.3a8 8 0 0 0 0 11.4M17.7 6.3a8 8 0 0 1 0 11.4")
    val Candado = trazo("M7 11h10a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2z", "M8 11V8a4 4 0 0 1 8 0v3")
    val Ubicacion = trazo("M12 21s-7-6.2-7-11.5a7 7 0 0 1 14 0C19 14.8 12 21 12 21z", circulo(12f, 9.5f, 2.5f))
    val Bluetooth = trazo("M7 7l10 10-5 4V3l5 4L7 17")
    val Sms = trazo("M4 5h16v11H8l-4 4zM8 10h8")
    val Bateria = trazo("M3 8h15v8H3zM21 11v2M6 11h5")
    val Campana = trazo("M6 16v-5a6 6 0 0 1 12 0v5l2 2H4l2-2zM10 20a2 2 0 0 0 4 0")
    val Apagado = trazo("M12 4v8M7.5 6.5a7 7 0 1 0 9 0")
    val Qr = trazo("M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h2v2h-2zM18 18h2v2h-2zM18 14h2M14 18v2")
    val Compartir = trazo("M12 15V4M8 8l4-4 4 4M5 13v6h14v-6")
    val Celular = trazo("M8 3h8a1 1 0 0 1 1 1v16a1 1 0 0 1-1 1H8a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1zM11 18h2")
    val Mapa = trazo("M12 21s-7-6.2-7-11.5a7 7 0 0 1 14 0C19 14.8 12 21 12 21z")
    val Pegar = trazo("M9 4h6v3H9zM7 5H5v15h14V5h-2M9 12h6M9 16h4")
}
