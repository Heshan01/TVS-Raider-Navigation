package com.heshan.tvsraider

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for IconTable known arrow masks.
 */
class IconTableTest {

    private fun decodeMask(hex: String): BooleanArray {
        val ink = BooleanArray(32 * 32)
        for (i in hex.indices) {
            val nib = Character.digit(hex[i], 16)
            for (b in 0 until 4) {
                ink[i * 4 + b] = ((nib shr (3 - b)) and 1) == 1
            }
        }
        return ink
    }

    @Test
    fun testKnownIconsLookupConsistency() {
        // RB_LEFT (Roundabout 1st exit clockwise)
        val maskRbLeft = decodeMask(
            "0000000000000000000000000000000000000000000000000060000000f0000001f0000003e0000007c000000f8000001fffe0003fffe0003fffe0001ffff0000f80f00007c0780003e03f8001f03f8000f00f800060038000000380000003800000038000000380000003800000038000000000000000000000000000000000"
        )
        assertEquals(TvsProtocol.P_RB_LEFT, IconTable.lookup(maskRbLeft))

        // RB_RIGHT (Roundabout exit right)
        val maskRbRight = decodeMask(
            "0000000000000000000000000000000000000000000000000000060007f00f000ffc0f801ffc07c03e1e03e03c0f01f0380ffff87807fffc7807fffc7807fff83c0001f03e0003e01fc007c00fe00f8007e00f0001c0060001c0000001c0000001c0000001c0000001c0000001c0000000000000000000000000000000000000"
        )
        assertEquals(TvsProtocol.P_RB_RIGHT, IconTable.lookup(maskRbRight))

        // ARRIVE (Destination pin)
        val maskArrive = decodeMask(
            "000000000000000000000000000ff000003ffc00007c3e0000f00f0001e0078001c00380038001c0038001c00303c0c00707c0e00707e0e00703c0e00703c0e0030000c0038001c0038001c001c0038000e0070000f00f0000701e0000383c00001c7800000ff0000007e0000003c00000018000000000000000000000000000"
        )
        assertEquals(TvsProtocol.P_ARRIVE, IconTable.lookup(maskArrive))

        // RB_STRAIGHT (Roundabout straight)
        val maskRbStraight = decodeMask(
            "00000000000180000003c0000007e000000ff000001ff800003ffc00007bde0000f3cf0000e3c7000043c2000007c000001fc000003fc000003c0000007800000070000000700000007000000070000000780000003c0000003fc000001fc0000007c0000003c0000003c0000003c0000000000000000000000000000000000"
        )
        assertEquals(TvsProtocol.P_RB_STRAIGHT, IconTable.lookup(maskRbStraight))

        // U_TURN_RIGHT
        val maskUTurn = decodeMask(
            "00000000000000000000000000000000003f000000ffc00001ffe00003f3f00007c0780007803c000f003c000f001c000e001c000e001c000e001c000e031c300e079c780e07dcf00e03fde00e01ffc00e00ff800e007f000e003f000e001e000e000c000e0000000e0000000e00000000000000000000000000000000000000"
        )
        assertEquals(TvsProtocol.P_UTURN_RIGHT, IconTable.lookup(maskUTurn))
    }
}
