package com.mnikita.knowyourrunway.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

data class ProductCardUi(
    val id: Long,
    val title: String,
    val brand: String,
    val priceInr: Int,
    val coverUrl: String?,
    val gender: String = "UNISEX",
    val mrpInr: Int? = null,
    val rating: Double? = null,
    val ratingTotal: Int? = null,
    val discountPct: Int? = null,
    val sellerName: String? = null,
    val productUrl: String? = null,
    val asin: String? = null
)

enum class SwipeDir { LIKE, DISLIKE }

@Composable
fun SwipeDeck(
    items: List<ProductCardUi>,
    onSwipe: (productId: Long, dir: SwipeDir) -> Unit,
    onCardClick: (product: ProductCardUi) -> Unit,
    modifier: Modifier = Modifier
) {
    val top = items.take(3)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        for (i in top.indices.reversed()) {
            val item = top[i]
            val depth = i

            val scale = when (depth) {
                0 -> 1f
                1 -> 0.96f
                else -> 0.92f
            }
            val yOffset = when (depth) {
                0 -> 0.dp
                1 -> 10.dp
                else -> 18.dp
            }

            if (depth == 0) {
                SwipeableTopCard(
                    item = item,
                    onSwipe = onSwipe,
                    onClick = onCardClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.72f)
                )
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.72f)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .offset(y = yOffset)
                        .clickable { onCardClick(item) },
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    ProductCardContent(item, onCardClick)
                }
            }
        }
    }
}

@Composable
private fun SwipeableTopCard(
    item: ProductCardUi,
    onSwipe: (Long, SwipeDir) -> Unit,
    onClick: (ProductCardUi) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    // ✅ Reset animation state whenever top item changes (prevents flash)
    val animX = remember(item.id) { Animatable(0f) }
    val animY = remember(item.id) { Animatable(0f) }

    val thresholdPx = 260f

    val likeAlpha by derivedStateOf { (animX.value / thresholdPx).coerceIn(0f, 1f) }
    val dislikeAlpha by derivedStateOf { (-animX.value / thresholdPx).coerceIn(0f, 1f) }

    Card(
        modifier = modifier
            .offset { IntOffset(animX.value.roundToInt(), animY.value.roundToInt()) }
            .graphicsLayer { rotationZ = (animX.value / 30f).coerceIn(-18f, 18f) }
            .pointerInput(item.id) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            animX.snapTo(animX.value + dragAmount.x)
                            animY.snapTo(animY.value + dragAmount.y)
                        }
                    },
                    onDragEnd = {
                        val x = animX.value
                        if (abs(x) > thresholdPx) {
                            val dir = if (x > 0) SwipeDir.LIKE else SwipeDir.DISLIKE
                            val targetX = if (x > 0) 1400f else -1400f

                            scope.launch {
                                animX.animateTo(targetX, spring(stiffness = Spring.StiffnessMediumLow))
                                // ✅ update list AFTER animation; don’t snap back (no flash)
                                onSwipe(item.id, dir)
                            }
                        } else {
                            scope.launch {
                                animX.animateTo(0f, spring(stiffness = Spring.StiffnessLow))
                                animY.animateTo(0f, spring(stiffness = Spring.StiffnessLow))
                            }
                        }
                    }
                )
            },
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(Modifier.fillMaxSize()) {
            ProductCardContent(item, onClick)

            Text(
                text = "LIKE",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(18.dp)
                    .graphicsLayer { alpha = likeAlpha }
            )
            Text(
                text = "NOPE",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(18.dp)
                    .graphicsLayer { alpha = dislikeAlpha }
            )
        }
    }
}

@Composable
private fun ProductCardContent(item: ProductCardUi, onClick: (ProductCardUi) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        AsyncImage(
            model = item.coverUrl,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Surface(
            tonalElevation = 3.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(14.dp)
                .clip(RoundedCornerShape(22.dp))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Text(item.brand, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                Spacer(Modifier.height(8.dp))
                Text("₹${item.priceInr}", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Button(onClick = { onClick(item) }, modifier = Modifier.fillMaxWidth()) {
                    Text("View details")
                }
            }
        }
    }
}