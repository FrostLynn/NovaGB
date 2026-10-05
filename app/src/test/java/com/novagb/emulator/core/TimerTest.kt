package com.novagb.emulator.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TimerTest {

    private lateinit var mmu: Mmu
    private lateinit var timer: Timer

    @Before
    fun setup() {
        mmu = Mmu()
        timer = Timer(mmu)
        mmu.timer = timer
    }

    @Test
    fun testDivResetTriggersFallingEdgeTimaTick() {
        // Reset DIV first, then enable timer with bit 3 selected
        timer.write(0xFF04, 0x00) // DIV = 0
        timer.write(0xFF05, 0x00) // TIMA = 0
        timer.write(0xFF07, 0x05) // TAC = 0x05 (Enabled, bit 3)

        // Advance 8 cycles: internalDivider becomes 8 (bit 3 = 1). Rising edge, TIMA remains 0.
        timer.step(8)
        assertEquals(0, timer.tima)

        // Writing to DIV resets internal counter from 8 (bit 3 = 1) to 0 (bit 3 = 0).
        // This generates a falling edge, so TIMA must increment!
        timer.write(0xFF04, 0x00)
        assertEquals(1, timer.tima)
    }

    @Test
    fun testTimaOverflow4CycleDelay() {
        // Correct initialization order: DIV -> TMA -> TIMA -> TAC
        timer.write(0xFF04, 0x00) // Reset DIV
        timer.write(0xFF06, 0x88) // TMA = 0x88
        timer.write(0xFF05, 0xFF) // TIMA = 0xFF
        timer.write(0xFF07, 0x05) // TAC = 0x05 (bit 3 selected, period = 16 cycles)
        mmu.ifReg = 0x00

        // Advance 16 cycles: falling edge triggers increment -> 0xFF overflows to 0x00
        timer.step(16)
        assertEquals("TIMA must be 0x00 during overflow delay window", 0x00, timer.tima)
        assertEquals("Timer interrupt must not fire immediately on cycle 0", 0x00, mmu.ifReg and 0x04)

        // Advance 3 more cycles (total 3 of 4 delay cycles)
        timer.step(3)
        assertEquals("TIMA must still be 0x00 at 3 cycles", 0x00, timer.tima)
        assertEquals(0x00, mmu.ifReg and 0x04)

        // Advance 1 more cycle (4th cycle: reload occurs!)
        timer.step(1)
        assertEquals("TIMA must be reloaded with TMA (0x88) on 4th cycle", 0x88, timer.tima)
        assertEquals("Timer interrupt must be requested on 4th cycle", 0x04, mmu.ifReg and 0x04)
    }

    @Test
    fun testWriteToTimaDuringOverflowCancelsReload() {
        timer.write(0xFF04, 0x00) // Reset DIV
        timer.write(0xFF06, 0x88) // TMA = 0x88
        timer.write(0xFF05, 0xFF) // TIMA = 0xFF
        timer.write(0xFF07, 0x05) // TAC = 0x05
        mmu.ifReg = 0x00

        // Trigger overflow at cycle 16
        timer.step(16)
        assertEquals(0x00, timer.tima)

        // Software writes to TIMA during the 4-cycle delay window
        timer.write(0xFF05, 0x42)

        // Complete the remaining cycles
        timer.step(4)
        assertEquals("TIMA reload should be cancelled, keeping written value", 0x42, timer.tima)
        assertEquals("Interrupt should not be requested when reload cancelled", 0x00, mmu.ifReg and 0x04)
    }
}
