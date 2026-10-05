package com.novagb.emulator.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PpuTest {

    private lateinit var mmu: Mmu
    private lateinit var ppu: Ppu

    @Before
    fun setup() {
        mmu = Mmu()
        ppu = Ppu(mmu)
        mmu.ppu = ppu
    }

    @Test
    fun testMode0HblankStatInterruptTriggeredOnRisingEdge() {
        // Power cycle LCD: off then on to start at scanline 0 Mode 2
        ppu.write(0xFF40, 0x00) // LCD off
        ppu.write(0xFF40, 0x80) // LCD on -> mode 2, ly = 0
        ppu.write(0xFF41, 0x08) // STAT Mode 0 H-Blank int enabled
        mmu.ifReg = 0x00

        // PPU starts in Mode 2 (OAM search, 80 cycles)
        assertEquals("Initial mode should be Mode 2", 2, ppu.stat and 0x03)
        ppu.step(80)
        // Transitioned to Mode 3 (Pixel transfer, 172 cycles)
        assertEquals("Mode should be Mode 3", 3, ppu.stat and 0x03)
        assertEquals("STAT interrupt should not have fired yet", 0x00, mmu.ifReg and 0x02)

        // Advance 172 cycles -> Mode 3 finishes and transitions to Mode 0
        ppu.step(172)
        assertEquals("Mode should now be Mode 0", 0, ppu.stat and 0x03)
        assertEquals("STAT interrupt must be requested on rising edge to Mode 0", 0x02, mmu.ifReg and 0x02)
    }

    @Test
    fun testStatBlockingGlitchPreventsDuplicateInterruptWhenLineAlreadyHigh() {
        // Power cycle LCD: off then on to ensure Mode 2
        ppu.write(0xFF40, 0x00)
        ppu.write(0xFF40, 0x80)
        assertEquals(2, ppu.stat and 0x03) // Mode 2

        // Enable both Mode 2 (0x20) and LYC (0x40) interrupts
        ppu.write(0xFF41, 0x60)
        ppu.write(0xFF45, 0x00) // LYC = 0

        // In Mode 2 on line 0: Mode 2 is active (0x20) AND LYC=LY match is active (0x40).
        // Rising edge occurs -> STAT interrupt fired!
        assertTrue("STAT interrupt should be requested", (mmu.ifReg and 0x02) != 0)

        // Clear IF interrupt flag
        mmu.ifReg = 0x00

        // While still in Mode 2, software writes a different LYC (e.g. 1) then back to 0.
        // Even if LYC matches again, because Mode 2 condition is still keeping the STAT line HIGH,
        // no rising edge (0 -> 1) occurs, so no new interrupt is fired (STAT blocking glitch)!
        ppu.write(0xFF45, 0x01) // LYC = 1 (LYC match becomes false, but Mode 2 is still true: line stays true)
        ppu.write(0xFF45, 0x00) // LYC = 0 (LYC match becomes true again, line was already true)
        assertEquals("No new interrupt should fire when STAT line remained high (STAT blocking glitch)", 0x00, mmu.ifReg and 0x02)
    }

    @Test
    fun testLycMatchBitTracksLy() {
        ppu.write(0xFF40, 0x80)
        ppu.write(0xFF45, 0x02) // LYC = 2
        ppu.ly = 1
        ppu.write(0xFF45, 0x02)
        assertEquals("LYC match bit (bit 2) should be 0 when LY!=LYC", 0, ppu.stat and 0x04)

        // Simulate advancing to LY = 2 (each scanline = 456 cycles)
        // Step remainder of line 1 (456 cycles)
        ppu.step(456)
        assertEquals(2, ppu.ly)
        assertEquals("LYC match bit (bit 2) should be 1 when LY==LYC", 0x04, ppu.stat and 0x04)

        // Step to next line (LY = 3)
        ppu.step(456)
        assertEquals(3, ppu.ly)
        assertEquals("LYC match bit (bit 2) should be 0 when LY!=LYC", 0, ppu.stat and 0x04)
    }
}
