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

    @Test
    fun testDoubleBufferingPreventsMidFrameTearing() {
        ppu.write(0xFF40, 0x00) // Off
        // Fill VRAM tile 0 with non-zero color index
        for (i in 0 until 16) {
            mmu.vram[i] = 0xFF.toByte()
        }
        // Palette 0xFF (all shades darkest)
        ppu.write(0xFF47, 0xFF)

        // Turn on LCD
        ppu.write(0xFF40, 0x91) // LCD on, BG on

        val initialPixel = ppu.framebuffer[0]

        // Advance 10 scanlines (10 * 456 cycles) - halfway through the active frame
        ppu.step(10 * 456)
        assertEquals(10, ppu.ly)

        // Mid-frame: framebuffer (front buffer) must NOT reflect uncompleted frame pixels
        assertEquals(
            "Front framebuffer must not change mid-frame to prevent tearing",
            initialPixel,
            ppu.framebuffer[0]
        )

        // Complete the remaining lines up to V-Blank (total 144 lines = 144 * 456 = 65664)
        ppu.step(134 * 456)
        assertEquals(144, ppu.ly)

        // Now that V-Blank is reached, front buffer is updated atomically!
        assertEquals(
            "Front framebuffer should be updated upon entering V-Blank",
            ppu.paletteColors[3],
            ppu.framebuffer[0]
        )
    }

    @Test
    fun testLcdQuickDisableDoesNotWipeFrontBufferImmediately() {
        ppu.write(0xFF40, 0x00) // LCD off
        // Complete a full frame so framebuffer has valid game graphics
        for (i in 0 until 16) {
            mmu.vram[i] = 0xFF.toByte()
        }
        ppu.write(0xFF47, 0xFF)
        ppu.write(0xFF40, 0x91) // LCD on -> mode 2, ly = 0

        // Run until V-Blank to populate front buffer
        ppu.step(144 * 456)
        val renderedPixel = ppu.framebuffer[0]
        assertEquals(ppu.paletteColors[3], renderedPixel)

        // Game disables LCD during V-Blank to perform quick VRAM transfer
        ppu.write(0xFF40, 0x00)

        // Front buffer must NOT be wiped immediately to prevent 1-frame screen flashes
        assertEquals(
            "Front buffer must keep last valid frame during brief LCD disable to prevent flickering",
            renderedPixel,
            ppu.framebuffer[0]
        )
    }

    @Test
    fun testWindowLineCounterDoesNotOverflowVramBounds() {
        // Reproduce issue: LCD disabled at line 145, then restarted, accumulating window lines
        ppu.write(0xFF40, 0x00) // LCD off
        ppu.write(0xFF4A, 0) // WY = 0
        ppu.write(0xFF4B, 7) // WX = 7
        // LCD on, Window on, BG on, Window map at 0x9C00 (mapBase = 0x1C00 = 7168)
        // 0x40 (Window tile map 0x9C00) | 0x20 (Window on) | 0x10 | 0x01 | 0x80 = 0xF1
        ppu.write(0xFF40, 0xF1)

        // Run multiple frames where LCD is disabled during V-Blank before line 154
        for (f in 0..4) {
            // Step 145 scanlines
            ppu.step(145 * 456)
            // LCD disabled at line 145 (exact behavior of Pokémon Red DisableLCD)
            ppu.write(0xFF40, 0x00)
            // Re-enabled
            ppu.write(0xFF40, 0xF1)
        }
    }
}
