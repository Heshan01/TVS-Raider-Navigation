package com.heshan.tvsraider

import java.util.ArrayDeque
import java.util.Arrays
import java.util.Locale

/**
 * Works out turn direction from Google Maps arrow icon shape using BFS topology analysis.
 */
object ArrowShape {
    @Volatile var lastDebug: String = ""
    @Volatile var lastAngle: Double = Double.NaN

    fun classify(ink: BooleanArray, n: Int, leftHandTraffic: Boolean): Int {
        lastAngle = Double.NaN
        var count = 0
        var minX = n
        var maxX = -1
        var minY = n
        var maxY = -1

        for (y in 0 until n) {
            for (x in 0 until n) {
                if (ink[y * n + x]) {
                    count++
                    minX = Math.min(minX, x)
                    maxX = Math.max(maxX, x)
                    minY = Math.min(minY, y)
                    maxY = Math.max(maxY, y)
                }
            }
        }
        if (count < n * n / 60 || count > n * n * 3 / 4) {
            lastDebug = "ink=$count"
            return -1
        }

        // Roundabout hole detection
        val outside = BooleanArray(n * n)
        val q = ArrayDeque<Int>()
        for (i in 0 until n) {
            seed(ink, outside, q, i, 0, n)
            seed(ink, outside, q, i, n - 1, n)
            seed(ink, outside, q, 0, i, n)
            seed(ink, outside, q, n - 1, i, n)
        }
        while (!q.isEmpty()) {
            val p = q.poll()
            val x = p % n
            val y = p / n
            seed(ink, outside, q, x + 1, y, n)
            seed(ink, outside, q, x - 1, y, n)
            seed(ink, outside, q, x, y + 1, n)
            seed(ink, outside, q, x, y - 1, n)
        }
        var hole = 0
        var hx = 0.0
        var hy = 0.0
        for (i in 0 until n * n) {
            if (!ink[i] && !outside[i]) {
                hole++
                hx += (i % n)
                hy += (i / n)
            }
        }
        if (hole >= Math.max(3, n * n / 200)) {
            if (isPin(ink, n, minY, maxY)) {
                lastDebug = "destination pin"
                return TvsProtocol.P_ARRIVE
            }
            return roundabout(ink, n, hx / hole, hy / hole, Math.sqrt(hole / Math.PI), leftHandTraffic)
        }

        // Stem detection
        val runs = runs(ink, n, maxY)
        var stemA = runs[0]
        var stemB = runs[1]
        var headA = -1
        var headB = -1
        var i = 2
        while (i + 1 < runs.size && runs[i] >= 0) {
            if (runs[i + 1] - runs[i] < stemB - stemA) {
                headA = stemA
                headB = stemB
                stemA = runs[i]
                stemB = runs[i + 1]
            } else if (headA < 0 || runs[i + 1] - runs[i] > headB - headA) {
                headA = runs[i]
                headB = runs[i + 1]
            }
            i += 2
        }

        // Distance along ink from stem
        val dist = IntArray(n * n)
        val parent = IntArray(n * n)
        Arrays.fill(dist, -1)
        for (x in stemA..stemB) {
            dist[maxY * n + x] = 0
            q.add(maxY * n + x)
        }
        var maxD = 0
        while (!q.isEmpty()) {
            val p = q.poll()
            val x = p % n
            val y = p / n
            val d = dist[p]
            maxD = Math.max(maxD, d)
            for (dy in -2..2) {
                for (dx in -2..2) {
                    val nx = x + dx
                    val ny = y + dy
                    if (nx !in 0 until n || ny !in 0 until n) continue
                    val np = ny * n + nx
                    if (ink[np] && dist[np] < 0) {
                        dist[np] = d + 1
                        parent[np] = p
                        q.add(np)
                    }
                }
            }
        }
        if (maxD < n / 4) {
            lastDebug = "short stroke $maxD"
            return -1
        }

        var fx = 0.0
        var fy = 0.0
        var mx = 0.0
        var my = 0.0
        var fc = 0
        var mc = 0
        var tip = -1
        var farMinX = n
        var farMaxX = -1

        for (idx in 0 until n * n) {
            if (dist[idx] < 0) continue
            if (dist[idx] == maxD && tip < 0) tip = idx
            if (dist[idx] >= maxD * 0.8) {
                fx += (idx % n)
                fy += (idx / n)
                fc++
                farMinX = Math.min(farMinX, idx % n)
                farMaxX = Math.max(farMaxX, idx % n)
            } else if (dist[idx] >= maxD * 0.5) {
                mx += (idx % n)
                my += (idx / n)
                mc++
            }
        }
        if (fc == 0 || mc == 0) return -1
        fx /= fc
        fy /= fc
        mx /= mc
        my /= mc
        var a = Math.toDegrees(Math.atan2(my - fy, fx - mx))
        if (a < -90) a += 360.0
        val stemX = (stemA + stemB) / 2.0
        lastAngle = a
        lastDebug = String.format(Locale.ROOT, "angle=%.0f tip=(%.1f,%.1f) stem=%.1f", a, fx, fy, stemX)

        var pathMinX = n
        var pathMaxX = -1
        var p = tip
        while (p >= 0 && dist[p] > 0) {
            pathMinX = Math.min(pathMinX, p % n)
            pathMaxX = Math.max(pathMaxX, p % n)
            p = parent[p]
        }
        val swing = Math.max(3, n / 8)
        val loopLeft = pathMinX <= Math.min(stemA, farMinX) - swing
        val loopRight = pathMaxX >= Math.max(stemB, farMaxX) + swing
        if (loopLeft != loopRight) {
            lastDebug += " loop" + (if (loopLeft) "L" else "R") + " path x " + pathMinX + ".." + pathMaxX
            return roundaboutExit(a, leftHandTraffic)
        }

        if (a in 65.0..115.0) return TvsProtocol.P_STRAIGHT
        if (a > 115.0 && a <= 155.0) return TvsProtocol.P_SLIGHT_LEFT
        if (a > 155.0 && a <= 205.0) return TvsProtocol.P_LEFT
        if (a > 205.0 && a <= 238.0) return TvsProtocol.P_SHARP_LEFT
        if (a >= 25.0 && a < 65.0) return TvsProtocol.P_SLIGHT_RIGHT
        if (a >= -25.0 && a < 25.0) return TvsProtocol.P_RIGHT
        if (a >= -58.0 && a < -25.0) return TvsProtocol.P_SHARP_RIGHT

        val tipX = if (headA >= 0) (headA + headB) / 2.0 else fx
        return if (tipX < stemX) TvsProtocol.P_UTURN_LEFT else TvsProtocol.P_UTURN_RIGHT
    }

    fun isPin(ink: BooleanArray, n: Int, minY: Int, maxY: Int): Boolean {
        if (maxY - minY < 8) return false
        var prev = Int.MAX_VALUE
        for (y in (maxY - 4)..maxY) {
            var a = n
            var b = -1
            for (x in 0 until n) {
                if (ink[y * n + x]) {
                    a = Math.min(a, x)
                    b = Math.max(b, x)
                }
            }
            val w = b - a + 1
            if (b < 0 || w >= prev) return false
            prev = w
        }
        return prev <= 4
    }

    private fun roundabout(ink: BooleanArray, n: Int, cx: Double, cy: Double, r: Double, lh: Boolean): Int {
        var best = -1.0
        var bx = cx
        var by = cy
        for (i in 0 until n * n) {
            if (!ink[i]) continue
            val x = i % n
            val y = i / n
            if (y > cy + r && Math.abs(x - cx) < r) continue
            val d = (x - cx) * (x - cx) + (y - cy) * (y - cy)
            if (d > best) {
                best = d
                bx = x.toDouble()
                by = y.toDouble()
            }
        }
        var a = Math.toDegrees(Math.atan2(cy - by, bx - cx))
        if (a < -90) a += 360.0
        lastDebug = String.format(Locale.ROOT, "roundabout exit angle=%.0f", a)
        return roundaboutExit(a, lh)
    }

    fun roundaboutExit(a: Double, lh: Boolean): Int = TvsProtocol.roundaboutCode(a, lh)

    private fun runs(ink: BooleanArray, n: Int, y: Int): IntArray {
        val r = IntArray(n + 2)
        Arrays.fill(r, -1)
        var k = 0
        for (x in 0 until n) {
            if (ink[y * n + x] && (x == 0 || !ink[y * n + x - 1])) r[k] = x
            if (ink[y * n + x] && (x == n - 1 || !ink[y * n + x + 1])) {
                r[k + 1] = x
                k += 2
            }
        }
        return r
    }

    private fun seed(ink: BooleanArray, seen: BooleanArray, q: ArrayDeque<Int>, x: Int, y: Int, n: Int) {
        if (x !in 0 until n || y !in 0 until n) return
        val p = y * n + x
        if (ink[p] || seen[p]) return
        seen[p] = true
        q.add(p)
    }
}
