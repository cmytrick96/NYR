package com.mnikita.knowyourrunway.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mnikita.knowyourrunway.data.SelectedProduct
import com.mnikita.knowyourrunway.data.ShopSession
import com.mnikita.knowyourrunway.data.TokenStore
import com.mnikita.knowyourrunway.data.UserProfile
import com.mnikita.knowyourrunway.data.WishlistStore
import com.mnikita.knowyourrunway.network.ApiService
import com.mnikita.knowyourrunway.ui.components.ProductCardUi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException
import java.util.Locale

private const val FILTER_FOR_YOU = "__FOR_YOU__"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    api: ApiService,
    tokenStore: TokenStore,
    onChooseCategory: () -> Unit,
    onOpenWishlist: () -> Unit,
    onOpenProduct: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val category = ShopSession.category

    val profile by tokenStore.profileFlow.collectAsState(initial = UserProfile())
    val userTags = remember(profile.tags) { profile.tags.map { it.trim() }.filter { it.isNotBlank() }.take(5) }

    var query by remember { mutableStateOf("") }
    var chipFilter by remember { mutableStateOf<String?>(null) }

    // ✅ prevents double-fetch when a chip triggers an immediate search
    var skipDebounceForQuery by remember { mutableStateOf<String?>(null) }

    var loading by remember { mutableStateOf(false) }
    var isFetching by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var endReached by remember { mutableStateOf(false) }

    val results = remember { mutableStateListOf<ProductCardUi>() }
    val seen = remember { HashSet<Long>() }

    fun bearer(token: String) = "Bearer $token"

    fun tokenizeQuery(raw: String): List<String> {
        val loc = Locale.getDefault()
        return raw.trim()
            .lowercase(loc)
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .map { it.trim() }
            .filter { it.length >= 2 }
            .distinct()
            .take(8)
    }

    fun resetResults() {
        results.clear()
        seen.clear()
        endReached = false
    }

    suspend fun fetchMore(searchQ: String? = null) {
        if (isFetching) return
        if (category.isBlank()) return

        val qParam = searchQ?.trim()?.takeIf { it.length >= 2 }
        val isSearch = qParam != null
        if (isSearch && endReached) return

        isFetching = true
        loading = results.isEmpty()
        error = null

        try {
            val token = tokenStore.tokenFlow.first().orEmpty()
            if (token.isBlank()) {
                error = "Please sign in again."
                return
            }

            val res = api.feed(bearer(token), q = qParam, limit = 20)

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

            val finalList = if (isSearch) mapped else mapped.shuffled()

            if (isSearch && finalList.isEmpty()) {
                endReached = true
                return
            }

            var added = 0
            finalList.forEach { p ->
                if (seen.add(p.id)) {
                    results.add(p)
                    added++
                }
            }

            if (isSearch && added == 0) endReached = true

        } catch (e: SocketTimeoutException) {
            error = "Server is taking too long. Please try again."
        } catch (e: Exception) {
            error = e.message ?: "Network error"
        } finally {
            loading = false
            isFetching = false
        }
    }

    // initial load on category change
    LaunchedEffect(category) {
        resetResults()
        query = ""
        chipFilter = null
        skipDebounceForQuery = null
        if (category.isNotBlank()) fetchMore(searchQ = null)
    }

    // ✅ server search debounce (typing)
    LaunchedEffect(query, category) {
        if (category.isBlank()) return@LaunchedEffect
        val q = query.trim()

        val skip = skipDebounceForQuery
        if (!skip.isNullOrBlank() && q == skip) {
            // chip already fetched immediately, so skip debounce once
            skipDebounceForQuery = null
            return@LaunchedEffect
        }

        if (q.isEmpty()) {
            resetResults()
            fetchMore(searchQ = null)
            return@LaunchedEffect
        }
        if (q.length < 2) return@LaunchedEffect

        delay(350)
        if (q != query.trim()) return@LaunchedEffect

        resetResults()
        fetchMore(searchQ = q)
    }

    // trending brands from current results
    val trendingBrands = remember(results) {
        results
            .map { it.brand.trim() }
            .filter { it.isNotBlank() && it.lowercase(Locale.getDefault()) != "brand" }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
            .distinct()
            .take(10)
    }
    val presetBrands = remember {
        listOf("Nike", "Adidas", "Puma", "Levi's", "Zara", "H&M", "U.S. Polo", "Allen Solly", "Roadster", "HRX")
    }
    val brandChips = remember(trendingBrands) { if (trendingBrands.isNotEmpty()) trendingBrands else presetBrands }

    val filtered by remember(query, chipFilter, userTags, results) {
        derivedStateOf {
            val loc = Locale.getDefault()
            val qTerms = tokenizeQuery(query)
            val tagSet = userTags.map { it.lowercase(loc) }

            results.filter { p ->
                val titleL = p.title.lowercase(loc)
                val brandL = p.brand.lowercase(loc)

                val okQ = qTerms.isEmpty() || qTerms.all { term ->
                    titleL.contains(term) || brandL.contains(term)
                }

                val okChip = when {
                    chipFilter.isNullOrBlank() -> true
                    chipFilter == FILTER_FOR_YOU ->
                        tagSet.isEmpty() || tagSet.any { t -> titleL.contains(t) || brandL.contains(t) }
                    else -> {
                        val f = chipFilter!!.trim().lowercase(loc)
                        titleL.contains(f) || brandL.contains(f)
                    }
                }

                okQ && okChip
            }
        }
    }

    val gridState = rememberLazyGridState()
    LaunchedEffect(gridState, category, query, endReached) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collect { last ->
                if (category.isBlank()) return@collect
                if (endReached) return@collect
                if (!isFetching && last >= (filtered.size - 6).coerceAtLeast(0)) {
                    val q = query.trim().takeIf { it.length >= 2 }
                    fetchMore(searchQ = q)
                }
            }
    }

    val wishCount = WishlistStore.items.size
    val badgeText = when {
        wishCount <= 0 -> ""
        wishCount >= 100 -> "99+"
        else -> wishCount.toString()
    }

    // ✅ Chip -> immediate server search (same behavior as typing)
    fun runChipSearch(sel: String?) {
        chipFilter = sel

        if (sel.isNullOrBlank()) return
        if (sel == FILTER_FOR_YOU) return // For-you can stay local filter

        val q = sel.trim()
        if (q.length < 2) return

        skipDebounceForQuery = q
        query = q
        scope.launch {
            resetResults()
            fetchMore(searchQ = q)
        }
    }

    Scaffold(
        containerColor = cs.background,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = { Text("Search", color = cs.onBackground) },
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
                        val label = category.lowercase().replaceFirstChar { it.uppercase() }
                        Text("Shop: $label")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.background)
            )
        }
    ) { padding ->

        if (category.isBlank()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Please choose what you want to shop.", color = cs.onBackground)
                Spacer(Modifier.height(10.dp))
                Button(onClick = onChooseCategory) { Text("Choose category") }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    // typing overrides chip filter highlight (prevents confusing mismatch)
                    if (chipFilter != null) chipFilter = null
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search products, brands...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = cs.outline.copy(alpha = 0.5f),
                    unfocusedBorderColor = cs.outline.copy(alpha = 0.35f),
                    focusedContainerColor = cs.surface,
                    unfocusedContainerColor = cs.surface
                )
            )

            Spacer(Modifier.height(12.dp))

            PersonalizeChips(
                userTags = userTags,
                brandChips = brandChips,
                selected = chipFilter,
                onSelect = { runChipSearch(it) }
            )

            Spacer(Modifier.height(12.dp))

            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = cs.primary)
                }

                error != null -> {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                        Text(error!!, color = cs.error)
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    val q = query.trim().takeIf { it.length >= 2 }
                                    fetchMore(searchQ = q)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
                        ) { Text("Retry", color = cs.onPrimary) }
                    }
                }

                filtered.isEmpty() -> {
                    val hasServerResultsButFilteredOut = results.isNotEmpty()
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            if (hasServerResultsButFilteredOut) "Nothing matches your current filters."
                            else "No results found.",
                            color = cs.onBackground,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (hasServerResultsButFilteredOut) "Try clearing chips."
                            else "Try a different keyword.",
                            color = cs.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { chipFilter = null }, shape = RoundedCornerShape(16.dp)) {
                                Text("Clear chips")
                            }
                            OutlinedButton(onClick = { query = "" }, shape = RoundedCornerShape(16.dp)) {
                                Text("Clear search")
                            }
                        }
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(bottom = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filtered, key = { it.id }) { p ->
                            PinterestCard(
                                product = p,
                                onOpen = {
                                    SelectedProduct.current = p
                                    onOpenProduct()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalizeChips(
    userTags: List<String>,
    brandChips: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        modifier = Modifier.fillMaxWidth().border(1.dp, cs.outline.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Personalize", fontWeight = FontWeight.SemiBold, color = cs.onSurface, modifier = Modifier.weight(1f))
                if (selected != null) TextButton(onClick = { onSelect(null) }) { Text("Clear") }
            }

            val tagScroll = rememberScrollState()
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(tagScroll),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("All") })

                if (userTags.isNotEmpty()) {
                    FilterChip(selected = selected == FILTER_FOR_YOU, onClick = { onSelect(FILTER_FOR_YOU) }, label = { Text("For you") })
                    userTags.forEach { tag ->
                        FilterChip(selected = selected == tag, onClick = { onSelect(tag) }, label = { Text(tag) })
                    }
                }
            }

            Text("Brands", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            val brandScroll = rememberScrollState()
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(brandScroll),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                brandChips.take(12).forEach { b ->
                    FilterChip(selected = selected == b, onClick = { onSelect(b) }, label = { Text(b) })
                }
            }
        }
    }
}

@Composable
private fun PinterestCard(
    product: ProductCardUi,
    onOpen: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)

    Card(
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, cs.outline.copy(alpha = 0.40f), shape)
            .clickable { onOpen() }
    ) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth().aspectRatio(0.82f).clip(shape)
            ) {
                val url = product.coverUrl?.trim().orEmpty()
                if (url.isBlank() || url == "-") {
                    Box(Modifier.fillMaxSize().background(cs.surfaceVariant.copy(alpha = 0.35f)))
                } else {
                    AsyncImage(
                        model = url,
                        contentDescription = product.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                val saved = WishlistStore.items.any { it.id == product.id }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(cs.surface.copy(alpha = 0.92f))
                        .border(1.dp, cs.outline.copy(alpha = 0.35f), CircleShape)
                        .clickable {
                            if (saved) WishlistStore.remove(product.id) else WishlistStore.items.add(product)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (saved) "♥" else "♡",
                        color = if (saved) cs.primary else cs.onSurface
                    )
                }
            }

            Column(Modifier.padding(12.dp)) {
                Text(
                    product.brand.ifBlank { "Brand" },
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurface.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    product.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "₹${product.priceInr}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface
                )
            }
        }
    }
}