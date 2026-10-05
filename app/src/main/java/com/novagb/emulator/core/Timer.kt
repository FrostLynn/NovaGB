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

    // Overflow reload state machine (4 T-cycles delay on overflow)
    private var overflowDelay: Int = 0
    private var timaReloadPending: Boolean = false

    val div: Int
        get() = (internalDivider ushr 8) and 0xFF

    fun reset() {
        internalDivider = 0xABCC
        tima = 0
        tma = 0
        tac = 0
        overflowDelay = 0
        timaReloadPending = false
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
            0xFF04 -> {
                val prevBit = timerBit()
                internalDivider = 0
                val newBit = timerBit()
                if (prevBit && !newBit) {
                    incrementTima()
                }
            }
            0xFF05 -> {
                // If software writes to TIMA during the 4-cycle overflow delay, cancel reload
                if (timaReloadPending && overflowDelay > 0) {
                    timaReloadPending = false
                    overflowDelay = 0
                }
                tima = v
            }
            0xFF06 -> {
                tma = v
                if (timaReloadPending && overflowDelay == 0) {
                    tima = v
                }
            }
            0xFF07 -> {
                val prevBit = timerBit()
                tac = v and 0x07
                val newBit = timerBit()
                if (prevBit && !newBit) {
                    incrementTima()
                }
            }
        }
    }

    fun step(cycles: Int) {
        for (i in 0 until cycles) {
            if (timaReloadPending) {
                overflowDelay--
                if (overflowDelay == 0) {
                    tima = tma
                    mmu.requestInterrupt(InterruptType.TIMER)
                    timaReloadPending = false
                }
            }

            val prevBit = timerBit()
            internalDivider = (internalDivider + 1) and 0xFFFF
            val newBit = timerBit()

            if (prevBit && !newBit) {
                incrementTima()
            }
        }
    }

    private fun timerBit(): Boolean {
        if ((tac and 0x04) == 0) return false
        val bitIndex = when (tac and 0x03) {
            0 -> 9  // 4096 Hz (every 1024 cycles)
            1 -> 3  // 262144 Hz (every 16 cycles)
            2 -> 5  // 65536 Hz (every 64 cycles)
            3 -> 7  // 16384 Hz (every 256 cycles)
            else -> 9
        }
        return ((internalDivider ushr bitIndex) and 1) != 0
    }

    private fun incrementTima() {
        tima = (tima + 1) and 0xFF
        if (tima == 0) {
            // TIMA overflow: enters 4 T-cycles delay window
            timaReloadPending = true
            overflowDelay = 4
        }
    }
}
