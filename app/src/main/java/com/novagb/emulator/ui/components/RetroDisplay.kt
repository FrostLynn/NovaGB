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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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

            // Optional LCD Matrix dot grid
            if (showLcdGrid) {
                drawLcdGrid(canvasW, canvasH)
            }

            // Optional CRT Scanlines
            if (showScanlines) {
                drawScanlines(canvasW, canvasH)
            }
        }
    }
}

private fun DrawScope.drawLcdGrid(width: Float, height: Float) {
    val pixelW = width / 160f
    val pixelH = height / 144f
    val gridColor = Color(0x22000000)

    for (x in 0..160) {
        val posX = x * pixelW
        drawLine(
            color = gridColor,
            start = Offset(posX, 0f),
            end = Offset(posX, height),
            strokeWidth = 1f
        )
    }
    for (y in 0..144) {
        val posY = y * pixelH
        drawLine(
            color = gridColor,
            start = Offset(0f, posY),
            end = Offset(width, posY),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawScanlines(width: Float, height: Float) {
    val scanlineColor = Color(0x33000000)
    var y = 0f
    val step = maxOf(2f, height / 144f)
    while (y < height) {
        drawLine(
            color = scanlineColor,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1f
        )
        y += step * 2f
    }
}
