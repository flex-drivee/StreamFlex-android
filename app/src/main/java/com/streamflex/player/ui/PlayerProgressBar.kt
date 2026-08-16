package com.streamflex.player.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun PlayerProgressBar(
    positionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragPosition by remember { mutableStateOf<Long?>(null) }
    
    val currentFraction = if (durationMs > 0) {
        (dragPosition ?: positionMs).toFloat() / durationMs
    } else 0f
    
    val bufferedFraction = if (durationMs > 0) {
        bufferedPositionMs.toFloat() / durationMs
    } else 0f

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .pointerInput(durationMs) {
                detectTapGestures { offset ->
                    if (durationMs > 0) {
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeekTo((fraction * durationMs).toLong())
                    }
                }
            }
            .pointerInput(durationMs) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (durationMs > 0) {
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            dragPosition = (fraction * durationMs).toLong()
                        }
                    },
                    onDrag = { change, _ ->
                        if (durationMs > 0) {
                            val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            dragPosition = (fraction * durationMs).toLong()
                        }
                    },
                    onDragEnd = {
                        dragPosition?.let { onSeekTo(it) }
                        dragPosition = null
                    },
                    onDragCancel = {
                        dragPosition = null
                    }
                )
            }
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val strokeWidth = 4.dp.toPx()

        // Background Track
        drawLine(
            color = Color.DarkGray.copy(alpha = 0.5f),
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Buffered Track
        if (bufferedFraction > 0f) {
            drawLine(
                color = Color.LightGray.copy(alpha = 0.5f),
                start = Offset(0f, centerY),
                end = Offset(width * bufferedFraction, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        // Active Track
        if (currentFraction > 0f) {
            drawLine(
                color = Color.Red,
                start = Offset(0f, centerY),
                end = Offset(width * currentFraction, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        // Thumb
        drawCircle(
            color = Color.Red,
            radius = 8.dp.toPx(),
            center = Offset(width * currentFraction, centerY)
        )
    }
}
