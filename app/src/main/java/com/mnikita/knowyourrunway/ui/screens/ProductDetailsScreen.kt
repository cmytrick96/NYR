package com.mnikita.knowyourrunway.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mnikita.knowyourrunway.data.CartStore
import com.mnikita.knowyourrunway.data.SelectedProduct
import com.mnikita.knowyourrunway.data.WishlistStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailsScreen(
    onBack: () -> Unit,
    onOpenWishlist: () -> Unit
) {
    val product = SelectedProduct.current
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val clipboard = LocalClipboardManager.current

    val wishCount = WishlistStore.items.size
    val badgeText = when {
        wishCount <= 0 -> ""
        wishCount >= 100 -> "99+"
        else -> wishCount.toString()
    }

    Scaffold(
        containerColor = cs.background,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                title = { Text(product?.title ?: "Product details", color = cs.onBackground, maxLines = 1) },
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = cs.background,
                    titleContentColor = cs.onBackground
                )
            )
        }
    ) { padding ->
        if (product == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No product selected.", style = MaterialTheme.typography.titleMedium, color = cs.onBackground)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
                ) { Text("Back", color = cs.onPrimary) }
            }
            return@Scaffold
        }

        val inWishlist = WishlistStore.isInWishlist(product.id)
        val cartQty = CartStore.qty(product.id)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            val cover = product.coverUrl?.trim().orEmpty()
            if (cover.isBlank() || cover == "-") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(cs.surfaceVariant.copy(alpha = 0.35f))
                        .border(1.dp, cs.outline.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
                )
            } else {
                AsyncImage(
                    model = cover,
                    contentDescription = product.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, cs.outline.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(product.title, style = MaterialTheme.typography.headlineSmall, color = cs.onBackground)
            Spacer(Modifier.height(6.dp))
            Text(product.brand, style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("₹${product.priceInr}", style = MaterialTheme.typography.headlineSmall, color = cs.onBackground)

                val mrp = product.mrpInr ?: 0
                if (mrp > product.priceInr) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "₹$mrp",
                        style = MaterialTheme.typography.bodyLarge.merge(
                            TextStyle(textDecoration = TextDecoration.LineThrough)
                        ),
                        color = cs.onSurfaceVariant
                    )
                }

                val disc = product.discountPct ?: 0
                if (disc > 0) {
                    Spacer(Modifier.width(10.dp))
                    AssistChip(
                        onClick = {},
                        label = { Text("$disc% OFF") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = cs.primary.copy(alpha = 0.10f),
                            labelColor = cs.primary
                        ),
                        border = BorderStroke(1.dp, cs.primary.copy(alpha = 0.25f))
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text(product.gender) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = cs.surfaceVariant,
                        labelColor = cs.onSurfaceVariant
                    ),
                    border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.45f))
                )

                val r = product.rating
                if (r != null && r > 0) {
                    val total = product.ratingTotal ?: 0
                    AssistChip(
                        onClick = {},
                        label = { Text("⭐ $r ($total)") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = cs.surfaceVariant,
                            labelColor = cs.onSurfaceVariant
                        ),
                        border = BorderStroke(1.dp, cs.outline.copy(alpha = 0.45f))
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Divider(color = cs.outline.copy(alpha = 0.45f))
            Spacer(Modifier.height(14.dp))

            if (!product.sellerName.isNullOrBlank()) {
                Text("Seller", style = MaterialTheme.typography.titleMedium, color = cs.onBackground)
                Spacer(Modifier.height(6.dp))
                Text(product.sellerName!!, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
            }

            if (!product.asin.isNullOrBlank()) {
                Text("ASIN", style = MaterialTheme.typography.titleMedium, color = cs.onBackground)
                Spacer(Modifier.height(6.dp))
                Text(product.asin!!, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
            }

            val url = product.productUrl?.trim().orEmpty()

            if (url.isNotBlank()) {
                OutlinedButton(
                    onClick = {
                        val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        ctx.startActivity(i)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.primary)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, tint = cs.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Open product page")
                }

                Spacer(Modifier.height(12.dp))

                // Share / Copy section
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cs.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, cs.outline.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // ✅ onSurface inside a surface card
                        Text("Share & copy", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                        Text(
                            url,
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                            maxLines = 2
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(url))
                                    Toast.makeText(ctx, "Link copied", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.primary)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Copy link")
                            }

                            Button(
                                onClick = {
                                    val shareText = "${product.title}\n$url"
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, product.title)
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    ctx.startActivity(Intent.createChooser(shareIntent, "Share product"))
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = cs.onPrimary)
                                Spacer(Modifier.width(8.dp))
                                Text("Share", color = cs.onPrimary)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { WishlistStore.toggle(product) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.primary)
                ) {
                    Text(if (inWishlist) "Wishlisted ✓" else "Wishlist")
                }

                Button(
                    onClick = { CartStore.add(product) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
                ) {
                    Text(if (cartQty > 0) "In cart ($cartQty)" else "Add to cart", color = cs.onPrimary)
                }
            }

            Spacer(Modifier.height(18.dp))
        }
    }
}