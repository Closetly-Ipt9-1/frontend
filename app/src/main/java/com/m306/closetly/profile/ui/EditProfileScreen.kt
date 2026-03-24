package com.m306.closetly.profile.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

@Composable
fun EditProfileScreen(
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser

    var displayName by remember { mutableStateOf(user?.displayName ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var isSaving by remember { mutableStateOf(false) }
    var pendingUpdates by remember { mutableIntStateOf(0) }
    var hasError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Profil editieren",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Display Name") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null
                        )
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null
                        )
                    },
                    singleLine = true
                )

                Text(
                    text = "User ID: ${user?.uid ?: "No UID"}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isSaving) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            onClick = {
                if (user == null) {
                    Toast.makeText(context, "No user logged in", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val trimmedName = displayName.trim()
                val trimmedEmail = email.trim()

                if (trimmedName.isBlank()) {
                    Toast.makeText(context, "Display name cannot be empty", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                if (trimmedEmail.isBlank()) {
                    Toast.makeText(context, "Email cannot be empty", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                isSaving = true
                hasError = false
                pendingUpdates = 0

                if (trimmedName != (user.displayName ?: "")) {
                    pendingUpdates++
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(trimmedName)
                        .build()

                    user.updateProfile(profileUpdates)
                        .addOnSuccessListener {
                            pendingUpdates--
                            if (pendingUpdates == 0 && !hasError) {
                                isSaving = false
                                Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                                onSaveSuccess()
                            }
                        }
                        .addOnFailureListener {
                            hasError = true
                            isSaving = false
                            Toast.makeText(
                                context,
                                it.localizedMessage ?: "Failed to update display name",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }

                if (trimmedEmail != (user.email ?: "")) {
                    pendingUpdates++
                    user.verifyBeforeUpdateEmail(trimmedEmail)
                        .addOnSuccessListener {
                            pendingUpdates--
                            if (pendingUpdates == 0 && !hasError) {
                                isSaving = false
                                Toast.makeText(
                                    context,
                                    "Verification mail sent. Confirm the new email and log in again if needed.",
                                    Toast.LENGTH_LONG
                                ).show()
                                onSaveSuccess()
                            }
                        }
                        .addOnFailureListener {
                            hasError = true
                            isSaving = false
                            Toast.makeText(
                                context,
                                it.localizedMessage ?: "Failed to update email",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }

                if (pendingUpdates == 0) {
                    isSaving = false
                    Toast.makeText(context, "Nothing changed", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        ) 
        {
            Text("Speichern")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onBackClick,
            enabled = !isSaving
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back"
            )
            Text(" Zurück")
        }
    }
}