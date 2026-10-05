package com.novagb.emulator.core

/**
 * Emulates the Sharp LR35902 8-bit CPU used in Game Boy (DMG) and Game Boy Color.
 * Operates at 4.194304 MHz clock speed.
 */
class Cpu(private val mmu: Mmu) {

    var a: Int = 0x01
    var f: Int = 0xB0
    var b: Int = 0x00
    var c: Int = 0x13
    var d: Int = 0x00
    var e: Int = 0xD8
    var h: Int = 0x01
    var l: Int = 0x4D

    var sp: Int = 0xFFFE
    var pc: Int = 0x0100

    var flagZ: Boolean
        get() = (f and 0x80) != 0
        set(value) { f = if (value) f or 0x80 else f and 0x80.inv() }

    var flagN: Boolean
        get() = (f and 0x40) != 0
        set(value) { f = if (value) f or 0x40 else f and 0x40.inv() }

    var flagH: Boolean
        get() = (f and 0x20) != 0
        set(value) { f = if (value) f or 0x20 else f and 0x20.inv() }

    var flagC: Boolean
        get() = (f and 0x10) != 0
        set(value) { f = if (value) f or 0x10 else f and 0x10.inv() }

    var af: Int
        get() = (a shl 8) or (f and 0xF0)
        set(v) { a = (v ushr 8) and 0xFF; f = v and 0xF0 }

    var bc: Int
        get() = (b shl 8) or c
        set(v) { b = (v ushr 8) and 0xFF; c = v and 0xFF }

    var de: Int
        get() = (d shl 8) or e
        set(v) { d = (v ushr 8) and 0xFF; e = v and 0xFF }

    var hl: Int
        get() = (h shl 8) or l
        set(v) { h = (v ushr 8) and 0xFF; l = v and 0xFF }

    var ime: Boolean = false
    var imeDelay: Int = 0
    var imePending: Boolean
        get() = imeDelay > 0
        set(value) { imeDelay = if (value) 2 else 0 }
    var haltBug: Boolean = false
    var halted: Boolean = false
    var stopped: Boolean = false

    fun reset() {
        a = 0x01
        f = 0xB0
        b = 0x00
        c = 0x13
        d = 0x00
        e = 0xD8
        h = 0x01
        l = 0x4D
        sp = 0xFFFE
        pc = 0x0100
        ime = false
        imeDelay = 0
        haltBug = false
        halted = false
        stopped = false
    }

    private fun readByte(addr: Int): Int = mmu.readByte(addr)
    private fun writeByte(addr: Int, value: Int) = mmu.writeByte(addr, value)

    private fun fetchByte(): Int {
        val v = readByte(pc)
        if (haltBug) {
            haltBug = false
        } else {
            pc = (pc + 1) and 0xFFFF
        }
        return v
    }

    private fun fetchWord(): Int {
        val low = fetchByte()
        val high = fetchByte()
        return (high shl 8) or low
    }

    private fun push(value: Int) {
        sp = (sp - 1) and 0xFFFF
        writeByte(sp, (value ushr 8) and 0xFF)
        sp = (sp - 1) and 0xFFFF
        writeByte(sp, value and 0xFF)
    }

    private fun pop(): Int {
        val low = readByte(sp)
        sp = (sp + 1) and 0xFFFF
        val high = readByte(sp)
        sp = (sp + 1) and 0xFFFF
        return (high shl 8) or low
    }

    fun step(): Int {
        val intCycles = handleInterrupts()
        if (intCycles > 0) return intCycles

        if (halted) return 4

        val opcode = fetchByte()
        val cycles = executeOpcode(opcode)

        if (imeDelay > 0) {
            imeDelay--
            if (imeDelay == 0) {
                ime = true
            }
        }

        return cycles
    }

    private fun handleInterrupts(): Int {
        val ifReg = mmu.readByte(0xFF0F)
        val ieReg = mmu.readByte(0xFFFF)
        val pending = ifReg and ieReg and 0x1F

        if (pending == 0) return 0

        if (halted) halted = false
        if (!ime) return 0

        ime = false
        imeDelay = 0

        for (bit in 0..4) {
            val mask = 1 shl bit
            if ((pending and mask) != 0) {
                mmu.writeByte(0xFF0F, ifReg and mask.inv())
                push(pc)
                pc = 0x0040 + (bit * 8)
                return 20
            }
        }
        return 0
    }

    private fun executeOpcode(op: Int): Int {
        return when (op) {
            0x00 -> 4
            0x01 -> { bc = fetchWord(); 12 }
            0x02 -> { writeByte(bc, a); 8 }
            0x03 -> { bc = (bc + 1) and 0xFFFF; 8 }
            0x04 -> { b = inc8(b); 4 }
            0x05 -> { b = dec8(b); 4 }
            0x06 -> { b = fetchByte(); 8 }
            0x07 -> {
                val carry = (a and 0x80) != 0
                a = ((a shl 1) or (if (carry) 1 else 0)) and 0xFF
                flagZ = false; flagN = false; flagH = false; flagC = carry
                4
            }
            0x08 -> {
                val addr = fetchWord()
                writeByte(addr, sp and 0xFF)
                writeByte((addr + 1) and 0xFFFF, (sp ushr 8) and 0xFF)
                20
            }
            0x09 -> { addHl(bc); 8 }
            0x0A -> { a = readByte(bc); 8 }
            0x0B -> { bc = (bc - 1) and 0xFFFF; 8 }
            0x0C -> { c = inc8(c); 4 }
            0x0D -> { c = dec8(c); 4 }
            0x0E -> { c = fetchByte(); 8 }
            0x0F -> {
                val carry = (a and 1) != 0
                a = (a ushr 1) or (if (carry) 0x80 else 0)
                flagZ = false; flagN = false; flagH = false; flagC = carry
                4
            }
            0x10 -> { stopped = true; 4 }
            0x11 -> { de = fetchWord(); 12 }
            0x12 -> { writeByte(de, a); 8 }
            0x13 -> { de = (de + 1) and 0xFFFF; 8 }
            0x14 -> { d = inc8(d); 4 }
            0x15 -> { d = dec8(d); 4 }
            0x16 -> { d = fetchByte(); 8 }
            0x17 -> {
                val carry = flagC
                val newCarry = (a and 0x80) != 0
                a = ((a shl 1) or (if (carry) 1 else 0)) and 0xFF
                flagZ = false; flagN = false; flagH = false; flagC = newCarry
                4
            }
            0x18 -> {
                val offset = fetchByte().toByte().toInt()
                pc = (pc + offset) and 0xFFFF
                12
            }
            0x19 -> { addHl(de); 8 }
            0x1A -> { a = readByte(de); 8 }
            0x1B -> { de = (de - 1) and 0xFFFF; 8 }
            0x1C -> { e = inc8(e); 4 }
            0x1D -> { e = dec8(e); 4 }
            0x1E -> { e = fetchByte(); 8 }
            0x1F -> {
                val carry = flagC
                val newCarry = (a and 1) != 0
                a = (a ushr 1) or (if (carry) 0x80 else 0)
                flagZ = false; flagN = false; flagH = false; flagC = newCarry
                4
            }
            0x20 -> {
                val offset = fetchByte().toByte().toInt()
                if (!flagZ) { pc = (pc + offset) and 0xFFFF; 12 } else 8
            }
            0x21 -> { hl = fetchWord(); 12 }
            0x22 -> { writeByte(hl, a); hl = (hl + 1) and 0xFFFF; 8 }
            0x23 -> { hl = (hl + 1) and 0xFFFF; 8 }
            0x24 -> { h = inc8(h); 4 }
            0x25 -> { h = dec8(h); 4 }
            0x26 -> { h = fetchByte(); 8 }
            0x27 -> { daa(); 4 }
            0x28 -> {
                val offset = fetchByte().toByte().toInt()
                if (flagZ) { pc = (pc + offset) and 0xFFFF; 12 } else 8
            }
            0x29 -> { addHl(hl); 8 }
            0x2A -> { a = readByte(hl); hl = (hl + 1) and 0xFFFF; 8 }
            0x2B -> { hl = (hl - 1) and 0xFFFF; 8 }
            0x2C -> { l = inc8(l); 4 }
            0x2D -> { l = dec8(l); 4 }
            0x2E -> { l = fetchByte(); 8 }
            0x2F -> { a = a.inv() and 0xFF; flagN = true; flagH = true; 4 }
            0x30 -> {
                val offset = fetchByte().toByte().toInt()
                if (!flagC) { pc = (pc + offset) and 0xFFFF; 12 } else 8
            }
            0x31 -> { sp = fetchWord(); 12 }
            0x32 -> { writeByte(hl, a); hl = (hl - 1) and 0xFFFF; 8 }
            0x33 -> { sp = (sp + 1) and 0xFFFF; 8 }
            0x34 -> { writeByte(hl, inc8(readByte(hl))); 12 }
            0x35 -> { writeByte(hl, dec8(readByte(hl))); 12 }
            0x36 -> { writeByte(hl, fetchByte()); 12 }
            0x37 -> { flagN = false; flagH = false; flagC = true; 4 }
            0x38 -> {
                val offset = fetchByte().toByte().toInt()
                if (flagC) { pc = (pc + offset) and 0xFFFF; 12 } else 8
            }
            0x39 -> { addHl(sp); 8 }
            0x3A -> { a = readByte(hl); hl = (hl - 1) and 0xFFFF; 8 }
            0x3B -> { sp = (sp - 1) and 0xFFFF; 8 }
            0x3C -> { a = inc8(a); 4 }
            0x3D -> { a = dec8(a); 4 }
            0x3E -> { a = fetchByte(); 8 }
            0x3F -> { flagN = false; flagH = false; flagC = !flagC; 4 }

            0x40 -> 4
            0x41 -> { b = c; 4 }
            0x42 -> { b = d; 4 }
            0x43 -> { b = e; 4 }
            0x44 -> { b = h; 4 }
            0x45 -> { b = l; 4 }
            0x46 -> { b = readByte(hl); 8 }
            0x47 -> { b = a; 4 }

            0x48 -> { c = b; 4 }
            0x49 -> 4
            0x4A -> { c = d; 4 }
            0x4B -> { c = e; 4 }
            0x4C -> { c = h; 4 }
            0x4D -> { c = l; 4 }
            0x4E -> { c = readByte(hl); 8 }
            0x4F -> { c = a; 4 }

            0x50 -> { d = b; 4 }
            0x51 -> { d = c; 4 }
            0x52 -> 4
            0x53 -> { d = e; 4 }
            0x54 -> { d = h; 4 }
            0x55 -> { d = l; 4 }
            0x56 -> { d = readByte(hl); 8 }
            0x57 -> { d = a; 4 }

            0x58 -> { e = b; 4 }
            0x59 -> { e = c; 4 }
            0x5A -> { e = d; 4 }
            0x5B -> 4
            0x5C -> { e = h; 4 }
            0x5D -> { e = l; 4 }
            0x5E -> { e = readByte(hl); 8 }
            0x5F -> { e = a; 4 }

            0x60 -> { h = b; 4 }
            0x61 -> { h = c; 4 }
            0x62 -> { h = d; 4 }
            0x63 -> { h = e; 4 }
            0x64 -> 4
            0x65 -> { h = l; 4 }
            0x66 -> { h = readByte(hl); 8 }
            0x67 -> { h = a; 4 }

            0x68 -> { l = b; 4 }
            0x69 -> { l = c; 4 }
            0x6A -> { l = d; 4 }
            0x6B -> { l = e; 4 }
            0x6C -> { l = h; 4 }
            0x6D -> 4
            0x6E -> { l = readByte(hl); 8 }
            0x6F -> { l = a; 4 }

            0x70 -> { writeByte(hl, b); 8 }
            0x71 -> { writeByte(hl, c); 8 }
            0x72 -> { writeByte(hl, d); 8 }
            0x73 -> { writeByte(hl, e); 8 }
            0x74 -> { writeByte(hl, h); 8 }
            0x75 -> { writeByte(hl, l); 8 }
            0x76 -> {
                val pending = (mmu.readByte(0xFF0F) and mmu.readByte(0xFFFF) and 0x1F) != 0
                if (!ime && pending) {
                    haltBug = true
                } else {
                    halted = true
                }
                4
            }
            0x77 -> { writeByte(hl, a); 8 }

            0x78 -> { a = b; 4 }
            0x79 -> { a = c; 4 }
            0x7A -> { a = d; 4 }
            0x7B -> { a = e; 4 }
            0x7C -> { a = h; 4 }
            0x7D -> { a = l; 4 }
            0x7E -> { a = readByte(hl); 8 }
            0x7F -> 4

            0x80 -> { add(b); 4 }
            0x81 -> { add(c); 4 }
            0x82 -> { add(d); 4 }
            0x83 -> { add(e); 4 }
            0x84 -> { add(h); 4 }
            0x85 -> { add(l); 4 }
            0x86 -> { add(readByte(hl)); 8 }
            0x87 -> { add(a); 4 }

            0x88 -> { adc(b); 4 }
            0x89 -> { adc(c); 4 }
            0x8A -> { adc(d); 4 }
            0x8B -> { adc(e); 4 }
            0x8C -> { adc(h); 4 }
            0x8D -> { adc(l); 4 }
            0x8E -> { adc(readByte(hl)); 8 }
            0x8F -> { adc(a); 4 }

            0x90 -> { sub(b); 4 }
            0x91 -> { sub(c); 4 }
            0x92 -> { sub(d); 4 }
            0x93 -> { sub(e); 4 }
            0x94 -> { sub(h); 4 }
            0x95 -> { sub(l); 4 }
            0x96 -> { sub(readByte(hl)); 8 }
            0x97 -> { sub(a); 4 }

            0x98 -> { sbc(b); 4 }
            0x99 -> { sbc(c); 4 }
            0x9A -> { sbc(d); 4 }
            0x9B -> { sbc(e); 4 }
            0x9C -> { sbc(h); 4 }
            0x9D -> { sbc(l); 4 }
            0x9E -> { sbc(readByte(hl)); 8 }
            0x9F -> { sbc(a); 4 }

            0xA0 -> { andOp(b); 4 }
            0xA1 -> { andOp(c); 4 }
            0xA2 -> { andOp(d); 4 }
            0xA3 -> { andOp(e); 4 }
            0xA4 -> { andOp(h); 4 }
            0xA5 -> { andOp(l); 4 }
            0xA6 -> { andOp(readByte(hl)); 8 }
            0xA7 -> { andOp(a); 4 }

            0xA8 -> { xorOp(b); 4 }
            0xA9 -> { xorOp(c); 4 }
            0xAA -> { xorOp(d); 4 }
            0xAB -> { xorOp(e); 4 }
            0xAC -> { xorOp(h); 4 }
            0xAD -> { xorOp(l); 4 }
            0xAE -> { xorOp(readByte(hl)); 8 }
            0xAF -> { xorOp(a); 4 }

            0xB0 -> { orOp(b); 4 }
            0xB1 -> { orOp(c); 4 }
            0xB2 -> { orOp(d); 4 }
            0xB3 -> { orOp(e); 4 }
            0xB4 -> { orOp(h); 4 }
            0xB5 -> { orOp(l); 4 }
            0xB6 -> { orOp(readByte(hl)); 8 }
            0xB7 -> { orOp(a); 4 }

            0xB8 -> { cp(b); 4 }
            0xB9 -> { cp(c); 4 }
            0xBA -> { cp(d); 4 }
            0xBB -> { cp(e); 4 }
            0xBC -> { cp(h); 4 }
            0xBD -> { cp(l); 4 }
            0xBE -> { cp(readByte(hl)); 8 }
            0xBF -> { cp(a); 4 }

            0xC0 -> { if (!flagZ) { pc = pop(); 20 } else 8 }
            0xC1 -> { bc = pop(); 12 }
            0xC2 -> { val addr = fetchWord(); if (!flagZ) { pc = addr; 16 } else 12 }
            0xC3 -> { pc = fetchWord(); 16 }
            0xC4 -> { val addr = fetchWord(); if (!flagZ) { push(pc); pc = addr; 24 } else 12 }
            0xC5 -> { push(bc); 16 }
            0xC6 -> { add(fetchByte()); 8 }
            0xC7 -> { push(pc); pc = 0x0000; 16 }
            0xC8 -> { if (flagZ) { pc = pop(); 20 } else 8 }
            0xC9 -> { pc = pop(); 16 }
            0xCA -> { val addr = fetchWord(); if (flagZ) { pc = addr; 16 } else 12 }
            0xCB -> executeCbOpcode(fetchByte())
            0xCC -> { val addr = fetchWord(); if (flagZ) { push(pc); pc = addr; 24 } else 12 }
            0xCD -> { val addr = fetchWord(); push(pc); pc = addr; 24 }
            0xCE -> { adc(fetchByte()); 8 }
            0xCF -> { push(pc); pc = 0x0008; 16 }

            0xD0 -> { if (!flagC) { pc = pop(); 20 } else 8 }
            0xD1 -> { de = pop(); 12 }
            0xD2 -> { val addr = fetchWord(); if (!flagC) { pc = addr; 16 } else 12 }
            0xD3 -> 4
            0xD4 -> { val addr = fetchWord(); if (!flagC) { push(pc); pc = addr; 24 } else 12 }
            0xD5 -> { push(de); 16 }
            0xD6 -> { sub(fetchByte()); 8 }
            0xD7 -> { push(pc); pc = 0x0010; 16 }
            0xD8 -> { if (flagC) { pc = pop(); 20 } else 8 }
            0xD9 -> { pc = pop(); ime = true; imeDelay = 0; 16 }
            0xDA -> { val addr = fetchWord(); if (flagC) { pc = addr; 16 } else 12 }
            0xDB -> 4
            0xDC -> { val addr = fetchWord(); if (flagC) { push(pc); pc = addr; 24 } else 12 }
            0xDD -> 4
            0xDE -> { sbc(fetchByte()); 8 }
            0xDF -> { push(pc); pc = 0x0018; 16 }

            0xE0 -> { writeByte(0xFF00 or fetchByte(), a); 12 }
            0xE1 -> { hl = pop(); 12 }
            0xE2 -> { writeByte(0xFF00 or c, a); 8 }
            0xE3 -> 4
            0xE4 -> 4
            0xE5 -> { push(hl); 16 }
            0xE6 -> { andOp(fetchByte()); 8 }
            0xE7 -> { push(pc); pc = 0x0020; 16 }
            0xE8 -> {
                val offset = fetchByte().toByte().toInt()
                flagZ = false; flagN = false
                flagH = ((sp and 0x0F) + (offset and 0x0F)) > 0x0F
                flagC = ((sp and 0xFF) + (offset and 0xFF)) > 0xFF
                sp = (sp + offset) and 0xFFFF
                16
            }
            0xE9 -> { pc = hl; 4 }
            0xEA -> { writeByte(fetchWord(), a); 16 }
            0xEB -> 4
            0xEC -> 4
            0xED -> 4
            0xEE -> { xorOp(fetchByte()); 8 }
            0xEF -> { push(pc); pc = 0x0028; 16 }

            0xF0 -> { a = readByte(0xFF00 or fetchByte()); 12 }
            0xF1 -> { af = pop(); 12 }
            0xF2 -> { a = readByte(0xFF00 or c); 8 }
            0xF3 -> { ime = false; imeDelay = 0; 4 }
            0xF4 -> 4
            0xF5 -> { push(af); 16 }
            0xF6 -> { orOp(fetchByte()); 8 }
            0xF7 -> { push(pc); pc = 0x0030; 16 }
            0xF8 -> {
                val offset = fetchByte().toByte().toInt()
                flagZ = false; flagN = false
                flagH = ((sp and 0x0F) + (offset and 0x0F)) > 0x0F
                flagC = ((sp and 0xFF) + (offset and 0xFF)) > 0xFF
                hl = (sp + offset) and 0xFFFF
                12
            }
            0xF9 -> { sp = hl; 8 }
            0xFA -> { a = readByte(fetchWord()); 16 }
            0xFB -> { imeDelay = 2; 4 }
            0xFC -> 4
            0xFD -> 4
            0xFE -> { cp(fetchByte()); 8 }
            0xFF -> { push(pc); pc = 0x0038; 16 }
            else -> 4
        }
    }

    private fun executeCbOpcode(op: Int): Int {
        val regIndex = op and 0x07
        val bit = (op ushr 3) and 0x07
        val isMemory = regIndex == 6
        val cycles = if (isMemory) 16 else 8

        val value = when (regIndex) {
            0 -> b
            1 -> c
            2 -> d
            3 -> e
            4 -> h
            5 -> l
            6 -> readByte(hl)
            7 -> a
            else -> 0
        }

        val result: Int = when (op ushr 6) {
            0 -> {
                when ((op ushr 3) and 0x07) {
                    0 -> {
                        val carry = (value and 0x80) != 0
                        val res = ((value shl 1) or (if (carry) 1 else 0)) and 0xFF
                        flagZ = res == 0; flagN = false; flagH = false; flagC = carry
                        res
                    }
                    1 -> {
                        val carry = (value and 1) != 0
                        val res = (value ushr 1) or (if (carry) 0x80 else 0)
                        flagZ = res == 0; flagN = false; flagH = false; flagC = carry
                        res
                    }
                    2 -> {
                        val carry = flagC
                        val newCarry = (value and 0x80) != 0
                        val res = ((value shl 1) or (if (carry) 1 else 0)) and 0xFF
                        flagZ = res == 0; flagN = false; flagH = false; flagC = newCarry
                        res
                    }
                    3 -> {
                        val carry = flagC
                        val newCarry = (value and 1) != 0
                        val res = (value ushr 1) or (if (carry) 0x80 else 0)
                        flagZ = res == 0; flagN = false; flagH = false; flagC = newCarry
                        res
                    }
                    4 -> {
                        val carry = (value and 0x80) != 0
                        val res = (value shl 1) and 0xFF
                        flagZ = res == 0; flagN = false; flagH = false; flagC = carry
                        res
                    }
                    5 -> {
                        val carry = (value and 1) != 0
                        val res = (value ushr 1) or (value and 0x80)
                        flagZ = res == 0; flagN = false; flagH = false; flagC = carry
                        res
                    }
                    6 -> {
                        val res = ((value and 0x0F) shl 4) or ((value ushr 4) and 0x0F)
                        flagZ = res == 0; flagN = false; flagH = false; flagC = false
                        res
                    }
                    7 -> {
                        val carry = (value and 1) != 0
                        val res = (value ushr 1) and 0xFF
                        flagZ = res == 0; flagN = false; flagH = false; flagC = carry
                        res
                    }
                    else -> value
                }
            }
            1 -> {
                val isSet = (value and (1 shl bit)) != 0
                flagZ = !isSet
                flagN = false
                flagH = true
                return if (isMemory) 12 else 8
            }
            2 -> value and (1 shl bit).inv()
            3 -> value or (1 shl bit)
            else -> value
        }

        when (regIndex) {
            0 -> b = result
            1 -> c = result
            2 -> d = result
            3 -> e = result
            4 -> h = result
            5 -> l = result
            6 -> writeByte(hl, result)
            7 -> a = result
        }

        return cycles
    }

    private fun inc8(value: Int): Int {
        val result = (value + 1) and 0xFF
        flagZ = result == 0
        flagN = false
        flagH = (value and 0x0F) == 0x0F
        return result
    }

    private fun dec8(value: Int): Int {
        val result = (value - 1) and 0xFF
        flagZ = result == 0
        flagN = true
        flagH = (value and 0x0F) == 0
        return result
    }

    private fun add(value: Int) {
        val res = a + value
        flagZ = (res and 0xFF) == 0
        flagN = false
        flagH = ((a and 0x0F) + (value and 0x0F)) > 0x0F
        flagC = res > 0xFF
        a = res and 0xFF
    }

    private fun adc(value: Int) {
        val c = if (flagC) 1 else 0
        val res = a + value + c
        flagZ = (res and 0xFF) == 0
        flagN = false
        flagH = ((a and 0x0F) + (value and 0x0F) + c) > 0x0F
        flagC = res > 0xFF
        a = res and 0xFF
    }

    private fun sub(value: Int) {
        val res = a - value
        flagZ = (res and 0xFF) == 0
        flagN = true
        flagH = (a and 0x0F) < (value and 0x0F)
        flagC = a < value
        a = res and 0xFF
    }

    private fun sbc(value: Int) {
        val c = if (flagC) 1 else 0
        val res = a - value - c
        flagZ = (res and 0xFF) == 0
        flagN = true
        flagH = (a and 0x0F) < ((value and 0x0F) + c)
        flagC = a < (value + c)
        a = res and 0xFF
    }

    private fun andOp(value: Int) {
        a = (a and value) and 0xFF
        flagZ = a == 0
        flagN = false
        flagH = true
        flagC = false
    }

    private fun xorOp(value: Int) {
        a = (a xor value) and 0xFF
        flagZ = a == 0
        flagN = false
        flagH = false
        flagC = false
    }

    private fun orOp(value: Int) {
        a = (a or value) and 0xFF
        flagZ = a == 0
        flagN = false
        flagH = false
        flagC = false
    }

    private fun cp(value: Int) {
        val res = a - value
        flagZ = (res and 0xFF) == 0
        flagN = true
        flagH = (a and 0x0F) < (value and 0x0F)
        flagC = a < value
    }

    private fun addHl(value: Int) {
        val res = hl + value
        flagN = false
        flagH = ((hl and 0x0FFF) + (value and 0x0FFF)) > 0x0FFF
        flagC = res > 0xFFFF
        hl = res and 0xFFFF
    }

    private fun daa() {
        var adj = 0
        var carry = flagC

        if (flagH || (!flagN && (a and 0x0F) > 0x09)) {
            adj = adj or 0x06
        }
        if (flagC || (!flagN && a > 0x99)) {
            adj = adj or 0x60
            carry = true
        }

        a = if (flagN) (a - adj) and 0xFF else (a + adj) and 0xFF
        flagZ = a == 0
        flagH = false
        flagC = carry
    }
}
