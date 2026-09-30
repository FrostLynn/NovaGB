package com.novagb.emulator.core

/**
 * JNI Bridge for optionally using native C/C++ Game Boy emulation cores (such as Gambatte or SameBoy).
 */
class NativeGbBridge {

    companion object {
        private var isLoaded = false

        fun init(): Boolean {
            if (!isLoaded) {
                try {
                    System.loadLibrary("novagb-native")
                    isLoaded = true
                } catch (e: UnsatisfiedLinkError) {
                    isLoaded = false
                }
            }
            return isLoaded
        }
    }

    external fun nativeLoadRom(romBytes: ByteArray): Boolean
    external fun nativeStepFrame(): Int
    external fun nativeSetButtons(buttonMask: Int)
    external fun nativeGetFramebuffer(outBuffer: IntArray)
}