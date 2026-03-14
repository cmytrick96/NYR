package com.mnikita.knowyourrunway.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun GetStartedScreen(
    ownersImageRes: Int,
    loginContent: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(WindowInsets.systemBars.asPaddingValues())
    ) {
        // Background photo (zoom a bit so faces are clearer)
        Image(
            painter = painterResource(id = ownersImageRes),
            contentDescription = "Owners",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.12f
                    scaleY = 1.12f
                }
        )

        // Dark gradient overlay for readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.45f),
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
        )

        // Top text / branding (simple + premium)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 18.dp, start = 18.dp, end = 18.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "KnowYourRunway",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Swipe up to login",
                color = Color.White.copy(alpha = 0.80f),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // --- Swipe-up live login sheet (custom, stable, NOT experimental) ---
        val sheetHeight = maxHeight * 0.92f
        val peekHeight = 86.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        val density = androidx.compose.ui.platform.LocalDensity.current
        val sheetHeightPx = with(density) { sheetHeight.toPx() }
        val peekHeightPx = with(density) { peekHeight.toPx() }
        val collapsedOffset = (sheetHeightPx - peekHeightPx).coerceAtLeast(0f)

        val offsetY = remember { androidx.compose.animation.core.Animatable(collapsedOffset) }

        fun settle(velocity: Float) {
            scope.launch {
                val shouldExpand = velocity < -900f || offsetY.value < collapsedOffset * 0.55f
                offsetY.animateTo(
                    targetValue = if (shouldExpand) 0f else collapsedOffset,
                    animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                )
            }
        }

        val dragState = rememberDraggableState { delta ->
            // delta: +down, -up
            val newValue = (offsetY.value + delta).coerceIn(0f, collapsedOffset)
            scope.launch { offsetY.snapTo(newValue) }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .height(sheetHeight)
                .fillMaxWidth()
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(Modifier.fillMaxSize()) {
                    // Handle area (drag here)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 10.dp)
                            .draggable(
                                orientation = Orientation.Vertical,
                                state = dragState,
                                onDragStopped = { velocity -> settle(velocity) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .width(46.dp)
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color(0xFFB8B8B8))
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Swipe up to Login",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Login content (reveals live as you drag)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        loginContent()
                    }
                }
            }
        }
    }
}