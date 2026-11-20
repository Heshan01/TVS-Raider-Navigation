package com.heshan.tvsraider

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import java.util.HashSet

/**
 * Turns Google Maps large navigation notification icon into a 32x32 ink mask and hash.
 */
object IconReader {
    const val N: Int = 32
    private val logged = HashSet<String>()

    data class Result(
        var hash: String = "",
        var ascii: String = "",
        var picto: Int = -1,
        var debug: String = ""
    )

    fun read(ctx: Context, n: Notification): Result? {
        try {
            val icon = n.getLargeIcon() ?: return null
            val d = icon.loadDrawable(ctx) ?: return null
            val bmp = Bitmap.createBitmap(N, N, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            d.setBounds(0, 0, N, N)
            d.draw(c)

            val px = IntArray(N * N)
            bmp.getPixels(px, 0, N, 0, 0, N, N)
            bmp.recycle()

            var opaque = 0
            var lumSum = 0L
            val lum = IntArray(px.size)
            for (i in px.indices) {
                val p = px[i]
                if (Color.alpha(p) > 128) opaque++
                lum[i] = (Color.red(p) * 30 + Color.green(p) * 59 + Color.blue(p) * 11) / 100
                lumSum += lum[i]
            }
            val useLum = opaque > px.size * 9 / 10
            val mean = (lumSum / px.size).toInt()
            val ink = BooleanArray(px.size)
            val faint = BooleanArray(px.size)
            var faintCount = 0
            for (i in px.indices) {
                ink[i] = if (useLum) lum[i] > mean else Color.alpha(px[i]) > 128
                faint[i] = !useLum && !ink[i] && Color.alpha(px[i]) > 40
            }
            for (i in px.indices) {
                if (faint[i] && !nextToInk(ink, i)) faintCount++
            }

            val r = Result()
            val hex = java.lang.StringBuilder()
            for (i in px.indices step 4) {
                val nib = (if (ink[i]) 8 else 0) or
                        (if (i + 1 < px.size && ink[i + 1]) 4 else 0) or
                        (if (i + 2 < px.size && ink[i + 2]) 2 else 0) or
                        (if (i + 3 < px.size && ink[i + 3]) 1 else 0)
                hex.append(Character.forDigit(nib, 16))
            }
            r.hash = hex.toString()
            val art = java.lang.StringBuilder()
            for (y in 0 until N) {
                for (x in 0 until N) {
                    val idx = y * N + x
                    art.append(if (ink[idx]) '#' else if (faint[idx]) '+' else '.')
                }
                if (y < N - 1) art.append('\n')
            }
            r.ascii = art.toString()
            r.picto = IconTable.lookup(ink)
            if (r.picto < 0) {
                r.picto = ArrowShape.classify(ink, N, MapsParser.leftHandTraffic)
                r.debug = ArrowShape.lastDebug
                if (faintCount >= 15 && !ArrowShape.lastAngle.isNaN() && !isRoundabout(r.picto)) {
                    r.picto = ArrowShape.roundaboutExit(ArrowShape.lastAngle, MapsParser.leftHandTraffic)
                    r.debug += " faint ring ($faintCount px) -> roundabout"
                }
            } else {
                r.debug = "known icon"
            }
            return r
        } catch (e: Exception) {
            BikeService.log("Icon read failed: $e")
            return null
        }
    }

    private fun nextToInk(ink: BooleanArray, i: Int): Boolean {
        val x = i % N
        val y = i / N
        for (dy in -1..1) {
            for (dx in -1..1) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until N && ny in 0 until N && ink[ny * N + nx]) return true
            }
        }
        return false
    }

    fun isRoundabout(p: Int): Boolean = TvsProtocol.isRoundabout(p)

    fun forgetLogged() {
        logged.clear()
    }

    fun logIfNew(r: Result?) {
        if (r == null || logged.contains(r.hash) || logged.size > 60) return
        logged.add(r.hash)
        BikeService.log("ICON ${r.hash} picto=${r.picto}${if (r.picto < 0) " (unknown)" else ""} ${r.debug}")
        for (line in r.ascii.split("\n")) {
            BikeService.log("ICON|$line")
        }
    }
}
