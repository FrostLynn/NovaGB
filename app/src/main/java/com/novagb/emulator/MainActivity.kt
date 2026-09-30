package com.novagb.emulator

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.novagb.emulator.core.JoypadButton
import com.novagb.emulator.data.RomMetadata
import com.novagb.emulator.data.RomRepository
import com.novagb.emulator.ui.screens.EmulatorScreen
import com.novagb.emulator.ui.screens.LibraryScreen
import com.novagb.emulator.ui.screens.SettingsScreen
import com.novagb.emulator.ui.theme.DarkBackground
import com.novagb.emulator.ui.theme.NovaGBTheme

class MainActivity : ComponentActivity() {

    private val repository by lazy { RomRepository(this) }
    var activeHardwareButtonListener: ((JoypadButton, Boolean) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var initialRom: RomMetadata? = null
        if (intent?.action == Intent.ACTION_VIEW && intent.data != null) {
            val uri = intent.data!!
            try {
                val bytes = repository.readRomBytes(uri.toString(), false)
                initialRom = repository.parseRomMetadata(uri, bytes)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        setContent {
            NovaGBTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    val navController = rememberNavController()
                    var selectedGame by remember { mutableStateOf<RomMetadata?>(initialRom) }

                    val startDestination = if (initialRom != null) "emulator" else "library"

                    NavHost(
                        navController = navController,
                        startDestination = startDestination
                    ) {
                        composable("library") {
                            LibraryScreen(
                                onLaunchGame = { game ->
                                    selectedGame = game
                                    navController.navigate("emulator")
                                },
                                onOpenSettings = {
                                    navController.navigate("settings")
                                }
                            )
                        }

                        composable("emulator") {
                            val game = selectedGame
                            if (game != null) {
                                EmulatorScreen(
                                    game = game,
                                    onExit = {
                                        navController.popBackStack("library", false)
                                    }
                                )
                            } else {
                                navController.popBackStack()
                            }
                        }

                        composable("settings") {
                            SettingsScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Hardware Bluetooth / USB Gamepad Controller Support
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val button = mapKeyCodeToJoypad(keyCode)
        if (button != null) {
            activeHardwareButtonListener?.invoke(button, true)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val button = mapKeyCodeToJoypad(keyCode)
        if (button != null) {
            activeHardwareButtonListener?.invoke(button, false)
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if ((event.source and android.view.InputDevice.SOURCE_JOYSTICK) == android.view.InputDevice.SOURCE_JOYSTICK) {
            val x = event.getAxisValue(MotionEvent.AXIS_X)
            val y = event.getAxisValue(MotionEvent.AXIS_Y)
            val threshold = 0.5f

            activeHardwareButtonListener?.invoke(JoypadButton.LEFT, x < -threshold)
            activeHardwareButtonListener?.invoke(JoypadButton.RIGHT, x > threshold)
            activeHardwareButtonListener?.invoke(JoypadButton.UP, y < -threshold)
            activeHardwareButtonListener?.invoke(JoypadButton.DOWN, y > threshold)
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    private fun mapKeyCodeToJoypad(keyCode: Int): JoypadButton? {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W -> JoypadButton.UP
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_S -> JoypadButton.DOWN
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A -> JoypadButton.LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> JoypadButton.RIGHT
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_K -> JoypadButton.A
            KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_J -> JoypadButton.B
            KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_SPACE -> JoypadButton.SELECT
            KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_ENTER -> JoypadButton.START
            else -> null
        }
    }
}