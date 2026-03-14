package com.mnikita.knowyourrunway.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.mnikita.knowyourrunway.data.TokenStore
import com.mnikita.knowyourrunway.network.ApiClient

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val api = remember { ApiClient.create() }

    val context = LocalContext.current
    val tokenStore = remember(context) { TokenStore(context) }

    AppNavGraph(
        navController = navController,
        api = api,
        tokenStore = tokenStore
    )
}