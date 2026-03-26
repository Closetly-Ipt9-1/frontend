package com.m306.closetly.profile.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.util.UUID

@Composable
fun ProfileScreen(
    onEditClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onSavedOutfitsClick: () -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val storage = FirebaseStorage.getInstance()
    val user = auth.currentUser

    var photoUrl: String? by remember(user?.photoUrl) {
        mutableStateOf(user?.photoUrl?.toString())
    }
    var isUploading by remember { mutableStateOf(false) }

    var showSourceDialog by remember { mutableStateOf(false) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    fun uploadProfileImage(selectedImageUri: Uri) {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        isUploading = true

        val fileName = UUID.randomUUID().toString()
        val imageRef = storage.reference
            .child("users/$userId/profile_pictures/$fileName.jpg")

        imageRef.putFile(selectedImageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl
                    .addOnSuccessListener { downloadUri ->
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setPhotoUri(downloadUri)
                            .build()

                        currentUser.updateProfile(profileUpdates)
                            .addOnSuccessListener {
                                photoUrl = downloadUri.toString()
                                isUploading = false
                                pendingImageUri = null
                            }
                            .addOnFailureListener {
                                isUploading = false
                            }
                    }
                    .addOnFailureListener {
                        isUploading = false
                    }
            }
            .addOnFailureListener {
                isUploading = false
            }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { selectedImageUri: Uri? ->
        if (selectedImageUri != null) {
            pendingImageUri = selectedImageUri
            showPreviewDialog = true
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            pendingImageUri = cameraImageUri
            showPreviewDialog = true
        }
    }

    val displayName = user?.displayName?.takeIf { it.isNotBlank() } ?: "No display name"
    val email = user?.email ?: "No email"
    val uid = user?.uid ?: "No UID"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Profile picture",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Default profile icon",
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isUploading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedButton(
            onClick = {
                showSourceDialog = true
            }
        ) {
            Text("Choose profile picture")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "User Profile",
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileInfoItem(
                    title = "Display Name",
                    value = displayName
                )

                HorizontalDivider()

                ProfileInfoItem(
                    title = "Email",
                    value = email
                )

                HorizontalDivider()

                ProfileInfoItem(
                    title = "User ID",
                    value = uid
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onEditClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit profile"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Editieren")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSavedOutfitsClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Saved Outfits")
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onLogoutClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.ExitToApp,
                contentDescription = "Logout"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout")
        }
    }

    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text("Profilbild auswählen") },
            text = { Text("Willst du ein Bild aus der Galerie wählen oder mit der Kamera aufnehmen?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSourceDialog = false
                        imagePickerLauncher.launch("image/*")
                    }
                ) {
                    Text("Galerie")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSourceDialog = false
                        val newImageUri = createImageUri(context)
                        cameraImageUri = newImageUri
                        cameraLauncher.launch(newImageUri)
                    }
                ) {
                    Text("Kamera")
                }
            }
        )
    }

    if (showPreviewDialog && pendingImageUri != null) {
        AlertDialog(
            onDismissRequest = {
                showPreviewDialog = false
                pendingImageUri = null
            },
            title = { Text("Bild bestätigen") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = pendingImageUri,
                        contentDescription = "Selected profile picture preview",
                        modifier = Modifier
                            .size(220.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Möchtest du dieses Bild als Profilbild verwenden?")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = pendingImageUri
                        showPreviewDialog = false
                        if (uri != null) {
                            uploadProfileImage(uri)
                        }
                    }
                ) {
                    Text("Bestätigen")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPreviewDialog = false
                        pendingImageUri = null
                    }
                ) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

private fun createImageUri(context: Context): Uri {
    val imageFile = File.createTempFile(
        "profile_picture_${System.currentTimeMillis()}",
        ".jpg",
        context.cacheDir
    )

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        imageFile
    )
}

@Composable
fun ProfileInfoItem(
    title: String,
    value: String
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}