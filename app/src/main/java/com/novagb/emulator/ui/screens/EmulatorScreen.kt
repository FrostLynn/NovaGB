package com.novagb.emulator.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import com.novagb.emulator.data.AppSettings
import com.novagb.emulator.data.ColorPalette
import com.novagb.emulator.data.EmulatorLogger
import com.novagb.emulator.data.RomMetadata
import com.novagb.emulator.data.RomRepository
import com.novagb.emulator.ui.components.RetroDisplay
import com.novagb.emulator.ui.components.TouchController
import com.novagb.emulator.ui.theme.AccentDanger
import com.novagb.emulator.ui.theme.AccentPrimary
import com.novagb.emulator.ui.theme.AccentSecondary
import com.novagb.emulator.ui.theme.AccentSuccess
import com.novagb.emulator.ui.theme.DarkBackground
import com.novagb.emulator.ui.theme.DarkBorder
import com.novagb.emulator.ui.theme.DarkBorderSubtle
import com.novagb.emulator.ui.theme.DarkSurface
import com.novagb.emulator.ui.theme.DarkSurfaceSubtle
import com.novagb.emulator.ui.theme.DarkSurfaceVariant
import com.novagb.emulator.ui.theme.DmgMagentaButton
import com.novagb.emulator.ui.theme.DmgShellBody
import com.novagb.emulator.ui.theme.DmgTextBlue
import com.novagb.emulator.ui.theme.SlotActiveLoadBg
import com.novagb.emulator.ui.theme.SlotDangerBg
import com.novagb.emulator.ui.theme.SlotSuccessBg
import com.novagb.emulator.ui.theme.TextMuted
import com.novagb.emulator.ui.theme.TextPrimary
import com.novagb.emulator.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    var emulationCrashError by remember { mutableStateOf<String?>(null) }

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
            val baseFrameNs = 16_742_706L // 1,000,000,000 / 59.7275 FPS
            val targetFrameTimeNs = if (isFastForward) {
                (baseFrameNs / settings.fastForwardSpeed).coerceAtLeast(1_000_000L)
            } else {
                baseFrameNs
            }
            var nextFrameTimeNs = System.nanoTime()

            while (isActive) {
                try {
                    gameBoy.stepFrame()
                    frameVersion.longValue++
                } catch (t: Throwable) {
                    EmulatorLogger.logError("GameLoop", "Emulation loop stepFrame crash", t, gameBoy)
                    withContext(Dispatchers.Main) {
                        emulationCrashError = t.message ?: "Unknown core emulation error"
                    }
                    break
                }

                frameCount++
                val nowMs = System.currentTimeMillis()
                if (nowMs - lastFpsCheck >= 1000) {
                    fpsDisplay = (frameCount * 1000 / (nowMs - lastFpsCheck)).toInt()
                    frameCount = 0
                    lastFpsCheck = nowMs
                }

                nextFrameTimeNs += targetFrameTimeNs
                val nowNs = System.nanoTime()
                val diffNs = nextFrameTimeNs - nowNs

                if (diffNs > 2_500_000L) {
                    val sleepMs = (diffNs / 1_000_000L) - 1
                    delay(sleepMs)
                    while (System.nanoTime() < nextFrameTimeNs && isActive) {
                        Thread.yield()
                    }
                } else if (diffNs > 0) {
                    while (System.nanoTime() < nextFrameTimeNs && isActive) {
                        Thread.yield()
                    }
                } else if (diffNs < -50_000_000L) {
                    nextFrameTimeNs = System.nanoTime()
                }
            }
        }
    }

    val isDmgShell = settings.showClassicConsoleShell

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDmgShell) DmgShellBody else DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isDmgShell) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(DarkBorder)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = if (isDmgShell) 6.dp else 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = game.title,
                    color = if (isDmgShell) DmgTextBlue else TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isFastForward) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isDmgShell) DmgMagentaButton else AccentSecondary,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "${settings.fastForwardSpeed}X TURBO",
                                color = if (isDmgShell) Color.White else Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (settings.showFps) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isDmgShell) DmgTextBlue else DarkSurfaceVariant,
                            border = if (!isDmgShell) androidx.compose.foundation.BorderStroke(0.5.dp, DarkBorder) else null
                        ) {
                            Text(
                                text = "$fpsDisplay FPS",
                                color = if (isDmgShell) Color.White else AccentPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            RetroDisplay(
                framebuffer = gameBoy.ppu.framebuffer,
                aspectRatioMode = settings.aspectRatio,
                showLcdGrid = settings.showLcdGrid,
                showScanlines = settings.showScanlines,
                showRetroBezel = settings.showRetroBezel,
                classicDmgShell = isDmgShell,
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
                classicDmgTheme = isDmgShell,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (showQuickMenu) {
            ModalBottomSheet(
                onDismissRequest = { showQuickMenu = false },
                sheetState = sheetState,
                containerColor = DarkSurface
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

        if (emulationCrashError != null) {
            AlertDialog(
                onDismissRequest = { emulationCrashError = null },
                title = {
                    Text("Emulation Error", fontWeight = FontWeight.Bold, color = AccentDanger)
                },
                text = {
                    Column {
                        Text(
                            text = "A core emulation exception occurred:\n$emulationCrashError",
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Detailed CPU, PPU, and stack trace dumps have been captured.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val success = EmulatorLogger.copyToClipboard(context)
                            Toast.makeText(
                                context,
                                if (success) "Logs copied to clipboard!" else "Failed to copy logs",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
                    ) {
                        Text("Copy Logs", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            val file = EmulatorLogger.exportToFile(context)
                            Toast.makeText(
                                context,
                                if (file != null) "Logs saved: ${file.name}" else "Failed to export",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Text("Export File", color = TextPrimary)
                    }
                },
                containerColor = DarkSurface,
                shape = RoundedCornerShape(16.dp)
            )
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
    var stateUpdateTrigger by remember { mutableIntStateOf(0) }
    var showResetConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "QUICK MENU",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentPrimary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = game.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Save State Slots",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (slot in 1..3) {
                @Suppress("UNUSED_VARIABLE")
                val trigger = stateUpdateTrigger
                val lastSaved = remember(slot, stateUpdateTrigger) {
                    repository.getStateSlotLastModified(game.title, slot)
                }
                val hasSlot = lastSaved > 0L
                val timeStr = if (hasSlot) {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(lastSaved))
                } else null

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (hasSlot) DarkBorder else DarkBorderSubtle
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Slot $slot",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (hasSlot) "Saved $timeStr" else "Empty",
                            fontSize = 10.sp,
                            color = if (hasSlot) AccentSuccess else TextMuted
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DarkSurfaceSubtle)
                                    .border(1.dp, AccentPrimary.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                    .clickable {
                                        val stateData = gameBoy.saveState()
                                        repository.saveStateSlot(game.title, slot, stateData)
                                        stateUpdateTrigger++
                                        saveStatusMsg = "Saved to Slot $slot!"
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "SAVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentPrimary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (hasSlot) SlotActiveLoadBg else DarkSurfaceSubtle)
                                    .border(
                                        1.dp,
                                        if (hasSlot) AccentSuccess.copy(alpha = 0.7f) else DarkBorderSubtle,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable(enabled = hasSlot) {
                                        val data = repository.loadStateSlot(game.title, slot)
                                        if (data != null && gameBoy.loadState(data)) {
                                            saveStatusMsg = "Loaded Slot $slot!"
                                        } else {
                                            saveStatusMsg = "Failed to load Slot $slot"
                                        }
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "LOAD",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasSlot) AccentSuccess else TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        if (saveStatusMsg.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SlotSuccessBg)
                    .border(1.dp, AccentSuccess.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = AccentSuccess,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = saveStatusMsg,
                    color = AccentSuccess,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Color Palette",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(ColorPalette.entries) { pal ->
                val isSelected = settings.selectedPalette == pal
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) DarkSurfaceVariant else DarkSurfaceSubtle,
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (isSelected) AccentPrimary else DarkBorderSubtle
                    ),
                    modifier = Modifier.clickable { onPaletteChanged(pal) }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            pal.colors.forEach { c ->
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(c))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = pal.displayName,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) AccentPrimary else TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Diagnostics & Logs",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DarkSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorderSubtle),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clickable {
                        val success = EmulatorLogger.copyToClipboard(context)
                        saveStatusMsg = if (success) "Logs copied to clipboard!" else "Failed to copy logs"
                    }
            ) {
                Box(
                    modifier = Modifier.padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Copy Logs",
                        color = AccentPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DarkSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorderSubtle),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .clickable {
                        val file = EmulatorLogger.exportToFile(context)
                        saveStatusMsg = if (file != null) "Saved to ${file.name}!" else "Failed to save file"
                    }
            ) {
                Box(
                    modifier = Modifier.padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Save Log File",
                        color = AccentPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
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
                color = SlotDangerBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentDanger.copy(alpha = 0.5f)),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clickable { showResetConfirm = true }
            ) {
                Box(
                    modifier = Modifier.padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Reset Game",
                        color = AccentDanger,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = DarkSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clickable { onExit() }
            ) {
                Box(
                    modifier = Modifier.padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Exit to Library",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = {
                Text("Reset Emulation?", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Text(
                    "Any unsaved progress in this session will be lost. Do you want to restart the Game Boy core?",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        onReset()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentDanger),
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Text("Reset Game", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showResetConfirm = false },
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Text("Cancel", color = TextPrimary)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
