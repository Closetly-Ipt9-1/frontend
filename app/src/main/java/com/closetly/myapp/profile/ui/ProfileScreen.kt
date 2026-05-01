package com.closetly.myapp.profile.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import com.closetly.myapp.premium.data.PremiumAccessRepository
import com.closetly.myapp.premium.model.SubscriptionStatus
import com.closetly.myapp.profile.model.Avatar
import com.closetly.myapp.profile.viewmodel.ProfileViewModel
import java.io.File
import java.util.UUID

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onEditClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onSavedOutfitsClick: () -> Unit,
    onCreateAvatarClick: () -> Unit,
    onCustomizeAvatarClick: (avatarType: String) -> Unit,
    onSwitchToCustomAvatarClick: () -> Unit,
    onPremiumClick: () -> Unit,
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val storage = FirebaseStorage.getInstance()
    val user = auth.currentUser
    val premiumAccessRepository = remember { PremiumAccessRepository() }

    val avatar by viewModel.avatar.collectAsState()
    var subscriptionStatus by remember { mutableStateOf(SubscriptionStatus()) }

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
        val imageRef = storage.reference.child("users/$userId/profile_pictures/$fileName.jpg")
        imageRef.putFile(selectedImageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setPhotoUri(downloadUri).build()
                    currentUser.updateProfile(profileUpdates)
                        .addOnSuccessListener {
                            photoUrl = downloadUri.toString()
                            isUploading = false
                            pendingImageUri = null
                        }
                        .addOnFailureListener { isUploading = false }
                }.addOnFailureListener { isUploading = false }
            }
            .addOnFailureListener { isUploading = false }
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

    val displayName = user?.displayName?.takeIf { it.isNotBlank() } ?: "Kein Anzeigename"
    val email = user?.email ?: "Keine E-Mail"
    val uid = user?.uid ?: "Keine UID"

    LaunchedEffect(Unit) {
        premiumAccessRepository.getSubscriptionStatus(
            onSuccess = { subscriptionStatus = it },
            onError = {}
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Profilbild (unabhängig vom Avatar)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
                .padding(22.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (!photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Profilbild",
                        modifier = Modifier
                            .size(124.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(124.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Standard-Profilbild",
                            modifier = Modifier.size(58.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isUploading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedButton(
            onClick = { showSourceDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Text("Profilbild ändern")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Profil",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileInfoItem(title = "Anzeigename", value = displayName)
                HorizontalDivider()
                ProfileInfoItem(title = "E-Mail", value = email)
                HorizontalDivider()
                ProfileInfoItem(title = "Benutzer-ID", value = uid)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onPremiumClick,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Icon(imageVector = Icons.Default.Edit, contentDescription = "Manage premium")
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (subscriptionStatus.isPremium) "Manage subscription" else "Buy Premium")

        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onEditClick,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Icon(imageVector = Icons.Default.Edit, contentDescription = "Profil bearbeiten")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Editieren")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onSavedOutfitsClick,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Text("Gespeicherte Outfits")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onLogoutClick,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Ausloggen")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Ausloggen")
        }

        Spacer(modifier = Modifier.height(32.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(24.dp))

        // Avatar-Bereich
        Text(
            text = "Mein Avatar",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        when {
            avatar?.type == "custom" && !avatar?.frontImageUrl.isNullOrBlank() -> {
                AsyncImage(
                    model = avatar!!.frontImageUrl,
                    contentDescription = "Custom Avatar",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                )
            }
            avatar?.type == "default" && !avatar!!.imageUrl.isNullOrBlank() -> {
                AsyncImage(
                    model = avatar!!.imageUrl,
                    contentDescription = "Standard Avatar",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                )
            }
            avatar?.type == "default" -> {
                DefaultAvatarPreview(avatar = avatar!!)
            }
            else -> {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Kein Avatar",
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (avatar?.type == "default") {
            Spacer(modifier = Modifier.height(12.dp))
            AvatarSettingsSummary(avatar = avatar!!)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (avatar == null) {
            Button(
                onClick = onCreateAvatarClick,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Text("Avatar erstellen")
            }
        } else {
            OutlinedButton(
                onClick = { onCustomizeAvatarClick(avatar!!.type) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Avatar anpassen",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Avatar anpassen")
            }
            if (avatar!!.type == "default") {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onSwitchToCustomAvatarClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("Zu Custom Avatar wechseln")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text("Profilbild auswählen") },
            text = { Text("Möchtest du ein Bild aus der Galerie wählen oder mit der Kamera aufnehmen?") },
            confirmButton = {
                TextButton(onClick = {
                    showSourceDialog = false
                    imagePickerLauncher.launch("image/*")
                }) { Text("Galerie") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSourceDialog = false
                    val newImageUri = createImageUri(context)
                    cameraImageUri = newImageUri
                    cameraLauncher.launch(newImageUri)
                }) { Text("Kamera") }
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(
                        model = pendingImageUri,
                        contentDescription = "Vorschau Profilbild",
                        modifier = Modifier
                            .size(220.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Möchtest du dieses Bild als Profilbild verwenden?")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val uri = pendingImageUri
                    showPreviewDialog = false
                    if (uri != null) uploadProfileImage(uri)
                }) { Text("Bestätigen") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPreviewDialog = false
                    pendingImageUri = null
                }) { Text("Abbrechen") }
            }
        )
    }
}

@Composable
private fun DefaultAvatarPreview(avatar: Avatar) {
    val skinColor = when (avatar.skinColor) {
        "hell" -> Color(0xFFFFDBAC)
        "mittel" -> Color(0xFFC68642)
        "dunkel" -> Color(0xFF4A2912)
        else -> Color(0xFFFFDBAC)
    }
    val hairColor = when (avatar.hairColor) {
        "hellblond" -> Color(0xFFFFE680)
        "dunkelblond" -> Color(0xFFD4A843)
        "hellbraun" -> Color(0xFFA0674A)
        "kastanienbraun" -> Color(0xFF6B2D0E)
        "dunkelbraun" -> Color(0xFF3B1C0C)
        "schwarz" -> Color(0xFF1A1A1A)
        else -> Color(0xFFA0674A)
    }

    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(skinColor),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(hairColor)
                .align(Alignment.TopCenter)
        )
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Avatar",
            modifier = Modifier.size(60.dp),
            tint = Color.White.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun AvatarSettingsSummary(avatar: Avatar) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (!avatar.gender.isNullOrBlank()) {
                Text(
                    text = avatar.gender!!.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium
                )
            }
            if (!avatar.skinColor.isNullOrBlank()) {
                Text(
                    text = "Haut: ${avatar.skinColor}",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            if (!avatar.hairColor.isNullOrBlank()) {
                Text(
                    text = "Haar: ${avatar.hairColor}",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
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
private fun ProfileInfoItem(title: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
