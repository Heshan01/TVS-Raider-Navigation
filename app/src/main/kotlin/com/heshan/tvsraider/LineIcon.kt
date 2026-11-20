package com.heshan.tvsraider

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View

/**
 * 2px vector line icons rendered mathematically on a 24x24 coordinate grid.
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
            HOME -> {
                p.moveTo(3f, 11f)
                p.lineTo(12f, 3.5f)
                p.lineTo(21f, 11f)
                p.lineTo(19f, 11f)
                p.lineTo(19f, 20f)
                p.lineTo(14.5f, 20f)
                p.lineTo(14.5f, 14.5f)
                p.lineTo(9.5f, 14.5f)
                p.lineTo(9.5f, 20f)
                p.lineTo(5f, 20f)
                p.lineTo(5f, 11f)
                p.close()
                c.drawPath(p, fill)
            }
            OVERVIEW -> {
                r.set(3f, 4f, 21f, 22f)
                c.drawArc(r, 150f, 240f, false, stroke)
                c.drawLine(12f, 13f, 16.5f, 8.5f, stroke)
                c.drawCircle(12f, 13f, 1.4f, fill)
            }
            LOG -> {
                r.set(5f, 3f, 19f, 21f)
                c.drawRoundRect(r, 2.5f, 2.5f, stroke)
                c.drawLine(9f, 8f, 15f, 8f, stroke)
                c.drawLine(9f, 12f, 15f, 12f, stroke)
                c.drawLine(9f, 16f, 12.5f, 16f, stroke)
            }
            SETTINGS -> {
                for (i in 0 until 8) {
                    c.save()
                    c.rotate(45f * i, 12f, 12f)
                    r.set(10.4f, 2.6f, 13.6f, 6.6f)
                    c.drawRoundRect(r, 1f, 1f, fill)
                    c.restore()
                }
                stroke.strokeWidth = 2.2f
                c.drawCircle(12f, 12f, 6.4f, stroke)
                stroke.strokeWidth = 2f
                c.drawCircle(12f, 12f, 2.6f, stroke)
            }
            SEARCH -> {
                c.drawCircle(11f, 11f, 7f, stroke)
                c.drawLine(16.5f, 16.5f, 20f, 20f, stroke)
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
                c.drawCircle(6f, 17f, 3f, stroke)
                c.drawCircle(18f, 17f, 3f, stroke)
                p.moveTo(6f, 17f)
                p.lineTo(10f, 10f)
                p.lineTo(15f, 10f)
                p.lineTo(18f, 17f)
                p.moveTo(13f, 6f)
                p.lineTo(16f, 6f)
                p.lineTo(18f, 10f)
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
            SEND -> {
                p.moveTo(3f, 11f)
                p.lineTo(21f, 3f)
                p.lineTo(13f, 21f)
                p.lineTo(11f, 13f)
                p.close()
                c.drawPath(p, stroke)
            }
            CHEVRON -> {
                p.moveTo(9f, 6f)
                p.lineTo(15f, 12f)
                p.lineTo(9f, 18f)
                c.drawPath(p, stroke)
            }
            TREND_UP -> {
                p.moveTo(3f, 17f)
                p.lineTo(9f, 11f)
                p.lineTo(13f, 15f)
                p.lineTo(21f, 7f)
                p.moveTo(15f, 7f)
                p.lineTo(21f, 7f)
                p.lineTo(21f, 13f)
                c.drawPath(p, stroke)
            }
            TREND_DOWN -> {
                p.moveTo(3f, 7f)
                p.lineTo(9f, 13f)
                p.lineTo(13f, 9f)
                p.lineTo(21f, 17f)
                p.moveTo(15f, 17f)
                p.lineTo(21f, 17f)
                p.lineTo(21f, 11f)
                c.drawPath(p, stroke)
            }
            CODE -> {
                p.moveTo(8f, 7f)
                p.lineTo(3f, 12f)
                p.lineTo(8f, 17f)
                p.moveTo(16f, 7f)
                p.lineTo(21f, 12f)
                p.lineTo(16f, 17f)
                p.moveTo(14f, 4f)
                p.lineTo(10f, 20f)
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
            CHECK -> {
                stroke.strokeWidth = 3.2f
                p.moveTo(5f, 12f)
                p.lineTo(10f, 17f)
                p.lineTo(20f, 7f)
                c.drawPath(p, stroke)
            }
        }
        c.restore()
    }
}
