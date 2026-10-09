package com.pulse.mobile.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/** Shared request gate survives navigation; coordinates never leave the caller. */
object LocationHelper {
    private lateinit var appContext: Context
    private val requestGate = AtomicBoolean(false)
    var isLocating by mutableStateOf(false)
        private set

    fun init(context: Context) { appContext = context.applicationContext }

    fun hasLocationPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Result<Location> {
        if (!requestGate.compareAndSet(false, true))
            return Result.failure(IllegalStateException("A GPS request is already running"))
        isLocating = true
        try {
            if (!hasLocationPermission(appContext))
                return Result.failure(IllegalStateException("Enable precise location in app settings"))
            val manager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                return Result.failure(IllegalStateException("GPS is off. Turn on device location and try again."))
            return suspendCancellableCoroutine { continuation ->
                val source = CancellationTokenSource()
                continuation.invokeOnCancellation { source.cancel() }
                LocationServices.getFusedLocationProviderClient(appContext)
                    .getCurrentLocation(CurrentLocationRequest.Builder()
                        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                        .setMaxUpdateAgeMillis(0)
                        .setDurationMillis(20_000)
                        .build(), source.token)
                    .addOnSuccessListener { fix ->
                        if (continuation.isActive) continuation.resume(
                            if (fix != null) Result.success(fix)
                            else Result.failure(IllegalStateException("No GPS fix. Move outdoors and try again.")))
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(Result.failure(
                            IllegalStateException("GPS request failed. Check location settings and try again.")))
                    }
                    .addOnCanceledListener {
                        if (continuation.isActive) continuation.resume(Result.failure(
                            IllegalStateException("GPS request cancelled. Try again.")))
                    }
            }
        } finally {
            isLocating = false
            requestGate.set(false)
        }
    }
}
