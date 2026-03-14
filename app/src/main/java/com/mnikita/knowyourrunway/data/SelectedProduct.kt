package com.mnikita.knowyourrunway.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mnikita.knowyourrunway.ui.components.ProductCardUi

object SelectedProduct {
    var current: ProductCardUi? by mutableStateOf(null)
}