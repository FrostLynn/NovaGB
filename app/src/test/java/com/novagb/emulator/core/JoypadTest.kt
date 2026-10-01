package com.novagb.emulator.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class JoypadTest {

    private lateinit var mmu: Mmu
    private lateinit var joypad: Joypad

    @Before
    fun setup() {
        mmu = Mmu()
        joypad = Joypad(mmu)
        mmu.joypad = joypad
    }

    @Test
    fun testInitialState() {
        // Default DMG state: both lines deselected (0x30), no buttons pressed -> 0xFF
        assertEquals(0xFF, joypad.read())
    }

    @Test
    fun testDirectionKeysSelection() {
        joypad.write(0x20) // Select direction keys (P14 = 0, P15 = 1)
        assertEquals(0xEF, joypad.read())

        joypad.setButtonPressed(JoypadButton.RIGHT, true)
        assertEquals(0xEE, joypad.read())

        joypad.setButtonPressed(JoypadButton.LEFT, true)
        assertEquals(0xEC, joypad.read())

        joypad.setButtonPressed(JoypadButton.UP, true)
        assertEquals(0xE8, joypad.read())

        joypad.setButtonPressed(JoypadButton.DOWN, true)
        assertEquals(0xE0, joypad.read())

        joypad.setButtonPressed(JoypadButton.RIGHT, false)
        joypad.setButtonPressed(JoypadButton.LEFT, false)
        joypad.setButtonPressed(JoypadButton.UP, false)
        joypad.setButtonPressed(JoypadButton.DOWN, false)
        assertEquals(0xEF, joypad.read())
    }

    @Test
    fun testActionKeysSelection() {
        joypad.write(0x10) // Select action keys (P15 = 0, P14 = 1)
        assertEquals(0xDF, joypad.read())

        joypad.setButtonPressed(JoypadButton.A, true)
        assertEquals(0xDE, joypad.read())

        joypad.setButtonPressed(JoypadButton.B, true)
        assertEquals(0xDC, joypad.read())

        joypad.setButtonPressed(JoypadButton.SELECT, true)
        assertEquals(0xD8, joypad.read())

        joypad.setButtonPressed(JoypadButton.START, true)
        assertEquals(0xD0, joypad.read())

        joypad.setButtonPressed(JoypadButton.A, false)
        joypad.setButtonPressed(JoypadButton.B, false)
        joypad.setButtonPressed(JoypadButton.SELECT, false)
        joypad.setButtonPressed(JoypadButton.START, false)
        assertEquals(0xDF, joypad.read())
    }

    @Test
    fun testNeitherLineSelected() {
        joypad.write(0x30) // Deselect both lines
        joypad.setButtonPressed(JoypadButton.A, true)
        joypad.setButtonPressed(JoypadButton.RIGHT, true)
        // With both select lines high, reading JOYP must return 0xFF
        assertEquals(0xFF, joypad.read())

        joypad.setButtonPressed(JoypadButton.A, false)
        joypad.setButtonPressed(JoypadButton.RIGHT, false)
    }

    @Test
    fun testBothLinesSelected() {
        joypad.write(0x00) // Select both lines
        assertEquals(0xCF, joypad.read())

        joypad.setButtonPressed(JoypadButton.A, true) // bit 0 (action)
        joypad.setButtonPressed(JoypadButton.UP, true) // bit 2 (direction)
        assertEquals(0xCA, joypad.read())
    }

    @Test
    fun testInterruptTriggeredOnSelectedButtonPress() {
        mmu.ifReg = 0
        joypad.write(0x20) // Direction keys selected

        // Pressing an action key while direction keys are selected should NOT trigger interrupt
        joypad.setButtonPressed(JoypadButton.A, true)
        assertEquals(0, mmu.ifReg and InterruptType.JOYPAD.mask)

        // Pressing a direction key while direction keys are selected SHOULD trigger interrupt
        joypad.setButtonPressed(JoypadButton.RIGHT, true)
        assertTrue((mmu.ifReg and InterruptType.JOYPAD.mask) != 0)
    }

    @Test
    fun testReset() {
        joypad.write(0x10)
        joypad.setButtonPressed(JoypadButton.A, true)
        joypad.reset()

        assertEquals(0xFF, joypad.read())
        assertFalse(joypad.isButtonPressed(JoypadButton.A))
    }
}
