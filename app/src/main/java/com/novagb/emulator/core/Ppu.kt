package com.novagb.emulator.core

/**
 * Pixel Processing Unit (PPU) for Game Boy DMG.
 * Renders 160x144 frame buffer at ~59.73 Hz.
 */
class Ppu(private val mmu: Mmu) {

    private val backBuffer = IntArray(160 * 144)
    val framebuffer = IntArray(160 * 144)
    var onFrameReady: ((IntArray) -> Unit)? = null

    // LCD Registers
    var lcdc: Int = 0x91 // LCD enabled, BG enabled
    var stat: Int = 0x85
    var scy: Int = 0
    var scx: Int = 0
    var ly: Int = 0
    var lyc: Int = 0
    var bgp: Int = 0xFC // Default DMG palette
    var obp0: Int = 0xFF
    var obp1: Int = 0xFF
    var wy: Int = 0
    var wx: Int = 0

    // Internal timing
    private var cycleCounter: Int = 0
    private var windowLineCounter: Int = 0
    private val scanlineBgColorIndices = IntArray(160) // For sprite priority checking
    private var prevStatLine: Boolean = false

    // Default DMG Green Palette (ARGB)
    var paletteColors = intArrayOf(
        0xFF9BBC0F.toInt(), // 0: Lightest
        0xFF8BAC0F.toInt(), // 1: Light
        0xFF306230.toInt(), // 2: Dark
        0xFF0F380F.toInt()  // 3: Darkest
    )

    fun reset() {
        backBuffer.fill(paletteColors[0])
        framebuffer.fill(paletteColors[0])
        lcdc = 0x91
        stat = 0x85
        scy = 0
        scx = 0
        ly = 0
        lyc = 0
        bgp = 0xFC
        obp0 = 0xFF
        obp1 = 0xFF
        wy = 0
        wx = 0
        cycleCounter = 0
        windowLineCounter = 0
        prevStatLine = false
    }

    fun read(addr: Int): Int {
        return when (addr) {
            0xFF40 -> lcdc
            0xFF41 -> stat or 0x80
            0xFF42 -> scy
            0xFF43 -> scx
            0xFF44 -> ly
            0xFF45 -> lyc
            0xFF47 -> bgp
            0xFF48 -> obp0
            0xFF49 -> obp1
            0xFF4A -> wy
            0xFF4B -> wx
            else -> 0xFF
        }
    }

    fun write(addr: Int, value: Int) {
        val v = value and 0xFF
        when (addr) {
            0xFF40 -> {
                val wasEnabled = (lcdc and 0x80) != 0
                val nowEnabled = (v and 0x80) != 0
                lcdc = v
                if (wasEnabled && !nowEnabled) {
                    // Disabling LCD
                    ly = 0
                    cycleCounter = 0
                    setMode(0)
                    prevStatLine = false
                    backBuffer.fill(paletteColors[0])
                } else if (!wasEnabled && nowEnabled) {
                    cycleCounter = 0
                    ly = 0
                    setMode(2)
                    checkLyc()
                }
            }
            0xFF41 -> {
                // Bits 0-2 are read-only
                stat = (stat and 0x07) or (v and 0xF8)
                updateStatLine()
            }
            0xFF42 -> scy = v
            0xFF43 -> scx = v
            0xFF44 -> ly = 0 // Read-only on real hardware; writes reset it
            0xFF45 -> {
                lyc = v
                checkLyc()
            }
            0xFF47 -> bgp = v
            0xFF48 -> obp0 = v
            0xFF49 -> obp1 = v
            0xFF4A -> wy = v
            0xFF4B -> wx = v
        }
    }

    fun step(cycles: Int) {
        // If LCD is disabled, do nothing
        if ((lcdc and 0x80) == 0) return

        cycleCounter += cycles

        var keepGoing = true
        while (keepGoing) {
            keepGoing = false
            when (getMode()) {
                2 -> { // Mode 2: OAM search (80 cycles)
                    if (cycleCounter >= 80) {
                        cycleCounter -= 80
                        setMode(3)
                        keepGoing = true
                    }
                }
                3 -> { // Mode 3: Pixel transfer (172 cycles)
                    if (cycleCounter >= 172) {
                        cycleCounter -= 172
                        setMode(0)
                        renderScanline()
                        keepGoing = true
                    }
                }
                0 -> { // Mode 0: H-Blank (204 cycles)
                    if (cycleCounter >= 204) {
                        cycleCounter -= 204
                        ly++
                        checkLyc()

                        if (ly == 144) {
                            // Enter V-Blank
                            setMode(1)
                            mmu.requestInterrupt(InterruptType.VBLANK)
                            System.arraycopy(backBuffer, 0, framebuffer, 0, 160 * 144)
                            onFrameReady?.invoke(framebuffer)
                        } else {
                            setMode(2)
                        }
                        keepGoing = true
                    }
                }
                1 -> { // Mode 1: V-Blank (456 cycles per line for 10 lines)
                    if (cycleCounter >= 456) {
                        cycleCounter -= 456
                        ly++
                        checkLyc()

                        if (ly > 153) {
                            // Frame complete, wrap back to scanline 0
                            ly = 0
                            windowLineCounter = 0
                            setMode(2)
                            checkLyc()
                        }
                        keepGoing = true
                    }
                }
            }
        }
    }

    private fun getMode(): Int = stat and 0x03

    private fun setMode(mode: Int) {
        stat = (stat and 0xFC) or (mode and 0x03)
        updateStatLine()
    }

    private fun checkLyc() {
        val equal = ly == lyc
        if (equal) {
            stat = stat or 0x04
        } else {
            stat = stat and 0x04.inv()
        }
        updateStatLine()
    }

    private fun updateStatLine() {
        if ((lcdc and 0x80) == 0) {
            prevStatLine = false
            return
        }

        val mode = stat and 0x03
        val lycMatch = (stat and 0x04) != 0

        val line = ((stat and 0x40) != 0 && lycMatch) ||
                   ((stat and 0x20) != 0 && mode == 2) ||
                   ((stat and 0x10) != 0 && mode == 1) ||
                   ((stat and 0x08) != 0 && mode == 0)

        if (!prevStatLine && line) {
            mmu.requestInterrupt(InterruptType.LCD_STAT)
        }
        prevStatLine = line
    }

    private fun renderScanline() {
        if (ly >= 144) return

        scanlineBgColorIndices.fill(0)

        // 1. Render Background
        if ((lcdc and 0x01) != 0) {
            renderBackground()
        }

        // 2. Render Window
        if ((lcdc and 0x20) != 0 && (lcdc and 0x01) != 0) {
            renderWindow()
        }

        // 3. Render Sprites
        if ((lcdc and 0x02) != 0) {
            renderSprites()
        }
    }

    private fun renderBackground() {
        val mapBase = if ((lcdc and 0x08) != 0) 0x1C00 else 0x1800 // 0x9C00 or 0x9800 relative to VRAM
        val signedTiles = (lcdc and 0x10) == 0
        val yPos = (scy + ly) and 0xFF
        val tileRow = yPos / 8

        val fbRowOffset = ly * 160

        for (x in 0 until 160) {
            val xPos = (scx + x) and 0xFF
            val tileCol = xPos / 8
            val tileMapIndex = mapBase + (tileRow * 32) + tileCol
            val tileId = mmu.vram[tileMapIndex].toInt() and 0xFF

            val tileDataOffset = if (signedTiles) {
                val signedId = tileId.toByte().toInt()
                0x1000 + (signedId * 16) // 0x9000 base
            } else {
                tileId * 16 // 0x8000 base
            }

            val lineInTile = (yPos % 8) * 2
            val lowByte = mmu.vram[tileDataOffset + lineInTile].toInt() and 0xFF
            val highByte = mmu.vram[tileDataOffset + lineInTile + 1].toInt() and 0xFF

            val bitInTile = 7 - (xPos % 8)
            val colorBit0 = (lowByte ushr bitInTile) and 1
            val colorBit1 = ((highByte ushr bitInTile) and 1) shl 1
            val colorIndex = colorBit0 or colorBit1

            scanlineBgColorIndices[x] = colorIndex
            val shade = (bgp ushr (colorIndex * 2)) and 0x03
            backBuffer[fbRowOffset + x] = paletteColors[shade]
        }
    }

    private fun renderWindow() {
        if (ly < wy || wx > 166) return
        val windowX = wx - 7

        val mapBase = if ((lcdc and 0x40) != 0) 0x1C00 else 0x1800
        val signedTiles = (lcdc and 0x10) == 0
        val tileRow = windowLineCounter / 8

        val fbRowOffset = ly * 160
        var windowDrawn = false

        for (x in maxOf(0, windowX) until 160) {
            windowDrawn = true
            val xPos = x - windowX
            val tileCol = xPos / 8
            val tileMapIndex = mapBase + (tileRow * 32) + tileCol
            val tileId = mmu.vram[tileMapIndex].toInt() and 0xFF

            val tileDataOffset = if (signedTiles) {
                val signedId = tileId.toByte().toInt()
                0x1000 + (signedId * 16)
            } else {
                tileId * 16
            }

            val lineInTile = (windowLineCounter % 8) * 2
            val lowByte = mmu.vram[tileDataOffset + lineInTile].toInt() and 0xFF
            val highByte = mmu.vram[tileDataOffset + lineInTile + 1].toInt() and 0xFF

            val bitInTile = 7 - (xPos % 8)
            val colorBit0 = (lowByte ushr bitInTile) and 1
            val colorBit1 = ((highByte ushr bitInTile) and 1) shl 1
            val colorIndex = colorBit0 or colorBit1

            scanlineBgColorIndices[x] = colorIndex
            val shade = (bgp ushr (colorIndex * 2)) and 0x03
            backBuffer[fbRowOffset + x] = paletteColors[shade]
        }

        if (windowDrawn) {
            windowLineCounter++
        }
    }

    private fun renderSprites() {
        val spriteHeight = if ((lcdc and 0x04) != 0) 16 else 8
        val fbRowOffset = ly * 160

        // Collect sprites on this scanline (max 10)
        var spriteCount = 0
        val spritesOnLine = IntArray(10)

        for (i in 0 until 40) {
            val spriteY = (mmu.oam[i * 4].toInt() and 0xFF) - 16
            if (ly >= spriteY && ly < (spriteY + spriteHeight)) {
                spritesOnLine[spriteCount++] = i
                if (spriteCount == 10) break
            }
        }

        // Render from last to first so earlier sprites have higher priority
        for (idx in (spriteCount - 1) downTo 0) {
            val i = spritesOnLine[idx]
            val spriteY = (mmu.oam[i * 4].toInt() and 0xFF) - 16
            val spriteX = (mmu.oam[i * 4 + 1].toInt() and 0xFF) - 8
            var tileIndex = mmu.oam[i * 4 + 2].toInt() and 0xFF
            val flags = mmu.oam[i * 4 + 3].toInt() and 0xFF

            if (spriteHeight == 16) {
                tileIndex = tileIndex and 0xFE
            }

            val priorityBehindBg = (flags and 0x80) != 0
            val yFlip = (flags and 0x40) != 0
            val xFlip = (flags and 0x20) != 0
            val palette = if ((flags and 0x10) != 0) obp1 else obp0

            var lineInSprite = ly - spriteY
            if (yFlip) {
                lineInSprite = (spriteHeight - 1) - lineInSprite
            }

            val tileDataOffset = (tileIndex * 16) + (lineInSprite * 2)
            val lowByte = mmu.vram[tileDataOffset].toInt() and 0xFF
            val highByte = mmu.vram[tileDataOffset + 1].toInt() and 0xFF

            for (bit in 0 until 8) {
                val screenX = spriteX + bit
                if (screenX !in 0 until 160) continue

                val bitInTile = if (xFlip) bit else 7 - bit
                val colorBit0 = (lowByte ushr bitInTile) and 1
                val colorBit1 = ((highByte ushr bitInTile) and 1) shl 1
                val colorIndex = colorBit0 or colorBit1

                // Color 0 is transparent for sprites
                if (colorIndex == 0) continue

                // Check priority
                if (priorityBehindBg && scanlineBgColorIndices[screenX] != 0) {
                    continue
                }

                val shade = (palette ushr (colorIndex * 2)) and 0x03
                backBuffer[fbRowOffset + screenX] = paletteColors[shade]
            }
        }
    }
}
