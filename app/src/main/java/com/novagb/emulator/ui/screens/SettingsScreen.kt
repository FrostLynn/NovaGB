package com.novagb.emulator.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.novagb.emulator.data.AppSettings
import com.novagb.emulator.data.EmulatorLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }

    var showLcdGrid by remember { mutableStateOf(settings.showLcdGrid) }
    var showScanlines by remember { mutableStateOf(settings.showScanlines) }
    var showRetroBezel by remember { mutableStateOf(settings.showRetroBezel) }
    var showClassicConsoleShell by remember { mutableStateOf(settings.showClassicConsoleShell) }
    var showFps by remember { mutableStateOf(settings.showFps) }
    var hapticsEnabled by remember { mutableStateOf(settings.hapticFeedbackEnabled) }
    var soundEnabled by remember { mutableStateOf(settings.soundEnabled) }
    var buttonOpacity by remember { mutableFloatStateOf(settings.buttonOpacity) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF101114)
                )
            )
        },
        containerColor = Color(0xFF101114)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SettingsSectionTitle(title = "VIDEO & DISPLAY")

            SettingsToggleItem(
                title = "Simulated LCD Grid",
                subtitle = "Subtle dot-matrix grid effect on game screen",
                checked = showLcdGrid,
                onCheckedChange = {
                    showLcdGrid = it
                    settings.showLcdGrid = it
                }
            )

            SettingsToggleItem(
                title = "CRT Scanlines",
                subtitle = "Simulates retro tube television scanlines",
                checked = showScanlines,
                onCheckedChange = {
                    showScanlines = it
                    settings.showScanlines = it
                }
            )

            SettingsToggleItem(
                title = "Authentic Retro Bezel",
                subtitle = "Classic Game Boy console frame with branding and glowing power LED",
                checked = showRetroBezel,
                onCheckedChange = {
                    showRetroBezel = it
                    settings.showRetroBezel = it
                }
            )

            SettingsToggleItem(
                title = "Classic DMG Console Shell",
                subtitle = "Authentic light gray ABS body, cross D-Pad, magenta A/B buttons, and speaker grille",
                checked = showClassicConsoleShell,
                onCheckedChange = {
                    showClassicConsoleShell = it
                    settings.showClassicConsoleShell = it
                }
            )

            SettingsToggleItem(
                title = "Show FPS Counter",
                subtitle = "Displays current frames-per-second",
                checked = showFps,
                onCheckedChange = {
                    showFps = it
                    settings.showFps = it
                }
            )

            SettingsSectionTitle(title = "AUDIO")

            SettingsToggleItem(
                title = "Enable Sound",
                subtitle = "Synthesize 4-channel Game Boy APU audio",
                checked = soundEnabled,
                onCheckedChange = {
                    soundEnabled = it
                    settings.soundEnabled = it
                }
            )

            SettingsSectionTitle(title = "TOUCH CONTROLS")

            SettingsToggleItem(
                title = "Haptic Tactile Feedback",
                subtitle = "Vibrate subtly when pressing D-pad and buttons",
                checked = hapticsEnabled,
                onCheckedChange = {
                    hapticsEnabled = it
                    settings.hapticFeedbackEnabled = it
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF181A22))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Controller Opacity", fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text("${(buttonOpacity * 100).toInt()}%", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = buttonOpacity,
                    onValueChange = {
                        buttonOpacity = it
                        settings.buttonOpacity = it
                    },
                    valueRange = 0.2f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00E5FF),
                        activeTrackColor = Color(0xFF00E5FF),
                        inactiveTrackColor = Color(0xFF33384A)
                    )
                )
            }

            SettingsSectionTitle(title = "DIAGNOSTICS & LOGS")

            SettingsActionItem(
                title = "Copy Logs to Clipboard",
                subtitle = "${EmulatorLogger.getLogCount()} diagnostic entries recorded",
                onClick = {
                    val success = EmulatorLogger.copyToClipboard(context)
                    Toast.makeText(
                        context,
                        if (success) "Logs copied to clipboard" else "Failed to copy logs",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )

            SettingsActionItem(
                title = "Save Logs to File",
                subtitle = "Exports novagb_logs.txt to app files directory",
                onClick = {
                    val file = EmulatorLogger.exportToFile(context)
                    Toast.makeText(
                        context,
                        if (file != null) "Logs saved: ${file.name}" else "Failed to save logs",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )

            SettingsSectionTitle(title = "ABOUT")

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF181A22))
                    .padding(14.dp)
            ) {
                Text("NovaGB Emulator", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Version 0.0.2 (Alpha)", color = Color(0xFF8E95A5), fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Built with Jetpack Compose & Kotlin. Clean architecture, high-performance cycle emulation, and modern Android Material 3 design.",
                    color = Color(0xFF6B7280),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF00E5FF),
        letterSpacing = 1.sp
    )
}

@Composable
private fun SettingsActionItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF181A22))
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = Color(0xFF8E95A5), fontSize = 12.sp)
        }
        Text(
            text = "EXPORT",
            color = Color(0xFF00E5FF),
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF181A22))
            .clickable { onCheckedChange(!checked) }
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = Color(0xFF8E95A5), fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = Color(0xFF00E5FF),
                uncheckedThumbColor = Color(0xFF8E95A5),
                uncheckedTrackColor = Color(0xFF262A36)
            )
        )
    }
}