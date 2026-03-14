package com.mnikita.knowyourrunway.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.mnikita.knowyourrunway.data.ResetFlow
import com.mnikita.knowyourrunway.network.ApiService
import com.mnikita.knowyourrunway.network.ResetPasswordReq
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPasswordScreen(
    api: ApiService,
    onResetDone: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme

    var p1 by remember { mutableStateOf("") }
    var p2 by remember { mutableStateOf("") }
    var show1 by remember { mutableStateOf(false) }
    var show2 by remember { mutableStateOf(false) }

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
                text = "New password",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = cs.onBackground
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Create a strong password you’ll remember.",
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
                        value = p1,
                        onValueChange = { p1 = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("New password") },
                        singleLine = true,
                        visualTransformation = if (show1) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { show1 = !show1 }) {
                                Icon(
                                    imageVector = if (show1) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = null,
                                    tint = cs.onSurfaceVariant
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        colors = fieldColors
                    )

                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = p2,
                        onValueChange = { p2 = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Confirm password") },
                        singleLine = true,
                        visualTransformation = if (show2) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { show2 = !show2 }) {
                                Icon(
                                    imageVector = if (show2) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = null,
                                    tint = cs.onSurfaceVariant
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
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
                    if (p1.length < 6) {
                        msg = "Password must be at least 6 characters."
                        return@Button
                    }
                    if (p1 != p2) {
                        msg = "Passwords do not match."
                        return@Button
                    }

                    loading = true
                    scope.launch {
                        try {
                            val res = api.resetPassword(
                                ResetPasswordReq(
                                    email = ResetFlow.email,
                                    code = ResetFlow.code,
                                    newPassword = p1
                                )
                            )
                            if (res.ok == true) {
                                ResetFlow.email = ""
                                ResetFlow.code = ""
                                onResetDone()
                            } else {
                                msg = res.error ?: "Failed to reset password"
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
                Text(if (loading) "Resetting..." else "Reset password", color = cs.onPrimary)
            }

            Spacer(Modifier.weight(1f))
        }
    }
}