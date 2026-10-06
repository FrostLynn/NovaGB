package com.novagb.emulator.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novagb.emulator.audio.GbAudioPlayer
import com.novagb.emulator.core.GameBoy
import com.novagb.emulator.core.JoypadButton
import com.novagb.emulator.data.AppSettings
import com.novagb.emulator.data.ColorPalette
import com.novagb.emulator.data.EmulatorLogger
import com.novagb.emulator.data.RomMetadata
import com.novagb.emulator.data.RomRepository
import com.novagb.emulator.ui.components.RetroDisplay
import com.novagb.emulator.ui.components.TouchController
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
    var isEditingControllerLayout by remember { mutableStateOf(false) }
    var layoutUpdateTrigger by remember { mutableIntStateOf(0) }
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
                    // Sleep coarse milliseconds, leaving headroom for fine-tuned precision yield
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
                    // Reset timing anchor if fallen more than 3 frames behind (e.g. app paused)
                    nextFrameTimeNs = System.nanoTime()
                }
            }
        }
    }

    val isDmgShell = settings.showClassicConsoleShell

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDmgShell) Color(0xFFC8CACC) else Color(0xFF0C0D10))
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
                        .background(Color(0xFFB0B3BA))
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
                    color = if (isDmgShell) Color(0xFF1E2438) else Color(0xFFD0D5E0),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isFastForward) {
                        Text(
                            text = "${settings.fastForwardSpeed}X FAST",
                            color = if (isDmgShell) Color(0xFF88153A) else Color(0xFFFF2A6D),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    if (settings.showFps) {
                        Text(
                            text = "$fpsDisplay FPS",
                            color = if (isDmgShell) Color(0xFF0F205A) else Color(0xFF00E5FF),
                            fontWeight = FontWeight.Bold,
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
                showRetroBezel = settings.showRetroBezel,
                classicDmgShell = isDmgShell,
                modifier = Modifier.weight(1f, fill = false),
                frameIndexProvider = { frameVersion.longValue }
            )

            @Suppress("UNUSED_VARIABLE")
            val trigger = layoutUpdateTrigger
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
                isEditingLayout = isEditingControllerLayout,
                dpadOffsetX = settings.dpadOffsetX,
                dpadOffsetY = settings.dpadOffsetY,
                dpadScale = settings.dpadScale,
                actionOffsetX = settings.actionOffsetX,
                actionOffsetY = settings.actionOffsetY,
                actionScale = settings.actionScale,
                selectStartOffsetX = settings.selectStartOffsetX,
                selectStartOffsetY = settings.selectStartOffsetY,
                selectStartScale = settings.selectStartScale,
                onDpadDrag = { dx, dy ->
                    settings.dpadOffsetX = (settings.dpadOffsetX + dx).coerceIn(-100f, 100f)
                    settings.dpadOffsetY = (settings.dpadOffsetY + dy).coerceIn(-120f, 120f)
                    layoutUpdateTrigger++
                },
                onActionDrag = { dx, dy ->
                    settings.actionOffsetX = (settings.actionOffsetX + dx).coerceIn(-100f, 100f)
                    settings.actionOffsetY = (settings.actionOffsetY + dy).coerceIn(-120f, 120f)
                    layoutUpdateTrigger++
                },
                onSelectStartDrag = { dx, dy ->
                    settings.selectStartOffsetX = (settings.selectStartOffsetX + dx).coerceIn(-80f, 80f)
                    settings.selectStartOffsetY = (settings.selectStartOffsetY + dy).coerceIn(-60f, 60f)
                    layoutUpdateTrigger++
                },
                onDpadScaleChange = { s ->
                    settings.dpadScale = s
                    layoutUpdateTrigger++
                },
                onActionScaleChange = { s ->
                    settings.actionScale = s
                    layoutUpdateTrigger++
                },
                onSelectStartScaleChange = { s ->
                    settings.selectStartScale = s
                    layoutUpdateTrigger++
                },
                onResetLayout = {
                    settings.resetControllerLayout()
                    layoutUpdateTrigger++
                },
                onFinishEditingLayout = {
                    isEditingControllerLayout = false
                },
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
                    onCustomizeLayout = {
                        showQuickMenu = false
                        isEditingControllerLayout = true
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
                    Text("Emulation Error", fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                },
                text = {
                    Column {
                        Text(
                            text = "A core emulation exception occurred:\n$emulationCrashError",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Detailed CPU, PPU, and stack trace dumps have been captured.",
                            color = Color(0xFF8E95A5),
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
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
                        Text("Export File", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1E212B)
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
    onCustomizeLayout: () -> Unit,
    onReset: () -> Unit,
    onExit: () -> Unit
) {
    var saveStatusMsg by remember { mutableStateOf("") }
    var stateUpdateTrigger by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

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
                @Suppress("UNUSED_VARIABLE")
                val trigger = stateUpdateTrigger
                val lastSaved = remember(slot, stateUpdateTrigger) {
                    repository.getStateSlotLastModified(game.title, slot)
                }
                val thumbnail = remember(slot, stateUpdateTrigger) {
                    repository.loadStateThumbnail(game.title, slot)
                }
                val hasSlot = lastSaved > 0L
                val timeStr = if (hasSlot) {
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(lastSaved))
                } else null

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E212A),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, if (hasSlot) Color(0xFF384052) else Color(0xFF282C38), RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Slot $slot",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (hasSlot) "Saved $timeStr" else "Empty",
                            fontSize = 9.sp,
                            color = if (hasSlot) Color(0xFF00E5FF) else Color(0xFF8E95A5)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF14161E))
                                .border(1.dp, Color(0xFF282C38), RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (thumbnail != null) {
                                Image(
                                    bitmap = thumbnail.asImageBitmap(),
                                    contentDescription = "Slot $slot Screenshot",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = "NO CAPTURE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6B7280)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF282D3A))
                                    .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .clickable {
                                        val stateData = gameBoy.saveState()
                                        repository.saveStateSlot(game.title, slot, stateData)
                                        repository.saveStateThumbnail(game.title, slot, gameBoy.ppu.framebuffer)
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
                                    color = Color(0xFF00E5FF)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (hasSlot) Color(0xFF1A3328) else Color(0xFF16181F))
                                    .border(
                                        1.dp,
                                        if (hasSlot) Color(0xFF05FFA1).copy(alpha = 0.6f) else Color(0xFF2E3342),
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
                                    color = if (hasSlot) Color(0xFF05FFA1) else Color(0xFF6B7280)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (saveStatusMsg.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
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

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(ColorPalette.entries) { pal ->
                val isSelected = settings.selectedPalette == pal
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Color(0xFF222B38) else Color(0xFF1B1D24),
                    modifier = Modifier
                        .border(
                            1.5.dp,
                            if (isSelected) Color(0xFF00E5FF) else Color(0xFF2A2E3B),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onPaletteChanged(pal) }
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
                            color = if (isSelected) Color(0xFF00E5FF) else Color(0xFFD0D5E0)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Controller & Layout",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF182230),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCustomizeLayout() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Edit On-Screen Controls",
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Drag buttons to reposition & adjust sizes",
                        color = Color(0xFF8E95A5),
                        fontSize = 11.sp
                    )
                }
                Text(
                    text = "EDIT",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .background(Color(0xFF00E5FF), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Diagnostics & Logs",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF182230),
                modifier = Modifier
                    .weight(1f)
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
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF182230),
                modifier = Modifier
                    .weight(1f)
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
                        color = Color(0xFF00E5FF),
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