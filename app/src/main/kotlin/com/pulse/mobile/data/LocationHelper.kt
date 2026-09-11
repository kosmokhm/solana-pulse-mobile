// LocationHelper.kt – provides fused location client and helper method
package com.pulse.mobile.data

import android.annotation.SuppressLint
import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object LocationHelper {
    private lateinit var fusedClient: FusedLocationProviderClient

    fun init(context: Context) {
        fusedClient = LocationServices.getFusedLocationProviderClient(context)
    }

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Result<Pair<Double, Double>> =
        suspendCancellableCoroutine { cont ->
            val tokenSource = CancellationTokenSource()
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        cont.resume(Result.success(location.latitude to location.longitude))
                    } else {
                        cont.resume(Result.failure(Exception("Location is null")))
                    }
                }
                .addOnFailureListener { e ->
                    cont.resume(Result.failure(e))
                }
        }
}
