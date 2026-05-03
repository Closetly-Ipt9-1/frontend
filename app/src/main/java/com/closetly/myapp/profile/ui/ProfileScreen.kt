package com.closetly.myapp.profile.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
    var photoUrl by remember(user?.photoUrl) { mutableStateOf(user?.photoUrl?.toString()) }
    var isUploading by remember { mutableStateOf(false) }
    var showSourceDialog by remember { mutableStateOf(false) }
    var showPreviewDialog by remember { mutableStateOf(false) }
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    val displayName = user?.displayName?.takeIf { it.isNotBlank() } ?: "Closetly User"
    val email = user?.email ?: "No email"

    fun uploadProfileImage(selectedImageUri: Uri) {
        val currentUser = auth.currentUser ?: return
        isUploading = true
        val imageRef = storage.reference.child(
            "users/${currentUser.uid}/profile_pictures/${UUID.randomUUID()}.jpg"
        )
        imageRef.putFile(selectedImageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    val updates = UserProfileChangeRequest.Builder()
                        .setPhotoUri(downloadUri)
                        .build()
                    currentUser.updateProfile(updates)
                        .addOnSuccessListener {
                            photoUrl = downloadUri.toString()
                            pendingImageUri = null
                            isUploading = false
                        }
                        .addOnFailureListener { isUploading = false }
                }.addOnFailureListener { isUploading = false }
            }
            .addOnFailureListener { isUploading = false }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { selectedImageUri ->
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
            .padding(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        ProfileHero(
            photoUrl = photoUrl,
            displayName = displayName,
            email = email,
            isUploading = isUploading,
            isPremium = subscriptionStatus.isPremium,
            onChangePhoto = { showSourceDialog = true }
        )

        ProfileStatsRow(
            isPremium = subscriptionStatus.isPremium,
            hasAvatar = avatar != null
        )

        AvatarPanel(
            avatar = avatar,
            onCreateAvatarClick = onCreateAvatarClick,
            onCustomizeAvatarClick = onCustomizeAvatarClick
        )

        ProfileActionsPanel(
            isPremium = subscriptionStatus.isPremium,
            onEditClick = onEditClick,
            onSavedOutfitsClick = onSavedOutfitsClick,
            onPremiumClick = onPremiumClick,
            onLogoutClick = onLogoutClick
        )
    }

    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text("Choose profile picture") },
            text = { Text("Choose an image from your gallery or take a new photo.") },
            confirmButton = {
                TextButton(onClick = {
                    showSourceDialog = false
                    imagePickerLauncher.launch("image/*")
                }) { Text("Gallery") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSourceDialog = false
                    val newImageUri = createImageUri(context)
                    cameraImageUri = newImageUri
                    cameraLauncher.launch(newImageUri)
                }) { Text("Camera") }
            }
        )
    }

    if (showPreviewDialog && pendingImageUri != null) {
        AlertDialog(
            onDismissRequest = {
                showPreviewDialog = false
                pendingImageUri = null
            },
            title = { Text("Confirm image") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AsyncImage(
                        model = pendingImageUri,
                        contentDescription = "Profile picture preview",
                        modifier = Modifier
                            .size(220.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Use this image as your profile picture?")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val uri = pendingImageUri
                    showPreviewDialog = false
                    if (uri != null) uploadProfileImage(uri)
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPreviewDialog = false
                    pendingImageUri = null
                }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ProfileHero(
    photoUrl: String?,
    displayName: String,
    email: String,
    isUploading: Boolean,
    isPremium: Boolean,
    onChangePhoto: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ProfilePhoto(photoUrl = photoUrl, isUploading = isUploading, onChangePhoto = onChangePhoto)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = CircleShape,
                color = if (isPremium) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                } else {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
                },
                contentColor = if (isPremium) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Text(
                    text = if (isPremium) "Premium active" else "Free Account",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun ProfilePhoto(
    photoUrl: String?,
    isUploading: Boolean,
    onChangePhoto: () -> Unit
) {
    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            modifier = Modifier
                .size(126.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background)
                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.75f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (!photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(118.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(54.dp)
                )
            }

            if (isUploading) {
                CircularProgressIndicator(modifier = Modifier.size(34.dp))
            }
        }

        Surface(
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onChangePhoto),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Change profile picture",
                modifier = Modifier
                    .padding(10.dp)
                    .size(18.dp)
            )
        }
    }
}

@Composable
private fun ProfileStatsRow(
    isPremium: Boolean,
    hasAvatar: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ProfileStatTile(
            label = "Status",
            value = if (isPremium) "Premium" else "Free",
            modifier = Modifier.weight(1f)
        )
        ProfileStatTile(
            label = "Avatar",
            value = if (hasAvatar) "Ready" else "Open",
            modifier = Modifier.weight(1f)
        )
        ProfileStatTile(
            label = "Looks",
            value = "Saved",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ProfileStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AvatarPanel(
    avatar: Avatar?,
    onCreateAvatarClick: () -> Unit,
    onCustomizeAvatarClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AvatarPreview(avatar = avatar)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (avatar == null) "Create avatar" else "Edit avatar",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (avatar == null) {
                        "Build your Closetly look for your profile."
                    } else {
                        avatar.summaryText()
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = if (avatar == null) onCreateAvatarClick else onCustomizeAvatarClick,
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(if (avatar == null) "Create now" else "Customize")
                }
            }
        }
    }
}

@Composable
private fun ProfileActionsPanel(
    isPremium: Boolean,
    onEditClick: () -> Unit,
    onSavedOutfitsClick: () -> Unit,
    onPremiumClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProfileActionTile(
                title = "Edit",
                subtitle = "Name and details",
                icon = Icons.Default.Edit,
                modifier = Modifier.weight(1f),
                onClick = onEditClick
            )
            ProfileActionTile(
                title = "Saved",
                subtitle = "View outfits",
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f),
                onClick = onSavedOutfitsClick
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProfileActionTile(
                title = if (isPremium) "Premium" else "Upgrade",
                subtitle = if (isPremium) "Manage subscription" else "More features",
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f),
                highlighted = true,
                onClick = onPremiumClick
            )
            ProfileActionTile(
                title = "Logout",
                subtitle = "Log out",
                icon = Icons.Default.ExitToApp,
                modifier = Modifier.weight(1f),
                danger = true,
                onClick = onLogoutClick
            )
        }
    }
}

@Composable
private fun ProfileActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val iconColor = when {
        danger -> MaterialTheme.colorScheme.error
        highlighted -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }

    Surface(
        modifier = modifier
            .height(118.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.16f),
                contentColor = iconColor
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(9.dp)
                        .size(19.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AvatarPreview(avatar: Avatar?) {
    val imageUrl = avatar?.imageUrl

    if (!imageUrl.isNullOrBlank()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Avatar",
            modifier = Modifier
                .size(86.dp)
                .clip(CircleShape)
        )
    } else if (avatar != null) {
        DefaultAvatarPreview(avatar = avatar, size = 86)
    } else {
        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(38.dp)
            )
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

private fun Avatar.summaryText(): String {
    val parts = listOfNotNull(
        gender?.takeIf { it.isNotBlank() }?.let { it.toAvatarLabel() },
        skinColor?.takeIf { it.isNotBlank() }?.let { "Skin: ${it.toAvatarLabel()}" },
        hairColor?.takeIf { it.isNotBlank() }?.let { "Hair: ${it.toAvatarLabel()}" }
    )
    return parts.joinToString(" · ").ifBlank { "Your avatar is ready." }
}

private fun String.toAvatarLabel(): String = when (this) {
    "männlich", "male" -> "Male"
    "weiblich", "female" -> "Female"
    "hell" -> "Light"
    "mittel" -> "Medium"
    "dunkel" -> "Dark"
    "hellblond" -> "Light blonde"
    "dunkelblond" -> "Dark blonde"
    "hellbraun" -> "Light brown"
    "kastanienbraun" -> "Chestnut brown"
    "dunkelbraun" -> "Dark brown"
    "schwarz" -> "Black"
    else -> replaceFirstChar { it.uppercase() }
}

private fun createImageUri(context: Context): Uri {
    val imageFile = File.createTempFile(
        "profile_picture_${System.currentTimeMillis()}",
        ".jpg",
        context.cacheDir
    )
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)
}
