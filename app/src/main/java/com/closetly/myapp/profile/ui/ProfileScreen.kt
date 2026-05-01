package com.closetly.myapp.profile.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.closetly.myapp.premium.data.PremiumAccessRepository
import com.closetly.myapp.premium.model.SubscriptionStatus
import com.closetly.myapp.profile.model.Avatar
import com.closetly.myapp.profile.viewmodel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.util.UUID

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onEditClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onSavedOutfitsClick: () -> Unit,
    onCreateAvatarClick: () -> Unit,
    onCustomizeAvatarClick: () -> Unit,
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
                        .setPhotoUri(downloadUri)
                        .build()
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
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Profil",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        ProfileHeroCard(
            photoUrl = photoUrl,
            displayName = displayName,
            email = email,
            isUploading = isUploading,
            onChangePhoto = { showSourceDialog = true }
        )

        AccountCard(displayName = displayName, email = email)

        Button(
            onClick = onPremiumClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text(if (subscriptionStatus.isPremium) "Abo verwalten" else "Premium freischalten")
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfileActionButton(
                    text = "Profil bearbeiten",
                    icon = Icons.Default.Edit,
                    onClick = onEditClick
                )
                ProfileActionButton(
                    text = "Gespeicherte Outfits",
                    icon = null,
                    onClick = onSavedOutfitsClick
                )
                ProfileActionButton(
                    text = "Ausloggen",
                    icon = Icons.Default.ExitToApp,
                    onClick = onLogoutClick
                )
            }
        }

        AvatarCard(
            avatar = avatar,
            onCreateAvatarClick = onCreateAvatarClick,
            onCustomizeAvatarClick = onCustomizeAvatarClick
        )

        Spacer(modifier = Modifier.height(8.dp))
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
private fun ProfileHeroCard(
    photoUrl: String?,
    displayName: String,
    email: String,
    isUploading: Boolean,
    onChangePhoto: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "Profilbild",
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Standard-Profilbild",
                        modifier = Modifier.size(54.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

            if (isUploading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }

            OutlinedButton(
                onClick = onChangePhoto,
                shape = MaterialTheme.shapes.large
            ) {
                Text("Profilbild ändern")
            }
        }
    }
}

@Composable
private fun AccountCard(displayName: String, email: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Account",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            ProfileInfoItem(title = "Anzeigename", value = displayName)
            HorizontalDivider()
            ProfileInfoItem(title = "E-Mail", value = email)
        }
    }
}

@Composable
private fun ProfileActionButton(
    text: String,
    icon: ImageVector?,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AvatarCard(
    avatar: Avatar?,
    onCreateAvatarClick: () -> Unit,
    onCustomizeAvatarClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Avatar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AvatarPreview(avatar = avatar)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (avatar == null) "Noch kein Avatar" else "Dein Avatar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (avatar == null) {
                            "Erstelle einen Look für dein Profil."
                        } else {
                            "Passe deinen Avatar jederzeit an."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (avatar != null) {
                AvatarSettingsSummary(avatar = avatar)
            }

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
                    onClick = onCustomizeAvatarClick,
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
            }
        }
    }
}

@Composable
private fun AvatarPreview(avatar: Avatar?) {
    when {
        avatar != null && !avatar.imageUrl.isNullOrBlank() -> {
            AsyncImage(
                model = avatar.imageUrl,
                contentDescription = "Standard Avatar",
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
            )
        }
        avatar != null -> {
            DefaultAvatarPreview(avatar = avatar, size = 84)
        }
        else -> {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Kein Avatar",
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DefaultAvatarPreview(avatar: Avatar, size: Int = 120) {
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
            .size(size.dp)
            .clip(CircleShape)
            .background(skinColor),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((size / 4).dp)
                .background(hairColor)
                .align(Alignment.TopCenter)
        )
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Avatar",
            modifier = Modifier.size((size / 2).dp),
            tint = Color.White.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun AvatarSettingsSummary(avatar: Avatar) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (!avatar.gender.isNullOrBlank()) {
                Text(
                    text = avatar.gender.replaceFirstChar { it.uppercase() },
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
