package com.composea11yscanner.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun BrokenLoginScreen(onViewFixed: (() -> Unit)? = null) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0D1117))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        SecureBankHeader(isFixed = false)
        LoginIntro()

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                trailingIcon = {
                    Text(
                        text = if (passwordVisible) "Hide" else "Show",
                        color = Color(0xFF6C63FF),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .size(width = 44.dp, height = 30.dp)
                            .clickable { passwordVisible = !passwordVisible },
                    )
                },
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        GradientSignInButton(isFixed = false)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Forgot password?",
                color = Color(0xFF4DA6FF),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .size(width = 108.dp, height = 30.dp)
                    .clickable { },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TinyClickableLabel("?", Color(0xFF1C2B3A))
                TinyClickableLabel("!", Color(0xFF2E2A1C))
                TinyClickableLabel("x", Color(0xFF281C2E))
            }
        }

        DeviceTrustCard(isFixed = false)

        onViewFixed?.let {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1C1C28))
                    .clickable { it() },
                contentAlignment = Alignment.Center,
            ) {
                Text("View Fixed Version")
            }
        }
    }
}

@Composable
fun FixedLoginScreen(onViewBroken: (() -> Unit)? = null) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0D1117))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        SecureBankHeader(isFixed = true)
        LoginIntro()

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                trailingIcon = {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .semantics {
                                contentDescription = if (passwordVisible) {
                                    "Hide password"
                                } else {
                                    "Show password"
                                }
                            }
                            .clickable(
                                role = Role.Button,
                                onClick = { passwordVisible = !passwordVisible },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (passwordVisible) "Hide" else "Show",
                            color = Color(0xFF6C63FF),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        GradientSignInButton(isFixed = true)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .semantics { contentDescription = "Reset password" }
                    .clickable(
                        role = Role.Button,
                        onClick = {},
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Forgot password?",
                    color = Color(0xFF4DA6FF),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FixedIconAction("?", "Open login help", Color(0xFF1C2B3A))
                FixedIconAction("!", "Show login requirements", Color(0xFF2E2A1C))
                FixedIconAction("x", "Dismiss login form", Color(0xFF281C2E))
            }
        }

        DeviceTrustCard(isFixed = true)

        onViewBroken?.let {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1C1C28))
                    .semantics { contentDescription = "View broken version" }
                    .clickable(
                        role = Role.Button,
                        onClick = { it() },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("View Broken Version")
            }
        }
    }
}

@Composable
private fun SecureBankHeader(isFixed: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BankLogoMark(
            modifier = if (isFixed) {
                Modifier
                    .semantics { contentDescription = "Open SecureBank home" }
                    .clickable(role = Role.Image, onClick = {})
            } else {
                // Intentionally broken: an actionable image needs an accessible description.
                Modifier.clickable(role = Role.Image, onClick = {})
            },
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "SecureBank",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Private client access",
                color = Color(0xFF8B949E),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun LoginIntro() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Welcome back",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Sign in to view balances, transfers, and card activity.",
            color = Color(0xFF8B949E),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun GradientSignInButton(isFixed: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF6C63FF), Color(0xFF00D4AA)),
                ),
            )
            .then(
                if (isFixed) {
                    Modifier
                        .semantics { contentDescription = "Sign in" }
                        .clickable(role = Role.Button, onClick = {})
                } else {
                    Modifier.clickable { }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Sign In",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DeviceTrustCard(isFixed: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF121826))
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Device trust",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Review this session",
                    color = Color(0xFF8B949E),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Box(
                modifier = Modifier
                    .size(if (isFixed) 56.dp else 32.dp)
                    .clip(RoundedCornerShape(if (isFixed) 16.dp else 10.dp))
                    .background(Color(0xFF00D4AA))
                    .then(
                        if (isFixed) {
                            Modifier
                                .semantics { contentDescription = "Review trusted device session" }
                                .clickable(role = Role.Button, onClick = {})
                        } else {
                            Modifier.clickable { }
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("Go", color = Color(0xFF0D1117), fontWeight = FontWeight.Bold)
            }
        }
    }
}
