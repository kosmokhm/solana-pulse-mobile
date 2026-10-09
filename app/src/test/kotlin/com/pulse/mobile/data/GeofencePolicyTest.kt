package com.pulse.mobile.data

import org.junit.Assert.*
import org.junit.Test

class GeofencePolicyTest {
    // Synthetic distances only; no geographic demo locations are invented.
    private fun check(distance: Float = 20f, accuracy: Float = 5f,
                      age: Long = 1_000_000_000L, mock: Boolean = false): String? =
        GeofencePolicy.rejectionReason(distance, accuracy, 100f, 30f, age, mock)

    @Test fun acceptsAccurateFixInsideRadius() { assertNull(check()) }
    @Test fun acceptsAccuracyCircleExactlyAtBoundary() { assertNull(check(95f, 5f)) }
    @Test fun rejectsFixOutsideRadius() { assertNotNull(check(101f)) }
    @Test fun rejectsUncertaintyOverlappingBoundary() { assertNotNull(check(96f, 5f)) }
    @Test fun rejectsPoorOrMissingAccuracy() {
        listOf(31f, Float.NaN, Float.POSITIVE_INFINITY, -1f).forEach {
            assertNotNull(check(accuracy = it))
        }
    }
    @Test fun rejectsStaleOrFutureFix() {
        assertNotNull(check(age = 30_000_000_001L))
        assertNotNull(check(age = -1L))
    }
    @Test fun rejectsMockFixEvenInsideRadius() { assertNotNull(check(mock = true)) }
    @Test fun acceptsAccuracyLimitAndRejectsWorse() {
        assertNull(check(distance = 10f, accuracy = 30f))
        assertNotNull(check(distance = 10f, accuracy = 30.01f))
    }
    @Test fun acceptsFreshnessBoundary() { assertNull(check(age = 30_000_000_000L)) }
    @Test fun rejectsInvalidDistance() {
        listOf(Float.NaN, Float.POSITIVE_INFINITY, -1f).forEach {
            assertNotNull(check(distance = it))
        }
    }
    @Test fun fullAccuracyCircleMustFitForDemoCheckIn() {
        assertNull(check(distance = 70f, accuracy = 30f))
        assertNotNull(check(distance = 70.01f, accuracy = 30f))
        assertNotNull(check(distance = 100f, accuracy = 0.01f))
    }
    @Test fun beingAtCenterDoesNotBypassGpsQualityRules() {
        assertNotNull(check(distance = 0f, accuracy = 31f))
        assertNotNull(check(distance = 0f, accuracy = 5f, mock = true))
        assertNotNull(check(distance = 0f, accuracy = 5f, age = 30_000_000_001L))
    }

}
