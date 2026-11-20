package com.heshan.tvsraider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for MapsParser parsing logic and edge cases.
 */
class MapsParserTest {

    @Before
    fun setUp() {
        MapsParser.reset()
    }

    @Test
    fun testStandardNavigationNotification() {
        val nav = MapsParser.parse(
            title = "250 m",
            text = "Turn left onto Galle Rd",
            sub = "18 min · 7.4 km · 11:42 PM ETA",
            big = null,
            iconPicto = -1
        )
        assertNotNull(nav)
        assertEquals(250, nav!!.distNextM)
        assertEquals(18, nav.minutesLeft)
        assertEquals(7400, nav.distLeftM)
        assertEquals(TvsProtocol.P_LEFT, nav.picto)
        assertEquals("Turn left", nav.text)
    }

    @Test
    fun testMilesAndHoursFormat() {
        val nav = MapsParser.parse(
            title = "0.5 mi",
            text = "Turn right onto High Street",
            sub = "1 hr 15 min · 45 mi · 4:15 PM ETA",
            big = null,
            iconPicto = -1
        )
        assertNotNull(nav)
        assertEquals(805, nav!!.distNextM) // 0.5 * 1609.34 = 804.67 -> 805
        assertEquals(75, nav.minutesLeft) // 1 hr + 15 min = 75
        assertEquals(72420, nav.distLeftM) // 45 * 1609.34 = 72420
        assertEquals(TvsProtocol.P_RIGHT, nav.picto)
    }

    @Test
    fun testFeetAndYards() {
        val navFeet = MapsParser.parse("500 ft", "Keep left", "5 min · 1.2 km", null, -1)
        assertNotNull(navFeet)
        assertEquals(152, navFeet!!.distNextM) // 500 * 0.3048 = 152.4 -> 152
        assertEquals(TvsProtocol.P_KEEP_LEFT, navFeet.picto)

        val navYards = MapsParser.parse("100 yd", "Sharp right", "2 min · 500 m", null, -1)
        assertNotNull(navYards)
        assertEquals(91, navYards!!.distNextM) // 100 * 0.9144 = 91.44 -> 91
        assertEquals(TvsProtocol.P_SHARP_RIGHT, navYards.picto)
    }

    @Test
    fun testNonBreakingSpacesHandling() {
        // Google Maps uses non-breaking spaces: U+00A0 and U+202F
        val title = "20\u00A0m"
        val text = "Continue straight"
        val sub = "12\u00A0min · 4.5\u00A0km · 6:52\u202FAM ETA"

        val nav = MapsParser.parse(title, text, sub, null, -1)
        assertNotNull(nav)
        assertEquals(20, nav!!.distNextM)
        assertEquals(12, nav.minutesLeft)
        assertEquals(4500, nav.distLeftM)
        assertEquals(TvsProtocol.P_STRAIGHT, nav.picto)
    }

    @Test
    fun testNoDistanceAfterTurn_ReturnsNull_ToPreventZeroDrop() {
        // Right after a turn Maps briefly shows road name with no distance
        // Sending 0m makes the bike cluster close navigation panel; parser must return null.
        val nav = MapsParser.parse(
            title = "Galle Road",
            text = "Toward Colombo",
            sub = "15 min · 6.0 km",
            big = null,
            iconPicto = -1
        )
        assertNull(nav)
    }

    @Test
    fun testRoundaboutExitParsing() {
        val nav = MapsParser.parse(
            title = "150 m",
            text = "At the roundabout, take the 2nd exit",
            sub = "10 min · 3.5 km",
            big = null,
            iconPicto = -1
        )
        assertNotNull(nav)
        assertEquals(150, nav!!.distNextM)
        assertEquals(TvsProtocol.P_RB_STRAIGHT, nav.picto) // 2nd exit -> straight
    }

    @Test
    fun testDestinationArrival() {
        val nav = MapsParser.parse(
            title = "You have arrived",
            text = "Your destination is on the left",
            sub = "0 min · 0 m",
            big = null,
            iconPicto = -1
        )
        assertNotNull(nav)
        assertEquals(TvsProtocol.P_ARRIVE, nav!!.picto)
        assertEquals("Arrived", nav.text)
    }

    @Test
    fun testRerouting() {
        val nav = MapsParser.parse(
            title = "Rerouting...",
            text = "Finding fastest route",
            sub = "",
            big = null,
            iconPicto = -1
        )
        assertNotNull(nav)
        assertEquals(TvsProtocol.P_REROUTING, nav!!.picto)
        assertEquals("Rerouting", nav.text)
    }
}
