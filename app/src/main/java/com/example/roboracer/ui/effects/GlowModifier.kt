package com.example.roboracer.ui.effects

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.cyberpunkBorder(
    borderColor: Color,
    glowColor: Color = borderColor.copy(alpha = 0.45f),
    backgroundColor: Color = Color(0xCC111622),
    borderWidth: Dp = 1.5.dp,
    glowRadius: Dp = 8.dp,
    cornerCut: Dp = 12.dp
): Modifier = this.drawBehind {
    val strokePx = borderWidth.toPx()
    val glowPx = glowRadius.toPx()
    val cutPx = cornerCut.toPx()
    val w = size.width
    val h = size.height

    if (w <= 0 || h <= 0) return@drawBehind

    // Chamfered corner polygon path
    val path = Path().apply {
        moveTo(cutPx, 0f)
        lineTo(w - cutPx, 0f)
        lineTo(w, cutPx)
        lineTo(w, h - cutPx)
        lineTo(w - cutPx, h)
        lineTo(cutPx, h)
        lineTo(0f, h - cutPx)
        lineTo(0f, cutPx)
        close()
    }

    // 1. Draw outer soft glow with native blur paint
    if (glowPx > 0f) {
        drawIntoCanvas { canvas ->
            val frameworkPaint = Paint().apply {
                color = glowColor.toArgb()
                style = Paint.Style.STROKE
                strokeWidth = strokePx + glowPx * 0.7f
                isAntiAlias = true
                maskFilter = BlurMaskFilter(glowPx, BlurMaskFilter.Blur.NORMAL)
            }
            val androidPath = android.graphics.Path().apply {
                moveTo(cutPx, 0f)
                lineTo(w - cutPx, 0f)
                lineTo(w, cutPx)
                lineTo(w, h - cutPx)
                lineTo(w - cutPx, h)
                lineTo(cutPx, h)
                lineTo(0f, h - cutPx)
                lineTo(0f, cutPx)
                close()
            }
            canvas.nativeCanvas.drawPath(androidPath, frameworkPaint)
        }
    }

    // 2. Fill background
    drawPath(path = path, color = backgroundColor)

    // 3. Draw sharp border line
    drawPath(
        path = path,
        color = borderColor,
        style = Stroke(width = strokePx)
    )

    // 4. Draw HUD corner bracket accents
    val cornerLen = cutPx * 1.6f
    // Top-Left corner accent
    drawLine(
        color = borderColor,
        start = androidx.compose.ui.geometry.Offset(0f, cutPx + cornerLen),
        end = androidx.compose.ui.geometry.Offset(0f, cutPx),
        strokeWidth = strokePx * 2f
    )
    drawLine(
        color = borderColor,
        start = androidx.compose.ui.geometry.Offset(0f, cutPx),
        end = androidx.compose.ui.geometry.Offset(cutPx, 0f),
        strokeWidth = strokePx * 2.5f
    )
    drawLine(
        color = borderColor,
        start = androidx.compose.ui.geometry.Offset(cutPx, 0f),
        end = androidx.compose.ui.geometry.Offset(cutPx + cornerLen, 0f),
        strokeWidth = strokePx * 2f
    )

    // Bottom-Right corner accent
    drawLine(
        color = borderColor,
        start = androidx.compose.ui.geometry.Offset(w, h - cutPx - cornerLen),
        end = androidx.compose.ui.geometry.Offset(w, h - cutPx),
        strokeWidth = strokePx * 2f
    )
    drawLine(
        color = borderColor,
        start = androidx.compose.ui.geometry.Offset(w, h - cutPx),
        end = androidx.compose.ui.geometry.Offset(w - cutPx, h),
        strokeWidth = strokePx * 2.5f
    )
    drawLine(
        color = borderColor,
        start = androidx.compose.ui.geometry.Offset(w - cutPx, h),
        end = androidx.compose.ui.geometry.Offset(w - cutPx - cornerLen, h),
        strokeWidth = strokePx * 2f
    )
}
