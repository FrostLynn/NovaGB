package com.novagb.emulator.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novagb.emulator.core.JoypadButton
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Modern semi-transparent frosted glass touch controller with tactile haptic feedback.
 */
@Composable
fun TouchController(
    onButtonChange: (JoypadButton, Boolean) -> Unit,
    onMenuClick: () -> Unit,
    onFastForwardToggle: () -> Unit,
    isFastForwardActive: Boolean,
    opacity: Float = 0.7f,
    scaleFactor: Float = 1.0f,
    hapticsEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    val triggerHaptic = {
        if (hapticsEnabled) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(25)
                }
            } catch (e: Exception) {}
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .scale(scaleFactor)
            .alpha(opacity)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Toolbar: Menu, Fast-Forward
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PillButton(
                text = "MENU",
                onClick = { triggerHaptic(); onMenuClick() }
            )

            PillButton(
                text = if (isFastForwardActive) "2X TURBO" else "1X PLAY",
                active = isFastForwardActive,
                onClick = { triggerHaptic(); onFastForwardToggle() }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Main controls row: D-Pad on Left, Action Buttons on Right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // Modern Circular D-Pad
            ModernDPad(
                onButtonChange = { btn, pressed ->
                    if (pressed) triggerHaptic()
                    onButtonChange(btn, pressed)
                }
            )

            // Modern Action Buttons (A, B, Turbo A, Turbo B)
            ActionButtonsGroup(
                onButtonChange = { btn, pressed ->
                    if (pressed) triggerHaptic()
                    onButtonChange(btn, pressed)
                }
            )
        }

        // Bottom Center: SELECT & START
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PillButton(
                text = "SELECT",
                onClick = {
                    triggerHaptic()
                    onButtonChange(JoypadButton.SELECT, true)
                    onButtonChange(JoypadButton.SELECT, false)
                }
            )
            Spacer(modifier = Modifier.width(32.dp))
            PillButton(
                text = "START",
                onClick = {
                    triggerHaptic()
                    onButtonChange(JoypadButton.START, true)
                    onButtonChange(JoypadButton.START, false)
                }
            )
        }
    }
}

@Composable
private fun ModernDPad(
    onButtonChange: (JoypadButton, Boolean) -> Unit
) {
    var activeDirection by remember { mutableStateOf<JoypadButton?>(null) }

    Box(
        modifier = Modifier
            .size(160.dp)
            .shadow(12.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF282B34), Color(0xFF16181F))
                )
            )
            .border(2.dp, Color(0xFF3B404E), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val dir = calculateDirection(offset.x, offset.y, size.width.toFloat(), size.height.toFloat())
                        if (dir != activeDirection) {
                            activeDirection?.let { onButtonChange(it, false) }
                            dir?.let { onButtonChange(it, true) }
                            activeDirection = dir
                        }
                    },
                    onDragEnd = {
                        activeDirection?.let { onButtonChange(it, false) }
                        activeDirection = null
                    },
                    onDragCancel = {
                        activeDirection?.let { onButtonChange(it, false) }
                        activeDirection = null
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val dir = calculateDirection(
                            change.position.x,
                            change.position.y,
                            size.width.toFloat(),
                            size.height.toFloat()
                        )
                        if (dir != activeDirection) {
                            activeDirection?.let { onButtonChange(it, false) }
                            dir?.let { onButtonChange(it, true) }
                            activeDirection = dir
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Direction indicators
        Text(
            text = "▲",
            color = if (activeDirection == JoypadButton.UP) Color(0xFF00E5FF) else Color(0xFF8E95A5),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)
        )
        Text(
            text = "▼",
            color = if (activeDirection == JoypadButton.DOWN) Color(0xFF00E5FF) else Color(0xFF8E95A5),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp)
        )
        Text(
            text = "◀",
            color = if (activeDirection == JoypadButton.LEFT) Color(0xFF00E5FF) else Color(0xFF8E95A5),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp)
        )
        Text(
            text = "▶",
            color = if (activeDirection == JoypadButton.RIGHT) Color(0xFF00E5FF) else Color(0xFF8E95A5),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp)
        )

        // Center hub
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E2028))
                .border(1.dp, Color(0xFF383C49), CircleShape)
        )
    }
}

private fun calculateDirection(x: Float, y: Float, width: Float, height: Float): JoypadButton? {
    val centerX = width / 2f
    val centerY = height / 2f
    val dx = x - centerX
    val dy = y - centerY
    val dist = sqrt(dx * dx + dy * dy)

    if (dist < 20f) return null // Deadzone

    val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    return when {
        angle in -45f..45f -> JoypadButton.RIGHT
        angle in 45f..135f -> JoypadButton.DOWN
        angle in -135f..-45f -> JoypadButton.UP
        else -> JoypadButton.LEFT
    }
}

@Composable
private fun ActionButtonsGroup(
    onButtonChange: (JoypadButton, Boolean) -> Unit
) {
    Box(
        modifier = Modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        // B Button (Bottom-Left)
        CircularActionButton(
            label = "B",
            color = Color(0xFFFF2A6D),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(y = 20.dp),
            onPressChange = { onButtonChange(JoypadButton.B, it) }
        )

        // A Button (Top-Right)
        CircularActionButton(
            label = "A",
            color = Color(0xFF00E5FF),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(y = (-20).dp),
            onPressChange = { onButtonChange(JoypadButton.A, it) }
        )
    }
}

@Composable
private fun CircularActionButton(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onPressChange: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(64.dp)
            .shadow(if (isPressed) 4.dp else 10.dp, CircleShape)
            .clip(CircleShape)
            .background(
                if (isPressed) color.copy(alpha = 0.9f)
                else Color(0xFF22252E)
            )
            .border(
                2.dp,
                if (isPressed) color else color.copy(alpha = 0.6f),
                CircleShape
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onPressChange(true)
                        tryAwaitRelease()
                        isPressed = false
                        onPressChange(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isPressed) Color.Black else color,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp
        )
    }
}

@Composable
private fun PillButton(
    text: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) Color(0xFF00E5FF) else Color(0xFF242731))
            .border(1.dp, if (active) Color(0xFF00E5FF) else Color(0xFF3B404E), RoundedCornerShape(20.dp))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            }
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (active) Color.Black else Color(0xFFD0D5E0),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.sp
        )
    }
}
