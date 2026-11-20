package com.heshan.tvsraider

import java.nio.charset.StandardCharsets
import java.util.Calendar

/**
 * TVS SmartXonnect (N360 / Raider) navigation frames. Every frame is 20 bytes and ends with 0xFF.
 */
object TvsProtocol {
    // Turn arrow ("pictogram") codes understood by the cluster.
    const val P_LEFT = 0
    const val P_SHARP_LEFT = 1
    const val P_SLIGHT_LEFT = 2
    const val P_RIGHT = 3
    const val P_SHARP_RIGHT = 4
    const val P_SLIGHT_RIGHT = 5
    const val P_UTURN_LEFT = 6
    const val P_STRAIGHT = 7
    const val P_ARRIVE = 8
    const val P_KEEP_LEFT = 15
    const val P_KEEP_RIGHT = 16
    const val P_LEAVE_HWY_LEFT = 34
    const val P_ENTER_HWY_RIGHT = 35
    const val P_LEAVE_HWY_RIGHT = 37
    const val P_UTURN_RIGHT = 41
    const val P_REROUTING = 72

    /**
     * Roundabouts. Every code is a ring entered from the bottom, and the code only says which
     * DIRECTION the exit arrow points, in 45 degree steps clockwise round the clock face.
     */
    const val P_RB_SHARP_LEFT = 65   // down-left
    const val P_RB_LEFT = 66         // left (9 o'clock)
    const val P_RB_SLIGHT_LEFT = 67  // up-left
    const val P_RB_STRAIGHT = 68     // straight up (12 o'clock)
    const val P_RB_SLIGHT_RIGHT = 69 // up-right
    const val P_RB_RIGHT = 70        // right (3 o'clock)
    const val P_RB_SHARP_RIGHT = 71  // down-right

    fun isRoundabout(p: Int): Boolean = p in P_RB_SHARP_LEFT..P_RB_SHARP_RIGHT

    /** Exit direction drawn by a roundabout code (degrees: 0 = right, 90 = up, 180 = left). */
    fun roundaboutAngle(p: Int): Int = 90 - (p - P_RB_STRAIGHT) * 45

    /**
     * Code for a roundabout whose exit leaves in direction a (degrees, 0 = right, 90 = up,
     * 180 = left, 270 = back down). Going all the way round (U-turn) there is no "down" picture:
     * show the last exit before the entry, which is down-right for clockwise (left-hand traffic).
     */
    fun roundaboutCode(angle: Double, leftHandTraffic: Boolean): Int {
        var a = angle
        while (a < -90) a += 360.0
        while (a >= 270) a -= 360.0
        if (a > 247.5 || a < -67.5) return if (leftHandTraffic) P_RB_SHARP_RIGHT else P_RB_SHARP_LEFT
        val steps = Math.round((90.0 - a) / 45.0).toInt()
        return Math.max(P_RB_SHARP_LEFT, Math.min(P_RB_SHARP_RIGHT, P_RB_STRAIGHT + steps))
    }

    /** One navigation update. */
    data class Nav(
        val distNextM: Int,   // metres to the next turn
        val minutesLeft: Int, // minutes to destination
        val distLeftM: Int,   // metres to destination
        val picto: Int,       // arrow code
        val text: String?     // short instruction, max 17 ASCII chars on the cluster
    ) {
        override fun toString(): String {
            return "\"$text\" picto=$picto next=${distNextM}m left=${distLeftM}m ${minutesLeft}min"
        }
    }

    /** Status byte (index 14). */
    fun status(n: Nav): Int {
        if (n.picto == P_ARRIVE) return 8
        if (n.picto == P_REROUTING) return 0
        if (isRoundabout(n.picto)) return if (n.distNextM in 65..1000) 1 else 0
        val def = if (n.picto == P_STRAIGHT) 5 else 0
        val t = n.text?.lowercase() ?: ""
        if (n.distNextM < 65) {
            if (t.contains("left")) return 3
            if (t.contains("right")) return 4
            return def
        }
        if (n.distNextM <= 1000) return 1
        return def
    }

    fun asciiText(s: String?): ByteArray {
        if (s == null) return ByteArray(0)
        val sb = java.lang.StringBuilder()
        for (c in s.toCharArray()) {
            if (c.code in 32..126) sb.append(c)
        }
        val raw = sb.toString().toByteArray(StandardCharsets.US_ASCII)
        if (raw.size > 17) {
            val cut = ByteArray(17)
            System.arraycopy(raw, 0, cut, 0, 17)
            return cut
        }
        return raw
    }

    /** Frame 1: 0x5A 0x4E ... (distances, arrow, status). */
    fun controlFrame(n: Nav): ByteArray {
        val f = ByteArray(20)
        val dNext = if (n.picto == P_ARRIVE || n.picto == P_REROUTING) 0 else clamp(n.distNextM, 0xFFFF)
        val mins = clamp(n.minutesLeft, 0xFFFF)
        val dLeft = clamp(n.distLeftM, 0xFFFFFF)
        f[0] = 0x5A.toByte()
        f[1] = 0x4E.toByte()
        f[2] = (dNext shr 8).toByte()
        f[3] = dNext.toByte()
        f[4] = (mins shr 8).toByte()
        f[5] = mins.toByte()
        f[6] = (dLeft shr 16).toByte()
        f[7] = (dLeft shr 8).toByte()
        f[8] = dLeft.toByte()
        f[9] = n.picto.toByte()
        f[10] = (if (asciiText(n.text).size > 17) 2 else 1).toByte()
        f[11] = 1.toByte()
        f[12] = 0.toByte()
        f[13] = 0.toByte()
        f[14] = status(n).toByte()
        // 15..18 stay 0
        f[19] = 0xFF.toByte()
        return f
    }

    /** Frame 2: 0x5B 0x4F + instruction text. */
    fun textFrame(n: Nav): ByteArray {
        val f = ByteArray(20)
        f[0] = 0x5B.toByte()
        f[1] = 0x4F.toByte()
        val t = asciiText(n.text)
        System.arraycopy(t, 0, f, 2, t.size)
        f[19] = 0xFF.toByte()
        return f
    }

    /** Second copy TVS sends: checksum in byte[len-2], then XOR bytes 2..len-2 with 0xEA. */
    fun protectedCopy(plain: ByteArray): ByteArray {
        val len = plain.size
        val out = plain.clone()
        var sum = 0
        for (i in 0 until len - 2) sum += plain[i].toInt() and 0xFF
        out[len - 2] = (255 - (sum % 256)).toByte()
        out[len - 1] = 0xFF.toByte()
        for (i in 2..len - 2) out[i] = (out[i].toInt() xor 0xEA).toByte()
        return out
    }

    /** All writes for one update, in TVS order: control (plain, protected), text (plain, protected). */
    fun frames(n: Nav, plain: Boolean, prot: Boolean): List<ByteArray> {
        val list = ArrayList<ByteArray>()
        for (f in arrayOf(controlFrame(n), textFrame(n))) {
            if (plain) list.add(f)
            if (prot) list.add(protectedCopy(f))
        }
        return list
    }

    /**
     * Phone status 0x5B 0x4A, sent every 2 s like TVS Connect: byte 2 = signal and battery bars as
     * two hex digits, 5 = 2, 6-9 = clock (12-hour, midnight is 12, then 0 AM / 1 PM), 12-14 = date
     * (day, month, year % 100). No checksum.
     */
    fun phoneStatusFrame(batteryBars: Int, cal: Calendar): ByteArray {
        val f = ByteArray(20)
        f[0] = 0x5B.toByte()
        f[1] = 0x4A.toByte()
        f[2] = (0x50 or clamp(batteryBars, 5)).toByte()
        f[5] = 2.toByte()
        val hour = cal.get(Calendar.HOUR)
        f[6] = (if (hour == 0) 12 else hour).toByte()
        f[7] = cal.get(Calendar.MINUTE).toByte()
        f[8] = cal.get(Calendar.SECOND).toByte()
        f[9] = (if (cal.get(Calendar.AM_PM) == Calendar.AM) 0 else 1).toByte()
        f[12] = cal.get(Calendar.DAY_OF_MONTH).toByte()
        f[13] = (cal.get(Calendar.MONTH) + 1).toByte()
        f[14] = (cal.get(Calendar.YEAR) % 100).toByte()
        f[19] = 0xFF.toByte()
        return f
    }

    fun clamp(v: Int, max: Int): Int = Math.max(0, Math.min(v, max))

    fun hex(b: ByteArray): String {
        val s = java.lang.StringBuilder()
        for (x in b) s.append(String.format("%02X ", x.toInt() and 0xFF))
        return s.toString().trim()
    }
}
