package com.novagb.emulator.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novagb.emulator.audio.GbAudioPlayer
import com.novagb.emulator.core.GameBoy
import com.novagb.emulator.core.JoypadButton
import com.novagb.emulator.data.AppSettings
import com.novagb.emulator.data.ColorPalette
import com.novagb.emulator.data.RomMetadata
import com.novagb.emulator.data.RomRepository
import com.novagb.emulator.ui.components.RetroDisplay
import com.novagb.emulator.ui.components.TouchController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmulatorScreen(
    game: RomMetadata,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { RomRepository(context) }
    val settings = remember { AppSettings(context) }

    val gameBoy = remember { GameBoy() }
    val audioPlayer = remember { GbAudioPlayer() }

    var isFastForward by remember { mutableStateOf(false) }
    var showQuickMenu by remember { mutableStateOf(false) }
    var fpsDisplay by remember { mutableIntStateOf(60) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val frameVersion = remember { mutableLongStateOf(0L) }
    var isRomLoaded by remember { mutableStateOf(false) }
    var romLoadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(game) {
        withContext(Dispatchers.IO) {
            try {
                val romBytes = repository.readRomBytes(game.uriString, game.isAsset)
                gameBoy.loadRom(romBytes)

                val saveRam = repository.loadBatteryRam(game.title)
                if (saveRam != null) {
                    gameBoy.loadBatterySave(saveRam)
                }

                gameBoy.ppu.paletteColors = settings.selectedPalette.colors

                gameBoy.apu.onAudioBufferFull = { buffer, size ->
                    if (settings.soundEnabled) {
                        audioPlayer.writeSamples(buffer, size)
                    }
                }
                isRomLoaded = true
            } catch (e: Exception) {
                e.printStackTrace()
                romLoadError = e.message ?: "Failed to load ROM"
            }
        }
    }

    DisposableEffect(Unit) {
        if (settings.soundEnabled) {
            audioPlayer.volume = settings.soundVolume
            audioPlayer.start()
        }
        onDispose {
            audioPlayer.stop()
            val batteryData = gameBoy.getBatterySave()
            if (batteryData != null) {
                repository.saveBatteryRam(game.title, batteryData)
            }
        }
    }

    LaunchedEffect(isRomLoaded, isFastForward) {
        if (!isRomLoaded) return@LaunchedEffect

        withContext(Dispatchers.Default) {
            var frameCount = 0
            var lastFpsCheck = System.currentTimeMillis()
            val targetFrameTimeMs = if (isFastForward) (16.666 / settings.fastForwardSpeed).toLong() else 16L

            while (isActive) {
                val startTime = System.currentTimeMillis()

                gameBoy.stepFrame()
                frameVersion.longValue++

                frameCount++
                val now = System.currentTimeMillis()
                if (now - lastFpsCheck >= 1000) {
                    fpsDisplay = (frameCount * 1000 / (now - lastFpsCheck)).toInt()
                    frameCount = 0
                    lastFpsCheck = now
                }

                val elapsed = System.currentTimeMillis() - startTime
                val sleepTime = targetFrameTimeMs - elapsed
                if (sleepTime > 0) {
                    delay(sleepTime)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0D10))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = game.title,
                    color = Color(0xFFD0D5E0),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isFastForward) {
                        Text(
                            text = "${settings.fastForwardSpeed}X FAST",
                            color = Color(0xFFFF2A6D),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    if (settings.showFps) {
                        Text(
                            text = "$fpsDisplay FPS",
                            color = Color(0xFF00E5FF),
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            RetroDisplay(
                framebuffer = gameBoy.ppu.framebuffer,
                aspectRatioMode = settings.aspectRatio,
                showLcdGrid = settings.showLcdGrid,
                showScanlines = settings.showScanlines,
                modifier = Modifier.weight(1f, fill = false),
                frameIndexProvider = { frameVersion.longValue }
            )

            TouchController(
                onButtonChange = { btn, pressed ->
                    gameBoy.setButton(btn, pressed)
                },
                onMenuClick = { showQuickMenu = true },
                onFastForwardToggle = { isFastForward = !isFastForward },
                isFastForwardActive = isFastForward,
                opacity = settings.buttonOpacity,
                scaleFactor = settings.controllerScale,
                hapticsEnabled = settings.hapticFeedbackEnabled,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (showQuickMenu) {
            ModalBottomSheet(
                onDismissRequest = { showQuickMenu = false },
                sheetState = sheetState,
                containerColor = Color(0xFF16181F)
            ) {
                QuickMenuSheetContent(
                    game = game,
                    repository = repository,
                    gameBoy = gameBoy,
                    settings = settings,
                    onPaletteChanged = { palette ->
                        settings.selectedPalette = palette
                        gameBoy.ppu.paletteColors = palette.colors
                    },
                    onReset = {
                        gameBoy.reset()
                        showQuickMenu = false
                    },
                    onExit = {
                        showQuickMenu = false
                        onExit()
                    }
                )
            }
        }
    }
}

@Composable
private fun QuickMenuSheetContent(
    game: RomMetadata,
    repository: RomRepository,
    gameBoy: GameBoy,
    settings: AppSettings,
    onPaletteChanged: (ColorPalette) -> Unit,
    onReset: () -> Unit,
    onExit: () -> Unit
) {
    var saveStatusMsg by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = "QUICK MENU",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00E5FF),
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Save State Slots",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (slot in 1..3) {
                val hasSlot = remember(slot) { repository.hasStateSlot(game.title, slot) }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF222632),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, Color(0xFF33384A), RoundedCornerShape(10.dp))
                        .clickable {
                            val stateData = gameBoy.saveState()
                            repository.saveStateSlot(game.title, slot, stateData)
                            saveStatusMsg = "Saved to Slot $slot!"
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Slot $slot",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (hasSlot) "Occupied" else "Empty",
                            fontSize = 11.sp,
                            color = if (hasSlot) Color(0xFF00E5FF) else Color(0xFF6B7280)
                        )
                    }
                }
            }
        }

        if (saveStatusMsg.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = saveStatusMsg,
                color = Color(0xFF05FFA1),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Color Palette",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ColorPalette.entries.take(4).forEach { pal ->
                val isSelected = settings.selectedPalette == pal
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFF00E5FF) else Color(0xFF222632))
                        .clickable { onPaletteChanged(pal) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = pal.displayName.split(" ").first(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF2D2024),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onReset() }
            ) {
                Box(
                    modifier = Modifier.padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Reset Game",
                        color = Color(0xFFFF5252),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF222632),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onExit() }
            ) {
                Box(
                    modifier = Modifier.padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Exit to Library",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}