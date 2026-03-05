package com.m306.closetly.auth.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import com.m306.closetly.auth.func.LoginFunc
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Email
import androidx.compose.ui.platform.LocalInspectionMode


@Composable
fun LoginScreen(){
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isPreview = LocalInspectionMode.current
    val loginFunc = if (!isPreview) remember { LoginFunc() } else null
    val context = LocalContext.current
    var passwordVisible by remember { mutableStateOf(false) }

    Column (horizontalAlignment = Alignment.CenterHorizontally){

        Text("Login")
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Email,
                    contentDescription = null,
                )
            },
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                )
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { passwordVisible = !passwordVisible }) {
                    Text(
                        text = if (passwordVisible) "Hide" else "Show",
                    )
                }
            },
            singleLine = true,

            )
        ElevatedButton(onClick = { if(isEmailVal(email)) {
            loginFunc?.login(email.trim(), password)} else {
            Toast.makeText(context, "Invalid email", Toast.LENGTH_SHORT).show()
        }
        }) {
            Text("Login")
        }
    }
}

fun isEmailVal(email: String): Boolean {
    val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()
    return email.matches(emailRegex)
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview(){
        LoginScreen()
}