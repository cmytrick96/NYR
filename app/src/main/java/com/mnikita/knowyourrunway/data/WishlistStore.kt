package com.mnikita.knowyourrunway.data

import androidx.compose.runtime.mutableStateListOf
import com.mnikita.knowyourrunway.ui.components.ProductCardUi

object WishlistStore {
    val items = mutableStateListOf<ProductCardUi>()

    fun isInWishlist(productId: Long): Boolean = items.any { it.id == productId }

    fun toggle(product: ProductCardUi) {
        val idx = items.indexOfFirst { it.id == product.id }
        if (idx >= 0) items.removeAt(idx) else items.add(0, product)
    }

    fun remove(productId: Long) {
        val idx = items.indexOfFirst { it.id == productId }
        if (idx >= 0) items.removeAt(idx)
    }

    fun clear() {
        items.clear()
    }
}