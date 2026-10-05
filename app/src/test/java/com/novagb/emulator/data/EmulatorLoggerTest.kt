package com.novagb.emulator.data

import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EmulatorLoggerTest {

    @Before
    fun setUp() {
        EmulatorLogger.clear()
    }

    @Test
    fun testLogInfoAndWarnAppendsLogs() {
        EmulatorLogger.logInfo("TestTag", "Sample info message")
        EmulatorLogger.logWarn("TestTag", "Sample warning message")

        val output = EmulatorLogger.getFormattedLogs()
        assertTrue(output.contains("[INFO/TestTag] Sample info message"))
        assertTrue(output.contains("[WARN/TestTag] Sample warning message"))
    }

    @Test
    fun testLogErrorCapturesStackTrace() {
        val exception = IllegalStateException("Test crash")
        EmulatorLogger.logError("CrashTag", "Fatal error occurred", exception)

        val output = EmulatorLogger.getFormattedLogs()
        assertTrue(output.contains("[ERROR/CrashTag] Fatal error occurred"))
        assertTrue(output.contains("IllegalStateException: Test crash"))
        assertTrue(output.contains("--- Stack Trace ---"))
    }
}
