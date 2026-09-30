package com.novagb.emulator.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CartridgeTest {

    @Test
    fun testCartridgeHeaderParsing() {
        val rom = ByteArray(32768)
        val title = "TETRIS"
        for (i in title.indices) {
            rom[0x0134 + i] = title[i].code.toByte()
        }
        rom[0x0147] = 0x01 // MBC1
        rom[0x0148] = 0x01 // 64KB ROM
        rom[0x0149] = 0x02 // 8KB RAM

        val cart = Cartridge.fromRom(rom)
        assertEquals("TETRIS", cart.header.title)
        assertEquals(1, cart.header.cartridgeType)
        assertEquals(65536, cart.header.romSizeBytes)
        assertEquals(8192, cart.header.ramSizeBytes)
        assertFalse(cart.header.isCgb)
    }

    @Test
    fun testMbc1RomBankSwitching() {
        // 128KB ROM (8 banks)
        val rom = ByteArray(131072)
        // Fill bank 2 with 0x42
        val bank2Offset = 2 * 0x4000
        rom[bank2Offset] = 0x42

        val mbc = Mbc1(rom, 8192, false)

        // Select bank 2: write 2 to 0x2000
        mbc.writeRom(0x2000, 2)
        val readVal = mbc.readRom(0x4000)
        assertEquals(0x42, readVal)
    }
}