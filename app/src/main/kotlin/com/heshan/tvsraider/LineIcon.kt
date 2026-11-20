package com.heshan.tvsraider

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View

/**
 * High-precision vector line icons rendered mathematically on a 24x24 coordinate grid.
 * Matches TVS Raider Obsidian Telemetry design system.
 */
class LineIcon(c: Context, private var kind: Int, color: Int) : View(c) {
    companion object {
        const val HOME = 0
        const val OVERVIEW = 1
        const val LOG = 2
        const val SETTINGS = 3
        const val SEARCH = 4
        const val SHARE = 5
        const val BIKE = 6
        const val FUEL = 7
        const val SEND = 8
        const val CHEVRON = 9
        const val CHECK = 10
        const val TREND_UP = 11
        const val TREND_DOWN = 12
        const val CODE = 13
        const val TRASH = 14
        const val SENSORS = 15
        const val PERSON = 16
        const val BOLT = 17
        const val ECO = 18
        const val SYNC = 19
        const val TIMER = 20
        const val BACK = 21
        const val DOWNLOAD = 22
        const val SPEED = 23
        const val NAV = 24
        const val TUNE = 25
        const val TERMINAL = 26
        const val BAR_CHART = 27
        const val PIN = 28
        const val ROUTE = 29
        const val BLUETOOTH = 30
    }

    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val p = Path()
    private val r = RectF()

    init {
        setColor(color)
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    fun setKind(k: Int) {
        if (k != kind) {
            kind = k
            invalidate()
        }
    }

    fun setColor(color: Int) {
        stroke.color = color
        fill.color = color
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        val s = Math.min(width, height) / 24f
        c.save()
        c.translate((width - 24 * s) / 2f, (height - 24 * s) / 2f)
        c.scale(s, s)
        stroke.strokeWidth = 2f
        p.reset()

        when (kind) {
            HOME, NAV -> {
                p.moveTo(12f, 3f)
                p.lineTo(20f, 20f)
                p.lineTo(12f, 16f)
                p.lineTo(4f, 20f)
                p.close()
                c.drawPath(p, stroke)
            }
            OVERVIEW, SPEED -> {
                r.set(3f, 4f, 21f, 22f)
                c.drawArc(r, 150f, 240f, false, stroke)
                c.drawLine(12f, 13f, 16.5f, 8.5f, stroke)
                c.drawCircle(12f, 13f, 1.6f, fill)
            }
            BAR_CHART, TREND_UP -> {
                c.drawLine(4f, 20f, 20f, 20f, stroke)
                r.set(5.5f, 13f, 8.5f, 20f)
                c.drawRoundRect(r, 1f, 1f, fill)
                r.set(10.5f, 7f, 13.5f, 20f)
                c.drawRoundRect(r, 1f, 1f, fill)
                r.set(15.5f, 10f, 18.5f, 20f)
                c.drawRoundRect(r, 1f, 1f, fill)
            }
            LOG, TERMINAL -> {
                p.moveTo(5f, 7f)
                p.lineTo(10f, 12f)
                p.lineTo(5f, 17f)
                p.moveTo(12f, 17f)
                p.lineTo(19f, 17f)
                c.drawPath(p, stroke)
            }
            SETTINGS, TUNE -> {
                c.drawLine(4f, 6f, 20f, 6f, stroke)
                c.drawCircle(9f, 6f, 2.5f, fill)
                c.drawLine(4f, 12f, 20f, 12f, stroke)
                c.drawCircle(15f, 12f, 2.5f, fill)
                c.drawLine(4f, 18f, 20f, 18f, stroke)
                c.drawCircle(7f, 18f, 2.5f, fill)
            }
            SEARCH -> {
                c.drawCircle(11f, 11f, 6.5f, stroke)
                c.drawLine(16f, 16f, 20f, 20f, stroke)
            }
            SHARE -> {
                c.drawLine(12f, 15f, 12f, 3f, stroke)
                p.moveTo(7f, 8f)
                p.lineTo(12f, 3f)
                p.lineTo(17f, 8f)
                p.moveTo(5f, 13f)
                p.lineTo(5f, 19f)
                p.quadTo(5f, 21f, 7f, 21f)
                p.lineTo(17f, 21f)
                p.quadTo(19f, 21f, 19f, 19f)
                p.lineTo(19f, 13f)
                c.drawPath(p, stroke)
            }
            BIKE -> {
                c.drawCircle(6f, 17f, 3.2f, stroke)
                c.drawCircle(18f, 17f, 3.2f, stroke)
                p.moveTo(6f, 17f)
                p.lineTo(10.5f, 10.5f)
                p.lineTo(14.5f, 10.5f)
                p.lineTo(18f, 17f)
                p.moveTo(13f, 6.5f)
                p.lineTo(16f, 6.5f)
                p.lineTo(18f, 10.5f)
                c.drawPath(p, stroke)
            }
            FUEL -> {
                p.moveTo(4f, 21f)
                p.lineTo(4f, 5f)
                p.quadTo(4f, 3f, 6f, 3f)
                p.lineTo(12f, 3f)
                p.quadTo(14f, 3f, 14f, 5f)
                p.lineTo(14f, 21f)
                p.moveTo(3f, 21f)
                p.lineTo(15f, 21f)
                p.moveTo(14f, 9f)
                p.lineTo(16f, 9f)
                p.quadTo(18f, 9f, 18f, 11f)
                p.lineTo(18f, 16f)
                p.quadTo(18f, 17.5f, 19.5f, 17.5f)
                p.quadTo(21f, 17.5f, 21f, 16f)
                p.lineTo(21f, 8f)
                p.lineTo(18f, 5f)
                p.moveTo(7f, 8f)
                p.lineTo(11f, 8f)
                c.drawPath(p, stroke)
            }
            CHEVRON -> {
                p.moveTo(9f, 6f)
                p.lineTo(15f, 12f)
                p.lineTo(9f, 18f)
                c.drawPath(p, stroke)
            }
            CHECK -> {
                stroke.strokeWidth = 3f
                p.moveTo(5f, 12f)
                p.lineTo(10f, 17f)
                p.lineTo(20f, 7f)
                c.drawPath(p, stroke)
            }
            TRASH -> {
                p.moveTo(4f, 7f)
                p.lineTo(20f, 7f)
                p.moveTo(9f, 7f)
                p.lineTo(9f, 4f)
                p.lineTo(15f, 4f)
                p.lineTo(15f, 7f)
                p.moveTo(6f, 7f)
                p.lineTo(7f, 20f)
                p.lineTo(17f, 20f)
                p.lineTo(18f, 7f)
                p.moveTo(10f, 11f)
                p.lineTo(10f, 16f)
                p.moveTo(14f, 11f)
                p.lineTo(14f, 16f)
                c.drawPath(p, stroke)
            }
            SENSORS -> {
                c.drawCircle(12f, 15f, 2f, fill)
                r.set(7f, 9f, 17f, 19f)
                c.drawArc(r, 210f, 120f, false, stroke)
                r.set(4f, 5f, 20f, 21f)
                c.drawArc(r, 210f, 120f, false, stroke)
            }
            PERSON -> {
                c.drawCircle(12f, 8f, 3.8f, stroke)
                p.moveTo(5f, 20f)
                p.quadTo(5f, 15f, 12f, 15f)
                p.quadTo(19f, 15f, 19f, 20f)
                c.drawPath(p, stroke)
            }
            BOLT -> {
                p.moveTo(13f, 3f)
                p.lineTo(6f, 13f)
                p.lineTo(12f, 13f)
                p.lineTo(11f, 21f)
                p.lineTo(18f, 11f)
                p.lineTo(12f, 11f)
                p.close()
                c.drawPath(p, fill)
            }
            ECO -> {
                p.moveTo(4f, 20f)
                p.quadTo(4f, 8f, 14f, 4f)
                p.quadTo(20f, 10f, 20f, 20f)
                p.lineTo(4f, 20f)
                p.moveTo(4f, 20f)
                p.quadTo(10f, 14f, 15f, 10f)
                c.drawPath(p, stroke)
            }
            SYNC -> {
                r.set(5f, 5f, 19f, 19f)
                c.drawArc(r, 45f, 240f, false, stroke)
                p.moveTo(18f, 5f)
                p.lineTo(19f, 10f)
                p.lineTo(14f, 9f)
                c.drawPath(p, stroke)
            }
            TIMER -> {
                c.drawCircle(12f, 12f, 8f, stroke)
                c.drawLine(12f, 7f, 12f, 12f, stroke)
                c.drawLine(12f, 12f, 16f, 12f, stroke)
            }
            BACK -> {
                c.drawLine(5f, 12f, 19f, 12f, stroke)
                p.moveTo(11f, 6f)
                p.lineTo(5f, 12f)
                p.lineTo(11f, 18f)
                c.drawPath(p, stroke)
            }
            DOWNLOAD -> {
                c.drawLine(12f, 4f, 12f, 15f, stroke)
                p.moveTo(7f, 10f)
                p.lineTo(12f, 15f)
                p.lineTo(17f, 10f)
                p.moveTo(5f, 19f)
                p.lineTo(19f, 19f)
                c.drawPath(p, stroke)
            }
            PIN -> {
                c.drawCircle(12f, 8.5f, 4.5f, stroke)
                c.drawLine(12f, 13f, 12f, 20f, stroke)
                c.drawCircle(12f, 8.5f, 1.5f, fill)
            }
            ROUTE -> {
                c.drawCircle(6f, 18f, 2.5f, stroke)
                c.drawCircle(18f, 6f, 2.5f, stroke)
                p.moveTo(6f, 15.5f)
                p.cubicTo(6f, 10f, 18f, 14f, 18f, 8.5f)
                c.drawPath(p, stroke)
            }
            BLUETOOTH -> {
                p.moveTo(7f, 7f)
                p.lineTo(17f, 17f)
                p.lineTo(12f, 22f)
                p.lineTo(12f, 2f)
                p.lineTo(17f, 7f)
                p.lineTo(7f, 17f)
                c.drawPath(p, stroke)
            }
            else -> {
                c.drawCircle(12f, 12f, 6f, stroke)
            }
        }
        c.restore()
    }
}
