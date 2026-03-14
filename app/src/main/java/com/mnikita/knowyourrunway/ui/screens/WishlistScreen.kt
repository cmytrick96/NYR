package com.mnikita.knowyourrunway.ui.screens

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mnikita.knowyourrunway.data.SelectedProduct
import com.mnikita.knowyourrunway.data.ShopSession
import com.mnikita.knowyourrunway.data.TokenStore
import com.mnikita.knowyourrunway.data.WishlistStore
import com.mnikita.knowyourrunway.network.ApiService
import com.mnikita.knowyourrunway.network.SwipeReq
import com.mnikita.knowyourrunway.ui.components.ProductCardUi
import com.mnikita.knowyourrunway.ui.components.SwipeDeck
import com.mnikita.knowyourrunway.ui.components.SwipeDir
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private data class PendingSwipe(
    val productId: Long,
    val action: String, // "LIKE" | "DISLIKE"
    val attempts: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    api: ApiService,
    tokenStore: TokenStore,
    onBack: () -> Unit,
    onOpenProduct: (() -> Unit)? = null
) {
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    var tab by remember { mutableStateOf(0) } // 0 swipe, 1 saved
    val category = ShopSession.category

    val deck = remember { mutableStateListOf<ProductCardUi>() }
    val deckSeen = remember { HashSet<Long>() }

    // ✅ Prevent the same card coming back if swipe.php is slow/fails
    val swipedIds = remember { HashSet<Long>() }

    // ✅ Retry queue for swipe.php
    val pendingQueue = remember { mutableStateListOf<PendingSwipe>() }
    var swipeWorkerRunning by remember { mutableStateOf(false) }

    var loading by remember { mutableStateOf(false) }
    var isFetching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var overlay by remember { mutableStateOf<Pair<SwipeDir, String>?>(null) }

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

    suspend fun fetchDeck(force: Boolean = false, append: Boolean = true) {
        if (isFetching) return
        if (category.isBlank()) return

        isFetching = true
        loading = deck.isEmpty()
        error = null

        try {
            val token = tokenStore.tokenFlow.first().orEmpty()
            if (token.isBlank()) {
                error = "No token. Please sign in again."
                return
            }

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

            val filtered = mapped.filter { matchesCategory(it, category) }
            val batch = (if (filtered.isNotEmpty()) filtered else mapped)
                .distinctBy { it.id }
                .shuffled()

            if (force || !append) {
                deck.clear()
                deckSeen.clear()
                // NOTE: do not clear swipedIds here; we want to block repeats within this session
            }

            // ✅ Don't re-add already swiped ids
            val newOnes = batch.filter { p ->
                !swipedIds.contains(p.id) && deckSeen.add(p.id)
            }

            if (newOnes.isNotEmpty()) deck.addAll(newOnes)
        } catch (e: Exception) {
            error = e.message ?: "Network error"
        } finally {
            loading = false
            isFetching = false
        }
    }

    // ✅ Retry worker for swipe.php
    LaunchedEffect(pendingQueue.size) {
        if (pendingQueue.isEmpty()) return@LaunchedEffect
        if (swipeWorkerRunning) return@LaunchedEffect

        swipeWorkerRunning = true
        try {
            while (pendingQueue.isNotEmpty()) {
                val current = pendingQueue.first()

                val token = tokenStore.tokenFlow.first().orEmpty()
                if (token.isBlank()) break

                val ok = runCatching {
                    api.swipe(bearer(token), SwipeReq(current.productId, current.action))
                }.isSuccess

                if (ok) {
                    pendingQueue.removeAt(0)
                } else {
                    val nextAttempts = current.attempts + 1
                    if (nextAttempts >= 3) {
                        pendingQueue.removeAt(0)
                    } else {
                        pendingQueue[0] = current.copy(attempts = nextAttempts)
                        delay(
                            when (nextAttempts) {
                                1 -> 650L
                                2 -> 1200L
                                else -> 1800L
                            }
                        )
                    }
                }
            }
        } finally {
            swipeWorkerRunning = false
        }
    }

    LaunchedEffect(category) {
        deck.clear()
        deckSeen.clear()
        swipedIds.clear()
        pendingQueue.clear()
        if (category.isNotBlank()) fetchDeck(force = true, append = false)
    }

    // Prefetch when low
    LaunchedEffect(deck.size, category, isFetching) {
        if (category.isBlank()) return@LaunchedEffect
        if (!isFetching && deck.size in 1..3) fetchDeck(force = false, append = true)
    }

    Scaffold(
        containerColor = cs.background,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = { Text("Liked", color = cs.onBackground) },
                actions = {
                    if (tab == 0) {
                        IconButton(onClick = { scope.launch { fetchDeck(force = true, append = false) } }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh deck", tint = cs.primary)
                        }
                    }
                    if (tab == 1 && WishlistStore.items.isNotEmpty()) {
                        TextButton(
                            onClick = { WishlistStore.clear() },
                            colors = ButtonDefaults.textButtonColors(contentColor = cs.primary)
                        ) { Text("Clear") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cs.background,
                    titleContentColor = cs.onBackground
                )
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            TabRow(selectedTabIndex = tab) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    selectedContentColor = cs.primary,
                    unselectedContentColor = cs.onSurfaceVariant,
                    text = { Text("Swipe") }
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    selectedContentColor = cs.primary,
                    unselectedContentColor = cs.onSurfaceVariant,
                    text = { Text("Saved") }
                )
            }

            when (tab) {
                0 -> {
                    if (category.isBlank()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(20.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Please choose what you want to shop first.", color = cs.onBackground)
                            Spacer(Modifier.height(10.dp))
                            Button(onClick = onBack) { Text("Go back") }
                        }
                        return@Column
                    }

                    when {
                        loading || (deck.isEmpty() && isFetching) -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = cs.primary)
                                    Spacer(Modifier.height(10.dp))
                                    Text("Loading more products…", color = cs.onSurfaceVariant)
                                }
                            }
                        }

                        error != null -> {
                            Column(Modifier.fillMaxSize().padding(20.dp)) {
                                Text(error!!, color = cs.error)
                                Spacer(Modifier.height(10.dp))
                                Button(
                                    onClick = { scope.launch { fetchDeck(force = true, append = false) } },
                                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
                                ) { Text("Retry", color = cs.onPrimary) }
                            }
                        }

                        deck.isEmpty() -> {
                            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.Center) {
                                Text("No more products right now.", color = cs.onBackground)
                                Spacer(Modifier.height(10.dp))
                                Button(
                                    onClick = { scope.launch { fetchDeck(force = true, append = false) } },
                                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
                                ) { Text("Refresh", color = cs.onPrimary) }
                            }
                        }

                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {

                                SwipeDeck(
                                    items = deck,
                                    onSwipe = { productId, dir ->
                                        val swiped = deck.firstOrNull { it.id == productId }

                                        if (dir == SwipeDir.LIKE) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        else haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                                        overlay = dir to (if (dir == SwipeDir.LIKE) "LIKED" else "NOPE")
                                        scope.launch { delay(550); overlay = null }

                                        // ✅ block locally immediately
                                        swipedIds.add(productId)

                                        if (dir == SwipeDir.LIKE && swiped != null && WishlistStore.items.none { it.id == swiped.id }) {
                                            WishlistStore.items.add(swiped)
                                        }

                                        // ✅ remove the exact swiped card (not always index 0)
                                        val idx = deck.indexOfFirst { it.id == productId }
                                        if (idx >= 0) deck.removeAt(idx) else if (deck.isNotEmpty()) deck.removeAt(0)

                                        // ✅ queue swipe call with retry
                                        val action = if (dir == SwipeDir.LIKE) "LIKE" else "DISLIKE"
                                        val existing = pendingQueue.indexOfFirst { it.productId == productId }
                                        if (existing >= 0) pendingQueue[existing] = PendingSwipe(productId, action, attempts = 0)
                                        else pendingQueue.add(PendingSwipe(productId, action))

                                        // keep deck filled
                                        scope.launch {
                                            if (!isFetching && deck.size <= 3) fetchDeck(force = false, append = true)
                                            if (deck.isEmpty() && !isFetching) fetchDeck(force = true, append = false)
                                        }
                                    },
                                    onCardClick = { product ->
                                        SelectedProduct.current = product
                                        onOpenProduct?.invoke()
                                    },
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = 120.dp, bottom = 110.dp)
                                )

                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = cs.surface),
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .fillMaxWidth()
                                        .border(1.dp, cs.outline.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                                ) {
                                    Row(
                                        Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                "Swipe right to save to wishlist",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = cs.onSurface
                                            )
                                            Text(
                                                "Saved: ${WishlistStore.items.size} • In deck: ${deck.size}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = cs.onSurfaceVariant
                                            )
                                        }
                                        TextButton(onClick = { tab = 1 }) { Text("Saved") }
                                    }
                                }

                                // ✅ IMPORTANT: explicit top-level call to avoid ColumnScope receiver issues
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = overlay != null,
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 82.dp),
                                    enter = fadeIn() + scaleIn(initialScale = 0.9f),
                                    exit = fadeOut() + scaleOut(targetScale = 0.92f)
                                ) {
                                    val o = overlay
                                    if (o != null) {
                                        val (dir, label) = o
                                        val color = if (dir == SwipeDir.LIKE) cs.primary else cs.error
                                        Surface(
                                            shape = RoundedCornerShape(18.dp),
                                            color = color.copy(alpha = 0.12f),
                                            modifier = Modifier.border(
                                                1.dp,
                                                color.copy(alpha = 0.35f),
                                                RoundedCornerShape(18.dp)
                                            )
                                        ) {
                                            Text(
                                                label,
                                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                                                color = color,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    val items = WishlistStore.items
                    if (items.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(20.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Your wishlist is empty.",
                                color = cs.onBackground,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Swipe right in the Swipe tab to save items here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurfaceVariant
                            )
                        }
                        return@Column
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(items, key = { it.id }) { p ->
                            WishlistGridCard(
                                product = p,
                                onOpen = { SelectedProduct.current = p; onOpenProduct?.invoke() },
                                onRemove = { WishlistStore.remove(p.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WishlistGridCard(
    product: ProductCardUi,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, cs.outline.copy(alpha = 0.55f), shape)
            .clickable { onOpen() }
    ) {
        Column {
            val url = product.coverUrl?.trim().orEmpty()
            if (url.isBlank() || url == "-") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(shape)
                        .background(cs.surfaceVariant.copy(alpha = 0.35f))
                )
            } else {
                AsyncImage(
                    model = url,
                    contentDescription = product.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(shape)
                )
            }

            Column(Modifier.padding(12.dp)) {
                Text(product.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, color = cs.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(product.brand, style = MaterialTheme.typography.bodyMedium, maxLines = 1, color = cs.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("₹${product.priceInr}", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = cs.primary)
                    }
                }
            }
        }
    }
}