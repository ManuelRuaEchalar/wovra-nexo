package com.dendra.app.data.finder

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Rumbo hacia el que mira quien sostiene el teléfono, en grados (0 = norte,
 * horario). Usa el sensor de rotación (giroscopio + brújula + acelerómetro).
 *
 * Sirve con el teléfono acostado o de pie frente al pecho: toma el eje del
 * equipo que esté más horizontal (la punta si está acostado, el dorso si
 * está de pie).
 */
class Compass(context: Context) {

    private val sensors = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotation: Sensor? =
        sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensors.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)

    val available: Boolean get() = rotation != null

    fun headings(): Flow<Float> = callbackFlow {
        val sensor = rotation
        if (sensor == null) {
            close()
            return@callbackFlow
        }
        val matrix = FloatArray(9)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(matrix, event.values)
                // Columnas de la matriz = ejes del equipo en el mundo (x este, y norte).
                val topEast = matrix[1]
                val topNorth = matrix[4]
                val backEast = -matrix[2]
                val backNorth = -matrix[5]
                val flat = hypot(topEast, topNorth) >= hypot(backEast, backNorth)
                val east = if (flat) topEast else backEast
                val north = if (flat) topNorth else backNorth
                val degrees = Math.toDegrees(atan2(east.toDouble(), north.toDouble())).toFloat()
                trySend((degrees + 360f) % 360f)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        awaitClose { sensors.unregisterListener(listener) }
    }
}
