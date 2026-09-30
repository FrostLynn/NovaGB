package com.novagb.emulator.core

/**
 * Game Boy hardware timer.
 * Emulates DIV (0xFF04), TIMA (0xFF05), TMA (0xFF06), and TAC (0xFF07).
 */
class Timer(private val mmu: Mmu) {

    private var internalDivider: Int = 0xABCC // Post-boot divider value
    var tima: Int = 0
    var tma: Int = 0
    var tac: Int = 0

    val div: Int
        get() = (internalDivider ushr 8) and 0xFF

    fun reset() {
        internalDivider = 0xABCC
        tima = 0
        tma = 0
        tac = 0
    }

    fun read(addr: Int): Int {
        return when (addr) {
            0xFF04 -> div
            0xFF05 -> tima
            0xFF06 -> tma
            0xFF07 -> tac or 0xF8
            else -> 0xFF
        }
    }

    fun write(addr: Int, value: Int) {
        val v = value and 0xFF
        when (addr) {
            0xFF04 -> internalDivider = 0 // Any write resets DIV
            0xFF05 -> tima = v
            0xFF06 -> tma = v
            0xFF07 -> tac = v and 0x07
        }
    }

    fun step(cycles: Int) {
        val prevDivider = internalDivider
        internalDivider = (internalDivider + cycles) and 0xFFFF

        val enabled = (tac and 0x04) != 0
        if (!enabled) return

        val rateMask = when (tac and 0x03) {
            0 -> 1024 // 4096 Hz (bit 9)
            1 -> 16   // 262144 Hz (bit 3)
            2 -> 64   // 65536 Hz (bit 5)
            3 -> 256  // 16384 Hz (bit 7)
            else -> 1024
        }

        // Detect falling edge of the selected bit
        val prevBit = (prevDivider and (rateMask ushr 1)) != 0
        val currentBit = (internalDivider and (rateMask ushr 1)) != 0

        if (prevBit && !currentBit) {
            tima++
            if (tima > 0xFF) {
                tima = tma
                mmu.requestInterrupt(InterruptType.TIMER)
            }
        }
    }
}
