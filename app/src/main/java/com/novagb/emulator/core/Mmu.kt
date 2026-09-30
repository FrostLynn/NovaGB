package com.novagb.emulator.core

/**
 * Memory Management Unit (MMU) / System Bus.
 * Handles read/write routing across Cartridge ROM, VRAM, WRAM, OAM, IO, and HRAM.
 */
class Mmu {

    var cartridge: Cartridge? = null
    var ppu: Ppu? = null
    var apu: Apu? = null
    var timer: Timer? = null
    var joypad: Joypad? = null

    // Video RAM (8KB)
    val vram = ByteArray(0x2000)

    // Work RAM (8KB)
    val wram = ByteArray(0x2000)

    // Sprite Attribute Table / OAM (160 bytes)
    val oam = ByteArray(0xA0)

    // High RAM (127 bytes)
    val hram = ByteArray(0x7F)

    // Interrupt registers
    var ifReg: Int = 0xE1
    var ieReg: Int = 0x00

    // Serial stub
    var serialData: Int = 0
    var serialControl: Int = 0

    fun reset() {
        vram.fill(0)
        wram.fill(0)
        oam.fill(0)
        hram.fill(0)
        ifReg = 0xE1
        ieReg = 0x00
        serialData = 0
        serialControl = 0
    }

    fun readByte(addr: Int): Int {
        val a = addr and 0xFFFF
        return when (a) {
            in 0x0000..0x7FFF -> cartridge?.readRom(a) ?: 0xFF
            in 0x8000..0x9FFF -> vram[a - 0x8000].toInt() and 0xFF
            in 0xA000..0xBFFF -> cartridge?.readRam(a) ?: 0xFF
            in 0xC000..0xDFFF -> wram[a - 0xC000].toInt() and 0xFF
            in 0xE000..0xFDFF -> wram[a - 0xE000].toInt() and 0xFF // Echo RAM
            in 0xFE00..0xFE9F -> oam[a - 0xFE00].toInt() and 0xFF
            in 0xFEA0..0xFEFF -> 0xFF // Not usable
            in 0xFF00..0xFF7F -> readIo(a)
            in 0xFF80..0xFFFE -> hram[a - 0xFF80].toInt() and 0xFF
            0xFFFF -> ieReg
            else -> 0xFF
        }
    }

    fun writeByte(addr: Int, value: Int) {
        val a = addr and 0xFFFF
        val v = value and 0xFF
        when (a) {
            in 0x0000..0x7FFF -> cartridge?.writeRom(a, v)
            in 0x8000..0x9FFF -> vram[a - 0x8000] = v.toByte()
            in 0xA000..0xBFFF -> cartridge?.writeRam(a, v)
            in 0xC000..0xDFFF -> wram[a - 0xC000] = v.toByte()
            in 0xE000..0xFDFF -> wram[a - 0xE000] = v.toByte() // Echo RAM
            in 0xFE00..0xFE9F -> oam[a - 0xFE00] = v.toByte()
            in 0xFEA0..0xFEFF -> {} // Unusable
            in 0xFF00..0xFF7F -> writeIo(a, v)
            in 0xFF80..0xFFFE -> hram[a - 0xFF80] = v.toByte()
            0xFFFF -> ieReg = v
        }
    }

    private fun readIo(addr: Int): Int {
        return when (addr) {
            0xFF00 -> joypad?.read() ?: 0xFF
            0xFF01 -> serialData
            0xFF02 -> serialControl or 0x7E
            in 0xFF04..0xFF07 -> timer?.read(addr) ?: 0xFF
            0xFF0F -> ifReg or 0xE0
            in 0xFF10..0xFF3F -> apu?.read(addr) ?: 0xFF
            in 0xFF40..0xFF4B -> ppu?.read(addr) ?: 0xFF
            else -> 0xFF
        }
    }

    private fun writeIo(addr: Int, value: Int) {
        when (addr) {
            0xFF00 -> joypad?.write(value)
            0xFF01 -> serialData = value
            0xFF02 -> {
                serialControl = value
                if ((value and 0x81) == 0x81) {
                    // Internal clock transfer requested: complete immediately
                    serialControl = value and 0x7F
                    requestInterrupt(InterruptType.SERIAL)
                }
            }
            in 0xFF04..0xFF07 -> timer?.write(addr, value)
            0xFF0F -> ifReg = value and 0x1F
            in 0xFF10..0xFF3F -> apu?.write(addr, value)
            0xFF46 -> runDmaTransfer(value) // OAM DMA
            in 0xFF40..0xFF4B -> ppu?.write(addr, value)
        }
    }

    private fun runDmaTransfer(sourceHighByte: Int) {
        val srcBase = (sourceHighByte shl 8) and 0xFFFF
        for (i in 0 until 0xA0) {
            oam[i] = readByte(srcBase + i).toByte()
        }
    }

    fun requestInterrupt(type: InterruptType) {
        ifReg = ifReg or type.mask
    }
}

enum class InterruptType(val mask: Int) {
    VBLANK(0x01),
    LCD_STAT(0x02),
    TIMER(0x04),
    SERIAL(0x08),
    JOYPAD(0x10)
}
