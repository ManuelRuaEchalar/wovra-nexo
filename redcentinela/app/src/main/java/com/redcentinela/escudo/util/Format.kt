package com.redcentinela.escudo.util

import com.redcentinela.escudo.domain.model.GeoPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Utilidades de formato para la UI. Centralizadas para no repetirlas (DRY). */
object Format {
    private val es = Locale("es", "BO")
    private val time = SimpleDateFormat("HH:mm", es)
    private val timeSeconds = SimpleDateFormat("HH:mm:ss", es)
    private val day = SimpleDateFormat("EEEE d 'de' MMMM", es)

    fun clock(epochMillis: Long): String = time.format(Date(epochMillis))
    fun clockSeconds(epochMillis: Long): String = timeSeconds.format(Date(epochMillis))
    fun day(epochMillis: Long): String = day.format(Date(epochMillis))

    fun coords(point: GeoPoint): String =
        String.format(Locale.US, "%.5f, %.5f", point.latitude, point.longitude)

    fun accuracy(point: GeoPoint): String = "±${point.accuracyMeters.toInt().coerceAtMost(9999)} m"

    /** "hace 40 s", "hace 6 min", "hace 2 h". */
    fun ago(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
        val sec = ((now - epochMillis) / 1000).coerceAtLeast(0)
        return when {
            sec < 5 -> "ahora"
            sec < 60 -> "hace $sec s"
            sec < 3600 -> "hace ${sec / 60} min"
            sec < 86_400 -> "hace ${sec / 3600} h"
            else -> "hace ${sec / 86_400} d"
        }
    }

    /** Distancia corta con coma decimal, como en el diseño: "0,4 m". */
    fun meters(value: Double): String =
        if (value < 10) String.format(es, "%.1f m", value) else "${value.toInt()} m"

    fun greeting(now: Long = System.currentTimeMillis()): String {
        val hour = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Buenos días"
            in 12..18 -> "Buenas tardes"
            else -> "Buenas noches"
        }
    }
}
