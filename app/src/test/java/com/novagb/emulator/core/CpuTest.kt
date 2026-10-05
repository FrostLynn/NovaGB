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

    @Test
    fun testEiDelayAllowsInstructionBeforeInterrupt() {
        // Setup: EI (0xFB) followed by NOP (0x00), then NOP (0x00)
        // Interrupt is enabled and pending
        mmu.wram[0] = 0xFB.toByte() // EI
        mmu.wram[1] = 0x00.toByte() // NOP
        mmu.wram[2] = 0x00.toByte() // NOP
        cpu.pc = 0xC000
        cpu.sp = 0xFFFE
        cpu.ime = false

        // Pending V-Blank interrupt
        mmu.ieReg = 0x01
        mmu.ifReg = 0x01

        // Step 1: Execute EI
        val c1 = cpu.step()
        assertEquals(4, c1)
        assertEquals(0xC001, cpu.pc)
        assertFalse("IME must still be false immediately after EI", cpu.ime)

        // Step 2: Execute NOP (the instruction following EI)
        val c2 = cpu.step()
        assertEquals(4, c2)
        assertEquals(0xC002, cpu.pc)
        assertTrue("IME must become true after instruction following EI completes", cpu.ime)

        // Step 3: Next step should service the pending interrupt (push PC and jump to 0x0040)
        val c3 = cpu.step()
        assertEquals(20, c3)
        assertEquals(0x0040, cpu.pc)
        assertFalse("IME must be disabled during ISR execution", cpu.ime)
    }

    @Test
    fun testEiFollowedByDiCancelsInterrupt() {
        // Setup: EI (0xFB) immediately followed by DI (0xF3)
        mmu.wram[0] = 0xFB.toByte() // EI
        mmu.wram[1] = 0xF3.toByte() // DI
        mmu.wram[2] = 0x00.toByte() // NOP
        cpu.pc = 0xC000
        cpu.ime = false

        mmu.ieReg = 0x01
        mmu.ifReg = 0x01

        // Step 1: EI
        cpu.step()
        assertFalse(cpu.ime)

        // Step 2: DI executes without interrupt firing in between
        cpu.step()
        assertEquals(0xC002, cpu.pc)
        assertFalse(cpu.ime)

        // Step 3: NOP executes, interrupt still does not fire because DI disabled it
        cpu.step()
        assertEquals(0xC003, cpu.pc)
    }

    @Test
    fun testHaltBugCausesOpcodeByteToRepeat() {
        // Hardware HALT bug occurs when IME=false and an interrupt is pending:
        // PC does NOT increment on the first fetch after HALT.
        // Instruction at 0xC001: 0x3E (LD A, d8), with operand 0x55 at 0xC002
        // Due to bug, 0x3E is read as opcode AND as immediate operand!
        mmu.wram[0] = 0x76.toByte() // HALT
        mmu.wram[1] = 0x3E.toByte() // LD A, d8
        mmu.wram[2] = 0x55.toByte() // operand
        cpu.pc = 0xC000
        cpu.ime = false

        // Interrupt pending
        mmu.ieReg = 0x01
        mmu.ifReg = 0x01

        // Step 1: HALT executes with pending interrupt -> triggers HALT bug
        val c1 = cpu.step()
        assertEquals(4, c1)
        assertFalse("CPU should not stay halted when interrupt is pending", cpu.halted)
        assertEquals(0xC001, cpu.pc)

        // Step 2: Opcode 0x3E is fetched, but PC is NOT incremented!
        // Then immediate operand fetch reads the same address (0xC001), getting 0x3E into A!
        val c2 = cpu.step()
        assertEquals(8, c2)
        assertEquals(0x3E, cpu.a)
        assertEquals(0xC002, cpu.pc) // Now points to 0xC002 (the unconsumed 0x55)
    }

    @Test
    fun testHaltWakeUpWithoutInterruptWhenImeDisabled() {
        // When HALT is executed with no pending interrupts, CPU halts
        mmu.wram[0] = 0x76.toByte() // HALT
        mmu.wram[1] = 0x00.toByte() // NOP
        cpu.pc = 0xC000
        cpu.ime = false
        mmu.ieReg = 0x01
        mmu.ifReg = 0x00

        // Step 1: HALT executes
        cpu.step()
        assertTrue("CPU must be halted", cpu.halted)

        // Interrupt arrives while halted
        mmu.ifReg = 0x01

        // Step 2: Next step wakes up CPU, but does NOT branch to interrupt vector (since IME=0)
        cpu.step()
        assertFalse("CPU must wake up", cpu.halted)
        assertEquals("CPU must resume from next instruction", 0xC002, cpu.pc)
    }

    @Test
    fun testDaaAdditionAndSubtraction() {
        // BCD Add: 0x45 + 0x38 = 0x7D -> DAA -> 0x83
        cpu.a = 0x45
        cpu.flagN = false
        cpu.flagH = ((0x45 and 0x0F) + (0x38 and 0x0F)) > 0x0F // true (5 + 8 = 13 > 9)
        cpu.flagC = false
        cpu.a = (0x45 + 0x38) and 0xFF // 0x7D

        mmu.wram[0] = 0x27.toByte() // DAA
        cpu.pc = 0xC000
        cpu.step()
        assertEquals(0x83, cpu.a)
        assertFalse(cpu.flagC)
        assertFalse(cpu.flagZ)

        // BCD Sub: 0x83 - 0x38 = 0x4B -> DAA -> 0x45
        cpu.a = 0x4B
        cpu.flagN = true
        cpu.flagH = true
        cpu.flagC = false
        cpu.pc = 0xC000
        cpu.step()
        assertEquals(0x45, cpu.a)
        assertFalse(cpu.flagC)
        assertFalse(cpu.flagZ)
    }
}