package com.novagb.emulator.core

data class CartridgeHeader(
    val title: String,
    val isCgb: Boolean,
    val cartridgeType: Int,
    val romSizeCode: Int,
    val ramSizeCode: Int,
    val romSizeBytes: Int,
    val ramSizeBytes: Int,
    val checksum: Int
)

interface Mbc {
    fun readRom(addr: Int): Int
    fun writeRom(addr: Int, value: Int)
    fun readRam(addr: Int): Int
    fun writeRam(addr: Int, value: Int)
    fun getBatterySave(): ByteArray?
    fun loadBatterySave(data: ByteArray)
}

class RomOnlyMbc(private val rom: ByteArray, private val hasRam: Boolean) : Mbc {
    private val ram = if (hasRam) ByteArray(8192) else ByteArray(0)

    override fun readRom(addr: Int): Int {
        return if (addr in rom.indices) rom[addr].toInt() and 0xFF else 0xFF
    }

    override fun writeRom(addr: Int, value: Int) {
        // ROM-only does not support bank switching
    }

    override fun readRam(addr: Int): Int {
        if (!hasRam) return 0xFF
        val offset = addr - 0xA000
        return if (offset in ram.indices) ram[offset].toInt() and 0xFF else 0xFF
    }

    override fun writeRam(addr: Int, value: Int) {
        if (!hasRam) return
        val offset = addr - 0xA000
        if (offset in ram.indices) {
            ram[offset] = value.toByte()
        }
    }

    override fun getBatterySave(): ByteArray? = if (hasRam) ram.clone() else null

    override fun loadBatterySave(data: ByteArray) {
        if (hasRam) {
            System.arraycopy(data, 0, ram, 0, minOf(data.size, ram.size))
        }
    }
}

class Mbc1(
    private val rom: ByteArray,
    private val ramSize: Int,
    private val hasBattery: Boolean
) : Mbc {
    private val ram = ByteArray(ramSize)
    private var ramEnabled: Boolean = false
    private var romBank: Int = 1
    private var ramBankOrUpperRom: Int = 0
    private var bankingMode: Int = 0 // 0 = ROM mode, 1 = RAM mode

    private val totalRomBanks = maxOf(2, rom.size / 0x4000)
    private val totalRamBanks = if (ramSize > 0) maxOf(1, ramSize / 0x2000) else 0

    override fun readRom(addr: Int): Int {
        val bank = if (addr < 0x4000) {
            if (bankingMode == 1 && totalRomBanks > 0) {
                (ramBankOrUpperRom shl 5) % totalRomBanks
            } else 0
        } else {
            val fullBank = if (totalRomBanks > 0) {
                val b = ((ramBankOrUpperRom shl 5) or romBank) % totalRomBanks
                if (b == 0) 1 else b
            } else 1
            fullBank
        }
        val romAddr = (bank * 0x4000) + (addr and 0x3FFF)
        return if (romAddr in rom.indices) rom[romAddr].toInt() and 0xFF else 0xFF
    }

    override fun writeRom(addr: Int, value: Int) {
        when (addr) {
            in 0x0000..0x1FFF -> {
                ramEnabled = (value and 0x0F) == 0x0A
            }
            in 0x2000..0x3FFF -> {
                var b = value and 0x1F
                if (b == 0) b = 1
                romBank = b
            }
            in 0x4000..0x5FFF -> {
                ramBankOrUpperRom = value and 0x03
            }
            in 0x6000..0x7FFF -> {
                bankingMode = value and 0x01
            }
        }
    }

    override fun readRam(addr: Int): Int {
        if (!ramEnabled || ram.isEmpty()) return 0xFF
        val bank = if (bankingMode == 1 && totalRamBanks > 0) ramBankOrUpperRom % totalRamBanks else 0
        val offset = (bank * 0x2000) + (addr - 0xA000)
        return if (offset in ram.indices) ram[offset].toInt() and 0xFF else 0xFF
    }

    override fun writeRam(addr: Int, value: Int) {
        if (!ramEnabled || ram.isEmpty()) return
        val bank = if (bankingMode == 1 && totalRamBanks > 0) ramBankOrUpperRom % totalRamBanks else 0
        val offset = (bank * 0x2000) + (addr - 0xA000)
        if (offset in ram.indices) {
            ram[offset] = value.toByte()
        }
    }

    override fun getBatterySave(): ByteArray? = if (hasBattery && ram.isNotEmpty()) ram.clone() else null

    override fun loadBatterySave(data: ByteArray) {
        if (hasBattery && ram.isNotEmpty()) {
            System.arraycopy(data, 0, ram, 0, minOf(data.size, ram.size))
        }
    }
}

class Mbc2(private val rom: ByteArray, private val hasBattery: Boolean) : Mbc {
    private val ram = ByteArray(512) // 512 x 4-bit nibbles
    private var ramEnabled: Boolean = false
    private var romBank: Int = 1
    private val totalRomBanks = maxOf(2, rom.size / 0x4000)

    override fun readRom(addr: Int): Int {
        val bank = if (addr < 0x4000) 0 else romBank % totalRomBanks
        val romAddr = (bank * 0x4000) + (addr and 0x3FFF)
        return if (romAddr in rom.indices) rom[romAddr].toInt() and 0xFF else 0xFF
    }

    override fun writeRom(addr: Int, value: Int) {
        if (addr in 0x0000..0x3FFF) {
            if ((addr and 0x0100) == 0) {
                ramEnabled = (value and 0x0F) == 0x0A
            } else {
                var b = value and 0x0F
                if (b == 0) b = 1
                romBank = b
            }
        }
    }

    override fun readRam(addr: Int): Int {
        if (!ramEnabled) return 0xFF
        val offset = (addr - 0xA000) and 0x1FF
        return (ram[offset].toInt() and 0x0F) or 0xF0
    }

    override fun writeRam(addr: Int, value: Int) {
        if (!ramEnabled) return
        val offset = (addr - 0xA000) and 0x1FF
        ram[offset] = (value and 0x0F).toByte()
    }

    override fun getBatterySave(): ByteArray? = if (hasBattery) ram.clone() else null

    override fun loadBatterySave(data: ByteArray) {
        if (hasBattery) {
            System.arraycopy(data, 0, ram, 0, minOf(data.size, ram.size))
        }
    }
}

class Mbc3(
    private val rom: ByteArray,
    private val ramSize: Int,
    private val hasBattery: Boolean
) : Mbc {
    private val ram = ByteArray(ramSize)
    private var ramEnabled: Boolean = false
    private var romBank: Int = 1
    private var ramBankOrRtc: Int = 0
    private val totalRomBanks = maxOf(2, rom.size / 0x4000)
    private val totalRamBanks = if (ramSize > 0) maxOf(1, ramSize / 0x2000) else 0

    // RTC registers
    private val rtcRegs = IntArray(5) // S, M, H, DL, DH
    private var latchState: Int = 0

    override fun readRom(addr: Int): Int {
        val bank = if (addr < 0x4000) 0 else {
            val b = if (totalRomBanks > 0) romBank % totalRomBanks else 1
            if (b == 0) 1 else b
        }
        val romAddr = (bank * 0x4000) + (addr and 0x3FFF)
        return if (romAddr in rom.indices) rom[romAddr].toInt() and 0xFF else 0xFF
    }

    override fun writeRom(addr: Int, value: Int) {
        when (addr) {
            in 0x0000..0x1FFF -> {
                ramEnabled = (value and 0x0F) == 0x0A
            }
            in 0x2000..0x3FFF -> {
                var b = value and 0x7F
                if (b == 0) b = 1
                romBank = b
            }
            in 0x4000..0x5FFF -> {
                ramBankOrRtc = value
            }
            in 0x6000..0x7FFF -> {
                if (latchState == 0 && value == 1) {
                    // Latch RTC timestamp
                    val nowSec = System.currentTimeMillis() / 1000
                    rtcRegs[0] = (nowSec % 60).toInt()
                    rtcRegs[1] = ((nowSec / 60) % 60).toInt()
                    rtcRegs[2] = ((nowSec / 3600) % 24).toInt()
                }
                latchState = value
            }
        }
    }

    override fun readRam(addr: Int): Int {
        if (!ramEnabled) return 0xFF
        if (ramBankOrRtc in 0x00..0x03 && ram.isNotEmpty() && totalRamBanks > 0) {
            val bank = ramBankOrRtc % totalRamBanks
            val offset = (bank * 0x2000) + (addr - 0xA000)
            return if (offset in ram.indices) ram[offset].toInt() and 0xFF else 0xFF
        } else if (ramBankOrRtc in 0x08..0x0C) {
            return rtcRegs[ramBankOrRtc - 0x08]
        }
        return 0xFF
    }

    override fun writeRam(addr: Int, value: Int) {
        if (!ramEnabled) return
        if (ramBankOrRtc in 0x00..0x03 && ram.isNotEmpty() && totalRamBanks > 0) {
            val bank = ramBankOrRtc % totalRamBanks
            val offset = (bank * 0x2000) + (addr - 0xA000)
            if (offset in ram.indices) {
                ram[offset] = value.toByte()
            }
        } else if (ramBankOrRtc in 0x08..0x0C) {
            rtcRegs[ramBankOrRtc - 0x08] = value and 0xFF
        }
    }

    override fun getBatterySave(): ByteArray? = if (hasBattery && ram.isNotEmpty()) ram.clone() else null

    override fun loadBatterySave(data: ByteArray) {
        if (hasBattery && ram.isNotEmpty()) {
            System.arraycopy(data, 0, ram, 0, minOf(data.size, ram.size))
        }
    }
}

class Mbc5(
    private val rom: ByteArray,
    private val ramSize: Int,
    private val hasBattery: Boolean
) : Mbc {
    private val ram = ByteArray(ramSize)
    private var ramEnabled: Boolean = false
    private var romBankLow: Int = 1
    private var romBankHigh: Int = 0
    private var ramBank: Int = 0
    private val totalRomBanks = maxOf(2, rom.size / 0x4000)
    private val totalRamBanks = if (ramSize > 0) maxOf(1, ramSize / 0x2000) else 0

    private val currentRomBank: Int
        get() = ((romBankHigh shl 8) or romBankLow) % totalRomBanks

    override fun readRom(addr: Int): Int {
        val bank = if (addr < 0x4000) 0 else currentRomBank
        val romAddr = (bank * 0x4000) + (addr and 0x3FFF)
        return if (romAddr in rom.indices) rom[romAddr].toInt() and 0xFF else 0xFF
    }

    override fun writeRom(addr: Int, value: Int) {
        when (addr) {
            in 0x0000..0x1FFF -> ramEnabled = (value and 0x0F) == 0x0A
            in 0x2000..0x2FFF -> romBankLow = value and 0xFF
            in 0x3000..0x3FFF -> romBankHigh = value and 0x01
            in 0x4000..0x5FFF -> ramBank = value and 0x0F
        }
    }

    override fun readRam(addr: Int): Int {
        if (!ramEnabled || ram.isEmpty()) return 0xFF
        val bank = if (totalRamBanks > 0) ramBank % totalRamBanks else 0
        val offset = (bank * 0x2000) + (addr - 0xA000)
        return if (offset in ram.indices) ram[offset].toInt() and 0xFF else 0xFF
    }

    override fun writeRam(addr: Int, value: Int) {
        if (!ramEnabled || ram.isEmpty()) return
        val bank = if (totalRamBanks > 0) ramBank % totalRamBanks else 0
        val offset = (bank * 0x2000) + (addr - 0xA000)
        if (offset in ram.indices) {
            ram[offset] = value.toByte()
        }
    }

    override fun getBatterySave(): ByteArray? = if (hasBattery && ram.isNotEmpty()) ram.clone() else null

    override fun loadBatterySave(data: ByteArray) {
        if (hasBattery && ram.isNotEmpty()) {
            System.arraycopy(data, 0, ram, 0, minOf(data.size, ram.size))
        }
    }
}

class Cartridge private constructor(
    val header: CartridgeHeader,
    private val mbc: Mbc
) : Mbc by mbc {

    companion object {
        fun fromRom(romBytes: ByteArray): Cartridge {
            val header = parseHeader(romBytes)
            val mbc = createMbc(romBytes, header)
            return Cartridge(header, mbc)
        }

        private fun parseHeader(rom: ByteArray): CartridgeHeader {
            val titleBuilder = StringBuilder()
            val endTitle = if (rom.size > 0x0143 && (rom[0x0143].toInt() and 0x80) != 0) 0x013E else 0x0142
            for (i in 0x0134..endTitle) {
                if (i < rom.size) {
                    val b = rom[i].toInt() and 0xFF
                    if (b == 0) break
                    if (b in 32..126) titleBuilder.append(b.toChar())
                }
            }
            val title = titleBuilder.toString().trim().ifEmpty { "UNKNOWN GAME" }
            val cgbFlag = if (rom.size > 0x0143) rom[0x0143].toInt() and 0xFF else 0
            val isCgb = (cgbFlag and 0x80) != 0

            val cartType = if (rom.size > 0x0147) rom[0x0147].toInt() and 0xFF else 0
            val romSizeCode = if (rom.size > 0x0148) rom[0x0148].toInt() and 0xFF else 0
            val ramSizeCode = if (rom.size > 0x0149) rom[0x0149].toInt() and 0xFF else 0
            val checksum = if (rom.size > 0x014D) rom[0x014D].toInt() and 0xFF else 0

            val romSizeBytes = 32768 shl romSizeCode
            val ramSizeBytes = when (ramSizeCode) {
                0x01 -> 2048
                0x02 -> 8192
                0x03 -> 32768
                0x04 -> 131072
                0x05 -> 65536
                else -> 0
            }

            return CartridgeHeader(
                title = title,
                isCgb = isCgb,
                cartridgeType = cartType,
                romSizeCode = romSizeCode,
                ramSizeCode = ramSizeCode,
                romSizeBytes = romSizeBytes,
                ramSizeBytes = ramSizeBytes,
                checksum = checksum
            )
        }

        private fun createMbc(rom: ByteArray, header: CartridgeHeader): Mbc {
            return when (header.cartridgeType) {
                0x00 -> RomOnlyMbc(rom, header.ramSizeBytes > 0)
                0x01, 0x02 -> Mbc1(rom, header.ramSizeBytes, false)
                0x03 -> Mbc1(rom, header.ramSizeBytes, true)
                0x05 -> Mbc2(rom, false)
                0x06 -> Mbc2(rom, true)
                0x0F, 0x10, 0x13 -> Mbc3(rom, header.ramSizeBytes, true)
                0x11, 0x12 -> Mbc3(rom, header.ramSizeBytes, false)
                0x19, 0x1A -> Mbc5(rom, header.ramSizeBytes, false)
                0x1B, 0x1E -> Mbc5(rom, header.ramSizeBytes, true)
                else -> Mbc1(rom, header.ramSizeBytes, true)
            }
        }
    }
}
