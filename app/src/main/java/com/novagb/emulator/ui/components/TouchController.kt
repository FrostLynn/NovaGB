package com.novagb.emulator.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novagb.emulator.core.JoypadButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Modern semi-transparent frosted glass touch controller with tactile haptic feedback,
 * 8-direction D-pad (diagonal input), holdable Start/Select, and Turbo buttons.
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
    classicDmgTheme: Boolean = false,
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
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .scale(scaleFactor)
            .alpha(opacity)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (classicDmgTheme) {
                ClassicDmgUtilityButton(
                    text = "MENU",
                    onClick = { triggerHaptic(); onMenuClick() }
                )

                ClassicDmgUtilityButton(
                    text = if (isFastForwardActive) "2X TURBO" else "1X PLAY",
                    active = isFastForwardActive,
                    onClick = { triggerHaptic(); onFastForwardToggle() }
                )
            } else {
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
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (classicDmgTheme) {
                ClassicDmgDPad(
                    onButtonChange = { btn, pressed ->
                        if (pressed) triggerHaptic()
                        onButtonChange(btn, pressed)
                    }
                )

                ClassicDmgActionButtons(
                    onButtonChange = { btn, pressed ->
                        if (pressed) triggerHaptic()
                        onButtonChange(btn, pressed)
                    }
                )
            } else {
                ModernDPad(
                    onButtonChange = { btn, pressed ->
                        if (pressed) triggerHaptic()
                        onButtonChange(btn, pressed)
                    }
                )

                ActionButtonsGroup(
                    onButtonChange = { btn, pressed ->
                        if (pressed) triggerHaptic()
                        onButtonChange(btn, pressed)
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (classicDmgTheme) {
                    ClassicDmgSelectStartButton(
                        label = "SELECT",
                        onPressChange = { pressed ->
                            if (pressed) triggerHaptic()
                            onButtonChange(JoypadButton.SELECT, pressed)
                        }
                    )
                    Spacer(modifier = Modifier.width(36.dp))
                    ClassicDmgSelectStartButton(
                        label = "START",
                        onPressChange = { pressed ->
                            if (pressed) triggerHaptic()
                            onButtonChange(JoypadButton.START, pressed)
                        }
                    )
                } else {
                    PillButton(
                        text = "SELECT",
                        onPressChange = { pressed ->
                            if (pressed) triggerHaptic()
                            onButtonChange(JoypadButton.SELECT, pressed)
                        }
                    )
                    Spacer(modifier = Modifier.width(32.dp))
                    PillButton(
                        text = "START",
                        onPressChange = { pressed ->
                            if (pressed) triggerHaptic()
                            onButtonChange(JoypadButton.START, pressed)
                        }
                    )
                }
            }

            if (classicDmgTheme) {
                ClassicDmgSpeakerGrille(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 4.dp, bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun ClassicDmgUtilityButton(
    text: String,
    active: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (active) Color(0xFF0F205A) else Color(0xFF565860),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (active) Color(0xFF0F205A) else Color(0xFF3E4048)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            color = if (active) Color.White else Color(0xFFE2E4E8),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ClassicDmgDPad(
    onButtonChange: (JoypadButton, Boolean) -> Unit
) {
    var activeDirections by remember { mutableStateOf<Set<JoypadButton>>(emptySet()) }

    Box(
        modifier = Modifier
            .size(160.dp)
            .clip(CircleShape)
            .background(Color(0xFFB8BAC0))
            .border(2.dp, Color(0xFFA0A3AB), CircleShape)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)

                    val updateDirections: (Float, Float) -> Unit = { px, py ->
                        val newDirs = calculateDirections(px, py, size.width.toFloat(), size.height.toFloat())
                        if (newDirs != activeDirections) {
                            for (b in activeDirections - newDirs) {
                                onButtonChange(b, false)
                            }
                            for (b in newDirs - activeDirections) {
                                onButtonChange(b, true)
                            }
                            activeDirections = newDirs
                        }
                    }

                    updateDirections(down.position.x, down.position.y)

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) {
                            for (b in activeDirections) {
                                onButtonChange(b, false)
                            }
                            activeDirections = emptySet()
                            break
                        }
                        updateDirections(change.position.x, change.position.y)
                        change.consume()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val isUp = JoypadButton.UP in activeDirections
        val isDown = JoypadButton.DOWN in activeDirections
        val isLeft = JoypadButton.LEFT in activeDirections
        val isRight = JoypadButton.RIGHT in activeDirections

        // Authentic Game Boy Black Cross Shape
        Box(
            modifier = Modifier
                .width(136.dp)
                .height(44.dp)
                .shadow(4.dp, RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF18191E))
                .border(1.dp, Color(0xFF2E313A), RoundedCornerShape(4.dp))
        )

        Box(
            modifier = Modifier
                .width(44.dp)
                .height(136.dp)
                .shadow(4.dp, RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF18191E))
                .border(1.dp, Color(0xFF2E313A), RoundedCornerShape(4.dp))
        )

        Text(
            text = "▲",
            color = if (isUp) Color(0xFF00E5FF) else Color(0xFF3E414B),
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
        )
        Text(
            text = "▼",
            color = if (isDown) Color(0xFF00E5FF) else Color(0xFF3E414B),
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
        )
        Text(
            text = "◀",
            color = if (isLeft) Color(0xFF00E5FF) else Color(0xFF3E414B),
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp)
        )
        Text(
            text = "▶",
            color = if (isRight) Color(0xFF00E5FF) else Color(0xFF3E414B),
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
        )

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFF101114))
                .border(1.5.dp, Color(0xFF282A33), CircleShape)
        )
    }
}

@Composable
private fun ClassicDmgActionButtons(
    onButtonChange: (JoypadButton, Boolean) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TurboActionButton(
                label = "TB",
                color = Color(0xFF88153A),
                button = JoypadButton.B,
                onButtonPulse = onButtonChange
            )
            TurboActionButton(
                label = "TA",
                color = Color(0xFF88153A),
                button = JoypadButton.A,
                onButtonPulse = onButtonChange
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .width(160.dp)
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(66.dp)
                    .graphicsLayer(rotationZ = -26f)
                    .clip(RoundedCornerShape(33.dp))
                    .background(Color(0xFFB8BAC0))
                    .border(1.5.dp, Color(0xFFA0A3AB), RoundedCornerShape(33.dp))
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = (-30).dp, y = 16.dp)
            ) {
                DmgRoundButton(
                    label = "",
                    color = Color(0xFF9E1644),
                    onPressChange = { onButtonChange(JoypadButton.B, it) }
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "B",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontStyle = FontStyle.Italic,
                    color = Color(0xFF0F205A)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = 30.dp, y = (-14).dp)
            ) {
                DmgRoundButton(
                    label = "",
                    color = Color(0xFF9E1644),
                    onPressChange = { onButtonChange(JoypadButton.A, it) }
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "A",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontStyle = FontStyle.Italic,
                    color = Color(0xFF0F205A)
                )
            }
        }
    }
}

@Composable
private fun DmgRoundButton(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onPressChange: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(52.dp)
            .shadow(if (isPressed) 2.dp else 6.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    colors = if (isPressed) {
                        listOf(Color(0xFF720B2E), Color(0xFF5A0824))
                    } else {
                        listOf(Color(0xFFB51D51), Color(0xFF8A1039))
                    }
                )
            )
            .border(
                1.5.dp,
                if (isPressed) Color(0xFF5A0824) else Color(0xFF6E0A2C),
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
        if (label.isNotEmpty()) {
            Text(text = label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ClassicDmgSelectStartButton(
    label: String,
    onPressChange: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp)
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(16.dp)
                .graphicsLayer(rotationZ = -26f)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isPressed) Color(0xFF383A42)
                    else Color(0xFF5A5C64)
                )
                .border(
                    1.dp,
                    if (isPressed) Color(0xFF28292E) else Color(0xFF42444C),
                    RoundedCornerShape(8.dp)
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
                }
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            fontStyle = FontStyle.Italic,
            color = Color(0xFF0F205A),
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun ClassicDmgSpeakerGrille(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val heights = listOf(18.dp, 24.dp, 30.dp, 30.dp, 24.dp, 18.dp)
        heights.forEach { h ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(h)
                    .graphicsLayer(rotationZ = -28f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF282B33))
            )
        }
    }
}

@Composable
private fun ModernDPad(
    onButtonChange: (JoypadButton, Boolean) -> Unit
) {
    var activeDirections by remember { mutableStateOf<Set<JoypadButton>>(emptySet()) }

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
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)

                    val updateDirections: (Float, Float) -> Unit = { px, py ->
                        val newDirs = calculateDirections(px, py, size.width.toFloat(), size.height.toFloat())
                        if (newDirs != activeDirections) {
                            for (b in activeDirections - newDirs) {
                                onButtonChange(b, false)
                            }
                            for (b in newDirs - activeDirections) {
                                onButtonChange(b, true)
                            }
                            activeDirections = newDirs
                        }
                    }

                    updateDirections(down.position.x, down.position.y)

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) {
                            for (b in activeDirections) {
                                onButtonChange(b, false)
                            }
                            activeDirections = emptySet()
                            break
                        }
                        updateDirections(change.position.x, change.position.y)
                        change.consume()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val isUp = JoypadButton.UP in activeDirections
        val isDown = JoypadButton.DOWN in activeDirections
        val isLeft = JoypadButton.LEFT in activeDirections
        val isRight = JoypadButton.RIGHT in activeDirections

        Text(
            text = "▲",
            color = if (isUp) Color(0xFF00E5FF) else Color(0xFF8E95A5),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp)
        )
        Text(
            text = "▼",
            color = if (isDown) Color(0xFF00E5FF) else Color(0xFF8E95A5),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp)
        )
        Text(
            text = "◀",
            color = if (isLeft) Color(0xFF00E5FF) else Color(0xFF8E95A5),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp)
        )
        Text(
            text = "▶",
            color = if (isRight) Color(0xFF00E5FF) else Color(0xFF8E95A5),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp)
        )

        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E2028))
                .border(1.dp, Color(0xFF383C49), CircleShape)
        )
    }
}

/**
 * Calculates 8-direction JoypadButton combinations based on touch angle.
 */
private fun calculateDirections(x: Float, y: Float, width: Float, height: Float): Set<JoypadButton> {
    val centerX = width / 2f
    val centerY = height / 2f
    val dx = x - centerX
    val dy = y - centerY
    val dist = sqrt(dx * dx + dy * dy)

    if (dist < 18f) return emptySet() // Deadzone

    val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

    return when {
        angle in -22.5f..22.5f -> setOf(JoypadButton.RIGHT)
        angle in 22.5f..67.5f -> setOf(JoypadButton.DOWN, JoypadButton.RIGHT)
        angle in 67.5f..112.5f -> setOf(JoypadButton.DOWN)
        angle in 112.5f..157.5f -> setOf(JoypadButton.DOWN, JoypadButton.LEFT)
        angle in -67.5f..-22.5f -> setOf(JoypadButton.UP, JoypadButton.RIGHT)
        angle in -112.5f..-67.5f -> setOf(JoypadButton.UP)
        angle in -157.5f..-112.5f -> setOf(JoypadButton.UP, JoypadButton.LEFT)
        else -> setOf(JoypadButton.LEFT)
    }
}

@Composable
private fun ActionButtonsGroup(
    onButtonChange: (JoypadButton, Boolean) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TurboActionButton(
                label = "TB",
                color = Color(0xFFFF2A6D),
                button = JoypadButton.B,
                onButtonPulse = onButtonChange
            )
            TurboActionButton(
                label = "TA",
                color = Color(0xFF00E5FF),
                button = JoypadButton.A,
                onButtonPulse = onButtonChange
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .width(152.dp)
                .height(80.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularActionButton(
                label = "B",
                color = Color(0xFFFF2A6D),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = 4.dp, y = 4.dp),
                onPressChange = { onButtonChange(JoypadButton.B, it) }
            )

            CircularActionButton(
                label = "A",
                color = Color(0xFF00E5FF),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-4).dp, y = (-8).dp),
                onPressChange = { onButtonChange(JoypadButton.A, it) }
            )
        }
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
            .size(56.dp)
            .shadow(if (isPressed) 3.dp else 8.dp, CircleShape)
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
            fontSize = 20.sp
        )
    }
}

@Composable
private fun TurboActionButton(
    label: String,
    color: Color,
    button: JoypadButton,
    modifier: Modifier = Modifier,
    onButtonPulse: (JoypadButton, Boolean) -> Unit
) {
    var isHolding by remember { mutableStateOf(false) }

    LaunchedEffect(isHolding) {
        if (!isHolding) return@LaunchedEffect
        while (isActive) {
            onButtonPulse(button, true)
            delay(50) // 50ms ON
            onButtonPulse(button, false)
            delay(50) // 50ms OFF
        }
    }

    val displayTextColor = if (isHolding) {
        Color.Black
    } else if (color == Color(0xFFFF2A6D)) {
        Color(0xFFFF5C8A)
    } else {
        color
    }

    Box(
        modifier = modifier
            .size(48.dp)
            .shadow(if (isHolding) 2.dp else 6.dp, CircleShape)
            .clip(CircleShape)
            .background(
                if (isHolding) color.copy(alpha = 0.85f)
                else Color(0xFF1E2129)
            )
            .border(
                1.5.dp,
                if (isHolding) color else color.copy(alpha = 0.6f),
                CircleShape
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isHolding = true
                        tryAwaitRelease()
                        isHolding = false
                        onButtonPulse(button, false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = displayTextColor,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun PillButton(
    text: String,
    active: Boolean = false,
    onClick: (() -> Unit)? = null,
    onPressChange: ((Boolean) -> Unit)? = null
) {
    var isPressed by remember { mutableStateOf(false) }
    val isHighlighted = active || isPressed

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isHighlighted) Color(0xFF00E5FF) else Color(0xFF242731))
            .border(1.dp, if (isHighlighted) Color(0xFF00E5FF) else Color(0xFF3B404E), RoundedCornerShape(20.dp))
            .pointerInput(onClick, onPressChange) {
                if (onPressChange != null) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            onPressChange(true)
                            tryAwaitRelease()
                            isPressed = false
                            onPressChange(false)
                        }
                    )
                } else if (onClick != null) {
                    detectTapGestures(onTap = { onClick() })
                }
            }
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (isHighlighted) Color.Black else Color(0xFFD0D5E0),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.sp
        )
    }
}
