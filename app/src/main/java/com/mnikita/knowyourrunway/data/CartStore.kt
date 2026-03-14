package com.mnikita.knowyourrunway.data

import androidx.compose.runtime.mutableStateListOf
import com.mnikita.knowyourrunway.ui.components.ProductCardUi

data class CartItem(
    val product: ProductCardUi,
    val qty: Int
)

object CartStore {
    val items = mutableStateListOf<CartItem>()

    fun qty(productId: Long): Int = items.firstOrNull { it.product.id == productId }?.qty ?: 0

    fun add(product: ProductCardUi) {
        val idx = items.indexOfFirst { it.product.id == product.id }
        if (idx >= 0) {
            val cur = items[idx]
            items[idx] = cur.copy(qty = cur.qty + 1)
        } else {
            items.add(0, CartItem(product, 1))
        }
    }

    fun inc(productId: Long) {
        val idx = items.indexOfFirst { it.product.id == productId }
        if (idx >= 0) {
            val cur = items[idx]
            items[idx] = cur.copy(qty = cur.qty + 1)
        }
    }

    fun dec(productId: Long) {
        val idx = items.indexOfFirst { it.product.id == productId }
        if (idx >= 0) {
            val cur = items[idx]
            val newQty = cur.qty - 1
            if (newQty <= 0) items.removeAt(idx) else items[idx] = cur.copy(qty = newQty)
        }
    }

    fun remove(productId: Long) {
        val idx = items.indexOfFirst { it.product.id == productId }
        if (idx >= 0) items.removeAt(idx)
    }

    fun totalInr(): Int = items.sumOf { it.product.priceInr * it.qty }

    fun clear() {
        items.clear()
    }
}