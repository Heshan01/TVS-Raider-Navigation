package com.heshan.tvsraider

import java.util.Arrays
import java.util.HashSet
import java.util.Locale
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Hides road and place names in logs before sharing for privacy.
 */
object LogRedactor {
    const val NAME: String = "[name]"

    private val KEEP: Set<String> = HashSet(
        Arrays.asList(
            "turn", "left", "right", "slight", "sharp", "keep", "stay", "make", "u-turn", "uturn",
            "continue", "straight", "head", "go", "take", "exit", "exits", "roundabout", "merge",
            "ramp", "fork", "onto", "on", "to", "toward", "towards", "via", "the", "a", "an", "at",
            "and", "then", "of", "in", "for", "your", "you", "will", "be", "is", "are", "have",
            "has", "arrive", "arrived", "arriving", "destination", "drive", "walk", "rerouting",
            "north", "south", "east", "west", "northeast", "northwest", "southeast", "southwest",
            "first", "second", "third", "fourth", "fifth", "sixth",
            "m", "km", "mi", "ft", "min", "mins", "hr", "hrs", "h", "am", "pm", "eta"
        )
    )

    private val FIELD: Pattern = Pattern.compile("\\b(title|text|sub|big)=\\[([^\\]]*)\\]")
    private val WORD: Pattern = Pattern.compile("[\\p{L}\\p{M}][\\p{L}\\p{M}\\p{N}'’-]*")
    private val ORDINAL: Set<String> = HashSet(Arrays.asList("st", "nd", "rd", "th"))
    private val NAME_RUN: Pattern = Pattern.compile(Pattern.quote(NAME) + "(?:[\\s  ]+" + Pattern.quote(NAME) + ")+")

    fun redact(log: String): String {
        val out = java.lang.StringBuilder(log.length)
        for (line in log.split("\n", limit = -1)) {
            if (out.isNotEmpty()) out.append('\n')
            out.append(redactLine(line))
        }
        return out.toString()
    }

    fun redactLine(line: String): String {
        val saved = line.indexOf("Saved icon ")
        if (saved >= 0) return line.substring(0, saved) + "Saved icon " + NAME
        if (!line.contains("=[")) return line
        val m = FIELD.matcher(line)
        val sb = StringBuffer()
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1).orEmpty() + "=[" + redactText(m.group(2) ?: "") + "]"))
        }
        m.appendTail(sb)
        return sb.toString()
    }

    fun redactText(s: String): String {
        val m = WORD.matcher(s)
        val sb = StringBuffer()
        while (m.find()) {
            val w = m.group()
            val lw = w.lowercase(Locale.ROOT)
            val afterDigit = m.start() > 0 && Character.isDigit(s[m.start() - 1])
            val keep = if (KEEP.contains(lw) || (afterDigit && ORDINAL.contains(lw))) w else NAME
            m.appendReplacement(sb, Matcher.quoteReplacement(keep))
        }
        m.appendTail(sb)
        return NAME_RUN.matcher(sb).replaceAll(Matcher.quoteReplacement(NAME))
    }
}
