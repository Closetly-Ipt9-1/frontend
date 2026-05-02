package com.closetly.myapp.auth.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.closetly.myapp.auth.func.GoogleAuthFunc
import com.closetly.myapp.auth.func.LoginFunc
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isPreview = LocalInspectionMode.current
    val loginFunc = if (!isPreview) remember { LoginFunc() } else null
    val googleAuthFunc = if (!isPreview) remember { GoogleAuthFunc() } else null
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var passwordVisible by remember { mutableStateOf(false) }
    var isGoogleLoading by remember { mutableStateOf(false) }

    AuthScreenShell(
        title = "Willkommen zurück",
        subtitle = "Melde dich mit deiner E-Mail an."
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-Mail") },
            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Passwort") },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(if (passwordVisible) "Ausblenden" else "Anzeigen")
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (!isEmailVal(email)) {
                    Toast.makeText(context, "Invalid email", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                loginFunc?.login(
                    email = email.trim(),
                    password = password,
                    onSuccess = {
                        Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    },
                    onError = {
                        Toast.makeText(context, "Login failed", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Einloggen")
        }

        GoogleSignInButton(
            text = "Mit Google einloggen",
            isLoading = isGoogleLoading,
            onClick = {
                val authFunc = googleAuthFunc ?: return@GoogleSignInButton
                isGoogleLoading = true
                coroutineScope.launch {
                    runCatching { authFunc.signIn(context) }
                        .onSuccess {
                            Toast.makeText(context, "Google login successful", Toast.LENGTH_SHORT).show()
                            onLoginSuccess()
                        }
                        .onFailure {
                            Toast.makeText(
                                context,
                                googleAuthFailureMessage("Google login failed", it),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    isGoogleLoading = false
                }
            }
        )

        OutlinedButton(
            onClick = {
                loginFunc?.login(
                    email = "timonsoom@gmail.com",
                    password = "Timon2008",
                    onSuccess = {
                        Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
                        onLoginSuccess()
                    },
                    onError = {
                        Toast.makeText(context, "Login failed", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Test Login")
        }

        TextButton(onClick = onNavigateToRegister) {
            Text("Account erstellen")
        }
    }
}

@Composable
fun GoogleSignInButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Text(
            text = "G",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4285F4)
        )
        Spacer(Modifier.width(10.dp))
        Text(if (isLoading) "Google wird geoeffnet..." else text)
    }
}

fun googleAuthFailureMessage(prefix: String, error: Throwable): String {
    val reason = error.localizedMessage ?: error::class.simpleName ?: "Unknown error"
    return "$prefix: $reason"
}

@Composable
fun AuthScreenShell(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface,
                        Color(0xFF102A34)
                    )
                )
            )
            .statusBarsPadding()
            .imePadding()
            .padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(Modifier.height(54.dp))

        RowLogo()

        Spacer(Modifier.height(28.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 14.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                content()
            }
        }
    }
}

@Composable
private fun RowLogo() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Closetly",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold
        )
    }
}

fun isEmailVal(email: String): Boolean {
    val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()
    return email.matches(emailRegex)
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(
        onNavigateToRegister = {},
        onLoginSuccess = {}
    )
}
