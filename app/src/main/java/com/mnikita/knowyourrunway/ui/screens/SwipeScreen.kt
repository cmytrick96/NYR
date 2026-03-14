package com.mnikita.knowyourrunway.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mnikita.knowyourrunway.data.*
import com.mnikita.knowyourrunway.network.ApiService
import com.mnikita.knowyourrunway.ui.components.ProductCardUi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeScreen(
    api: ApiService,
    tokenStore: TokenStore,
    onOpenWishlist: () -> Unit,
    onOpenCart: () -> Unit,
    onOpenProduct: () -> Unit,
    onOpenProfile: () -> Unit,
    onChooseCategory: () -> Unit,
    onLogoutDone: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme

    val category = ShopSession.category

    val profile by tokenStore.profileFlow.collectAsState(initial = UserProfile())
    val displayName = profile.name.trim().ifBlank { "Welcome" }
    val firstName = displayName.split(" ").firstOrNull().orEmpty().ifBlank { displayName }
    val titleText = if (profile.name.isBlank()) "Welcome" else "Hi, $firstName"

    var loading by remember { mutableStateOf(FeedCache.cards.isEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    var isFetching by remember { mutableStateOf(false) }

    val wishCount = WishlistStore.items.size
    val badgeText = when {
        wishCount <= 0 -> ""
        wishCount >= 100 -> "99+"
        else -> wishCount.toString()
    }

    fun bearer(token: String) = "Bearer $token"

    fun matchesCategory(item: ProductCardUi, cat: String): Boolean {
        val c = cat.trim().uppercase()
        val g = item.gender.uppercase()
        val t = item.title.uppercase()

        fun titleHasAny(vararg words: String) = words.any { t.contains(it) }

        return when (c) {
            "MEN" -> (g != "UNISEX" && g.contains("MEN")) || titleHasAny(" MEN", "MENS", "MALE")
            "WOMEN" -> (g != "UNISEX" && g.contains("WOMEN")) || titleHasAny(" WOMEN", "WOMENS", "FEMALE", "LADIES")
            "KIDS" -> (g != "UNISEX" && g.contains("KIDS")) || titleHasAny("KID", "KIDS", "BOY", "GIRL", "CHILD")
            "JEWELRY" -> titleHasAny("RING", "EARRING", "EARRINGS", "NECKLACE", "BRACELET", "PENDANT", "JEWEL")
            "ACCESSORIES" -> titleHasAny("BAG", "BELT", "CAP", "HAT", "SUNGLASS", "SUNGLASSES", "WALLET", "SCARF", "TIE", "WATCH")
            else -> true
        }
    }

    suspend fun loadFeedIfNeeded(force: Boolean = false) {
        if (isFetching) return
        if (!force && FeedCache.cards.isNotEmpty()) return

        isFetching = true
        loading = true
        error = null
        try {
            val token = tokenStore.tokenFlow.first().orEmpty()
            if (token.isBlank()) {
                error = "No token. Please sign in again."
                return
            }

            // Keep a snapshot of what the user already saw, so refresh avoids repeating
            val prevIds = FeedCache.cards.map { it.id }.toHashSet()

            val res = api.feed(bearer(token), limit = 20)

            val mapped = res.items.map {
                ProductCardUi(
                    id = it.id,
                    title = it.title,
                    brand = it.brand.orEmpty(),
                    priceInr = it.priceInr ?: 0,
                    coverUrl = it.coverUrl,
                    gender = it.gender ?: "UNISEX",
                    mrpInr = it.mrpInr,
                    rating = it.rating,
                    ratingTotal = it.ratingTotal,
                    discountPct = it.discountPct,
                    sellerName = it.sellerName,
                    productUrl = it.productUrl,
                    asin = it.asin
                )
            }

            // ✅ 1) Deduplicate within this response
            val deduped = mapped.distinctBy { it.id }

            // ✅ 2) Prefer items that are NOT already on-screen
            val preferNew = deduped.filterNot { prevIds.contains(it.id) }
            val basePool = if (preferNew.size >= 8) preferNew else deduped

            // Apply category heuristic
            val catPool = basePool.filter { matchesCategory(it, category) }
            val finalPool = (if (catPool.isNotEmpty()) catPool else basePool).shuffled()

            FeedCache.setAll(finalPool)
        } catch (e: Exception) {
            error = e.message ?: "Network error"
        } finally {
            loading = false
            isFetching = false
        }
    }

    LaunchedEffect(category) {
        if (category.isBlank()) return@LaunchedEffect
        FeedCache.clear()
        loadFeedIfNeeded(force = true)
    }

    // Home feed list (no home search / no personalize box now)
    val visibleProducts = FeedCache.cards

    Scaffold(
        containerColor = cs.background,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(titleText, color = cs.onBackground, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Discover your next look",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onBackground.copy(alpha = 0.6f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cs.background,
                    titleContentColor = cs.onBackground,
                    actionIconContentColor = cs.primary
                ),
                actions = {
                    IconButton(onClick = onOpenWishlist) {
                        BadgedBox(badge = { if (wishCount > 0) Badge { Text(badgeText) } }) {
                            Icon(Icons.Filled.Favorite, contentDescription = "Wishlist", tint = cs.primary)
                        }
                    }

                    TextButton(
                        onClick = onChooseCategory,
                        colors = ButtonDefaults.textButtonColors(contentColor = cs.primary)
                    ) {
                        val label = category.lowercase(Locale.getDefault()).replaceFirstChar { it.uppercase() }
                        Text("Shop: $label")
                    }

                    IconButton(
                        onClick = {
                            scope.launch {
                                ShopSession.category = ""
                                tokenStore.clearToken()
                                runCatching { tokenStore.clearProfile() }
                                FeedCache.clear()
                                onLogoutDone()
                            }
                        }
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = cs.primary)
                    }
                }
            )
        }
    ) { padding ->
        when {
            category.isBlank() -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Please choose what you want to shop.", color = cs.onBackground)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = onChooseCategory) { Text("Choose category") }
                }
            }

            loading -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(strokeWidth = 3.dp, color = cs.primary)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Loading your personalized fashion store…",
                            style = MaterialTheme.typography.titleMedium,
                            color = cs.onBackground.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            error != null -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    Text(error!!, color = cs.error)
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { scope.launch { loadFeedIfNeeded(force = true) } },
                        colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
                    ) { Text("Retry", color = cs.onPrimary) }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    item {
                        // ✅ Home Search Bar removed
                        // ✅ Home Personalize box removed

                        CategoryRow(
                            selected = category,
                            onQuickPick = { pick -> ShopSession.category = pick }
                        )

                        Spacer(Modifier.height(14.dp))

                        PromoBanner()

                        Spacer(Modifier.height(18.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Popular",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = cs.onBackground
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "${visibleProducts.size} items",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                    }

                    val rows = visibleProducts.chunked(2)
                    items(rows) { row ->
                        Row(Modifier.fillMaxWidth()) {
                            ProductTile(
                                product = row[0],
                                onClick = {
                                    SelectedProduct.current = row[0]
                                    onOpenProduct()
                                },
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(Modifier.width(12.dp))

                            if (row.size > 1) {
                                ProductTile(
                                    product = row[1],
                                    onClick = {
                                        SelectedProduct.current = row[1]
                                        onOpenProduct()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    item {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { if (!isFetching) scope.launch { loadFeedIfNeeded(force = true) } },
                            enabled = !isFetching,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(if (isFetching) "Refreshing…" else "Refresh products")
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
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
private fun CategoryRow(
    selected: String,
    onQuickPick: (String) -> Unit
) {
    val items = listOf(
        "MEN" to "Men",
        "WOMEN" to "Women",
        "KIDS" to "Kids",
        "JEWELRY" to "Jewelry",
        "ACCESSORIES" to "Accessories"
    )
    val scroll = rememberScrollState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { (value, label) ->
            CategoryPill(
                label = label,
                emoji = categoryEmoji(value),
                selected = selected.equals(value, ignoreCase = true),
                onClick = { onQuickPick(value) }
            )
        }
    }
}

@Composable
private fun CategoryPill(
    label: String,
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)

    val bg by animateColorAsState(
        targetValue = if (selected) cs.primary.copy(alpha = 0.10f) else cs.surface,
        label = "pillBg"
    )
    val border by animateColorAsState(
        targetValue = if (selected) cs.primary else cs.outline.copy(alpha = 0.45f),
        label = "pillBorder"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "pillScale"
    )

    Surface(
        modifier = Modifier
            .scale(scale)
            .border(1.dp, border, shape)
            .clip(shape)
            .clickable { onClick() },
        shape = shape,
        color = bg,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) cs.primary.copy(alpha = 0.16f)
                        else cs.surfaceVariant.copy(alpha = 0.35f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(label, fontWeight = FontWeight.SemiBold, color = cs.onSurface)
                Text(
                    if (selected) "Selected" else "Tap",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) cs.primary else cs.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PromoBanner() {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        cs.primary.copy(alpha = 0.14f),
                        cs.primary.copy(alpha = 0.06f)
                    )
                )
            )
            .border(1.dp, cs.outline.copy(alpha = 0.25f), shape)
            .padding(16.dp)
    ) {
        Column(Modifier.align(Alignment.CenterStart)) {
            Text(
                "Sale up to",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onBackground.copy(alpha = 0.75f)
            )
            Text(
                "35% OFF",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = cs.onBackground
            )
            Spacer(Modifier.height(10.dp))
            Button(onClick = { }, shape = RoundedCornerShape(14.dp)) { Text("Shop now") }
        }
    }
}

@Composable
private fun ProductTile(
    product: ProductCardUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)

    Card(
        modifier = modifier.height(250.dp).clickable { onClick() },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(shape)
            ) {
                val url = product.coverUrl?.trim().orEmpty()
                if (url.isBlank() || url == "-") {
                    Box(Modifier.fillMaxSize().background(cs.surfaceVariant.copy(alpha = 0.35f)))
                } else {
                    AsyncImage(model = url, contentDescription = product.title, modifier = Modifier.fillMaxSize())
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(cs.surface.copy(alpha = 0.92f))
                        .border(1.dp, cs.outline.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("♡", color = cs.onSurface)
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                product.brand.ifBlank { "Brand" },
                modifier = Modifier.padding(horizontal = 12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurface.copy(alpha = 0.65f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                product.title,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            Text(
                "₹${product.priceInr}",
                modifier = Modifier.padding(horizontal = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = cs.onSurface
            )
        }
    }
}