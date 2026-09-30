package com.novagb.emulator.core

enum class JoypadButton {
    RIGHT,
    LEFT,
    UP,
    DOWN,
    A,
    B,
    SELECT,
    START
}

/**
 * Game Boy Joypad controller (IO register 0xFF00).
 */
class Joypad(private val mmu: Mmu) {

    private var selectAction: Boolean = false
    private var selectDirection: Boolean = false

    private val buttonStates = BooleanArray(8) // true = pressed

    fun setButtonPressed(button: JoypadButton, pressed: Boolean) {
        val wasPressed = buttonStates[button.ordinal]
        buttonStates[button.ordinal] = pressed

        if (!wasPressed && pressed) {
            // Request joypad interrupt on button press
            mmu.requestInterrupt(InterruptType.JOYPAD)
        }
    }

    fun isButtonPressed(button: JoypadButton): Boolean = buttonStates[button.ordinal]

    fun read(): Int {
        var result = 0xCF

        if (!selectAction) {
            result = result and 0xDF
            if (buttonStates[JoypadButton.A.ordinal]) result = result and 0xFE
            if (buttonStates[JoypadButton.B.ordinal]) result = result and 0xFD
            if (buttonStates[JoypadButton.SELECT.ordinal]) result = result and 0xFB
            if (buttonStates[JoypadButton.START.ordinal]) result = result and 0xF7
        }

        if (!selectDirection) {
            result = result and 0xEF
            if (buttonStates[JoypadButton.RIGHT.ordinal]) result = result and 0xFE
            if (buttonStates[JoypadButton.LEFT.ordinal]) result = result and 0xFD
            if (buttonStates[JoypadButton.UP.ordinal]) result = result and 0xFB
            if (buttonStates[JoypadButton.DOWN.ordinal]) result = result and 0xF7
        }

        return result
    }

    fun write(value: Int) {
        selectAction = (value and 0x20) != 0
        selectDirection = (value and 0x10) != 0
    }
}
