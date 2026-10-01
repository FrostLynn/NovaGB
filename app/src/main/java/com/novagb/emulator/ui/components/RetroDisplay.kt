package com.novagb.emulator.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.novagb.emulator.data.AspectRatioMode

/**
 * Modern retro viewport component with custom LCD grid & CRT scanline effects.
 */
@Composable
fun RetroDisplay(
    framebuffer: IntArray,
    aspectRatioMode: AspectRatioMode,
    showLcdGrid: Boolean,
    showScanlines: Boolean,
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

    Box(
        modifier = modifier
            .padding(12.dp)
            .shadow(16.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF14161B))
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .aspectRatio(screenAspect)
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black)
        ) {
            // Read frame state inside Canvas draw scope so Compose re-triggers
            // only the draw phase every frame, bypassing recomposition and layout passes.
            @Suppress("UNUSED_VARIABLE")
            val currentFrame = frameIndexProvider()

            // Update bitmap pixel data with latest framebuffer
            bitmap.setPixels(framebuffer, 0, 160, 0, 0, 160, 144)

            val canvasW = size.width
            val canvasH = size.height

            // Draw game screen
            drawImage(
                image = imageBitmap,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(160, 144),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(canvasW.toInt(), canvasH.toInt()),
                filterQuality = androidx.compose.ui.graphics.FilterQuality.None
            )

            // Optional LCD Matrix dot grid (cached to avoid redundant per-frame drawLine calls)
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

            // Optional CRT Scanlines (cached)
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
