package com.novagb.emulator.data

import android.content.Context
import android.net.Uri
import com.novagb.emulator.core.Cartridge
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class RomRepository(private val context: Context) {

    private val saveDir = File(context.filesDir, "saves").apply { if (!exists()) mkdirs() }
    private val stateDir = File(context.filesDir, "states").apply { if (!exists()) mkdirs() }
    private val metaFile = File(context.filesDir, "rom_library.json")

    fun readRomBytes(uriString: String, isAsset: Boolean = false): ByteArray {
        return if (isAsset) {
            val assetPath = uriString.removePrefix("asset://")
            context.assets.open(assetPath).use { it.readBytes() }
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

    fun loadRomLibrary(): List<RomMetadata> {
        if (!metaFile.exists()) return emptyList()
        return try {
            val jsonStr = metaFile.readText()
            val array = JSONArray(jsonStr)
            val list = mutableListOf<RomMetadata>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    RomMetadata(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", "Unknown"),
                        uriString = obj.optString("uriString", ""),
                        isAsset = obj.optBoolean("isAsset", false),
                        isCgb = obj.optBoolean("isCgb", false),
                        cartridgeType = obj.optString("cartridgeType", "MBC1"),
                        romSizeBytes = obj.optLong("romSizeBytes", 0L),
                        lastPlayedTimestamp = obj.optLong("lastPlayedTimestamp", 0L),
                        totalPlayTimeSeconds = obj.optLong("totalPlayTimeSeconds", 0L),
                        bannerColorSeed = obj.optInt("bannerColorSeed", 0)
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveRomLibrary(list: List<RomMetadata>) {
        try {
            val array = JSONArray()
            for (rom in list) {
                val obj = JSONObject().apply {
                    put("id", rom.id)
                    put("title", rom.title)
                    put("uriString", rom.uriString)
                    put("isAsset", rom.isAsset)
                    put("isCgb", rom.isCgb)
                    put("cartridgeType", rom.cartridgeType)
                    put("romSizeBytes", rom.romSizeBytes)
                    put("lastPlayedTimestamp", rom.lastPlayedTimestamp)
                    put("totalPlayTimeSeconds", rom.totalPlayTimeSeconds)
                    put("bannerColorSeed", rom.bannerColorSeed)
                }
                array.put(obj)
            }
            metaFile.writeText(array.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
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
