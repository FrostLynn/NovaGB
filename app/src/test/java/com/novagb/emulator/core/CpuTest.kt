package com.novagb.emulator.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CpuTest {

    private lateinit var mmu: Mmu
    private lateinit var cpu: Cpu

    @Before
    fun setup() {
        mmu = Mmu()
        cpu = Cpu(mmu)
    }

    @Test
    fun testRegistersInitialState() {
        assertEquals(0x01, cpu.a)
        assertEquals(0xB0, cpu.f)
        assertEquals(0x0013, cpu.bc)
        assertEquals(0x00D8, cpu.de)
        assertEquals(0x014D, cpu.hl)
        assertEquals(0xFFFE, cpu.sp)
        assertEquals(0x0100, cpu.pc)
        assertTrue(cpu.flagZ)
        assertFalse(cpu.flagN)
        assertTrue(cpu.flagH)
        assertTrue(cpu.flagC)
    }

    @Test
    fun testFlagAccessors() {
        cpu.flagZ = true
        assertTrue(cpu.flagZ)
        cpu.flagZ = false
        assertFalse(cpu.flagZ)

        cpu.flagC = true
        assertTrue(cpu.flagC)
        cpu.flagC = false
        assertFalse(cpu.flagC)
    }

    @Test
    fun testXorInstruction() {
        // Opcode 0xAF: XOR A, A -> sets A=0, Z=1, N=0, H=0, C=0
        mmu.wram[0] = 0xAF.toByte()
        cpu.pc = 0xC000
        cpu.a = 0x55

        val cycles = cpu.step()
        assertEquals(4, cycles)
        assertEquals(0x00, cpu.a)
        assertTrue(cpu.flagZ)
        assertFalse(cpu.flagN)
        assertFalse(cpu.flagH)
        assertFalse(cpu.flagC)
    }

    @Test
    fun testAddInstructionWithCarry() {
        // Opcode 0xC6: ADD A, d8
        mmu.wram[0] = 0xC6.toByte()
        mmu.wram[1] = 0x01.toByte()
        cpu.pc = 0xC000
        cpu.a = 0xFF

        val cycles = cpu.step()
        assertEquals(8, cycles)
        assertEquals(0x00, cpu.a)
        assertTrue(cpu.flagZ)
        assertTrue(cpu.flagC)
    }

    @Test
    fun testCbSwapInstruction() {
        // Opcode 0xCB 0x37: SWAP A
        mmu.wram[0] = 0xCB.toByte()
        mmu.wram[1] = 0x37.toByte()
        cpu.pc = 0xC000
        cpu.a = 0x3F

        val cycles = cpu.step()
        assertEquals(8, cycles)
        assertEquals(0xF3, cpu.a)
        assertFalse(cpu.flagZ)
    }
}