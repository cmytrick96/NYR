package com.mnikita.knowyourrunway.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mnikita.knowyourrunway.data.SignUpFlow
import com.mnikita.knowyourrunway.data.TokenStore
import com.mnikita.knowyourrunway.network.ApiService
import com.mnikita.knowyourrunway.network.VerifyCodeReq
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpVerifyScreen(
    api: ApiService,
    tokenStore: TokenStore,
    onBack: () -> Unit,
    onDone: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var code by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }

    val cs = MaterialTheme.colorScheme

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = cs.primary,
        focusedLabelColor = cs.primary,
        cursorColor = cs.primary,
        unfocusedBorderColor = cs.outline,
        unfocusedLabelColor = cs.onSurfaceVariant
    )

    Scaffold(
        containerColor = cs.background,
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 22.dp)
        ) {
            Spacer(Modifier.height(22.dp))

            Text(
                text = "Verify your email",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = cs.onBackground
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "We sent a 6-digit code to",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onBackground.copy(alpha = 0.70f)
            )
            Text(
                text = SignUpFlow.email,
                style = MaterialTheme.typography.titleMedium,
                color = cs.onBackground
            )

            Spacer(Modifier.height(22.dp))

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { input ->
                            code = input.filter { it.isDigit() }.take(6)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Verification code") },
                        placeholder = { Text("Enter 6-digit code") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        colors = fieldColors
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Didn’t get it? Check spam/junk.",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                }
            }

            if (msg != null) {
                Spacer(Modifier.height(12.dp))
                Text(msg!!, color = cs.error)
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    msg = null
                    val c = code.trim()
                    if (c.length != 6) {
                        msg = "Please enter the 6-digit code."
                        return@Button
                    }

                    loading = true
                    scope.launch {
                        try {
                            val res = api.verifySignup(VerifyCodeReq(SignUpFlow.email, c))
                            val token = res.token
                            if (!token.isNullOrBlank()) {
                                tokenStore.setToken(token)

                                // ✅ Mark profile as NOT completed (so Splash can route to ProfileSetup if needed)
                                tokenStore.setProfileCompleted(false)

                                onDone()
                            } else {
                                msg = res.error ?: "Verification failed"
                            }
                        } catch (e: Exception) {
                            msg = e.message ?: "Network error"
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
                Text(if (loading) "Verifying..." else "Verify", color = cs.onPrimary)
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.onBackground)
            ) {
                Text("Back")
            }

            Spacer(Modifier.weight(1f))
        }
    }
}