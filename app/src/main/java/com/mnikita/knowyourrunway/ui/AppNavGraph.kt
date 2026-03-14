package com.mnikita.knowyourrunway.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.mnikita.knowyourrunway.data.FeedCache
import com.mnikita.knowyourrunway.data.ShopSession
import com.mnikita.knowyourrunway.data.TokenStore
import com.mnikita.knowyourrunway.network.ApiService
import com.mnikita.knowyourrunway.ui.screens.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object Routes {
    const val Splash = "splash"
    const val Onboarding = "onboarding"
    const val Walkthrough = "walkthrough"
    const val SignIn = "signin"
    const val SignUp = "signup"
    const val SignUpVerify = "signup_verify"
    const val ProfileSetup = "profile_setup"
    const val Profile = "profile"
    const val ProfileEdit = "profile_edit"
    const val Forgot = "forgot"
    const val Verify = "verify"
    const val NewPassword = "new_password"
    const val Swipe = "swipe"
    const val Search = "search"
    const val Product = "product"
    const val Wishlist = "wishlist"
    const val Cart = "cart"
}

private const val DUR_IN = 420
private const val DUR_OUT = 280

// ✅ Premium tab switch durations (shorter + smoother)
private const val TAB_IN = 260
private const val TAB_OUT = 220
private const val TAB_OFFSET_DIV = 5 // smaller swipe distance (width / 5)

private fun enterForward(): EnterTransition =
    fadeIn(tween(DUR_IN, easing = FastOutSlowInEasing)) +
            slideInHorizontally(tween(DUR_IN, easing = FastOutSlowInEasing)) { fullWidth -> fullWidth }

private fun exitForward(): ExitTransition =
    fadeOut(tween(DUR_OUT, easing = FastOutSlowInEasing)) +
            slideOutHorizontally(tween(DUR_OUT, easing = FastOutSlowInEasing)) { fullWidth -> -fullWidth / 3 }

private fun enterBack(): EnterTransition =
    fadeIn(tween(DUR_IN, easing = FastOutSlowInEasing)) +
            slideInHorizontally(tween(DUR_IN, easing = FastOutSlowInEasing)) { fullWidth -> -fullWidth / 3 }

private fun exitBack(): ExitTransition =
    fadeOut(tween(DUR_OUT, easing = FastOutSlowInEasing)) +
            slideOutHorizontally(tween(DUR_OUT, easing = FastOutSlowInEasing)) { fullWidth -> fullWidth }

private val BottomTabsOrder = listOf(
    Routes.Swipe,
    Routes.Search,
    Routes.Wishlist,
    Routes.Cart,
    Routes.Profile
)

private fun tabIndex(route: String?): Int = BottomTabsOrder.indexOf(route)

private fun isTabSwitch(from: String?, to: String?): Boolean {
    val a = tabIndex(from)
    val b = tabIndex(to)
    return a >= 0 && b >= 0 && from != to
}

// ✅ “Swipe + fade” for bottom-tab switching, direction based on tab order
private fun AnimatedContentTransitionScope<NavBackStackEntry>.enterTab(): EnterTransition {
    val from = initialState.destination.route
    val to = targetState.destination.route
    if (!isTabSwitch(from, to)) return enterForward()

    val forward = tabIndex(to) > tabIndex(from) // moving right
    val offset: (Int) -> Int = { w -> if (forward) w / TAB_OFFSET_DIV else -w / TAB_OFFSET_DIV }

    return fadeIn(tween(TAB_IN, easing = FastOutSlowInEasing)) +
            slideInHorizontally(tween(TAB_IN, easing = FastOutSlowInEasing), initialOffsetX = offset)
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exitTab(): ExitTransition {
    val from = initialState.destination.route
    val to = targetState.destination.route
    if (!isTabSwitch(from, to)) return exitForward()

    val forward = tabIndex(to) > tabIndex(from)
    val offset: (Int) -> Int = { w -> if (forward) -w / TAB_OFFSET_DIV else w / TAB_OFFSET_DIV }

    return fadeOut(tween(TAB_OUT, easing = FastOutSlowInEasing)) +
            slideOutHorizontally(tween(TAB_OUT, easing = FastOutSlowInEasing), targetOffsetX = offset)
}

private data class BottomItem(val route: String, val label: String, val icon: ImageVector)

// ✅ IMPORTANT: don’t popBackStack for tabs (it skips animations). Always navigate.
private fun NavHostController.bottomNavigate(route: String) {
    navigate(route) {
        launchSingleTop = true
        restoreState = true
        popUpTo(graph.findStartDestination().id) { saveState = true }
    }
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    api: ApiService,
    tokenStore: TokenStore
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = currentRoute in setOf(
        Routes.Swipe, Routes.Search, Routes.Product, Routes.Wishlist, Routes.Cart, Routes.Profile
    )

    val bottomItems = listOf(
        BottomItem(Routes.Swipe, "Home", Icons.Filled.Home),
        BottomItem(Routes.Search, "Search", Icons.Filled.Search),
        BottomItem(Routes.Wishlist, "Liked", Icons.Filled.Favorite),
        BottomItem(Routes.Cart, "Cart", Icons.Filled.ShoppingCart),
        BottomItem(Routes.Profile, "Profile", Icons.Filled.Person)
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        val selected = when {
                            currentRoute == item.route -> true
                            currentRoute == Routes.Product && item.route == Routes.Swipe -> true
                            else -> false
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.bottomNavigate(item.route) },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = Routes.Splash,
            modifier = Modifier.padding(innerPadding)
        ) {

            composable(
                route = Routes.Splash,
                enterTransition = { EnterTransition.None },
                exitTransition = { fadeOut(tween(220)) },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { fadeOut(tween(220)) }
            ) {
                val scope = rememberCoroutineScope()
                SplashScreen(
                    onDone = {
                        scope.launch {
                            ShopSession.category = ""

                            val (token, profileDone) = withContext(Dispatchers.IO) {
                                val t = tokenStore.tokenFlow.first().orEmpty()
                                val p = tokenStore.profileCompletedFlow.first()
                                t to p
                            }

                            if (token.isBlank()) {
                                navController.navigate(Routes.Onboarding) {
                                    popUpTo(Routes.Splash) { inclusive = true }
                                }
                            } else if (!profileDone) {
                                navController.navigate(Routes.ProfileSetup) {
                                    popUpTo(Routes.Splash) { inclusive = true }
                                }
                            } else {
                                navController.navigate(Routes.Walkthrough) {
                                    popUpTo(Routes.Splash) { inclusive = true }
                                }
                            }
                        }
                    }
                )
            }

            composable(
                route = Routes.Onboarding,
                enterTransition = { enterForward() }, exitTransition = { exitForward() },
                popEnterTransition = { enterBack() }, popExitTransition = { exitBack() }
            ) {
                OnboardingLoginScreen(
                    api = api,
                    tokenStore = tokenStore,
                    onSignUp = { navController.navigate(Routes.SignUp) },
                    onForgot = { navController.navigate(Routes.Forgot) },
                    onSuccess = {
                        navController.navigate(Routes.Walkthrough) {
                            popUpTo(Routes.Onboarding) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Routes.Walkthrough,
                enterTransition = { enterForward() }, exitTransition = { exitForward() },
                popEnterTransition = { enterBack() }, popExitTransition = { exitBack() }
            ) {
                WalkthroughScreen(
                    onDone = { selected ->
                        ShopSession.category = selected
                        FeedCache.clear()
                        navController.navigate(Routes.Swipe) {
                            popUpTo(Routes.Walkthrough) { inclusive = true }
                        }
                    }
                )
            }

            // ✅ Bottom tabs use the premium “swipe + fade” transitions
            composable(
                route = Routes.Swipe,
                enterTransition = { enterTab() }, exitTransition = { exitTab() },
                popEnterTransition = { enterTab() }, popExitTransition = { exitTab() }
            ) {
                LaunchedEffect(Unit) {
                    if (ShopSession.category.isBlank()) {
                        navController.navigate(Routes.Walkthrough) {
                            popUpTo(Routes.Swipe) { inclusive = true }
                        }
                    }
                }

                SwipeScreen(
                    api = api,
                    tokenStore = tokenStore,
                    onOpenWishlist = { navController.navigate(Routes.Wishlist) },
                    onOpenCart = { navController.navigate(Routes.Cart) },
                    onOpenProduct = { navController.navigate(Routes.Product) },
                    onOpenProfile = { navController.navigate(Routes.Profile) },
                    onChooseCategory = { navController.navigate(Routes.Walkthrough) },
                    onLogoutDone = {
                        ShopSession.category = ""
                        navController.navigate(Routes.Onboarding) {
                            popUpTo(Routes.Swipe) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Routes.Search,
                enterTransition = { enterTab() }, exitTransition = { exitTab() },
                popEnterTransition = { enterTab() }, popExitTransition = { exitTab() }
            ) {
                SearchScreen(
                    api = api,
                    tokenStore = tokenStore,
                    onChooseCategory = { navController.navigate(Routes.Walkthrough) },
                    onOpenWishlist = { navController.navigate(Routes.Wishlist) },
                    onOpenProduct = { navController.navigate(Routes.Product) }
                )
            }

            composable(
                route = Routes.Wishlist,
                enterTransition = { enterTab() }, exitTransition = { exitTab() },
                popEnterTransition = { enterTab() }, popExitTransition = { exitTab() }
            ) {
                WishlistScreen(
                    api = api,
                    tokenStore = tokenStore,
                    onBack = { navController.popBackStack() },
                    onOpenProduct = { navController.navigate(Routes.Product) }
                )
            }

            composable(
                route = Routes.Cart,
                enterTransition = { enterTab() }, exitTransition = { exitTab() },
                popEnterTransition = { enterTab() }, popExitTransition = { exitTab() }
            ) {
                CartScreen(
                    onBack = { navController.popBackStack() },
                    onOpenWishlist = { navController.navigate(Routes.Wishlist) }
                )
            }

            composable(
                route = Routes.Profile,
                enterTransition = { enterTab() }, exitTransition = { exitTab() },
                popEnterTransition = { enterTab() }, popExitTransition = { exitTab() }
            ) {
                ProfileScreen(
                    api = api,
                    tokenStore = tokenStore,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.ProfileEdit) },
                    onChooseCategory = { navController.navigate(Routes.Walkthrough) },
                    onOpenWishlist = { navController.navigate(Routes.Wishlist) },
                    onLogoutDone = {
                        ShopSession.category = ""
                        navController.navigate(Routes.Onboarding) {
                            popUpTo(Routes.Swipe) { inclusive = true }
                        }
                    }
                )
            }

            // Everything else keeps your existing forward/back transitions
            composable(
                route = Routes.Product,
                enterTransition = { enterForward() }, exitTransition = { exitForward() },
                popEnterTransition = { enterBack() }, popExitTransition = { exitBack() }
            ) {
                ProductDetailsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenWishlist = { navController.navigate(Routes.Wishlist) }
                )
            }

            composable(
                route = Routes.SignIn,
                enterTransition = { enterForward() }, exitTransition = { exitForward() },
                popEnterTransition = { enterBack() }, popExitTransition = { exitBack() }
            ) {
                SignInScreen(
                    api = api,
                    tokenStore = tokenStore,
                    onSignUp = { navController.navigate(Routes.SignUp) },
                    onForgot = { navController.navigate(Routes.Forgot) },
                    onSuccess = {
                        navController.navigate(Routes.Walkthrough) {
                            popUpTo(Routes.SignIn) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Routes.SignUp,
                enterTransition = { enterForward() }, exitTransition = { exitForward() },
                popEnterTransition = { enterBack() }, popExitTransition = { exitBack() }
            ) {
                SignUpScreen(
                    api = api,
                    onSignIn = { navController.popBackStack() },
                    onGoVerify = { navController.navigate(Routes.SignUpVerify) }
                )
            }

            composable(
                route = Routes.SignUpVerify,
                enterTransition = { enterForward() }, exitTransition = { exitForward() },
                popEnterTransition = { enterBack() }, popExitTransition = { exitBack() }
            ) {
                SignUpVerifyScreen(
                    api = api,
                    tokenStore = tokenStore,
                    onBack = { navController.popBackStack() },
                    onDone = {
                        navController.navigate(Routes.ProfileSetup) {
                            popUpTo(Routes.SignUp) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Routes.ProfileSetup,
                enterTransition = { enterForward() }, exitTransition = { exitForward() },
                popEnterTransition = { enterBack() }, popExitTransition = { exitBack() }
            ) {
                ProfileSetupScreen(
                    api = api,
                    tokenStore = tokenStore,
                    onDone = {
                        navController.navigate(Routes.Walkthrough) {
                            popUpTo(Routes.ProfileSetup) { inclusive = true }
                        }
                    }
                )
            }

            composable(
                route = Routes.ProfileEdit,
                enterTransition = { enterForward() }, exitTransition = { exitForward() },
                popEnterTransition = { enterBack() }, popExitTransition = { exitBack() }
            ) {
                ProfileSetupScreen(api = api, tokenStore = tokenStore, onDone = { navController.popBackStack() })
            }

            composable(
                route = Routes.Forgot,
                enterTransition = { enterForward() }, exitTransition = { exitForward() },
                popEnterTransition = { enterBack() }, popExitTransition = { exitBack() }
            ) {
                ForgotPasswordScreen(
                    api = api,
                    onBack = { navController.popBackStack() },
                    onVerify = { navController.navigate(Routes.Verify) }
                )
            }

            composable(
                route = Routes.Verify,
                enterTransition = { enterForward() }, exitTransition = { exitForward() },
                popEnterTransition = { enterBack() }, popExitTransition = { exitBack() }
            ) {
                VerifyCodeScreen(
                    api = api,
                    onBack = { navController.popBackStack() },
                    onVerified = { navController.navigate(Routes.NewPassword) }
                )
            }

            composable(
                route = Routes.NewPassword,
                enterTransition = { enterForward() }, exitTransition = { exitForward() },
                popEnterTransition = { enterBack() }, popExitTransition = { exitBack() }
            ) {
                NewPasswordScreen(
                    api = api,
                    onResetDone = {
                        navController.navigate(Routes.SignIn) {
                            popUpTo(Routes.SignIn) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}