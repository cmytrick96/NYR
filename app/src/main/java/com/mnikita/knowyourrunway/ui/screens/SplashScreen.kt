package com.mnikita.knowyourrunway.ui.screens

import android.widget.ImageView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val appIconResId = remember { context.applicationInfo.icon }

    val rotation = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }

    // Slightly bigger overall during transition
    val scale = remember { Animatable(0.92f) }

    val textAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(18f) }

    LaunchedEffect(Unit) {
        rotation.snapTo(0f)
        alpha.snapTo(0f)
        scale.snapTo(0.92f)
        textAlpha.snapTo(0f)
        textOffsetY.snapTo(18f)

        coroutineScope {
            launch {
                alpha.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
            }
            launch {
                // Slightly larger final scale than before
                scale.animateTo(1.06f, tween(650, easing = FastOutSlowInEasing))
            }
            launch {
                rotation.animateTo(360f, tween(3000, easing = LinearOutSlowInEasing))
            }
            launch {
                delay(1600)
                launch { textAlpha.animateTo(1f, tween(650, easing = FastOutSlowInEasing)) }
                launch { textOffsetY.animateTo(0f, tween(650, easing = FastOutSlowInEasing)) }
            }
        }

        delay(150)
        onDone()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AndroidView(
                factory = { ctx ->
                    ImageView(ctx).apply {
                        setImageResource(appIconResId)
                        scaleType = ImageView.ScaleType.FIT_CENTER
                    }
                },
                modifier = Modifier
                    // Slightly bigger base size than before (was 220.dp)
                    .size(240.dp)
                    .graphicsLayer {
                        rotationZ = rotation.value
                        this.alpha = alpha.value
                        scaleX = scale.value
                        scaleY = scale.value
                    }
            )
        }

        Text(
            text = "•SWIPE•STYLE•SERVE•",
            color = Color(0xFF2B2B2B),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
                .graphicsLayer {
                    this.alpha = textAlpha.value
                    translationY = textOffsetY.value
                }
        )
    }
}