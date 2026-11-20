package com.heshan.tvsraider

/**
 * Turns a raw frame from the bike into a readable log line.
 * TVS SmartXonnect / Raider frame decoder.
 */
object BikeFrames {
    @Volatile var fuelBars: Int = -1      // 0x10 byte 6, 0..5 (5 = full)
    @Volatile var avgKmL: Int = -1        // 0x10 byte 13, cluster "AVG MILEAGE"
    @Volatile var tripBTenths: Int = -1   // 0x19 bytes 8-9, cluster "TRIP B" in 0.1 km
    @Volatile var batteryTenths: Int = -1 // 0x18 byte 11, volts x10
    @Volatile var updatedAt: Long = 0

    /** Stores the known values from a frame with a good checksum. */
    fun update(v: ByteArray?) {
        if (v == null || v.size < 20 || u(v, 0) != 0x5A) return
        var sum = 0
        for (i in 0 until 18) sum += u(v, i)
        if ((255 - sum % 256) != u(v, 18)) return
        when (u(v, 1)) {
            0x10 -> {
                if (u(v, 6) <= 5) fuelBars = u(v, 6)
                avgKmL = u(v, 13)
            }
            0x19 -> tripBTenths = be(v, 8, 2)
            0x18 -> batteryTenths = u(v, 11)
            else -> return
        }
        updatedAt = System.currentTimeMillis()
    }

    fun tenthsText(n: Int): String = tenths(n)

    /** A readable line for a known frame type, or null. */
    fun describe(v: ByteArray?): String? {
        if (v == null || v.size < 20 || u(v, 0) != 0x5A) return null
        return when (u(v, 1)) {
            0x10 -> "   = speed=" + u(v, 2) + "km/h" +
                    " odo=" + tenths(be(v, 3, 3)) + "km" +
                    " fuel[6]=0x" + h(u(v, 6)) + "(bars?" + (u(v, 6) and 0x0F) + ")" +
                    " avgSpeed[7]=" + u(v, 7) +
                    " tripMode[8]=" + u(v, 8) +
                    " topSpeed[9]=" + u(v, 9) +
                    " throttle[10]=" + u(v, 10) +
                    " light[11]=0x" + h(u(v, 11)) +
                    " buttons[12]=" + u(v, 12) +
                    " trip[14-16]=" + tenths(be(v, 14, 3)) + "km" +
                    " rpm[17-18]=" + be(v, 17, 2) + " rpm[16-17]=" + be(v, 16, 2)
            0x11 -> "   = fuelSensorFault[2]=" + u(v, 2) +
                    " state[3]=" + u(v, 3) +
                    " serviceDue[4]=" + u(v, 4) +
                    " state1[6]=" + u(v, 6) +
                    " firmware[7]=" + u(v, 7) +
                    " milCode[8]=" + u(v, 8) +
                    " model[9]=" + u(v, 9) +
                    " diag[10]=0x" + h(u(v, 10)) + "(engineLight=" + ((u(v, 10) shr 2) and 1) + ")" +
                    " issCode[11]=" + u(v, 11) +
                    " indicators[12]=0x" + h(u(v, 12)) +
                    " connector[13]=" + u(v, 13)
            0x18 -> "   = load[2]=" + u(v, 2) +
                    " injTimeTotal[3-4]=" + be(v, 3, 2) +
                    " map[5]=" + u(v, 5) + "kPa" +
                    " intake[7]=" + (u(v, 7) - 40) + "C" +
                    " engineTemp[8]=" + u(v, 8) + "C" +
                    " baro[9-10]=" + be(v, 9, 2) +
                    " battery[11]=" + tenths(u(v, 11)) + "V" +
                    " runtime[12-13]=" + be(v, 12, 2) + "s" +
                    " dist[14-15]=" + be(v, 14, 2) +
                    " injVolume[16-17]=" + be(v, 16, 2)
            0x19 -> "   = tripB[8-9]=" + tenths(be(v, 8, 2)) + "km (cluster)" +
                    " avgKmL[8]=" + u(v, 8) + "(alt)" +
                    " nowKmL[9]=" + u(v, 9) +
                    " issTime[2-4]=" + be(v, 2, 3) +
                    " range[11-12]=" + be(v, 11, 2) + "km // UNVERIFIED"
            else -> null
        }
    }

    private fun u(v: ByteArray, i: Int): Int = v[i].toInt() and 0xFF

    private fun be(v: ByteArray, at: Int, len: Int): Int {
        var n = 0
        for (i in 0 until len) n = (n shl 8) or u(v, at + i)
        return n
    }

    private fun h(b: Int): String = String.format("%02X", b)

    private fun tenths(n: Int): String = "${n / 10}.${n % 10}"
}
