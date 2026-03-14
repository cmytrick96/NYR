package com.mnikita.knowyourrunway.data

import androidx.compose.runtime.mutableStateListOf
import com.mnikita.knowyourrunway.ui.components.ProductCardUi

object FeedCache {
    val cards = mutableStateListOf<ProductCardUi>()

    fun setAll(newItems: List<ProductCardUi>) {
        cards.clear()
        cards.addAll(newItems)
    }

    fun dropTop() {
        if (cards.isNotEmpty()) cards.removeAt(0)
    }

    fun clear() {
        cards.clear()
    }
}