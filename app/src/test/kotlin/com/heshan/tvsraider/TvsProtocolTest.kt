package com.heshan.tvsraider

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * Unit tests verifying Golden Test Vectors from Master Prompt Section 3.8.
 */
class TvsProtocolTest {

    private fun parseHex(hex: String): ByteArray {
        val clean = hex.replace(" ", "").trim()
        val len = clean.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(clean[i], 16) shl 4) + Character.digit(clean[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    @Test
    fun testGoldenVector_ControlFrame_LeftTurn() {
        // controlFrame(next=250 m, mins=18, left=7400 m, arrow=LEFT(0), status=1)
        // 5A 4E 00 FA 00 12 00 1C E8 00 01 01 00 00 01 00 00 00 00 FF
        val nav = TvsProtocol.Nav(250, 18, 7400, TvsProtocol.P_LEFT, "Turn left")
        val expected = parseHex("5A 4E 00 FA 00 12 00 1C E8 00 01 01 00 00 01 00 00 00 00 FF")
        val actual = TvsProtocol.controlFrame(nav)
        assertArrayEquals(expected, actual)
    }

    @Test
    fun testGoldenVector_TextFrame_TurnLeft() {
        // textFrame("Turn left")
        // 5B 4F 54 75 72 6E 20 6C 65 66 74 00 00 00 00 00 00 00 00 FF
        val nav = TvsProtocol.Nav(250, 18, 7400, TvsProtocol.P_LEFT, "Turn left")
        val expected = parseHex("5B 4F 54 75 72 6E 20 6C 65 66 74 00 00 00 00 00 00 00 00 FF")
        val actual = TvsProtocol.textFrame(nav)
        assertArrayEquals(expected, actual)
    }

    @Test
    fun testGoldenVector_ControlFrame_RightTurn_CloseDistance() {
        // controlFrame(next=40, 18, 7190, arrow=RIGHT(3), status=4)  // <65 m and text has "right"
        // 5A 4E 00 28 00 12 00 1C 16 03 01 01 00 00 04 00 00 00 00 FF
        val nav = TvsProtocol.Nav(40, 18, 7190, TvsProtocol.P_RIGHT, "Turn right")
        val expected = parseHex("5A 4E 00 28 00 12 00 1C 16 03 01 01 00 00 04 00 00 00 00 FF")
        val actual = TvsProtocol.controlFrame(nav)
        assertArrayEquals(expected, actual)
    }

    @Test
    fun testGoldenVector_ControlFrame_Arrive() {
        // controlFrame(0, 5, 0, ARRIVE(8), status=8)
        // 5A 4E 00 00 00 05 00 00 00 08 01 01 00 00 08 00 00 00 00 FF
        val nav = TvsProtocol.Nav(0, 5, 0, TvsProtocol.P_ARRIVE, "Arrived")
        val expected = parseHex("5A 4E 00 00 00 05 00 00 00 08 01 01 00 00 08 00 00 00 00 FF")
        val actual = TvsProtocol.controlFrame(nav)
        assertArrayEquals(expected, actual)
    }

    @Test
    fun testGoldenVector_ControlFrame_Rerouting() {
        // controlFrame(0, 0, 0, REROUTING(72), status=0)
        // 5A 4E 00 00 00 00 00 00 00 48 01 01 00 00 00 00 00 00 00 FF
        val nav = TvsProtocol.Nav(0, 0, 0, TvsProtocol.P_REROUTING, "Rerouting")
        val expected = parseHex("5A 4E 00 00 00 00 00 00 00 48 01 01 00 00 00 00 00 00 00 FF")
        val actual = TvsProtocol.controlFrame(nav)
        assertArrayEquals(expected, actual)
    }

    @Test
    fun testGoldenVector_ControlFrame_RoundaboutStraight() {
        // controlFrame(500, 12, 3000, RB_STRAIGHT(68), status=1)
        // 5A 4E 01 F4 00 0C 00 0B B8 44 01 01 00 00 01 00 00 00 00 FF
        val nav = TvsProtocol.Nav(500, 12, 3000, TvsProtocol.P_RB_STRAIGHT, "Roundabout ahead")
        val expected = parseHex("5A 4E 01 F4 00 0C 00 0B B8 44 01 01 00 00 01 00 00 00 00 FF")
        val actual = TvsProtocol.controlFrame(nav)
        assertArrayEquals(expected, actual)
    }

    @Test
    fun testGoldenVector_PhoneStatusFrame() {
        // phoneStatus(batteryBars=4, 2026-11-20 14:05:09 local)
        // 5B 4A 54 00 00 02 02 05 09 01 00 00 14 0B 1A 00 00 00 00 FF
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, 2026)
        cal.set(Calendar.MONTH, Calendar.NOVEMBER) // 10 (0-indexed) -> Nov
        cal.set(Calendar.DAY_OF_MONTH, 20)
        cal.set(Calendar.HOUR_OF_DAY, 14) // 2 PM
        cal.set(Calendar.MINUTE, 5)
        cal.set(Calendar.SECOND, 9)

        val expected = parseHex("5B 4A 54 00 00 02 02 05 09 01 00 00 14 0B 1A 00 00 00 00 FF")
        val actual = TvsProtocol.phoneStatusFrame(4, cal)
        assertArrayEquals(expected, actual)
    }

    @Test
    fun testGoldenVector_TelemetryDecode() {
        // telemetry decode: speed 42, odo 12345.6 km (123456 = 0x01E240), fuel 5, valid checksum
        // 5A 10 2A 01 E2 40 05 00 00 00 00 00 00 00 00 00 00 00 43 FF
        val frame = parseHex("5A 10 2A 01 E2 40 05 00 00 00 00 00 00 00 00 00 00 00 43 FF")
        val ok = BikeService.decodeTelemetry(frame)
        assertTrue(ok)
        assertEquals(42, BikeService.speedKmh)
        assertEquals(123456, BikeService.odoTenths)

        // Invalid checksum must be rejected
        val badFrame = frame.clone()
        badFrame[18] = 0x99.toByte()
        val badOk = BikeService.decodeTelemetry(badFrame)
        assertFalse(badOk)
    }

    @Test
    fun testGoldenVector_ProtectedCopy() {
        // protectedCopy(controlFrame(250,18,7400,LEFT,1))
        // 5A 4E EA 10 EA F8 EA F6 02 EA EB EB EA EA EB EA EA EA AE FF
        val plain = parseHex("5A 4E 00 FA 00 12 00 1C E8 00 01 01 00 00 01 00 00 00 00 FF")
        val expected = parseHex("5A 4E EA 10 EA F8 EA F6 02 EA EB EB EA EA EB EA EA EA AE FF")
        val actual = TvsProtocol.protectedCopy(plain)
        assertArrayEquals(expected, actual)
    }

    @Test
    fun testRoundaboutAnglesAndCodes() {
        // roundaboutCode(angleDeg, leftHandTraffic)
        assertEquals(68, TvsProtocol.roundaboutCode(90.0, true))   // straight up
        assertEquals(66, TvsProtocol.roundaboutCode(180.0, true))  // left
        assertEquals(70, TvsProtocol.roundaboutCode(0.0, true))    // right
        assertEquals(71, TvsProtocol.roundaboutCode(270.0, true))  // U-turn LHT -> RB_SHARP_RIGHT
        assertEquals(71, TvsProtocol.roundaboutCode(-90.0, true))  // down -> RB_SHARP_RIGHT
        assertEquals(65, TvsProtocol.roundaboutCode(270.0, false)) // RHT -> RB_SHARP_LEFT
    }

    @Test
    fun testTextTruncationAndClamping() {
        val longText = "Very long instruction text exceeding seventeen characters"
        val cut = TvsProtocol.asciiText(longText)
        assertEquals(17, cut.size)

        val nonAscii = "Turn left onto මාර්ගය"
        val cleaned = TvsProtocol.asciiText(nonAscii)
        val textStr = String(cleaned)
        assertEquals("Turn left onto ", textStr)
    }
}
