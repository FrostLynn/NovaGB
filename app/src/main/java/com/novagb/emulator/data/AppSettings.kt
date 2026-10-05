package com.novagb.emulator.data

import android.content.Context
import android.content.SharedPreferences

enum class ColorPalette(val displayName: String, val colors: IntArray) {
    DMG_CLASSIC(
        "DMG Classic",
        intArrayOf(0xFF9BBC0F.toInt(), 0xFF8BAC0F.toInt(), 0xFF306230.toInt(), 0xFF0F380F.toInt())
    ),
    POCKET_GRAY(
        "Pocket Gray",
        intArrayOf(0xFFFFFFFF.toInt(), 0xFFAAAAAA.toInt(), 0xFF555555.toInt(), 0xFF000000.toInt())
    ),
    LIGHT_TEAL(
        "Game Boy Light",
        intArrayOf(0xFF4DB6AC.toInt(), 0xFF26A69A.toInt(), 0xFF006555.toInt(), 0xFF003830.toInt())
    ),
    CYBERPUNK(
        "Cyberpunk Neon",
        intArrayOf(0xFF4CC9F0.toInt(), 0xFFF72585.toInt(), 0xFF7209B7.toInt(), 0xFF1A0033.toInt())
    ),
    GOLDEN_AMBER(
        "Golden Amber",
        intArrayOf(0xFFFFC077.toInt(), 0xFFD9822B.toInt(), 0xFF784400.toInt(), 0xFF2B1700.toInt())
    )
}

enum class AspectRatioMode(val displayName: String) {
    ORIGINAL("Original 10:9"),
    INTEGER_SCALE("Integer Scale 3x"),
    FIT_SCREEN("Fit Screen"),
    FULL_SCREEN("Stretch Full")
}

class AppSettings(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("novagb_prefs", Context.MODE_PRIVATE)

    var selectedPalette: ColorPalette
        get() {
            val name = prefs.getString("selected_palette", ColorPalette.DMG_CLASSIC.name)
            return try { ColorPalette.valueOf(name!!) } catch (e: Exception) { ColorPalette.DMG_CLASSIC }
        }
        set(value) = prefs.edit().putString("selected_palette", value.name).apply()

    var aspectRatio: AspectRatioMode
        get() {
            val name = prefs.getString("aspect_ratio", AspectRatioMode.ORIGINAL.name)
            return try { AspectRatioMode.valueOf(name!!) } catch (e: Exception) { AspectRatioMode.ORIGINAL }
        }
        set(value) = prefs.edit().putString("aspect_ratio", value.name).apply()

    var showLcdGrid: Boolean
        get() = prefs.getBoolean("show_lcd_grid", true)
        set(value) = prefs.edit().putBoolean("show_lcd_grid", value).apply()

    var showScanlines: Boolean
        get() = prefs.getBoolean("show_scanlines", false)
        set(value) = prefs.edit().putBoolean("show_scanlines", value).apply()

    var showRetroBezel: Boolean
        get() = prefs.getBoolean("show_retro_bezel", true)
        set(value) = prefs.edit().putBoolean("show_retro_bezel", value).apply()

    var showClassicConsoleShell: Boolean
        get() = prefs.getBoolean("show_classic_console_shell", true)
        set(value) = prefs.edit().putBoolean("show_classic_console_shell", value).apply()

    var showFps: Boolean
        get() = prefs.getBoolean("show_fps", true)
        set(value) = prefs.edit().putBoolean("show_fps", value).apply()

    var hapticFeedbackEnabled: Boolean
        get() = prefs.getBoolean("haptic_feedback", true)
        set(value) = prefs.edit().putBoolean("haptic_feedback", value).apply()

    var buttonOpacity: Float
        get() = prefs.getFloat("button_opacity", 0.65f)
        set(value) = prefs.edit().putFloat("button_opacity", value).apply()

    var controllerScale: Float
        get() = prefs.getFloat("controller_scale", 1.0f)
        set(value) = prefs.edit().putFloat("controller_scale", value).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound_enabled", true)
        set(value) = prefs.edit().putBoolean("sound_enabled", value).apply()

    var soundVolume: Float
        get() = prefs.getFloat("sound_volume", 0.8f)
        set(value) = prefs.edit().putFloat("sound_volume", value).apply()

    var fastForwardSpeed: Int
        get() = prefs.getInt("fast_forward_speed", 2) // 2x, 4x, 8x
        set(value) = prefs.edit().putInt("fast_forward_speed", value).apply()

    var autoSaveOnExit: Boolean
        get() = prefs.getBoolean("auto_save_exit", true)
        set(value) = prefs.edit().putBoolean("auto_save_exit", value).apply()
}
