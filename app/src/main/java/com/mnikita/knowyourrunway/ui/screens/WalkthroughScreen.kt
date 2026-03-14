package com.mnikita.knowyourrunway.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkthroughScreen(
    onDone: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var selected by remember { mutableStateOf<String?>(null) }

    val options = listOf(
        "MEN" to "Men",
        "WOMEN" to "Women",
        "KIDS" to "Kids",
        "JEWELRY" to "Jewelry",
        "ACCESSORIES" to "Accessories"
    )

    // ✅ premium entrance animation
    var animateIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(80)
        animateIn = true
    }

    Scaffold(
        containerColor = cs.background,
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Spacer(Modifier.height(6.dp))

            Text(
                text = "What do you want to shop?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = cs.onBackground
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Choose a category for this session.",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onBackground.copy(alpha = 0.70f)
            )

            Spacer(Modifier.height(16.dp))

            // ✅ Icon grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                itemsIndexed(options) { index, (value, label) ->
                    // stagger: later items appear slightly later
                    val show = remember(animateIn) { animateIn }
                    val delayMs = (index * 70L).coerceAtMost(350L)

                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(show) {
                        if (show) {
                            delay(delayMs)
                            visible = true
                        } else visible = false
                    }

                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn() + scaleIn(initialScale = 0.92f)
                    ) {
                        CategoryTile(
                            label = label,
                            emoji = categoryEmoji(value),
                            selected = selected == value,
                            onClick = { selected = value }
                        )
                    }
                }
            }

            Button(
                onClick = { onDone(selected!!) },
                enabled = selected != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
            ) {
                Text(if (selected == null) "Continue" else "Continue • ${selected!!.lowercase().replaceFirstChar { it.uppercase() }}",
                    color = cs.onPrimary
                )
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}

private fun categoryEmoji(value: String): String {
    return when (value.uppercase()) {
        "MEN" -> "🧥"
        "WOMEN" -> "👗"
        "KIDS" -> "🧸"
        "JEWELRY" -> "💎"
        "ACCESSORIES" -> "👜"
        else -> "✨"
    }
}

@Composable
private fun CategoryTile(
    label: String,
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(22.dp)

    val borderColor = if (selected) cs.primary else cs.outline.copy(alpha = 0.45f)
    val bg = if (selected) cs.primary.copy(alpha = 0.10f) else cs.surface

    // ✅ selection bounce
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tileScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.05f)
            .scale(scale)
            .border(1.dp, borderColor, shape)
            .clip(shape)
            .clickable { onClick() },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon circle
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) cs.primary.copy(alpha = 0.16f)
                        else cs.surfaceVariant.copy(alpha = 0.35f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, style = MaterialTheme.typography.headlineSmall)
            }

            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (selected) "Selected" else "Tap to choose",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) cs.primary else cs.onSurfaceVariant
                )
            }
        }
    }
}