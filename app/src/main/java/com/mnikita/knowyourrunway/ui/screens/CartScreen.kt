package com.mnikita.knowyourrunway.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mnikita.knowyourrunway.data.CartStore
import com.mnikita.knowyourrunway.data.WishlistStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onBack: () -> Unit,
    onOpenWishlist: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val cartItems = CartStore.items
    val total = CartStore.totalInr()

    val animatedTotal by animateIntAsState(
        targetValue = total,
        animationSpec = tween(durationMillis = 220),
        label = "totalAnim"
    )

    val itemCount = cartItems.sumOf { it.qty }
    val subtotal = cartItems.sumOf { it.product.priceInr * it.qty }
    val deliveryFee = 0
    val grandTotal = subtotal + deliveryFee

    val animatedGrand by animateIntAsState(
        targetValue = grandTotal,
        animationSpec = tween(durationMillis = 220),
        label = "grandAnim"
    )

    val wishCount = WishlistStore.items.size
    val badgeText = when {
        wishCount <= 0 -> ""
        wishCount >= 100 -> "99+"
        else -> wishCount.toString()
    }

    // checkout bottom sheet
    val showSheet = remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    if (showSheet.value && cartItems.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { showSheet.value = false },
            sheetState = sheetState,
            // ✅ looks like a proper sheet in both light/dark
            containerColor = cs.surface
        ) {
            // drag handle replacement
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(cs.onSurfaceVariant.copy(alpha = 0.35f))
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Checkout",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    // ✅ onSurface because the sheet is a surface
                    color = cs.onSurface
                )
                Text(
                    "This is a premium UI placeholder. Payments will be connected later.",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )

                CheckoutRow(
                    icon = Icons.Default.LocationOn,
                    title = "Delivery address",
                    subtitle = "Add your address (coming soon)",
                    onClick = { Toast.makeText(ctx, "Address flow will be added later", Toast.LENGTH_SHORT).show() }
                )

                CheckoutRow(
                    icon = Icons.Default.CreditCard,
                    title = "Payment method",
                    subtitle = "Select a payment method (coming soon)",
                    onClick = { Toast.makeText(ctx, "Payment flow will be added later", Toast.LENGTH_SHORT).show() }
                )

                CheckoutRow(
                    icon = Icons.Default.ConfirmationNumber,
                    title = "Offers & promo",
                    subtitle = "Apply coupon (coming soon)",
                    onClick = { Toast.makeText(ctx, "Offers will be added later", Toast.LENGTH_SHORT).show() }
                )

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, cs.outline.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Order summary", fontWeight = FontWeight.SemiBold, color = cs.onSurface)

                        SummaryRow(label = "Items ($itemCount)", value = "₹$subtotal")
                        SummaryRow(label = "Delivery", value = if (deliveryFee == 0) "FREE" else "₹$deliveryFee")
                        Divider(color = cs.outline.copy(alpha = 0.25f))
                        SummaryRow(label = "Grand total", value = "₹$animatedGrand", valueEmphasis = true)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = cs.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Secure checkout (placeholder)",
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSurfaceVariant
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        Toast.makeText(ctx, "Checkout will be connected later", Toast.LENGTH_SHORT).show()
                        scope.launch {
                            sheetState.hide()
                            showSheet.value = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
                ) {
                    Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = cs.onPrimary)
                    Spacer(Modifier.width(10.dp))
                    Text("Place order • ₹$animatedGrand", color = cs.onPrimary)
                }

                Spacer(Modifier.height(10.dp))
            }
        }
    }

    Scaffold(
        containerColor = cs.background,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = { Text("Cart", color = cs.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = cs.onBackground)
                    }
                },
                actions = {
                    IconButton(onClick = onOpenWishlist) {
                        BadgedBox(badge = { if (wishCount > 0) Badge { Text(badgeText) } }) {
                            Icon(Icons.Filled.Favorite, contentDescription = "Wishlist", tint = cs.primary)
                        }
                    }

                    if (cartItems.isNotEmpty()) {
                        TextButton(
                            onClick = { CartStore.clear() },
                            colors = ButtonDefaults.textButtonColors(contentColor = cs.primary)
                        ) { Text("Clear") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cs.background,
                    titleContentColor = cs.onBackground,
                    navigationIconContentColor = cs.onBackground,
                    actionIconContentColor = cs.primary
                )
            )
        },
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                Surface(color = cs.surface, tonalElevation = 0.dp, shadowElevation = 8.dp) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Total • $itemCount items", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                                Text("₹$animatedTotal", style = MaterialTheme.typography.headlineSmall, color = cs.onSurface, fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = { showSheet.value = true },
                                modifier = Modifier.height(48.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
                            ) {
                                Text("Checkout", color = cs.onPrimary)
                            }
                        }

                        Text(
                            "Tip: Checkout is UI-only right now. Payments will be added later.",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (cartItems.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Your cart is empty.", color = cs.onBackground, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Text("Tap Add to cart on a product to see it here.", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surface),
                    modifier = Modifier.fillMaxWidth().border(1.dp, cs.outline.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Order overview", fontWeight = FontWeight.SemiBold, color = cs.onSurface)
                        Text("$itemCount item(s) in your cart", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                        Row { Text("Subtotal", color = cs.onSurfaceVariant, modifier = Modifier.weight(1f)); Text("₹$subtotal", color = cs.onSurface) }
                        Row { Text("Delivery", color = cs.onSurfaceVariant, modifier = Modifier.weight(1f)); Text("FREE", color = cs.primary) }
                    }
                }
            }

            items(cartItems, key = { it.product.id }) { item ->
                CartRow(
                    title = item.product.title,
                    brand = item.product.brand,
                    priceInr = item.product.priceInr,
                    mrpInr = item.product.mrpInr,
                    qty = item.qty,
                    imageUrl = item.product.coverUrl,
                    onInc = { CartStore.inc(item.product.id) },
                    onDec = { CartStore.dec(item.product.id) },
                    onRemove = { CartStore.remove(item.product.id) }
                )
            }

            item {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showSheet.value = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Review & checkout")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CheckoutRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)

    Surface(
        shape = shape,
        color = cs.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, cs.outline.copy(alpha = 0.55f), shape)
            .clip(shape)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(cs.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = cs.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = cs.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = cs.onSurfaceVariant)
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueEmphasis: Boolean = false
) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = cs.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, color = cs.onSurface, fontWeight = if (valueEmphasis) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun CartRow(
    title: String,
    brand: String,
    priceInr: Int,
    mrpInr: Int?,
    qty: Int,
    imageUrl: String?,
    onInc: () -> Unit,
    onDec: () -> Unit,
    onRemove: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, cs.outline.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val url = imageUrl?.trim().orEmpty()
            if (url.isBlank() || url == "-") {
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .clip(shape)
                        .background(cs.surfaceVariant.copy(alpha = 0.35f))
                        .border(1.dp, cs.outline.copy(alpha = 0.30f), shape)
                )
            } else {
                AsyncImage(
                    model = url,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(78.dp).clip(shape)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, color = cs.onSurface)
                Spacer(Modifier.height(2.dp))
                Text(brand, style = MaterialTheme.typography.bodyMedium, maxLines = 1, color = cs.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("₹$priceInr", style = MaterialTheme.typography.titleMedium, color = cs.onSurface, fontWeight = FontWeight.SemiBold)
                    val mrp = mrpInr ?: 0
                    if (mrp > priceInr) {
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "₹$mrp",
                            style = MaterialTheme.typography.bodyMedium.merge(TextStyle(textDecoration = TextDecoration.LineThrough)),
                            color = cs.onSurfaceVariant
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = cs.primary)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDec) { Icon(Icons.Default.Remove, contentDescription = "Minus", tint = cs.onSurfaceVariant) }
                    Text(qty.toString(), style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                    IconButton(onClick = onInc) { Icon(Icons.Default.Add, contentDescription = "Plus", tint = cs.onSurfaceVariant) }
                }
            }
        }
    }
}