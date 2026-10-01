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
 *
 * Register 0xFF00 (P1/JOYP):
 * Bit 7-6: Unused, always return 1
 * Bit 5: Select Button keys (0 = select, 1 = deselect) -> P15
 * Bit 4: Select Direction keys (0 = select, 1 = deselect) -> P14
 * Bit 3: Down or Start (0 = pressed, 1 = released)
 * Bit 2: Up or Select (0 = pressed, 1 = released)
 * Bit 1: Left or B (0 = pressed, 1 = released)
 * Bit 0: Right or A (0 = pressed, 1 = released)
 */
class Joypad(private val mmu: Mmu) {

    // Bits 4 and 5 of 0xFF00. Default state on DMG boot is both deselected (0x30).
    private var joypSelect: Int = 0x30

    private val buttonStates = BooleanArray(8) // true = pressed

    fun reset() {
        joypSelect = 0x30
        buttonStates.fill(false)
    }

    fun setButtonPressed(button: JoypadButton, pressed: Boolean) {
        val wasPressed = buttonStates[button.ordinal]
        buttonStates[button.ordinal] = pressed

        if (!wasPressed && pressed) {
            val isAction = isActionButton(button)
            val selectAction = (joypSelect and 0x20) == 0
            val selectDirection = (joypSelect and 0x10) == 0

            // Joypad interrupt is triggered when a selected input line transitions to low
            if ((isAction && selectAction) || (!isAction && selectDirection)) {
                mmu.requestInterrupt(InterruptType.JOYPAD)
            }
        }
    }

    fun isButtonPressed(button: JoypadButton): Boolean = buttonStates[button.ordinal]

    fun read(): Int {
        var result = 0xC0 or (joypSelect and 0x30)
        var lowNibble = 0x0F

        val selectAction = (joypSelect and 0x20) == 0
        val selectDirection = (joypSelect and 0x10) == 0

        if (selectAction) {
            if (buttonStates[JoypadButton.A.ordinal]) lowNibble = lowNibble and 0x0E
            if (buttonStates[JoypadButton.B.ordinal]) lowNibble = lowNibble and 0x0D
            if (buttonStates[JoypadButton.SELECT.ordinal]) lowNibble = lowNibble and 0x0B
            if (buttonStates[JoypadButton.START.ordinal]) lowNibble = lowNibble and 0x07
        }

        if (selectDirection) {
            if (buttonStates[JoypadButton.RIGHT.ordinal]) lowNibble = lowNibble and 0x0E
            if (buttonStates[JoypadButton.LEFT.ordinal]) lowNibble = lowNibble and 0x0D
            if (buttonStates[JoypadButton.UP.ordinal]) lowNibble = lowNibble and 0x0B
            if (buttonStates[JoypadButton.DOWN.ordinal]) lowNibble = lowNibble and 0x07
        }

        return result or lowNibble
    }

    fun write(value: Int) {
        val oldSelectAction = (joypSelect and 0x20) == 0
        val oldSelectDirection = (joypSelect and 0x10) == 0

        joypSelect = value and 0x30

        val newSelectAction = (joypSelect and 0x20) == 0
        val newSelectDirection = (joypSelect and 0x10) == 0

        val actionTransition = !oldSelectAction && newSelectAction && hasAnyActionPressed()
        val directionTransition = !oldSelectDirection && newSelectDirection && hasAnyDirectionPressed()

        if (actionTransition || directionTransition) {
            mmu.requestInterrupt(InterruptType.JOYPAD)
        }
    }

    private fun isActionButton(button: JoypadButton): Boolean {
        return button == JoypadButton.A ||
                button == JoypadButton.B ||
                button == JoypadButton.SELECT ||
                button == JoypadButton.START
    }

    private fun hasAnyActionPressed(): Boolean {
        return buttonStates[JoypadButton.A.ordinal] ||
                buttonStates[JoypadButton.B.ordinal] ||
                buttonStates[JoypadButton.SELECT.ordinal] ||
                buttonStates[JoypadButton.START.ordinal]
    }

    private fun hasAnyDirectionPressed(): Boolean {
        return buttonStates[JoypadButton.RIGHT.ordinal] ||
                buttonStates[JoypadButton.LEFT.ordinal] ||
                buttonStates[JoypadButton.UP.ordinal] ||
                buttonStates[JoypadButton.DOWN.ordinal]
    }
}
