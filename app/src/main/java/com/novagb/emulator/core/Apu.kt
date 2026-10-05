package com.novagb.emulator.core

/**
 * Game Boy Audio Processing Unit (APU).
 * Generates stereo 16-bit PCM audio samples at 44100 Hz.
 */
class Apu {

    var enabled: Boolean = true

    // Audio sample buffer for streaming to AudioTrack (~1 frame worth at 44.1kHz stereo)
    private val sampleBuffer = ShortArray(1470)
    private var sampleBufferIndex = 0
    var onAudioBufferFull: ((ShortArray, Int) -> Unit)? = null

    // Master registers
    var nr50: Int = 0x77 // Volume Left & Right
    var nr51: Int = 0xF3 // Sound panning
    var nr52: Int = 0xF1 // Master control & flags

    // Channel 1 (Pulse with sweep)
    private var ch1SweepTime: Int = 0
    private var ch1SweepDir: Int = 0
    private var ch1SweepShift: Int = 0
    private var ch1Duty: Int = 2
    private var ch1Length: Int = 0
    private var ch1InitialVolume: Int = 0
    private var ch1EnvelopeDir: Int = 0
    private var ch1EnvelopePace: Int = 0
    private var ch1Freq: Int = 0
    private var ch1LengthEnabled: Boolean = false
    private var ch1Active: Boolean = false
    private var ch1Timer: Int = 0
    private var ch1WaveIndex: Int = 0
    private var ch1CurrentVolume: Int = 0
    private var ch1EnvelopeTimer: Int = 0
    private var ch1SweepTimer: Int = 0
    private var ch1ShadowFreq: Int = 0

    // Channel 2 (Pulse)
    private var ch2Duty: Int = 2
    private var ch2Length: Int = 0
    private var ch2InitialVolume: Int = 0
    private var ch2EnvelopeDir: Int = 0
    private var ch2EnvelopePace: Int = 0
    private var ch2Freq: Int = 0
    private var ch2LengthEnabled: Boolean = false
    private var ch2Active: Boolean = false
    private var ch2Timer: Int = 0
    private var ch2WaveIndex: Int = 0
    private var ch2CurrentVolume: Int = 0
    private var ch2EnvelopeTimer: Int = 0

    // Channel 3 (Wave)
    private var ch3DacEnabled: Boolean = false
    private var ch3Length: Int = 0
    private var ch3VolumeCode: Int = 0
    private var ch3Freq: Int = 0
    private var ch3LengthEnabled: Boolean = false
    private var ch3Active: Boolean = false
    private var ch3Timer: Int = 0
    private var ch3SampleIndex: Int = 0
    private val wavePattern = ByteArray(16) // 32 4-bit samples

    // Channel 4 (Noise)
    private var ch4Length: Int = 0
    private var ch4InitialVolume: Int = 0
    private var ch4EnvelopeDir: Int = 0
    private var ch4EnvelopePace: Int = 0
    private var ch4ClockShift: Int = 0
    private var ch4WidthMode7: Boolean = false
    private var ch4DivisorCode: Int = 0
    private var ch4LengthEnabled: Boolean = false
    private var ch4Active: Boolean = false
    private var ch4Timer: Int = 0
    private var ch4Lfsr: Int = 0x7FFF
    private var ch4CurrentVolume: Int = 0
    private var ch4EnvelopeTimer: Int = 0

    // Timing & Sequencer
    private var frameSequencerCycles: Int = 0
    private var frameSequencerStep: Int = 0
    private var sampleCycles: Float = 0f
    private val cyclesPerSample = 4194304f / 44100f

    // High-pass DC filter capacitor state (simulates analog output capacitor)
    private var leftCap: Float = 0f
    private var rightCap: Float = 0f
    private val highPassRate: Float = 0.9995f

    // Wave duty waveforms (8 steps)
    private val dutyPatterns = arrayOf(
        intArrayOf(0, 0, 0, 0, 0, 0, 0, 1), // 12.5%
        intArrayOf(1, 0, 0, 0, 0, 0, 0, 1), // 25%
        intArrayOf(1, 0, 0, 0, 0, 1, 1, 1), // 50%
        intArrayOf(0, 1, 1, 1, 1, 1, 1, 0)  // 75%
    )

    fun reset() {
        nr50 = 0x77
        nr51 = 0xF3
        nr52 = 0xF1
        ch1Active = false
        ch2Active = false
        ch3Active = false
        ch4Active = false
        ch4Lfsr = 0x7FFF
        sampleBufferIndex = 0
        frameSequencerCycles = 0
        frameSequencerStep = 0
        sampleCycles = 0f
        wavePattern.fill(0)
        leftCap = 0f
        rightCap = 0f
    }

    fun read(addr: Int): Int {
        return when (addr) {
            0xFF10 -> (ch1SweepTime shl 4) or (ch1SweepDir shl 3) or ch1SweepShift or 0x80
            0xFF11 -> (ch1Duty shl 6) or 0x3F
            0xFF12 -> (ch1InitialVolume shl 4) or (ch1EnvelopeDir shl 3) or ch1EnvelopePace
            0xFF14 -> (if (ch1LengthEnabled) 0x40 else 0) or 0xBF

            0xFF16 -> (ch2Duty shl 6) or 0x3F
            0xFF17 -> (ch2InitialVolume shl 4) or (ch2EnvelopeDir shl 3) or ch2EnvelopePace
            0xFF19 -> (if (ch2LengthEnabled) 0x40 else 0) or 0xBF

            0xFF1A -> (if (ch3DacEnabled) 0x80 else 0) or 0x7F
            0xFF1C -> (ch3VolumeCode shl 5) or 0x9F
            0xFF1E -> (if (ch3LengthEnabled) 0x40 else 0) or 0xBF

            0xFF21 -> (ch4InitialVolume shl 4) or (ch4EnvelopeDir shl 3) or ch4EnvelopePace
            0xFF22 -> (ch4ClockShift shl 4) or (if (ch4WidthMode7) 0x08 else 0) or ch4DivisorCode
            0xFF23 -> (if (ch4LengthEnabled) 0x40 else 0) or 0xBF

            0xFF24 -> nr50
            0xFF25 -> nr51
            0xFF26 -> {
                var v = if (enabled) 0x80 else 0
                if (ch1Active) v = v or 0x01
                if (ch2Active) v = v or 0x02
                if (ch3Active) v = v or 0x04
                if (ch4Active) v = v or 0x08
                v or 0x70
            }
            in 0xFF30..0xFF3F -> wavePattern[addr - 0xFF30].toInt() and 0xFF
            else -> 0xFF
        }
    }

    fun write(addr: Int, value: Int) {
        val v = value and 0xFF

        // Master on/off check
        if (addr == 0xFF26) {
            enabled = (v and 0x80) != 0
            if (!enabled) {
                reset()
                nr52 = 0
            } else {
                nr52 = 0x80
            }
            return
        }

        if (!enabled && addr !in 0xFF30..0xFF3F) return

        when (addr) {
            // Channel 1
            0xFF10 -> {
                ch1SweepTime = (v ushr 4) and 0x07
                ch1SweepDir = (v ushr 3) and 0x01
                ch1SweepShift = v and 0x07
            }
            0xFF11 -> {
                ch1Duty = (v ushr 6) and 0x03
                ch1Length = 64 - (v and 0x3F)
            }
            0xFF12 -> {
                ch1InitialVolume = (v ushr 4) and 0x0F
                ch1EnvelopeDir = (v ushr 3) and 0x01
                ch1EnvelopePace = v and 0x07
            }
            0xFF13 -> {
                ch1Freq = (ch1Freq and 0x700) or v
            }
            0xFF14 -> {
                ch1Freq = (ch1Freq and 0xFF) or ((v and 0x07) shl 8)
                ch1LengthEnabled = (v and 0x40) != 0
                if ((v and 0x80) != 0) triggerCh1()
            }

            // Channel 2
            0xFF16 -> {
                ch2Duty = (v ushr 6) and 0x03
                ch2Length = 64 - (v and 0x3F)
            }
            0xFF17 -> {
                ch2InitialVolume = (v ushr 4) and 0x0F
                ch2EnvelopeDir = (v ushr 3) and 0x01
                ch2EnvelopePace = v and 0x07
            }
            0xFF18 -> {
                ch2Freq = (ch2Freq and 0x700) or v
            }
            0xFF19 -> {
                ch2Freq = (ch2Freq and 0xFF) or ((v and 0x07) shl 8)
                ch2LengthEnabled = (v and 0x40) != 0
                if ((v and 0x80) != 0) triggerCh2()
            }

            // Channel 3
            0xFF1A -> {
                ch3DacEnabled = (v and 0x80) != 0
                if (!ch3DacEnabled) ch3Active = false
            }
            0xFF1B -> ch3Length = 256 - v
            0xFF1C -> ch3VolumeCode = (v ushr 5) and 0x03
            0xFF1D -> ch3Freq = (ch3Freq and 0x700) or v
            0xFF1E -> {
                ch3Freq = (ch3Freq and 0xFF) or ((v and 0x07) shl 8)
                ch3LengthEnabled = (v and 0x40) != 0
                if ((v and 0x80) != 0) triggerCh3()
            }

            // Channel 4
            0xFF20 -> ch4Length = 64 - (v and 0x3F)
            0xFF21 -> {
                ch4InitialVolume = (v ushr 4) and 0x0F
                ch4EnvelopeDir = (v ushr 3) and 0x01
                ch4EnvelopePace = v and 0x07
            }
            0xFF22 -> {
                ch4ClockShift = (v ushr 4) and 0x0F
                ch4WidthMode7 = (v and 0x08) != 0
                ch4DivisorCode = v and 0x07
            }
            0xFF23 -> {
                ch4LengthEnabled = (v and 0x40) != 0
                if ((v and 0x80) != 0) triggerCh4()
            }

            0xFF24 -> nr50 = v
            0xFF25 -> nr51 = v

            in 0xFF30..0xFF3F -> wavePattern[addr - 0xFF30] = v.toByte()
        }
    }

    private fun triggerCh1() {
        ch1Active = true
        ch1CurrentVolume = ch1InitialVolume
        ch1EnvelopeTimer = if (ch1EnvelopePace > 0) ch1EnvelopePace else 8
        ch1Timer = (2048 - ch1Freq) * 4
        ch1ShadowFreq = ch1Freq
        ch1SweepTimer = if (ch1SweepTime > 0) ch1SweepTime else 8
    }

    private fun triggerCh2() {
        ch2Active = true
        ch2CurrentVolume = ch2InitialVolume
        ch2EnvelopeTimer = if (ch2EnvelopePace > 0) ch2EnvelopePace else 8
        ch2Timer = (2048 - ch2Freq) * 4
    }

    private fun triggerCh3() {
        if (!ch3DacEnabled) return
        ch3Active = true
        ch3Timer = (2048 - ch3Freq) * 2
        ch3SampleIndex = 0
    }

    private fun triggerCh4() {
        ch4Active = true
        ch4CurrentVolume = ch4InitialVolume
        ch4EnvelopeTimer = if (ch4EnvelopePace > 0) ch4EnvelopePace else 8
        ch4Lfsr = 0x7FFF
        ch4Timer = getNoisePeriod()
    }

    private fun getNoisePeriod(): Int {
        val divisor = when (ch4DivisorCode) {
            0 -> 8
            else -> ch4DivisorCode * 16
        }
        return divisor shl ch4ClockShift
    }

    fun step(cycles: Int) {
        if (!enabled) return

        // 1. Clock channel timers
        clockTimers(cycles)

        // 2. Clock 512 Hz Frame Sequencer
        frameSequencerCycles += cycles
        if (frameSequencerCycles >= 8192) {
            frameSequencerCycles -= 8192
            stepFrameSequencer()
        }

        // 3. Audio sampling
        sampleCycles += cycles
        while (sampleCycles >= cyclesPerSample) {
            sampleCycles -= cyclesPerSample
            generateSample()
        }
    }

    private fun clockTimers(cycles: Int) {
        // Ch1
        val ch1Period = maxOf(4, (2048 - ch1Freq) * 4)
        ch1Timer -= cycles
        while (ch1Timer <= 0) {
            ch1Timer += ch1Period
            ch1WaveIndex = (ch1WaveIndex + 1) and 0x07
        }

        // Ch2
        val ch2Period = maxOf(4, (2048 - ch2Freq) * 4)
        ch2Timer -= cycles
        while (ch2Timer <= 0) {
            ch2Timer += ch2Period
            ch2WaveIndex = (ch2WaveIndex + 1) and 0x07
        }

        // Ch3
        val ch3Period = maxOf(2, (2048 - ch3Freq) * 2)
        ch3Timer -= cycles
        while (ch3Timer <= 0) {
            ch3Timer += ch3Period
            ch3SampleIndex = (ch3SampleIndex + 1) and 0x1F
        }

        // Ch4
        ch4Timer -= cycles
        while (ch4Timer <= 0) {
            ch4Timer += getNoisePeriod()
            val xorBit = (ch4Lfsr and 1) xor ((ch4Lfsr ushr 1) and 1)
            ch4Lfsr = (ch4Lfsr ushr 1) or (xorBit shl 14)
            if (ch4WidthMode7) {
                ch4Lfsr = (ch4Lfsr and 0x7FBF) or (xorBit shl 6)
            }
        }
    }

    private fun stepFrameSequencer() {
        when (frameSequencerStep) {
            0 -> clockLength()
            2 -> { clockLength(); clockSweep() }
            4 -> clockLength()
            6 -> { clockLength(); clockSweep() }
            7 -> clockEnvelope()
        }
        frameSequencerStep = (frameSequencerStep + 1) and 0x07
    }

    private fun clockLength() {
        if (ch1LengthEnabled && ch1Length > 0) {
            ch1Length--
            if (ch1Length == 0) ch1Active = false
        }
        if (ch2LengthEnabled && ch2Length > 0) {
            ch2Length--
            if (ch2Length == 0) ch2Active = false
        }
        if (ch3LengthEnabled && ch3Length > 0) {
            ch3Length--
            if (ch3Length == 0) ch3Active = false
        }
        if (ch4LengthEnabled && ch4Length > 0) {
            ch4Length--
            if (ch4Length == 0) ch4Active = false
        }
    }

    private fun clockSweep() {
        if (ch1SweepTime > 0) {
            ch1SweepTimer--
            if (ch1SweepTimer <= 0) {
                ch1SweepTimer = if (ch1SweepTime > 0) ch1SweepTime else 8
                if (ch1SweepShift > 0 && ch1Active) {
                    val delta = ch1ShadowFreq ushr ch1SweepShift
                    val newFreq = if (ch1SweepDir == 0) ch1ShadowFreq + delta else ch1ShadowFreq - delta
                    if (newFreq <= 2047) {
                        ch1ShadowFreq = newFreq
                        ch1Freq = newFreq
                    } else {
                        ch1Active = false
                    }
                }
            }
        }
    }

    private fun clockEnvelope() {
        // Ch1
        if (ch1EnvelopePace > 0) {
            ch1EnvelopeTimer--
            if (ch1EnvelopeTimer <= 0) {
                ch1EnvelopeTimer = ch1EnvelopePace
                if (ch1EnvelopeDir == 1 && ch1CurrentVolume < 15) ch1CurrentVolume++
                else if (ch1EnvelopeDir == 0 && ch1CurrentVolume > 0) ch1CurrentVolume--
            }
        }
        // Ch2
        if (ch2EnvelopePace > 0) {
            ch2EnvelopeTimer--
            if (ch2EnvelopeTimer <= 0) {
                ch2EnvelopeTimer = ch2EnvelopePace
                if (ch2EnvelopeDir == 1 && ch2CurrentVolume < 15) ch2CurrentVolume++
                else if (ch2EnvelopeDir == 0 && ch2CurrentVolume > 0) ch2CurrentVolume--
            }
        }
        // Ch4
        if (ch4EnvelopePace > 0) {
            ch4EnvelopeTimer--
            if (ch4EnvelopeTimer <= 0) {
                ch4EnvelopeTimer = ch4EnvelopePace
                if (ch4EnvelopeDir == 1 && ch4CurrentVolume < 15) ch4CurrentVolume++
                else if (ch4EnvelopeDir == 0 && ch4CurrentVolume > 0) ch4CurrentVolume--
            }
        }
    }

    private fun generateSample() {
        var sample1 = 0
        var sample2 = 0
        var sample3 = 0
        var sample4 = 0

        if (ch1Active) {
            val dutyBit = dutyPatterns[ch1Duty][ch1WaveIndex]
            sample1 = if (dutyBit == 1) ch1CurrentVolume else -ch1CurrentVolume
        }

        if (ch2Active) {
            val dutyBit = dutyPatterns[ch2Duty][ch2WaveIndex]
            sample2 = if (dutyBit == 1) ch2CurrentVolume else -ch2CurrentVolume
        }

        if (ch3Active && ch3DacEnabled) {
            val byteIndex = ch3SampleIndex / 2
            val rawByte = wavePattern[byteIndex].toInt() and 0xFF
            val sample4bit = if ((ch3SampleIndex and 1) == 0) rawByte ushr 4 else rawByte and 0x0F
            val shifted = when (ch3VolumeCode) {
                1 -> sample4bit
                2 -> sample4bit ushr 1
                3 -> sample4bit ushr 2
                else -> 0
            }
            sample3 = (shifted - 8) * 2
        }

        if (ch4Active) {
            val bit = ch4Lfsr and 1
            sample4 = if (bit == 0) ch4CurrentVolume else -ch4CurrentVolume
        }

        var left = 0
        var right = 0

        // Panning (NR51)
        if ((nr51 and 0x10) != 0) left += sample1
        if ((nr51 and 0x01) != 0) right += sample1
        if ((nr51 and 0x20) != 0) left += sample2
        if ((nr51 and 0x02) != 0) right += sample2
        if ((nr51 and 0x40) != 0) left += sample3
        if ((nr51 and 0x04) != 0) right += sample3
        if ((nr51 and 0x80) != 0) left += sample4
        if ((nr51 and 0x08) != 0) right += sample4

        // Volume scaling (NR50)
        val leftVol = ((nr50 ushr 4) and 0x07) + 1
        val rightVol = (nr50 and 0x07) + 1

        val leftRaw = (left * leftVol * 64).toFloat()
        val rightRaw = (right * rightVol * 64).toFloat()

        // Apply high-pass capacitor filter to eliminate DC bias and popping clicks
        val leftFiltered = leftRaw - leftCap
        leftCap = leftRaw - leftFiltered * highPassRate

        val rightFiltered = rightRaw - rightCap
        rightCap = rightRaw - rightFiltered * highPassRate

        val leftFinal = leftFiltered.coerceIn(-32768f, 32767f).toInt().toShort()
        val rightFinal = rightFiltered.coerceIn(-32768f, 32767f).toInt().toShort()

        if (sampleBufferIndex + 1 < sampleBuffer.size) {
            sampleBuffer[sampleBufferIndex++] = leftFinal
            sampleBuffer[sampleBufferIndex++] = rightFinal
        }

        if (sampleBufferIndex >= sampleBuffer.size) {
            onAudioBufferFull?.invoke(sampleBuffer, sampleBufferIndex)
            sampleBufferIndex = 0
        }
    }

    /**
     * Flushes any remaining audio samples to the audio output callback at frame boundaries.
     */
    fun flushAudio() {
        if (sampleBufferIndex > 0) {
            onAudioBufferFull?.invoke(sampleBuffer, sampleBufferIndex)
            sampleBufferIndex = 0
        }
    }
}
