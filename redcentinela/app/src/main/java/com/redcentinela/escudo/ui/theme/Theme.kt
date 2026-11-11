@file:OptIn(ExperimentalTextApi::class)

package com.redcentinela.escudo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.redcentinela.escudo.R

private fun sora(weight: Int) = Font(
    R.font.sora, FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

private fun hanken(weight: Int) = Font(
    R.font.hanken_grotesk, FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

/** Sora 200/300 para títulos y cifras. */
val Sora = FontFamily(sora(200), sora(300), sora(400), sora(600))

/** Hanken Grotesk 400/500/600 para interfaz. */
val Hanken = FontFamily(hanken(400), hanken(500), hanken(600))

/** Estilos de texto con nombre, tal como aparecen en las líneas de diseño. */
object Tipo {
    val Cifra = TextStyle(fontFamily = Sora, fontWeight = FontWeight(200), fontSize = 56.sp, letterSpacing = (-0.045).em, color = Tinta)
    val Display = TextStyle(fontFamily = Sora, fontWeight = FontWeight(300), fontSize = 34.sp, lineHeight = 39.sp, letterSpacing = (-0.035).em, color = Tinta)
    val Titulo = TextStyle(fontFamily = Sora, fontWeight = FontWeight(300), fontSize = 30.sp, lineHeight = 35.sp, letterSpacing = (-0.03).em, color = Tinta)
    val Subtitulo = TextStyle(fontFamily = Sora, fontWeight = FontWeight(300), fontSize = 22.sp, lineHeight = 27.sp, letterSpacing = (-0.02).em, color = Tinta)
    val Fuerte = TextStyle(fontFamily = Hanken, fontWeight = FontWeight(600), fontSize = 15.sp, lineHeight = 20.sp, color = Tinta)
    val Cuerpo = TextStyle(fontFamily = Hanken, fontWeight = FontWeight(400), fontSize = 15.sp, lineHeight = 22.sp, color = Texto2)
    val Fila = TextStyle(fontFamily = Hanken, fontWeight = FontWeight(400), fontSize = 14.sp, lineHeight = 19.sp, color = Tinta)
    val Nota = TextStyle(fontFamily = Hanken, fontWeight = FontWeight(400), fontSize = 13.sp, lineHeight = 18.sp, color = Texto2)
    val Etiqueta = TextStyle(fontFamily = Hanken, fontWeight = FontWeight(600), fontSize = 12.sp, letterSpacing = 0.14.em, color = Texto2)
    val Boton = TextStyle(fontFamily = Hanken, fontWeight = FontWeight(600), fontSize = 16.sp)
}

private val Colores = lightColorScheme(
    primary = Noche,
    onPrimary = Superficie,
    primaryContainer = Bruma,
    onPrimaryContainer = Medio,
    secondary = Medio,
    background = Fondo,
    onBackground = Tinta,
    surface = Superficie,
    onSurface = Tinta,
    surfaceVariant = Fondo,
    onSurfaceVariant = Texto2,
    error = Ambar,
    onError = AmbarOscuro,
    outline = Linea
)

private val base = TextStyle(fontFamily = Hanken, color = Tinta)

private val Tipografia = Typography(
    headlineMedium = Tipo.Titulo,
    titleLarge = Tipo.Subtitulo,
    titleMedium = Tipo.Fuerte,
    bodyLarge = base.copy(fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = base.copy(fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = base.copy(fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = base.copy(fontSize = 15.sp, fontWeight = FontWeight(600)),
    labelSmall = base.copy(fontSize = 12.sp, fontWeight = FontWeight(500))
)

/** Tema de la app: siempre el esquema claro "Halo"; lo nocturno son tarjetas puntuales. */
@Composable
fun RedCentinelaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colores, typography = Tipografia, content = content)
}
