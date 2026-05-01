package com.closetly.myapp.auth.ui

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.closetly.myapp.auth.func.RegisterFunc

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordconfirm by remember { mutableStateOf("") }
    val isPreview = LocalInspectionMode.current
    val registerFunc = if (!isPreview) remember { RegisterFunc() } else null
    val context = LocalContext.current
    var passwordVisible by remember { mutableStateOf(false) }
    var passwordconfirmVisible by remember { mutableStateOf(false) }

    AuthScreenShell(
        title = "Account erstellen",
        subtitle = "Starte deinen digitalen Kleiderschrank."
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            leadingIcon = { Icon(Icons.Filled.Face, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
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
        OutlinedTextField(
            value = passwordconfirm,
            onValueChange = { passwordconfirm = it },
            label = { Text("Passwort bestätigen") },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            visualTransformation = if (passwordconfirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { passwordconfirmVisible = !passwordconfirmVisible }) {
                    Text(if (passwordconfirmVisible) "Ausblenden" else "Anzeigen")
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (password.trim() == passwordconfirm.trim()) {
                    if (!isEmailVal(email)) {
                        Toast.makeText(context, "Invalid email", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    registerFunc?.register(
                        name = name.trim(),
                        username = username.trim(),
                        email = email.trim(),
                        password = password.trim(),
                        onSuccess = {
                            Toast.makeText(context, "Registration successful", Toast.LENGTH_SHORT).show()
                            onRegisterSuccess()
                        },
                        onError = {
                            Toast.makeText(context, "Registration failed", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    Toast.makeText(context, "Passwords don't match", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Registrieren")
        }

        TextButton(onClick = onNavigateToLogin) {
            Text("Schon registriert? Einloggen")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    RegisterScreen(
        onNavigateToLogin = {},
        onRegisterSuccess = {}
    )
}
