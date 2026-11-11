package com.redcentinela.escudo.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.redcentinela.escudo.domain.model.GeoPoint
import com.redcentinela.escudo.domain.model.LocationSource
import com.redcentinela.escudo.domain.port.LocationTracker
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Implementación de [LocationTracker] sobre el Fused Location Provider de
 * Google Play Services. Funciona sin internet: el GPS entrega coordenadas
 * aunque no haya datos (la primera lectura puede tardar).
 *
 * El permiso de ubicación se verifica antes de iniciar el servicio; aquí se
 * asume concedido (de ahí el @SuppressLint, documentado).
 */
class FusedLocationTracker(context: Context) : LocationTracker {

    private val client = LocationServices.getFusedLocationProviderClient(
        context.applicationContext
    )

    @SuppressLint("MissingPermission")
    override fun stream(intervalMillis: Long): Flow<GeoPoint> = callbackFlow {
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            intervalMillis
        ).setMinUpdateIntervalMillis(intervalMillis / 2).build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it.toGeoPoint()) }
            }
        }

        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }

    @SuppressLint("MissingPermission")
    override suspend fun lastKnown(): GeoPoint? = suspendCancellableCoroutine { cont ->
        client.lastLocation
            .addOnSuccessListener { loc -> cont.resume(loc?.toGeoPoint()) }
            .addOnFailureListener { cont.resume(null) }
    }
}

/** Traduce una Location de Android al modelo de dominio. */
private fun android.location.Location.toGeoPoint(): GeoPoint = GeoPoint(
    latitude = latitude,
    longitude = longitude,
    accuracyMeters = if (hasAccuracy()) accuracy else Float.MAX_VALUE,
    source = when (provider) {
        android.location.LocationManager.GPS_PROVIDER -> LocationSource.GPS
        android.location.LocationManager.NETWORK_PROVIDER -> LocationSource.WIFI
        else -> LocationSource.FUSED
    },
    speedMps = if (hasSpeed()) speed else null,
    bearingDeg = if (hasBearing()) bearing else null,
    capturedAt = System.currentTimeMillis()
)
