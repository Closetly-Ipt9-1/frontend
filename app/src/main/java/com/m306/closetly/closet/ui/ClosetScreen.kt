package com.m306.closetly.closet.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.m306.closetly.ai.AdEngine
import com.m306.closetly.ai.DailyOutfitManager
import com.m306.closetly.closet.func.Images
import com.m306.closetly.data.ClothingItem
import com.m306.closetly.data.ClothingRepository

@Composable
fun ClosetScreen() {
    val repository = remember { ClothingRepository() }
    val images = remember { Images() }

    var clothes by remember { mutableStateOf<List<ClothingItem>>(emptyList()) }
    var showAd by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var uploadMessage by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        uploadMessage = null
    }

    // Load clothes and generate daily outfit on first composition
    LaunchedEffect(Unit) {
        clothes = repository.getUserClothes()
        if (clothes.isNotEmpty()) {
            showAd = true
        }
        DailyOutfitManager().generateTodayOutfit()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ClosetHeader()

        Spacer(modifier = Modifier.height(24.dp))

        if (selectedImageUri == null) {
            ImagePickerButton(onClick = { imagePicker.launch("image/*") })
        } else {
            ImagePreviewSection(
                imageUri = selectedImageUri!!,
                isBusy = isBusy,
                onAccept = {
                    isBusy = true
                    images.uploadClothes(
                        imageUri = selectedImageUri!!,
                        onSuccess = {
                            selectedImageUri = null
                            isBusy = false
                        },
                        onError = { error ->
                            uploadMessage = "Error: ${error.message}"
                            isBusy = false
                        }
                    )
                },
                onCancel = { selectedImageUri = null }
            )
        }

        uploadMessage?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }

    // Style tip popup
    if (showAd) {
        StyleTipDialog(
            text = AdEngine.generateAd(clothes),
            onDismiss = { showAd = false }
        )
    }
}

// --- Extracted Components ---

@Composable
private fun ClosetHeader() {
    Text("Closet", style = MaterialTheme.typography.headlineLarge)
    Text("Your wardrobe items", style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun ImagePickerButton(onClick: () -> Unit) {
    Button(onClick = onClick) {
        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Choose image")
    }
}

@Composable
private fun ImagePreviewSection(
    imageUri: Uri,
    isBusy: Boolean,
    onAccept: () -> Unit,
    onCancel: () -> Unit
) {
    Text(text = "Preview", style = MaterialTheme.typography.titleMedium)

    Spacer(modifier = Modifier.height(12.dp))

    Card(shape = RoundedCornerShape(16.dp)) {
        AsyncImage(
            model = imageUri,
            contentDescription = "Selected clothing preview",
            modifier = Modifier
                .size(250.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    Button(onClick = onAccept, enabled = !isBusy) {
        Icon(imageVector = Icons.Filled.Check, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Accept")
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedButton(onClick = onCancel, enabled = !isBusy) {
        Icon(imageVector = Icons.Filled.Close, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Cancel")
    }
}

@Composable
private fun StyleTipDialog(text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Style Tip") },
        text = { Text(text) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun ClosetScreenPreview() {
    ClosetScreen()
}
