package com.novagb.emulator.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novagb.emulator.data.AspectRatioMode

/**
 * Modern retro viewport component with custom LCD grid, CRT scanlines, and authentic console bezel with power LED.
 */
@Composable
fun RetroDisplay(
    framebuffer: IntArray,
    aspectRatioMode: AspectRatioMode,
    showLcdGrid: Boolean,
    showScanlines: Boolean,
    showRetroBezel: Boolean = true,
    classicDmgShell: Boolean = false,
    modifier: Modifier = Modifier,
    frameIndexProvider: () -> Long = { 0L }
) {
    val bitmap = remember {
        Bitmap.createBitmap(160, 144, Bitmap.Config.ARGB_8888)
    }
    val imageBitmap = remember(bitmap) {
        bitmap.asImageBitmap()
    }

    var cachedGridBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var lastGridWidth by remember { mutableStateOf(0) }
    var lastGridHeight by remember { mutableStateOf(0) }

    var cachedScanlineBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var lastScanlineWidth by remember { mutableStateOf(0) }
    var lastScanlineHeight by remember { mutableStateOf(0) }

    val screenAspect = when (aspectRatioMode) {
        AspectRatioMode.ORIGINAL -> 160f / 144f
        AspectRatioMode.INTEGER_SCALE -> 160f / 144f
        AspectRatioMode.FIT_SCREEN -> 160f / 144f
        AspectRatioMode.FULL_SCREEN -> 16f / 9f
    }

    val outerShape = RoundedCornerShape(16.dp)

    val bezelBgColor = if (classicDmgShell) Color(0xFF525660) else if (showRetroBezel) Color(0xFF262933) else Color(0xFF14161B)
    val bezelBorderColor = if (classicDmgShell) Color(0xFF3C3E46) else if (showRetroBezel) Color(0xFF383D4C) else Color(0xFF262A36)

    Box(
        modifier = modifier
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .shadow(if (classicDmgShell) 6.dp else 16.dp, outerShape)
            .clip(outerShape)
            .background(bezelBgColor)
            .border(
                if (showRetroBezel) 2.dp else 1.dp,
                bezelBorderColor,
                outerShape
            )
            .padding(if (showRetroBezel) 10.dp else 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (showRetroBezel) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.dp)
                                .background(Color(0xFF882040))
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.dp)
                                .background(Color(0xFF202060))
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "DOT MATRIX WITH STEREO SOUND",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFF8E95A5),
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.dp)
                                .background(Color(0xFF882040))
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.dp)
                                .background(Color(0xFF202060))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (showRetroBezel) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .shadow(8.dp, CircleShape)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(Color(0xFFFF3333), Color(0xFF990000))
                                    )
                                )
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "BATTERY",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFA0A6B8)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black)
                        .border(1.5.dp, Color(0xFF161820), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .aspectRatio(screenAspect)
                            .fillMaxSize()
                    ) {
                        @Suppress("UNUSED_VARIABLE")
                        val currentFrame = frameIndexProvider()

                        bitmap.setPixels(framebuffer, 0, 160, 0, 0, 160, 144)

                        val canvasW = size.width
                        val canvasH = size.height

                        drawImage(
                            image = imageBitmap,
                            srcOffset = IntOffset.Zero,
                            srcSize = IntSize(160, 144),
                            dstOffset = IntOffset.Zero,
                            dstSize = IntSize(canvasW.toInt(), canvasH.toInt()),
                            filterQuality = androidx.compose.ui.graphics.FilterQuality.None
                        )

                        if (showLcdGrid) {
                            val w = canvasW.toInt()
                            val h = canvasH.toInt()
                            if (w > 0 && h > 0) {
                                if (cachedGridBitmap == null || lastGridWidth != w || lastGridHeight != h) {
                                    val gBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                    val gCanvas = android.graphics.Canvas(gBmp)
                                    val paint = android.graphics.Paint().apply {
                                        color = 0x22000000.toInt()
                                        strokeWidth = 1f
                                    }
                                    val pixelW = w.toFloat() / 160f
                                    val pixelH = h.toFloat() / 144f
                                    for (x in 0..160) {
                                        val px = x * pixelW
                                        gCanvas.drawLine(px, 0f, px, h.toFloat(), paint)
                                    }
                                    for (y in 0..144) {
                                        val py = y * pixelH
                                        gCanvas.drawLine(0f, py, w.toFloat(), py, paint)
                                    }
                                    cachedGridBitmap = gBmp.asImageBitmap()
                                    lastGridWidth = w
                                    lastGridHeight = h
                                }
                                cachedGridBitmap?.let { drawImage(it) }
                            }
                        }

                        if (showScanlines) {
                            val w = canvasW.toInt()
                            val h = canvasH.toInt()
                            if (w > 0 && h > 0) {
                                if (cachedScanlineBitmap == null || lastScanlineWidth != w || lastScanlineHeight != h) {
                                    val sBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                    val sCanvas = android.graphics.Canvas(sBmp)
                                    val paint = android.graphics.Paint().apply {
                                        color = 0x33000000.toInt()
                                        strokeWidth = 1f
                                    }
                                    val step = maxOf(2f, h.toFloat() / 144f)
                                    var y = 0f
                                    while (y < h) {
                                        sCanvas.drawLine(0f, y, w.toFloat(), y, paint)
                                        y += step * 2f
                                    }
                                    cachedScanlineBitmap = sBmp.asImageBitmap()
                                    lastScanlineWidth = w
                                    lastScanlineHeight = h
                                }
                                cachedScanlineBitmap?.let { drawImage(it) }
                            }
                        }
                    }
                }
            }

            if (showRetroBezel) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GAME BOY",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontStyle = FontStyle.Italic,
                        color = if (classicDmgShell) Color(0xFF102055) else Color(0xFFD0D5E0),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "™",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        color = if (classicDmgShell) Color(0xFF102055) else Color(0xFFD0D5E0)
                    )
                }
            }
        }
    }
}
