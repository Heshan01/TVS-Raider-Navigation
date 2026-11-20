package com.heshan.tvsraider

import java.util.Locale

/**
 * Turns bike speed + odometer samples into rides.
 */
class RideTracker(private val listener: Listener) {
    companion object {
        const val MOVING_KMH = 3
        const val END_SILENCE_MS = 2 * 60_000L
        const val END_IDLE_MS = 15 * 60_000L
        const val MAX_STEP_MS = 5_000L
        const val MIN_KM = 0.3

        fun summary(r: Ride): String {
            return String.format(
                Locale.ROOT,
                "%.1f km  ·  %s  ·  avg %.0f  ·  max %d km/h",
                r.km(),
                duration(r.durationMs()),
                r.avgKmh(),
                r.maxKmh
            )
        }

        fun duration(ms: Long): String {
            val min = Math.round(ms / 60_000.0)
            return if (min >= 60) "${min / 60} h ${min % 60} min" else "$min min"
        }
    }

    class Ride {
        var start: Long = 0
        var end: Long = 0
        var odoStart: Int = 0
        var odoEnd: Int = 0
        var maxKmh: Int = 0
        var movingMs: Long = 0
        var speedKm: Double = 0.0
        var avgKmL: Int = 0

        fun km(): Double {
            val d = odoEnd - odoStart
            return if (d in 1..19999) d / 10.0 else speedKm
        }

        fun durationMs(): Long = Math.max(0, end - start)

        fun avgKmh(): Double = if (movingMs < 10_000) 0.0 else km() / (movingMs / 3_600_000.0)
    }

    interface Listener {
        fun finished(r: Ride)
        fun changed(r: Ride?)
    }

    private var cur: Ride? = null
    private var lastSample: Long = 0
    private var lastMoving: Long = 0
    private var lastKmh: Int = 0

    fun current(): Ride? = cur

    fun resume(r: Ride, lastMovingAt: Long) {
        cur = r
        lastMoving = lastMovingAt
        lastSample = 0
    }

    fun sample(kmh: Int, odo: Int, now: Long) {
        if (odo <= 0) return
        val moving = kmh >= MOVING_KMH
        val active = cur
        if (active == null) {
            if (!moving) {
                lastSample = now
                lastKmh = kmh
                return
            }
            val newRide = Ride().apply {
                start = now
                odoStart = odo
                odoEnd = odo
            }
            cur = newRide
            lastMoving = now
        } else if (lastSample > 0) {
            val dt = now - lastSample
            if (dt in 1..MAX_STEP_MS) {
                if (lastKmh >= MOVING_KMH || moving) active.movingMs += dt
                active.speedKm += lastKmh * dt / 3_600_000.0
            }
        }
        val currentRide = cur ?: return
        if (moving) {
            lastMoving = now
            currentRide.end = now
            currentRide.odoEnd = odo
        }
        currentRide.maxKmh = Math.max(currentRide.maxKmh, kmh)
        lastSample = now
        lastKmh = kmh
        listener.changed(currentRide)
    }

    fun tick(now: Long) {
        if (cur == null) return
        val silent = if (lastSample == 0L) now - lastMoving > END_SILENCE_MS else now - lastSample > END_SILENCE_MS
        val idle = now - lastMoving > END_IDLE_MS
        if (silent || idle) finish()
    }

    fun finish() {
        val r = cur
        cur = null
        lastSample = 0
        if (r == null) return
        if (r.end <= r.start) r.end = r.start
        if (r.km() >= MIN_KM) listener.finished(r)
        else listener.changed(null)
    }
}
