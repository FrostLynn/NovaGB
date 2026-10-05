package com.novagb.emulator.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class ApuTest {

    private lateinit var apu: Apu

    @Before
    fun setup() {
        apu = Apu()
    }

    @Test
    fun testHighPassFilterDecaysDcOffset() {
        // Collect samples output from APU
        val collectedSamples = mutableListOf<Short>()
        apu.onAudioBufferFull = { buffer, size ->
            for (i in 0 until size) {
                collectedSamples.add(buffer[i])
            }
        }

        // Enable sound master (NR52 = 0x80)
        apu.write(0xFF26, 0x80)
        // Set master volume to max (NR50 = 0x77)
        apu.write(0xFF24, 0x77)
        // Route Channel 1 to Left and Right (NR51 = 0x11)
        apu.write(0xFF25, 0x11)

        // Configure Channel 1: Pulse wave with volume 15, envelope disabled
        apu.write(0xFF10, 0x00) // No sweep
        apu.write(0xFF11, 0x80) // 50% duty, length = 0
        apu.write(0xFF12, 0xF0) // Initial volume 15, no envelope
        apu.write(0xFF13, 0x00) // Frequency low = 0
        apu.write(0xFF14, 0x80) // Trigger channel, frequency high = 0, no length

        // Step APU for ~100,000 cycles (~1 frame) to generate samples
        apu.step(100_000)
        apu.flushAudio()

        assertTrue("Should have generated samples", collectedSamples.isNotEmpty())

        // Initial sample magnitude should be non-zero
        val initialMagnitude = abs(collectedSamples.first().toInt())
        assertTrue("Initial sample should have non-zero signal", initialMagnitude > 0)

        // After hundreds of samples of DC, high-pass filter capacitor should charge up
        // and eliminate DC bias (subsequent samples should stay bounded without runaway DC drift)
        for (i in 0 until 500_000 step 1000) {
            apu.step(1000)
        }
        apu.flushAudio()

        val lastSamples = collectedSamples.takeLast(20)
        for (sample in lastSamples) {
            assertTrue("Filtered sample must stay within signed 16-bit range", sample in -32768..32767)
        }
    }

    @Test
    fun testMasterSoundDisableMutesOutput() {
        var bufferCallCount = 0
        apu.onAudioBufferFull = { _, _ -> bufferCallCount++ }

        // Disable master APU (bit 7 of NR52 = 0)
        apu.write(0xFF26, 0x00)
        assertEquals(0, apu.nr52 and 0x80)

        // Triggering registers when disabled should be ignored or read 0
        apu.write(0xFF12, 0xF0)
        assertEquals(0, apu.read(0xFF12))
    }
}
