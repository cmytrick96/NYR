package com.mnikita.knowyourrunway.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mnikita.knowyourrunway.R
import com.mnikita.knowyourrunway.data.TokenStore
import com.mnikita.knowyourrunway.network.ApiService
import com.mnikita.knowyourrunway.ui.components.SwipeToStart

@Composable
fun OnboardingLoginScreen(
    api: ApiService,
    tokenStore: TokenStore,
    onSignUp: () -> Unit,
    onForgot: () -> Unit,
    onSuccess: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    var swipeProgress by remember { mutableStateOf(0f) } // 0..1

    val bg = Brush.verticalGradient(
        listOf(Color(0xFF1B1714), Color(0xFF0F0D0B))
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val maxWidthPx = with(density) { maxWidth.toPx() }

        // =========================
        // 1) LOGIN UNDERLAY (full screen)
        // =========================
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.systemBars.asPaddingValues())
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
            ) {
                SignInScreen(
                    api = api,
                    tokenStore = tokenStore,
                    onSignUp = onSignUp,
                    onForgot = onForgot,
                    onSuccess = onSuccess,
                    embedded = true
                )
            }
        }

        // =========================
        // 2) ONBOARDING TOP LAYER (clipped from right -> reveals login)
        // + card-like tilt/parallax
        // =========================
        val p = swipeProgress.coerceIn(0f, 1f)
        val remainingWidth = maxWidth * (1f - p)

        // Card swipe feel
        val parallaxX = -maxWidthPx * 0.06f * p        // subtle slide left
        val liftY = -10f * p                           // slight lift
        val tilt = -2.6f * p                           // subtle tilt
        val scale = 1f - 0.010f * p                    // tiny scale down
        val shadowAlpha = (0.10f + 0.20f * p).coerceIn(0f, 0.35f)

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(remainingWidth.coerceAtLeast(0.dp))
                .clipToBounds()
                .graphicsLayer {
                    translationX = parallaxX
                    translationY = liftY
                    rotationZ = tilt
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0.5f) // pivot from left edge
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bg)
                    .padding(WindowInsets.systemBars.asPaddingValues())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = accent.copy(alpha = 0.95f)
                        ) {
                            Text(
                                text = "NYR",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        Text(
                            text = "SWIPE STYLES",
                            color = Color.White.copy(alpha = 0.75f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            letterSpacing = 1.5.sp
                        )

                        Spacer(Modifier.weight(1f))

                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFD36E))
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {

                            BoxWithConstraints(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                val artHeight = (maxHeight * 0.60f).coerceIn(300.dp, 420.dp)
                                val centerSize = (maxWidth * 0.52f).coerceIn(160.dp, 210.dp)
                                val sideSize = (centerSize * 0.70f).coerceIn(110.dp, 145.dp)

                                val leftX = -(maxWidth * 0.28f)
                                val leftY = (artHeight * 0.18f)

                                val rightX = (maxWidth * 0.30f)
                                val rightY = -(artHeight * 0.18f)

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(artHeight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val stroke = size.minDimension * 0.10f
                                        val ovalW = size.width * 0.88f
                                        val ovalH = size.height * 0.62f
                                        val topLeft = Offset(
                                            (size.width - ovalW) / 2f,
                                            (size.height - ovalH) / 2.35f
                                        )

                                        drawOval(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    accent.copy(alpha = 0.95f),
                                                    accent.copy(alpha = 0.55f)
                                                ),
                                                start = Offset(topLeft.x, topLeft.y),
                                                end = Offset(topLeft.x + ovalW, topLeft.y + ovalH)
                                            ),
                                            topLeft = topLeft,
                                            size = Size(ovalW, ovalH),
                                            style = Stroke(width = stroke)
                                        )
                                    }

                                    TagChip(
                                        text = "#Street",
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(start = 10.dp, top = 10.dp)
                                    )
                                    TagChip(
                                        text = "#Runway",
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(end = 10.dp, bottom = 10.dp)
                                    )

                                    FounderCircle(
                                        resId = R.drawable.founder2,
                                        size = centerSize,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                    FounderCircle(
                                        resId = R.drawable.founder1,
                                        size = sideSize,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .offset(x = leftX, y = leftY)
                                    )
                                    FounderCircle(
                                        resId = R.drawable.founder3,
                                        size = sideSize,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .offset(x = rightX, y = rightY)
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .offset(y = centerSize * 0.42f)
                                    ) {
                                        Text(
                                            text = "Say more than \"Hey\"",
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            color = Color(0xFF1B1714),
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            Text(
                                text = "Get Ready To Make Your Personal\nWishlist",
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                lineHeight = 30.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Curate the looks you love from our founders — saved in one place.",
                                color = Color.White.copy(alpha = 0.70f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(
                        Modifier
                            .height(62.dp)
                            .padding(WindowInsets.navigationBars.asPaddingValues())
                    )
                }
            }
        }

        // Soft “wipe edge” shadow
        if (p in 0.02f..0.98f) {
            val edgeWidth = 28.dp
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(edgeWidth)
                    .align(Alignment.CenterStart)
                    .offset(x = remainingWidth - (edgeWidth / 2))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = shadowAlpha),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // =========================
        // 3) SWIPE CONTROL OVERLAY
        // =========================
        if (p < 0.995f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .padding(WindowInsets.navigationBars.asPaddingValues())
            ) {
                SwipeToStart(
                    text = "Swipe to continue",
                    onDone = { /* login is already revealed */ },
                    onProgress = { swipeProgress = it },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = Color.White.copy(alpha = 0.07f),
                    borderColor = Color.White.copy(alpha = 0.14f),
                    labelColor = Color.White.copy(alpha = 0.85f),
                    thumbColor = accent,
                    thumbIconTint = Color.White
                )
            }
        }
    }
}

@Composable
private fun FounderCircle(
    resId: Int,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    Image(
        painter = painterResource(id = resId),
        contentDescription = "Founder",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(4.dp, Color(0xFF0F0D0B), CircleShape)
            .border(2.dp, accent.copy(alpha = 0.50f), CircleShape)
    )
}

@Composable
private fun TagChip(text: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xFFFFD36E),
        modifier = modifier
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = Color(0xFF1B1714),
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
    }
}