package cl.kcorp.syncpass.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object LocationProvider {

    private const val LAST_KNOWN_MAX_AGE_MS = 2 * 60 * 1000L
    private const val LOCATION_TIMEOUT_MS = 10_000L

    fun hasPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun hasFinePermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun currentLocation(context: Context): LocationSnapshot? {
        if (!hasPermission(context)) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        val fineGranted = hasFinePermission(context)
        val providers = if (fineGranted) {
            listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER,
            )
        } else {
            listOf(
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER,
            )
        }

        val lastKnown = providers
            .asSequence()
            .filter { provider -> runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false) }
            .mapNotNull { provider ->
                runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
            }
            .maxByOrNull { it.time }

        if (lastKnown != null &&
            System.currentTimeMillis() - lastKnown.time < LAST_KNOWN_MAX_AGE_MS
        ) {
            return lastKnown.toSnapshot()
        }

        val activeProviders = if (fineGranted) {
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        } else {
            listOf(LocationManager.NETWORK_PROVIDER)
        }
        val activeProvider = activeProviders.firstOrNull { provider ->
            runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)
        }

        val fresh = activeProvider?.let { provider ->
            withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
                suspendCancellableCoroutine<Location?> { continuation ->
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            runCatching { manager.removeUpdates(this) }
                            if (continuation.isActive) continuation.resume(location)
                        }

                        override fun onProviderEnabled(provider: String) = Unit

                        override fun onProviderDisabled(provider: String) = Unit

                        @Deprecated("Deprecated in Java")
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                    }
                    runCatching {
                        manager.requestLocationUpdates(
                            provider,
                            0L,
                            0f,
                            listener,
                            Looper.getMainLooper(),
                        )
                    }.onFailure {
                        if (continuation.isActive) continuation.resume(null)
                    }
                    continuation.invokeOnCancellation {
                        runCatching { manager.removeUpdates(listener) }
                    }
                }
            }?.toSnapshot()
        }

        // Si no hubo una lectura fresca, se usa la última conocida (aunque sea
        // antigua) para no bloquear la demo en interiores. El validador acota su
        // uso según la precisión informada.
        return fresh ?: lastKnown?.toSnapshot()
    }

    fun distanceMeters(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double,
    ): Float = LocationValidator.distanciaMetros(
        startLatitude,
        startLongitude,
        endLatitude,
        endLongitude,
    ).toFloat()

    private fun Location.toSnapshot(): LocationSnapshot = LocationSnapshot(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = if (hasAccuracy()) accuracy.toDouble() else null,
        timestampMillis = time.takeIf { it > 0 } ?: System.currentTimeMillis(),
        provider = provider,
    )
}
