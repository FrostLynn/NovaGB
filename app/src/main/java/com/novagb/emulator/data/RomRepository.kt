package com.novagb.emulator.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.novagb.emulator.core.Cartridge
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
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

    fun buildCoverUrl(isCgb: Boolean, queryName: String): String {
        val systemFolder = if (isCgb) "Nintendo_-_Game_Boy_Color" else "Nintendo_-_Game_Boy"
        val sanitized = queryName.trim().replace(Regex("[/\\\\:*?\"<>|]"), "_")
        val encoded = try {
            URLEncoder.encode(sanitized, "UTF-8").replace("+", "%20")
        } catch (_: Exception) {
            sanitized
        }
        return "https://raw.githubusercontent.com/libretro-thumbnails/$systemFolder/master/Named_Boxarts/$encoded.png"
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

        val docFile = try {
            DocumentFile.fromSingleUri(context, uri)
        } catch (_: Exception) {
            null
        }
        val queryTitle = docFile?.name?.replace(Regex("\\.(gb|gbc|bin|zip)$", RegexOption.IGNORE_CASE), "")
            ?.takeIf { it.isNotBlank() } ?: cart.header.title

        val cover = buildCoverUrl(cart.header.isCgb, queryTitle)

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
            bannerColorSeed = cart.header.title.hashCode(),
            coverUrl = cover
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
                val isCgb = obj.optBoolean("isCgb", false)
                val title = obj.optString("title", "Unknown")
                val cover = obj.optString("coverUrl", "").takeIf { it.isNotBlank() }
                    ?: buildCoverUrl(isCgb, title)

                list.add(
                    RomMetadata(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = title,
                        uriString = obj.optString("uriString", ""),
                        isAsset = obj.optBoolean("isAsset", false),
                        isCgb = isCgb,
                        cartridgeType = obj.optString("cartridgeType", "MBC1"),
                        romSizeBytes = obj.optLong("romSizeBytes", 0L),
                        lastPlayedTimestamp = obj.optLong("lastPlayedTimestamp", 0L),
                        totalPlayTimeSeconds = obj.optLong("totalPlayTimeSeconds", 0L),
                        bannerColorSeed = obj.optInt("bannerColorSeed", 0),
                        coverUrl = cover
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
                    put("coverUrl", rom.coverUrl ?: "")
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

    fun getStateSlotLastModified(romTitle: String, slotIndex: Int): Long {
        val safeName = romTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val file = File(stateDir, "${safeName}_slot$slotIndex.state")
        return if (file.exists()) file.lastModified() else 0L
    }

    fun saveStateThumbnail(romTitle: String, slotIndex: Int, framebuffer: IntArray) {
        try {
            val safeName = romTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val file = File(stateDir, "${safeName}_slot$slotIndex.png")
            val bitmap = Bitmap.createBitmap(160, 144, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(framebuffer, 0, 160, 0, 0, 160, 144)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
        } catch (_: Exception) {}
    }

    fun loadStateThumbnail(romTitle: String, slotIndex: Int): Bitmap? {
        val safeName = romTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val file = File(stateDir, "${safeName}_slot$slotIndex.png")
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }
}
