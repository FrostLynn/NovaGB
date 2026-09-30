package com.novagb.emulator.core

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * Master Game Boy console instance.
 * Coordinates CPU, MMU, PPU, APU, Timer, and Joypad.
 */
class GameBoy {

    val mmu = Mmu()
    val cpu = Cpu(mmu)
    val ppu = Ppu(mmu)
    val apu = Apu()
    val timer = Timer(mmu)
    val joypad = Joypad(mmu)

    var cartridge: Cartridge? = null
        private set

    val cyclesPerFrame = 70224 // 4194304 Hz / ~59.73 FPS

    init {
        mmu.ppu = ppu
        mmu.apu = apu
        mmu.timer = timer
        mmu.joypad = joypad
    }

    fun loadRom(romBytes: ByteArray) {
        val cart = Cartridge.fromRom(romBytes)
        this.cartridge = cart
        mmu.cartridge = cart
        reset()
    }

    fun reset() {
        mmu.reset()
        cpu.reset()
        ppu.reset()
        apu.reset()
        timer.reset()
    }

    fun setButton(button: JoypadButton, pressed: Boolean) {
        joypad.setButtonPressed(button, pressed)
    }

    /**
     * Executes enough CPU/PPU cycles to complete one full frame (~70,224 cycles).
     */
    fun stepFrame(): Int {
        var frameCycles = 0
        while (frameCycles < cyclesPerFrame) {
            val stepCycles = cpu.step()
            timer.step(stepCycles)
            ppu.step(stepCycles)
            apu.step(stepCycles)
            frameCycles += stepCycles
        }
        return frameCycles
    }

    fun getBatterySave(): ByteArray? = cartridge?.getBatterySave()

    fun loadBatterySave(data: ByteArray) {
        cartridge?.loadBatterySave(data)
    }

    /**
     * Serializes complete emulator state into a binary byte array.
     */
    fun saveState(): ByteArray {
        val byteOut = ByteArrayOutputStream()
        val out = DataOutputStream(byteOut)

        // Magic & Version
        out.writeBytes("NVGB")
        out.writeInt(1) // version 1

        // CPU
        out.writeInt(cpu.a)
        out.writeInt(cpu.f)
        out.writeInt(cpu.b)
        out.writeInt(cpu.c)
        out.writeInt(cpu.d)
        out.writeInt(cpu.e)
        out.writeInt(cpu.h)
        out.writeInt(cpu.l)
        out.writeInt(cpu.sp)
        out.writeInt(cpu.pc)
        out.writeBoolean(cpu.ime)
        out.writeBoolean(cpu.imePending)
        out.writeBoolean(cpu.halted)

        // MMU RAM
        out.writeInt(mmu.vram.size)
        out.write(mmu.vram)
        out.writeInt(mmu.wram.size)
        out.write(mmu.wram)
        out.writeInt(mmu.oam.size)
        out.write(mmu.oam)
        out.writeInt(mmu.hram.size)
        out.write(mmu.hram)
        out.writeInt(mmu.ifReg)
        out.writeInt(mmu.ieReg)

        // PPU
        out.writeInt(ppu.lcdc)
        out.writeInt(ppu.stat)
        out.writeInt(ppu.scy)
        out.writeInt(ppu.scx)
        out.writeInt(ppu.ly)
        out.writeInt(ppu.lyc)
        out.writeInt(ppu.bgp)
        out.writeInt(ppu.obp0)
        out.writeInt(ppu.obp1)
        out.writeInt(ppu.wy)
        out.writeInt(ppu.wx)

        // Timer
        out.writeInt(timer.div)
        out.writeInt(timer.tima)
        out.writeInt(timer.tma)
        out.writeInt(timer.tac)

        out.flush()
        return byteOut.toByteArray()
    }

    /**
     * Restores emulator state from a binary snapshot.
     */
    fun loadState(bytes: ByteArray): Boolean {
        try {
            val byteIn = ByteArrayInputStream(bytes)
            val inStream = DataInputStream(byteIn)

            val magic = ByteArray(4)
            inStream.readFully(magic)
            if (String(magic) != "NVGB") return false

            val version = inStream.readInt()
            if (version != 1) return false

            // CPU
            cpu.a = inStream.readInt()
            cpu.f = inStream.readInt()
            cpu.b = inStream.readInt()
            cpu.c = inStream.readInt()
            cpu.d = inStream.readInt()
            cpu.e = inStream.readInt()
            cpu.h = inStream.readInt()
            cpu.l = inStream.readInt()
            cpu.sp = inStream.readInt()
            cpu.pc = inStream.readInt()
            cpu.ime = inStream.readBoolean()
            cpu.imePending = inStream.readBoolean()
            cpu.halted = inStream.readBoolean()

            // MMU
            val vramSize = inStream.readInt()
            inStream.readFully(mmu.vram, 0, minOf(vramSize, mmu.vram.size))
            val wramSize = inStream.readInt()
            inStream.readFully(mmu.wram, 0, minOf(wramSize, mmu.wram.size))
            val oamSize = inStream.readInt()
            inStream.readFully(mmu.oam, 0, minOf(oamSize, mmu.oam.size))
            val hramSize = inStream.readInt()
            inStream.readFully(mmu.hram, 0, minOf(hramSize, mmu.hram.size))
            mmu.ifReg = inStream.readInt()
            mmu.ieReg = inStream.readInt()

            // PPU
            ppu.lcdc = inStream.readInt()
            ppu.stat = inStream.readInt()
            ppu.scy = inStream.readInt()
            ppu.scx = inStream.readInt()
            ppu.ly = inStream.readInt()
            ppu.lyc = inStream.readInt()
            ppu.bgp = inStream.readInt()
            ppu.obp0 = inStream.readInt()
            ppu.obp1 = inStream.readInt()
            ppu.wy = inStream.readInt()
            ppu.wx = inStream.readInt()

            // Timer
            val divVal = inStream.readInt()
            timer.tima = inStream.readInt()
            timer.tma = inStream.readInt()
            timer.tac = inStream.readInt()

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
