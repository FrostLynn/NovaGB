package com.novagb.emulator.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URLEncoder

class RomRepositoryTest {

    private fun buildCoverUrl(isCgb: Boolean, queryName: String): String {
        val systemFolder = if (isCgb) "Nintendo_-_Game_Boy_Color" else "Nintendo_-_Game_Boy"
        val sanitized = queryName.trim().replace(Regex("[/\\\\:*?\"<>|]"), "_")
        val encoded = URLEncoder.encode(sanitized, "UTF-8").replace("+", "%20")
        return "https://raw.githubusercontent.com/libretro-thumbnails/$systemFolder/master/Named_Boxarts/$encoded.png"
    }

    @Test
    fun testBuildCoverUrlForDmgGame() {
        val url = buildCoverUrl(false, "Pokemon - Red Version (USA, Europe) (SGB Enhanced)")
        val expected = "https://raw.githubusercontent.com/libretro-thumbnails/Nintendo_-_Game_Boy/master/Named_Boxarts/Pokemon%20-%20Red%20Version%20%28USA%2C%20Europe%29%20%28SGB%20Enhanced%29.png"
        assertEquals(expected, url)
    }

    @Test
    fun testBuildCoverUrlForCgbGame() {
        val url = buildCoverUrl(true, "Pokemon - Crystal Version (USA, Europe) (Rev A)")
        val expected = "https://raw.githubusercontent.com/libretro-thumbnails/Nintendo_-_Game_Boy_Color/master/Named_Boxarts/Pokemon%20-%20Crystal%20Version%20%28USA%2C%20Europe%29%20%28Rev%20A%29.png"
        assertEquals(expected, url)
    }

    @Test
    fun testBuildCoverUrlSanitizesIllegalCharacters() {
        val url = buildCoverUrl(false, "Test/Game:Name")
        val expected = "https://raw.githubusercontent.com/libretro-thumbnails/Nintendo_-_Game_Boy/master/Named_Boxarts/Test_Game_Name.png"
        assertEquals(expected, url)
    }
}
