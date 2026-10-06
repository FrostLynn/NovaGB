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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import com.novagb.emulator.ui.theme.AccentPrimary
import com.novagb.emulator.ui.theme.DarkBackground
import com.novagb.emulator.ui.theme.DarkBorder
import com.novagb.emulator.ui.theme.DarkBorderSubtle
import com.novagb.emulator.ui.theme.DarkSurface
import com.novagb.emulator.ui.theme.DarkSurfaceVariant
import com.novagb.emulator.ui.theme.TextMuted
import com.novagb.emulator.ui.theme.TextPrimary
import com.novagb.emulator.ui.theme.TextSecondary

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
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsSectionTitle(title = "VIDEO & DISPLAY")
                SettingsCardContainer {
                    SettingsToggleItem(
                        title = "Simulated LCD Grid",
                        subtitle = "Subtle dot-matrix grid effect on game screen",
                        checked = showLcdGrid,
                        onCheckedChange = {
                            showLcdGrid = it
                            settings.showLcdGrid = it
                        }
                    )
                    HorizontalDivider(color = DarkBorderSubtle, thickness = 0.5.dp)
                    SettingsToggleItem(
                        title = "CRT Scanlines",
                        subtitle = "Simulates retro tube television scanlines",
                        checked = showScanlines,
                        onCheckedChange = {
                            showScanlines = it
                            settings.showScanlines = it
                        }
                    )
                    HorizontalDivider(color = DarkBorderSubtle, thickness = 0.5.dp)
                    SettingsToggleItem(
                        title = "Authentic Retro Bezel",
                        subtitle = "Classic console frame with branding and glowing power LED",
                        checked = showRetroBezel,
                        onCheckedChange = {
                            showRetroBezel = it
                            settings.showRetroBezel = it
                        }
                    )
                    HorizontalDivider(color = DarkBorderSubtle, thickness = 0.5.dp)
                    SettingsToggleItem(
                        title = "Classic DMG Console Shell",
                        subtitle = "Authentic light gray ABS body, cross D-Pad, and magenta buttons",
                        checked = showClassicConsoleShell,
                        onCheckedChange = {
                            showClassicConsoleShell = it
                            settings.showClassicConsoleShell = it
                        }
                    )
                    HorizontalDivider(color = DarkBorderSubtle, thickness = 0.5.dp)
                    SettingsToggleItem(
                        title = "Show FPS Counter",
                        subtitle = "Displays current frames-per-second in emulation",
                        checked = showFps,
                        onCheckedChange = {
                            showFps = it
                            settings.showFps = it
                        }
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsSectionTitle(title = "AUDIO")
                SettingsCardContainer {
                    SettingsToggleItem(
                        title = "Enable APU Sound",
                        subtitle = "Synthesize 4-channel Game Boy sound processor audio",
                        checked = soundEnabled,
                        onCheckedChange = {
                            soundEnabled = it
                            settings.soundEnabled = it
                        }
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsSectionTitle(title = "TOUCH CONTROLS")
                SettingsCardContainer {
                    SettingsToggleItem(
                        title = "Haptic Tactile Feedback",
                        subtitle = "Vibrate subtly when pressing D-pad and buttons",
                        checked = hapticsEnabled,
                        onCheckedChange = {
                            hapticsEnabled = it
                            settings.hapticFeedbackEnabled = it
                        }
                    )
                    HorizontalDivider(color = DarkBorderSubtle, thickness = 0.5.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Controller Opacity",
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = DarkSurfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, DarkBorder)
                            ) {
                                Text(
                                    text = "${(buttonOpacity * 100).toInt()}%",
                                    color = AccentPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Adjust on-screen gamepad transparency during gameplay",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Slider(
                            value = buttonOpacity,
                            onValueChange = {
                                buttonOpacity = it
                                settings.buttonOpacity = it
                            },
                            valueRange = 0.2f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = AccentPrimary,
                                activeTrackColor = AccentPrimary,
                                inactiveTrackColor = DarkSurfaceVariant
                            )
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsSectionTitle(title = "DIAGNOSTICS & LOGS")
                SettingsCardContainer {
                    SettingsActionItem(
                        title = "Copy Logs to Clipboard",
                        subtitle = "${EmulatorLogger.getLogCount()} diagnostic entries recorded",
                        actionLabel = "COPY",
                        icon = Icons.Default.ContentCopy,
                        onClick = {
                            val success = EmulatorLogger.copyToClipboard(context)
                            Toast.makeText(
                                context,
                                if (success) "Logs copied to clipboard" else "Failed to copy logs",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    HorizontalDivider(color = DarkBorderSubtle, thickness = 0.5.dp)
                    SettingsActionItem(
                        title = "Save Logs to File",
                        subtitle = "Exports novagb_logs.txt to app files directory",
                        actionLabel = "EXPORT",
                        icon = Icons.Default.FileDownload,
                        onClick = {
                            val file = EmulatorLogger.exportToFile(context)
                            Toast.makeText(
                                context,
                                if (file != null) "Logs saved: ${file.name}" else "Failed to save logs",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsSectionTitle(title = "ABOUT")
                SettingsCardContainer {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AccentPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SportsEsports,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "NovaGB Emulator",
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Version 0.0.2 (Alpha)",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "High-performance cycle-accurate Game Boy (DMG) and Game Boy Color (CGB) emulator engineered with Jetpack Compose, Kotlin, and Material 3.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsCardContainer(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(AccentPrimary)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = AccentPrimary,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun SettingsActionItem(
    title: String,
    subtitle: String,
    actionLabel: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = TextMuted, fontSize = 12.sp)
        }
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = DarkSurfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorderSubtle)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AccentPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = actionLabel,
                    color = AccentPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
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
            .heightIn(min = 52.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, color = TextMuted, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = AccentPrimary,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = DarkSurfaceVariant
            )
        )
    }
}
