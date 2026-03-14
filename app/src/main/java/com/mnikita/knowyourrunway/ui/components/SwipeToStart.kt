package com.mnikita.knowyourrunway.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SwipeToStart(
    text: String = "Swipe to get started",
    onDone: () -> Unit,
    modifier: Modifier = Modifier,

    // ✅ NEW: live progress 0f..1f while swiping (safe default)
    onProgress: (Float) -> Unit = {},

    // styling controls (safe defaults)
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    thumbColor: Color = MaterialTheme.colorScheme.primary,
    thumbIconTint: Color = MaterialTheme.colorScheme.onPrimary
) {
    val scope = rememberCoroutineScope()

    val trackHeight = 62.dp
    val padding = 6.dp
    val thumbSize = 50.dp

    val density = LocalDensity.current
    val paddingPx = with(density) { padding.toPx() }
    val thumbPx = with(density) { thumbSize.toPx() }

    var trackWidthPx by remember { mutableStateOf(0f) }
    val maxOffsetPx by derivedStateOf { (trackWidthPx - thumbPx - paddingPx * 2).coerceAtLeast(0f) }

    val offsetX = remember { Animatable(0f) }
    var completed by remember { mutableStateOf(false) }
    var doneCalled by remember { mutableStateOf(false) }

    // ✅ report progress continuously (includes spring animations back to 0)
    LaunchedEffect(maxOffsetPx) {
        snapshotFlow { offsetX.value }
            .collect { x ->
                val p = if (maxOffsetPx <= 0f) 0f else (x / maxOffsetPx).coerceIn(0f, 1f)
                onProgress(p)
            }
    }

    Surface(
        modifier = modifier
            .height(trackHeight)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        color = trackColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { trackWidthPx = it.width.toFloat() }
                .padding(horizontal = padding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = labelColor,
                style = MaterialTheme.typography.titleMedium
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .size(thumbSize)
                    .clip(RoundedCornerShape(16.dp))
                    .pointerInput(completed, maxOffsetPx) {
                        if (completed) return@pointerInput
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { _, dragAmount ->
                                scope.launch {
                                    offsetX.snapTo((offsetX.value + dragAmount).coerceIn(0f, maxOffsetPx))
                                }
                            },
                            onDragEnd = {
                                scope.launch {
                                    val shouldComplete =
                                        maxOffsetPx > 0f && offsetX.value >= maxOffsetPx * 0.92f
                                    if (shouldComplete) {
                                        completed = true
                                        offsetX.animateTo(
                                            maxOffsetPx,
                                            spring(stiffness = Spring.StiffnessMediumLow)
                                        )
                                        if (!doneCalled) {
                                            doneCalled = true
                                            onDone()
                                        }
                                    } else {
                                        offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessLow))
                                    }
                                }
                            }
                        )
                    },
                color = thumbColor,
                tonalElevation = 6.dp
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = thumbIconTint)
                }
            }
        }
    }
}