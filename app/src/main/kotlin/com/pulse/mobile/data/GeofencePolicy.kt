package com.pulse.mobile.data

/** Conservative local GPS gate, not server-side proof of presence. */
internal object GeofencePolicy {
    fun rejectionReason(distanceMeters: Float, accuracyMeters: Float, radiusMeters: Float,
                        maxAccuracyMeters: Float, ageNanos: Long, isMock: Boolean): String? {
        if (isMock) return "Mock locations are not accepted"
        if (ageNanos < 0 || ageNanos > 30_000_000_000L) return "GPS fix is stale; scan again"
        if (!accuracyMeters.isFinite() || accuracyMeters < 0 || accuracyMeters > maxAccuracyMeters)
            return "GPS accuracy is insufficient; enable precise location"
        // The entire reported accuracy circle must fit inside the geofence.
        if (!distanceMeters.isFinite() || distanceMeters < 0 || distanceMeters + accuracyMeters > radiusMeters)
            return "Outside geofence: ${distanceMeters.toInt()} m away, radius ${radiusMeters.toInt()} m"
        return null
    }
}
