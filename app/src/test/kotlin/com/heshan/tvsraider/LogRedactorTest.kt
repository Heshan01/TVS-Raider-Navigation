package com.heshan.tvsraider

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for LogRedactor sensitive place and road name redaction.
 */
class LogRedactorTest {

    @Test
    fun testRedactsStreetNamesPreservesTurnInstructions() {
        val logLine = "MAPS title=[250 m] text=[Turn left onto Galle Road] sub=[18 min · 7.4 km · 11:42 PM ETA] big=[]"
        val redacted = LogRedactor.redactLine(logLine)

        // Street name "Galle Road" should become [name]
        // "Turn", "left", "onto", units "m", "km", "min", "ETA" are preserved
        assertEquals(
            "MAPS title=[250 m] text=[Turn left onto [name]] sub=[18 min · 7.4 km · 11:42 PM ETA] big=[]",
            redacted
        )
    }

    @Test
    fun testRedactsPlaceNames() {
        val logLine = "MAPS title=[500 m] text=[Head toward Kandy City Center] sub=[30 min · 12 km] big=[]"
        val redacted = LogRedactor.redactLine(logLine)

        assertEquals(
            "MAPS title=[500 m] text=[Head toward [name]] sub=[30 min · 12 km] big=[]",
            redacted
        )
    }

    @Test
    fun testNonMapsLinesUnchanged() {
        val bikeLine = "FROM BIKE 5A 10 2A 01 E2 40 05 00 00 00 00 00 00 00 00 00 00 00 43 FF"
        val redacted = LogRedactor.redactLine(bikeLine)
        assertEquals(bikeLine, redacted)
    }
}
