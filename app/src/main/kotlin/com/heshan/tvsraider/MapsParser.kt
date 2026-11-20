package com.heshan.tvsraider

import java.util.Locale
import java.util.regex.Pattern

/**
 * Turns Google Maps navigation notification text into a TVS navigation update.
 */
object MapsParser {
    private val DIST = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(km|m|mi|ft|yd)\\b", Pattern.CASE_INSENSITIVE)
    private val HOURS = Pattern.compile("(\\d+)\\s*(?:hr|hrs|hour|hours|h)\\b", Pattern.CASE_INSENSITIVE)
    private val MINS = Pattern.compile("(\\d+)\\s*(?:min|mins|minutes)\\b", Pattern.CASE_INSENSITIVE)
    private val EXIT_NUM = Pattern.compile("(\\d)(?:st|nd|rd|th)\\s+exit", Pattern.CASE_INSENSITIVE)

    const val leftHandTraffic: Boolean = true

    private var lastMinutes = -1
    private var lastDistLeft = -1

    fun reset() {
        lastMinutes = -1
        lastDistLeft = -1
    }

    fun parse(title: String?, text: String?, sub: String?, big: String?): TvsProtocol.Nav? {
        return parse(title, text, sub, big, -1)
    }

    fun parse(title: String?, text: String?, sub: String?, big: String?, iconPicto: Int): TvsProtocol.Nav? {
        val t = n(title)
        val tx = n(text)
        val s = n(sub)
        val bg = n(big)
        val main = "$t | $tx | $bg".lowercase(Locale.ROOT)

        var distNext = firstDistance(t)
        if (distNext < 0) distNext = firstDistance(tx)
        if (distNext < 0) distNext = firstDistance(bg)

        var distLeft = firstDistance(s)
        var minutes = minutes(s)

        if (distLeft >= 0) lastDistLeft = distLeft else distLeft = lastDistLeft
        if (minutes >= 0) lastMinutes = minutes else minutes = lastMinutes

        var picto = picto(main)
        val fromWords = picto >= 0 && picto != TvsProtocol.P_STRAIGHT
        if (iconPicto >= 0 && !fromWords) picto = iconPicto
        if (TvsProtocol.isRoundabout(picto) && TvsProtocol.isRoundabout(iconPicto)) picto = iconPicto

        if (!fromWords && distNext >= 0 && distLeft > 0 && firstDistance(s) >= 0
            && distNext >= distLeft && picto != TvsProtocol.P_REROUTING
        ) {
            picto = TvsProtocol.P_ARRIVE
        }
        if (picto < 0 && distNext < 0) return null
        if (picto < 0) picto = TvsProtocol.P_STRAIGHT
        if (distNext < 0 && picto != TvsProtocol.P_ARRIVE && picto != TvsProtocol.P_REROUTING) return null

        return TvsProtocol.Nav(
            Math.max(distNext, 0),
            Math.max(minutes, 0),
            Math.max(distLeft, 0),
            picto,
            label(picto, main)
        )
    }

    fun picto(s: String): Int {
        if (s.contains("arriv") || s.contains("destination") || s.contains("you have reached"))
            return TvsProtocol.P_ARRIVE
        if (s.contains("rerouting") || s.contains("re-routing")) return TvsProtocol.P_REROUTING
        if (s.contains("u-turn") || s.contains("u turn") || s.contains("make a u")) {
            if (s.contains("left")) return TvsProtocol.P_UTURN_LEFT
            if (s.contains("right")) return TvsProtocol.P_UTURN_RIGHT
            return if (leftHandTraffic) TvsProtocol.P_UTURN_RIGHT else TvsProtocol.P_UTURN_LEFT
        }
        if (s.contains("roundabout") || s.contains("traffic circle") || s.contains("rotary")) {
            val exit = roundaboutExit(s)
            if (s.contains("u-turn") || exit >= 4)
                return if (leftHandTraffic) TvsProtocol.P_RB_SHARP_RIGHT else TvsProtocol.P_RB_SHARP_LEFT
            if (exit == 2) return TvsProtocol.P_RB_STRAIGHT
            val towardsLeft = (exit <= 1) == leftHandTraffic
            return if (towardsLeft) TvsProtocol.P_RB_LEFT else TvsProtocol.P_RB_RIGHT
        }
        if (s.contains("sharp left")) return TvsProtocol.P_SHARP_LEFT
        if (s.contains("sharp right")) return TvsProtocol.P_SHARP_RIGHT
        if (s.contains("slight left") || s.contains("bear left")) return TvsProtocol.P_SLIGHT_LEFT
        if (s.contains("slight right") || s.contains("bear right")) return TvsProtocol.P_SLIGHT_RIGHT
        if (s.contains("keep left") || s.contains("stay left") || s.contains("fork left")
            || (s.contains("fork") && s.contains("left"))
        ) return TvsProtocol.P_KEEP_LEFT
        if (s.contains("keep right") || s.contains("stay right") || s.contains("fork right")
            || (s.contains("fork") && s.contains("right"))
        ) return TvsProtocol.P_KEEP_RIGHT
        if (s.contains("exit") || s.contains("ramp")) {
            if (s.contains("left")) return TvsProtocol.P_LEAVE_HWY_LEFT
            if (s.contains("right")) return TvsProtocol.P_LEAVE_HWY_RIGHT
        }
        if (s.contains("turn left") || s.matches(Regex(".*\\bleft\\b.*"))) return TvsProtocol.P_LEFT
        if (s.contains("turn right") || s.matches(Regex(".*\\bright\\b.*"))) return TvsProtocol.P_RIGHT
        if (s.contains("continue") || s.contains("straight") || s.contains("head ")
            || s.contains("merge") || s.contains("toward")
        ) return TvsProtocol.P_STRAIGHT
        return -1
    }

    fun roundaboutExit(s: String): Int {
        val m = EXIT_NUM.matcher(s)
        if (m.find()) return m.group(1)?.toIntOrNull() ?: 1
        val words = arrayOf("first", "second", "third", "fourth", "fifth", "sixth")
        for (i in words.indices) {
            if (s.contains(words[i] + " exit")) return i + 1
        }
        return 1
    }

    fun label(picto: Int, s: String): String {
        return when (picto) {
            TvsProtocol.P_LEFT -> "Turn left"
            TvsProtocol.P_RIGHT -> "Turn right"
            TvsProtocol.P_SHARP_LEFT -> "Sharp left"
            TvsProtocol.P_SHARP_RIGHT -> "Sharp right"
            TvsProtocol.P_SLIGHT_LEFT -> "Slight left"
            TvsProtocol.P_SLIGHT_RIGHT -> "Slight right"
            TvsProtocol.P_KEEP_LEFT -> "Keep left"
            TvsProtocol.P_KEEP_RIGHT -> "Keep right"
            TvsProtocol.P_UTURN_LEFT, TvsProtocol.P_UTURN_RIGHT -> "Make a U-turn"
            TvsProtocol.P_LEAVE_HWY_LEFT -> "Exit left"
            TvsProtocol.P_LEAVE_HWY_RIGHT -> "Exit right"
            TvsProtocol.P_ARRIVE -> "Arrived"
            TvsProtocol.P_REROUTING -> "Rerouting"
            TvsProtocol.P_RB_SHARP_LEFT -> "Rbt sharp left"
            TvsProtocol.P_RB_LEFT -> "Roundabout left"
            TvsProtocol.P_RB_SLIGHT_LEFT -> "Rbt slight left"
            TvsProtocol.P_RB_STRAIGHT -> "Roundabout ahead"
            TvsProtocol.P_RB_SLIGHT_RIGHT -> "Rbt slight right"
            TvsProtocol.P_RB_RIGHT -> "Roundabout right"
            TvsProtocol.P_RB_SHARP_RIGHT -> "Rbt sharp right"
            else -> "Go straight"
        }
    }

    fun firstDistance(s: String): Int {
        val m = DIST.matcher(s)
        if (!m.find()) return -1
        val numStr = m.group(1)?.replace(',', '.') ?: return -1
        val v = numStr.toDoubleOrNull() ?: return -1
        return when (m.group(2)?.lowercase(Locale.ROOT)) {
            "km" -> Math.round(v * 1000).toInt()
            "mi" -> Math.round(v * 1609.34).toInt()
            "ft" -> Math.round(v * 0.3048).toInt()
            "yd" -> Math.round(v * 0.9144).toInt()
            else -> Math.round(v).toInt()
        }
    }

    fun minutes(s: String): Int {
        var total = -1
        val h = HOURS.matcher(s)
        if (h.find()) total = (h.group(1)?.toIntOrNull() ?: 0) * 60
        val m = MINS.matcher(s)
        if (m.find()) total = Math.max(total, 0) + (m.group(1)?.toIntOrNull() ?: 0)
        return total
    }

    private fun n(s: String?): String {
        return s?.replace('\u00A0', ' ')?.replace('\u202F', ' ') ?: ""
    }
}
