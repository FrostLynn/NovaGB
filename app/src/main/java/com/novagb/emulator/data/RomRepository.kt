package com.novagb.emulator.data

import android.content.Context
import android.net.Uri
import com.novagb.emulator.core.Cartridge
import java.io.File
import java.util.UUID

class RomRepository(private val context: Context) {

    private val saveDir = File(context.filesDir, "saves").apply { if (!exists()) mkdirs() }
    private val stateDir = File(context.filesDir, "states").apply { if (!exists()) mkdirs() }
    private val metaFile = File(context.filesDir, "rom_library.json")

    fun getSampleRomBytes(): ByteArray {
        return context.assets.open("games/sample.gb").use { it.readBytes() }
    }

    fun readRomBytes(uriString: String, isAsset: Boolean): ByteArray {
        return if (isAsset) {
            getSampleRomBytes()
        } else {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("Cannot open ROM at $uriString")
        }
    }

    fun parseRomMetadata(uri: Uri, bytes: ByteArray): RomMetadata {
        val cart = Cartridge.fromRom(bytes)
        val cartTypeStr = when (cart.header.cartridgeType) {
            0 -> "ROM ONLY"
            1, 2, 3 -> "MBC1"
            5, 6 -> "MBC2"
            15, 16, 17, 18, 19 -> "MBC3"
            25, 26, 27 -> "MBC5"
            else -> "MBC"
        }

        return RomMetadata(
            id = UUID.randomUUID().toString(),
            title = cart.header.title,
            uriString = uri.toString(),
            isAsset = false,
            isCgb = cart.header.isCgb,
            cartridgeType = cartTypeStr,
            romSizeBytes = bytes.size.toLong(),
            lastPlayedTimestamp = System.currentTimeMillis(),
            totalPlayTimeSeconds = 0,
            bannerColorSeed = cart.header.title.hashCode()
        )
    }

    fun saveBatteryRam(romTitle: String, data: ByteArray) {
        val safeName = romTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val file = File(saveDir, "$safeName.sav")
        file.writeBytes(data)
    }

    fun loadBatteryRam(romTitle: String): ByteArray? {
        val safeName = romTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val file = File(saveDir, "$safeName.sav")
        return if (file.exists()) file.readBytes() else null
    }

    fun saveStateSlot(romTitle: String, slotIndex: Int, data: ByteArray) {
        val safeName = romTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val file = File(stateDir, "${safeName}_slot$slotIndex.state")
        file.writeBytes(data)
    }

    fun loadStateSlot(romTitle: String, slotIndex: Int): ByteArray? {
        val safeName = romTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val file = File(stateDir, "${safeName}_slot$slotIndex.state")
        return if (file.exists()) file.readBytes() else null
    }

    fun hasStateSlot(romTitle: String, slotIndex: Int): Boolean {
        val safeName = romTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val file = File(stateDir, "${safeName}_slot$slotIndex.state")
        return file.exists()
    }
}
