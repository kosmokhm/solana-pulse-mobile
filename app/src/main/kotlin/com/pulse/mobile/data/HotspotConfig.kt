package com.pulse.mobile.data

import android.location.Location
import android.os.SystemClock
import androidx.core.location.LocationCompat

/** A local user-created test zone. Never log or transmit its center. */
data class DemoHotspot(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val maxAccuracyMeters: Float,
    val rewardPoints: Int = 50,
    val hotspotType: HotspotType = HotspotType.STANDARD
) {
    init {
        require(id.isNotBlank() && name.isNotBlank())
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
        require(radiusMeters.isFinite() && radiusMeters > 0)
        require(maxAccuracyMeters.isFinite() && maxAccuracyMeters > 0 && maxAccuracyMeters <= radiusMeters)
        require(rewardPoints > 0)
    }

    override fun toString(): String = "DemoHotspot(id=$id, center=private, radiusMeters=$radiusMeters)"

    fun distanceMeters(location: Location): Float {
        val distances = FloatArray(1)
        Location.distanceBetween(location.latitude, location.longitude, latitude, longitude, distances)
        return distances[0]
    }

    fun rejectionReason(location: Location): String? {
        if (!location.latitude.isFinite() || location.latitude !in -90.0..90.0 ||
            !location.longitude.isFinite() || location.longitude !in -180.0..180.0)
            return "Invalid GPS coordinates"
        val ageNanos = SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos
        return GeofencePolicy.rejectionReason(distanceMeters(location),
            if (location.hasAccuracy()) location.accuracy else Float.NaN,
            radiusMeters, maxAccuracyMeters, ageNanos, LocationCompat.isMock(location))
    }
}

object HotspotConfig {
    const val DEMO_ID = "local-demo-hotspot"
    const val RADIUS_METERS = 100f
    const val MAX_ACCURACY_METERS = 30f

    internal fun localHotspot(latitude: Double, longitude: Double) = DemoHotspot(
        id = DEMO_ID,
        name = "Your test hotspot",
        latitude = latitude,
        longitude = longitude,
        radiusMeters = RADIUS_METERS,
        maxAccuracyMeters = MAX_ACCURACY_METERS
    )
}
