package com.heshan.tvsraider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for RideTracker session tracking and edge cases.
 */
class RideTrackerTest {

    @Test
    fun testRideStartAndFinish() {
        var finishedRide: RideTracker.Ride? = null
        var changedRide: RideTracker.Ride? = null

        val tracker = RideTracker(object : RideTracker.Listener {
            override fun finished(r: RideTracker.Ride) {
                finishedRide = r
            }

            override fun changed(r: RideTracker.Ride?) {
                changedRide = r
            }
        })

        val t0 = 100000L
        // Standstill sample (< 3 km/h) -> should not start ride
        tracker.sample(0, 10000, t0)
        assertNull(tracker.current())

        // Moving sample (>= 3 km/h) -> ride starts
        tracker.sample(25, 10000, t0 + 1000)
        assertNotNull(tracker.current())
        assertEquals(10000, tracker.current()!!.odoStart)

        // Progress ride over 5 km
        tracker.sample(40, 10050, t0 + 10000) // 5.0 km
        assertEquals(5.0, tracker.current()!!.km(), 0.01)

        // Stop moving
        tracker.sample(0, 10050, t0 + 20000)

        // Silence for 2 minutes (END_SILENCE_MS = 120_000 ms)
        tracker.tick(t0 + 20000 + 130000)

        assertNull(tracker.current())
        assertNotNull(finishedRide)
        assertEquals(5.0, finishedRide!!.km(), 0.01)
        assertEquals(40, finishedRide!!.maxKmh)
    }

    @Test
    fun testTinyRideDiscarded() {
        var finishedRide: RideTracker.Ride? = null

        val tracker = RideTracker(object : RideTracker.Listener {
            override fun finished(r: RideTracker.Ride) {
                finishedRide = r
            }

            override fun changed(r: RideTracker.Ride?) {}
        })

        val t0 = 100000L
        tracker.sample(10, 1000, t0)
        tracker.sample(10, 1001, t0 + 2000) // 0.1 km only (< 0.3 km minimum)

        tracker.finish()
        // Tiny ride (< 0.3 km) must be dropped
        assertNull(finishedRide)
    }

    @Test
    fun testOdometerGlitchFallbackToIntegratedSpeed() {
        val tracker = RideTracker(object : RideTracker.Listener {
            override fun finished(r: RideTracker.Ride) {}
            override fun changed(r: RideTracker.Ride?) {}
        })

        val t0 = 100000L
        tracker.sample(36, 1000, t0)
        // Next sample odometer jumps corrupted (e.g. difference >= 20000 or negative)
        tracker.sample(36, 999, t0 + 1000) // 1 second at 36 km/h = 10 metres = 0.01 km

        val r = tracker.current()
        assertNotNull(r)
        // When odometer looks broken, km() falls back to integrated speed
        assertEquals(0.01, r!!.km(), 0.005)
    }
}
