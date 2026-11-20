package com.heshan.tvsraider

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View

/**
 * Draws a TVS pictogram (turn arrow, roundabout, U-turn, flag ...) with vector paths.
 */
class ArrowView(c: Context) : View(c) {
    private var picto: Int = -1
    private var color: Int = -0x1
    private var dimColor: Int = 0x40FFFFFF

    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
        strokeJoin = Paint.Join.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val dim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    fun setPicto(p: Int) {
        if (p != picto) {
            picto = p
            invalidate()
        }
    }

    fun setColors(main: Int, faint: Int) {
        color = main
        dimColor = faint
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        val s = Math.min(width, height).toFloat()
        val ox = (width - s) / 2f
        val oy = (height - s) / 2f
        c.save()
        c.translate(ox, oy)
        c.scale(s / 100f, s / 100f)

        stroke.color = color
        fill.color = color
        dim.color = dimColor
        stroke.strokeWidth = 11f
        dim.strokeWidth = 7f

        when (picto) {
            TvsProtocol.P_STRAIGHT -> straight(c)
            TvsProtocol.P_LEFT -> turn(c, 60f, 180f)
            TvsProtocol.P_RIGHT -> turn(c, 40f, 0f)
            TvsProtocol.P_SLIGHT_LEFT, TvsProtocol.P_KEEP_LEFT, TvsProtocol.P_LEAVE_HWY_LEFT ->
                turn(c, 58f, 135f)
            TvsProtocol.P_SLIGHT_RIGHT, TvsProtocol.P_KEEP_RIGHT, TvsProtocol.P_LEAVE_HWY_RIGHT,
            TvsProtocol.P_ENTER_HWY_RIGHT ->
                turn(c, 42f, 45f)
            TvsProtocol.P_SHARP_LEFT -> turn(c, 78f, 225f, 24f, 34f)
            TvsProtocol.P_SHARP_RIGHT -> turn(c, 22f, -45f, 24f, 34f)
            TvsProtocol.P_UTURN_LEFT -> uturn(c, true)
            TvsProtocol.P_UTURN_RIGHT -> uturn(c, false)
            TvsProtocol.P_RB_SHARP_LEFT, TvsProtocol.P_RB_LEFT, TvsProtocol.P_RB_SLIGHT_LEFT,
            TvsProtocol.P_RB_STRAIGHT, TvsProtocol.P_RB_SLIGHT_RIGHT, TvsProtocol.P_RB_RIGHT,
            TvsProtocol.P_RB_SHARP_RIGHT ->
                roundabout(c, TvsProtocol.roundaboutAngle(picto).toFloat(), MapsParser.leftHandTraffic)
            TvsProtocol.P_ARRIVE -> flag(c)
            TvsProtocol.P_REROUTING -> reroute(c)
            else -> idle(c)
        }
        c.restore()
    }

    private fun straight(c: Canvas) {
        val p = Path()
        p.moveTo(50f, 92f)
        p.lineTo(50f, 34f)
        c.drawPath(p, stroke)
        head(c, 50f, 12f, 90f)
    }

    private fun turn(c: Canvas, stemX: Float, angle: Float) {
        turn(c, stemX, angle, 50f, if (angle == 0f || angle == 180f) 24f else 22f)
    }

    private fun turn(c: Canvas, stemX: Float, angle: Float, bendY: Float, len: Float) {
        val p = Path()
        p.moveTo(stemX, 92f)
        p.lineTo(stemX, bendY)
        val r = Math.toRadians(angle.toDouble())
        val ex = (stemX + Math.cos(r) * len).toFloat()
        val ey = (bendY - Math.sin(r) * len).toFloat()
        p.lineTo(ex, ey)
        c.drawPath(p, stroke)
        head(c, (ex + Math.cos(r) * 20).toFloat(), (ey - Math.sin(r) * 20).toFloat(), angle)
    }

    private fun head(c: Canvas, tx: Float, ty: Float, angle: Float) {
        val r = Math.toRadians(angle.toDouble())
        val dx = Math.cos(r).toFloat()
        val dy = (-Math.sin(r)).toFloat()
        val bx = tx - dx * 22f
        val by = ty - dy * 22f
        val nx = -dy
        val ny = dx
        val h = Path()
        h.moveTo(tx, ty)
        h.lineTo(bx + nx * 16f, by + ny * 16f)
        h.lineTo(bx - nx * 16f, by - ny * 16f)
        h.close()
        c.drawPath(h, fill)
    }

    private fun uturn(c: Canvas, left: Boolean) {
        val a = if (left) 66f else 34f
        val b = if (left) 34f else 66f
        val p = Path()
        p.moveTo(a, 92f)
        p.lineTo(a, 40f)
        val arc = RectF(Math.min(a, b), 24f, Math.max(a, b), 56f)
        p.arcTo(arc, if (left) 0f else 180f, if (left) -180f else 180f)
        p.lineTo(b, 56f)
        c.drawPath(p, stroke)
        head(c, b, 78f, 270f)
    }

    private fun roundabout(c: Canvas, exitAngle: Float, clockwise: Boolean) {
        val cx = 50f
        val cy = 46f
        val r = 17f
        val ring = RectF(cx - r, cy - r, cx + r, cy + r)
        c.drawOval(ring, dim)
        val p = Path()
        p.moveTo(cx, 94f)
        p.lineTo(cx, cy + r)
        val exitAndroid = -exitAngle
        var sweep = if (clockwise) (exitAndroid - 90f + 360f) % 360f else -((90f - exitAndroid + 360f) % 360f)
        if (sweep == 0f) sweep = if (clockwise) 360f else -360f
        p.arcTo(ring, 90f, sweep)
        val er = Math.toRadians(exitAngle.toDouble())
        val sx = (cx + Math.cos(er) * r).toFloat()
        val sy = (cy - Math.sin(er) * r).toFloat()
        val ex = (cx + Math.cos(er) * (r + 12)).toFloat()
        val ey = (cy - Math.sin(er) * (r + 12)).toFloat()
        p.lineTo(sx, sy)
        p.lineTo(ex, ey)
        c.drawPath(p, stroke)
        head(c, (ex + Math.cos(er) * 18).toFloat(), (ey - Math.sin(er) * 18).toFloat(), exitAngle)
    }

    private fun flag(c: Canvas) {
        val pole = Path()
        pole.moveTo(30f, 92f)
        pole.lineTo(30f, 12f)
        stroke.strokeWidth = 8f
        c.drawPath(pole, stroke)
        val f = Path()
        f.moveTo(34f, 14f)
        f.lineTo(78f, 26f)
        f.lineTo(34f, 50f)
        f.close()
        c.drawPath(f, fill)
    }

    private fun reroute(c: Canvas) {
        stroke.strokeWidth = 9f
        val o = RectF(22f, 22f, 78f, 78f)
        c.drawArc(o, 200f, 250f, false, stroke)
        head(c, 28f, 78f, 180f)
    }

    private fun idle(c: Canvas) {
        val p = Path()
        p.moveTo(50f, 12f)
        p.lineTo(80f, 86f)
        p.lineTo(50f, 70f)
        p.lineTo(20f, 86f)
        p.close()
        dim.style = Paint.Style.FILL
        c.drawPath(p, dim)
        dim.style = Paint.Style.STROKE
    }
}
