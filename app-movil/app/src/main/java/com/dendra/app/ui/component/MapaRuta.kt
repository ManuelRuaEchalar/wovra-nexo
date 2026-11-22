package com.dendra.app.ui.component

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.dendra.app.domain.model.CommChannel
import com.dendra.app.domain.model.TrackPoint
import com.dendra.app.ui.theme.Ambar
import com.dendra.app.ui.theme.Aurora
import com.dendra.app.ui.theme.Destello
import com.dendra.app.ui.theme.Noche
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import java.io.File

/**
 * Mapa del seguimiento: calles reales de OpenStreetMap teñidas de "Nocturno"
 * y, encima, la ruta del celular con el trazo del diseño. Cada punto se
 * dibuja según el canal por el que llegó: lleno = internet, hueco = SMS,
 * punteado = Bluetooth. El punto ámbar es la última ubicación.
 */
@Composable
fun MapaRuta(points: List<TrackPoint>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val overlay = remember { RutaOverlay(context.resources.displayMetrics.density) }
    val map = remember {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = File(context.cacheDir, "osmdroid")
            osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")
        }
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(NOCTURNO))
            overlayManager.tilesOverlay.loadingBackgroundColor = Noche.toArgb()
            overlayManager.tilesOverlay.loadingLineColor = Noche.toArgb()
            setBackgroundColor(Noche.toArgb())
            controller.setZoom(16.5)
            // Sucre, hasta que llegue la primera ubicación.
            controller.setCenter(GeoPoint(-19.0478, -65.2596))
            overlays.add(overlay)
        }
    }
    DisposableEffect(map) {
        map.onResume()
        onDispose {
            map.onPause()
            map.onDetach()
        }
    }
    AndroidView(
        factory = { map },
        modifier = modifier,
        update = { view ->
            val anterior = overlay.points.lastOrNull()?.point?.capturedAt
            overlay.points = points
            val ultimo = points.lastOrNull()
            // Sigue al celular: se centra cada vez que llega un punto nuevo.
            if (ultimo != null && ultimo.point.capturedAt != anterior) {
                val destino = GeoPoint(ultimo.point.latitude, ultimo.point.longitude)
                if (anterior == null) view.controller.setCenter(destino) else view.controller.animateTo(destino)
            }
            view.invalidate()
        }
    )
}

/** Invierte la luminancia del mapa y la lleva al verde noche del sistema. */
private val NOCTURNO = floatArrayOf(
    -0.22f, -0.44f, -0.08f, 0f, 205f,
    -0.22f, -0.44f, -0.08f, 0f, 250f,
    -0.22f, -0.44f, -0.08f, 0f, 245f,
    0f, 0f, 0f, 1f, 0f
)

private class RutaOverlay(private val dp: Float) : Overlay() {

    var points: List<TrackPoint> = emptyList()

    private val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Aurora.copy(alpha = 0.4f).toArgb()
        strokeWidth = 12 * dp
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        maskFilter = BlurMaskFilter(6 * dp, BlurMaskFilter.Blur.NORMAL)
    }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Destello.toArgb()
        strokeWidth = 2 * dp
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Destello.toArgb()
        strokeWidth = 1.5f * dp
    }
    private val amberGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Ambar.copy(alpha = 0.55f).toArgb()
        maskFilter = BlurMaskFilter(9 * dp, BlurMaskFilter.Blur.NORMAL)
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow || points.isEmpty()) return
        val projection = mapView.projection
        val pixels = points.map { tp ->
            Point().also { projection.toPixels(GeoPoint(tp.point.latitude, tp.point.longitude), it) }
        }
        if (pixels.size > 1) {
            val path = Path().apply {
                moveTo(pixels[0].x.toFloat(), pixels[0].y.toFloat())
                pixels.drop(1).forEach { lineTo(it.x.toFloat(), it.y.toFloat()) }
            }
            canvas.drawPath(path, glow)
            canvas.drawPath(path, line)
        }
        // Marcadores intermedios (se espacian para no saturar la ruta).
        val paso = (pixels.size / 40).coerceAtLeast(1)
        for (i in 1 until pixels.lastIndex step paso) {
            val p = pixels[i]
            when (points[i].via) {
                CommChannel.INTERNET -> {
                    fill.color = Destello.toArgb()
                    canvas.drawCircle(p.x.toFloat(), p.y.toFloat(), 3 * dp, fill)
                }
                else -> {
                    fill.color = Noche.toArgb()
                    canvas.drawCircle(p.x.toFloat(), p.y.toFloat(), 4 * dp, fill)
                    ring.pathEffect = if (points[i].via == CommChannel.BLUETOOTH) DashPathEffect(floatArrayOf(2 * dp, 2 * dp), 0f) else null
                    canvas.drawCircle(p.x.toFloat(), p.y.toFloat(), 4 * dp, ring)
                }
            }
        }
        ring.pathEffect = null
        // Inicio de la ruta: anillo inclinado del sistema.
        if (pixels.size > 1) {
            val s = pixels.first()
            canvas.save()
            canvas.rotate(-14f, s.x.toFloat(), s.y.toFloat())
            canvas.drawOval(s.x - 22 * dp, s.y - 9 * dp, s.x + 22 * dp, s.y + 9 * dp, ring)
            canvas.restore()
            fill.color = android.graphics.Color.WHITE
            canvas.drawCircle(s.x.toFloat(), s.y.toFloat(), 3 * dp, fill)
        }
        // Última ubicación: luz ámbar.
        val last = pixels.last()
        canvas.drawCircle(last.x.toFloat(), last.y.toFloat(), 26 * dp, amberGlow)
        ring.color = Ambar.copy(alpha = 0.6f).toArgb()
        ring.strokeWidth = 1 * dp
        canvas.drawCircle(last.x.toFloat(), last.y.toFloat(), 14 * dp, ring)
        ring.color = Destello.toArgb()
        ring.strokeWidth = 1.5f * dp
        fill.color = Ambar.toArgb()
        canvas.drawCircle(last.x.toFloat(), last.y.toFloat(), 7 * dp, fill)
    }
}
