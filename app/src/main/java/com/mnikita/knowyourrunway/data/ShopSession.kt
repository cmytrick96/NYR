package com.mnikita.knowyourrunway.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ShopSession {
    // Session-only (resets when app process restarts; we also reset at Splash)
    var category by mutableStateOf("")
}