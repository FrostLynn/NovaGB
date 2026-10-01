package com.novagb.emulator.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class GameBoyTest {

    private lateinit var gameBoy: GameBoy

    @Before
    fun setup() {
        gameBoy = GameBoy()
    }

    @Test
    fun testStepFrameWithoutCartridgeDoesNotCorruptCpu() {
        assertNull(gameBoy.cartridge)
        val initialPc = gameBoy.cpu.pc
        val initialSp = gameBoy.cpu.sp

        val cycles = gameBoy.stepFrame()

        assertEquals(gameBoy.cyclesPerFrame, cycles)
        // CPU registers must remain intact instead of thrashing RST 0x38 (0x0038)
        assertEquals(initialPc, gameBoy.cpu.pc)
        assertEquals(initialSp, gameBoy.cpu.sp)
    }

    @Test
    fun testLoadRomInitializesCartridgeAndReset() {
        val dummyRom = ByteArray(32768)
        dummyRom[0x0147] = 0x00 // ROM ONLY
        val title = "TEST_GAME"
        for (i in title.indices) {
            dummyRom[0x0134 + i] = title[i].code.toByte()
        }

        gameBoy.loadRom(dummyRom)

        assertEquals("TEST_GAME", gameBoy.cartridge?.header?.title)
        assertEquals(0x0100, gameBoy.cpu.pc)
        assertEquals(0xFFFE, gameBoy.cpu.sp)
    }
}
