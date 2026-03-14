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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyCodeScreen(
    api: ApiService, // kept to avoid breaking nav signature (not needed here)
    onBack: () -> Unit,
    onVerified: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    var code by remember { mutableStateOf("") }
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
                text = "Verify code",
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
                text = ResetFlow.email,
                style = MaterialTheme.typography.titleMedium,
                color = cs.onBackground
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
                        text = "Tip: check Spam/Junk if you don’t see it.",
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
                    ResetFlow.code = c
                    onVerified()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = cs.primary)
            ) {
                Text("Continue", color = cs.onPrimary)
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