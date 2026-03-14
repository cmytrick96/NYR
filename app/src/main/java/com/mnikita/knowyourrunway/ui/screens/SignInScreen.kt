package com.mnikita.knowyourrunway.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.mnikita.knowyourrunway.data.TokenStore
import com.mnikita.knowyourrunway.network.ApiService
import com.mnikita.knowyourrunway.network.LoginReq
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInScreen(
    api: ApiService,
    tokenStore: TokenStore,
    onSignUp: () -> Unit,
    onForgot: () -> Unit,
    onSuccess: () -> Unit,
    embedded: Boolean = false // ✅ when shown inside swipe-up panel
) {
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme

    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // ✅ compatible premium entry animation (no animateFloatAsState)
    val enterAlpha = remember { Animatable(0f) }
    val enterOffset = remember { Animatable(18f) }

    LaunchedEffect(Unit) {
        enterAlpha.snapTo(0f)
        enterOffset.snapTo(18f)
        coroutineScope {
            launch { enterAlpha.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }
            launch { enterOffset.animateTo(0f, tween(420, easing = FastOutSlowInEasing)) }
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = cs.primary,
        focusedLabelColor = cs.primary,
        cursorColor = cs.primary,
        unfocusedBorderColor = cs.outline.copy(alpha = 0.75f),
        unfocusedLabelColor = cs.onSurfaceVariant,
        focusedContainerColor = cs.surface,
        unfocusedContainerColor = cs.surface
    )

    @Composable
    fun Content(innerPadding: PaddingValues) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = 22.dp)
                .graphicsLayer {
                    alpha = enterAlpha.value
                    translationY = enterOffset.value
                }
        ) {
            Spacer(Modifier.height(if (embedded) 8.dp else 22.dp))

            Text(
                text = "Welcome back",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = cs.onBackground
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Sign in to continue swiping.",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onBackground.copy(alpha = 0.70f)
            )

            Spacer(Modifier.height(18.dp))

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cs.outline.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
            ) {
                Column(Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email") },
                        placeholder = { Text("example@gmail.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        colors = fieldColors,
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { show = !show }) {
                                Icon(
                                    imageVector = if (show) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = null,
                                    tint = cs.onSurfaceVariant
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        colors = fieldColors,
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(Modifier.height(6.dp))

                    TextButton(
                        onClick = onForgot,
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.textButtonColors(contentColor = cs.primary)
                    ) {
                        Text("Forgot password?")
                    }
                }
            }

            AnimatedVisibility(
                visible = error != null,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(180))
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Text(error.orEmpty(), color = cs.error)
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    error = null
                    val e = email.trim()
                    if (e.isBlank() || pass.isBlank()) {
                        error = "Please enter email and password."
                        return@Button
                    }

                    loading = true
                    scope.launch {
                        try {
                            val res = api.login(LoginReq(e, pass))
                            val token = res.token
                            if (!token.isNullOrBlank()) {
                                tokenStore.setToken(token)
                                onSuccess()
                            } else {
                                error = res.error ?: "Login failed"
                            }
                        } catch (ex: Exception) {
                            error = ex.message ?: "Network error"
                        } finally {
                            loading = false
                        }
                    }
                },
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                        color = cs.onPrimary
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text(if (loading) "Signing in..." else "Sign In", color = cs.onPrimary)
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onSignUp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.onBackground)
            ) {
                Text("Create account")
            }

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Don’t have an account? ", color = cs.onBackground.copy(alpha = 0.70f))
                TextButton(
                    onClick = onSignUp,
                    colors = ButtonDefaults.textButtonColors(contentColor = cs.primary)
                ) {
                    Text("Sign Up")
                }
            }
        }
    }

    if (embedded) {
        // ✅ No scaffold/insets when embedded (prevents double padding in swipe-up panel)
        Content(innerPadding = PaddingValues(0.dp))
    } else {
        Scaffold(
            containerColor = cs.background,
            contentWindowInsets = WindowInsets.systemBars
        ) { padding ->
            Content(innerPadding = padding)
        }
    }
}