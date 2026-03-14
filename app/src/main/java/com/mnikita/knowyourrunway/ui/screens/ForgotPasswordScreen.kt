package com.mnikita.knowyourrunway.ui.screens

import androidx.compose.foundation.border
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
import com.mnikita.knowyourrunway.data.ResetFlow
import com.mnikita.knowyourrunway.network.ApiService
import com.mnikita.knowyourrunway.network.ForgotReq
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    api: ApiService,
    onBack: () -> Unit,
    onVerify: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme

    var email by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }

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
                text = "Reset password",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = cs.onBackground
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Enter your email to receive a verification code.",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onBackground.copy(alpha = 0.70f)
            )

            Spacer(Modifier.height(22.dp))

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cs.outline.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
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
                            imeAction = ImeAction.Done
                        ),
                        colors = fieldColors
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
                    val e = email.trim()
                    if (e.isBlank()) {
                        msg = "Please enter your email."
                        return@Button
                    }

                    loading = true
                    scope.launch {
                        try {
                            val res = api.forgot(ForgotReq(e))
                            if (res.ok == true) {
                                ResetFlow.email = e
                                onVerify()
                            } else {
                                msg = res.error ?: "Failed to send code"
                            }
                        } catch (ex: Exception) {
                            msg = ex.message ?: "Network error"
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
                Text(if (loading) "Sending..." else "Send code", color = cs.onPrimary)
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