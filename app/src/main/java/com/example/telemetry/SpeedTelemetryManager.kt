package com.example.telemetry

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class GpsSpeedSample(
    val speedKmh: Float,
    val accuracyMeters: Float,
    val timestampMillis: Long
)

class SpeedTelemetryManager(private val context: Context) {

    @SuppressLint("MissingPermission")
    fun observeRealGpsSpeed(): Flow<GpsSpeedSample> = callbackFlow {
        val fusedClient = try {
            LocationServices.getFusedLocationProviderClient(context)
        } catch (_: Exception) {
            null
        }

        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        val fusedCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                emitLocation(loc)
            }

            private fun emitLocation(loc: Location) {
                val rawMps = if (loc.hasSpeed()) loc.speed else 0f
                val kmh = (rawMps * 3.6f).coerceAtLeast(0f)
                // Filter tiny GPS drift under 1.2 km/h when stationary
                val cleanKmh = if (kmh < 1.2f) 0f else kmh
                trySend(
                    GpsSpeedSample(
                        speedKmh = cleanKmh,
                        accuracyMeters = loc.accuracy,
                        timestampMillis = System.currentTimeMillis()
                    )
                )
            }
        }

        val gpsListener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                val rawMps = if (loc.hasSpeed()) loc.speed else 0f
                val kmh = (rawMps * 3.6f).coerceAtLeast(0f)
                val cleanKmh = if (kmh < 1.2f) 0f else kmh
                trySend(
                    GpsSpeedSample(
                        speedKmh = cleanKmh,
                        accuracyMeters = loc.accuracy,
                        timestampMillis = System.currentTimeMillis()
                    )
                )
            }

            @Deprecated("Deprecated in API")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }

        try {
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500L)
                .setMinUpdateIntervalMillis(250L)
                .setWaitForAccurateLocation(false)
                .build()
            fusedClient?.requestLocationUpdates(request, fusedCallback, Looper.getMainLooper())
        } catch (_: Exception) {
        }

        try {
            if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    500L,
                    0f,
                    gpsListener,
                    Looper.getMainLooper()
                )
            }
        } catch (_: Exception) {
        }

        awaitClose {
            try {
                fusedClient?.removeLocationUpdates(fusedCallback)
            } catch (_: Exception) {
            }
            try {
                locationManager?.removeUpdates(gpsListener)
            } catch (_: Exception) {
            }
        }
    }
}
