package com.m306.closetly.profile.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.m306.closetly.profile.viewmodel.CreateAvatarViewModel

@Composable
fun CustomAvatarScreen(
    navController: NavController,
    viewModel: CreateAvatarViewModel = viewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()

    // Nach erfolgreichem Speichern zurücknavigieren
    LaunchedEffect(isSaved) {
        if (isSaved) navController.popBackStack()
    }

    var frontUri by remember { mutableStateOf<Uri?>(null) }
    var sideUri by remember { mutableStateOf<Uri?>(null) }

    // Für Kamera brauchen wir einen temporären URI
    var cameraTargetUri by remember { mutableStateOf<Uri?>(null) }
    var cameraTarget by remember { mutableStateOf("") } // "front" oder "side"

    // Galerie Launcher
    val frontGalleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) frontUri = uri }

    val sideGalleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) sideUri = uri }

    // Kamera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraTargetUri != null) {
            if (cameraTarget == "front") frontUri = cameraTargetUri
            else sideUri = cameraTargetUri
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    fun launchCamera(target: String) {
        val tmpFile = java.io.File.createTempFile("avatar_$target", ".jpg", context.cacheDir)
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            tmpFile
        )
        cameraTargetUri = uri
        cameraTarget = target
        cameraLauncher.launch(uri)
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(24.dp)
    ) {
        Text("Custom Avatar", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        // --- Front Bild ---
        Text("Front-Foto", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { frontGalleryLauncher.launch("image/*") }) {
                Text("Galerie")
            }
            Button(onClick = { launchCamera("front") }) {
                Text("Kamera")
            }
        }
        if (frontUri != null) Text("✓ Front Bild ausgewählt", color = MaterialTheme.colorScheme.primary)

        Spacer(modifier = Modifier.height(24.dp))

        // --- Seiten Bild ---
        Text("Seiten-Foto", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { sideGalleryLauncher.launch("image/*") }) {
                Text("Galerie")
            }
            Button(onClick = { launchCamera("side") }) {
                Text("Kamera")
            }
        }
        if (sideUri != null) Text("✓ Seiten Bild ausgewählt", color = MaterialTheme.colorScheme.primary)

        Spacer(modifier = Modifier.height(32.dp))

        // --- Speichern ---
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                enabled = frontUri != null && sideUri != null,
                onClick = {
                    viewModel.saveCustomAvatar(
                        frontUri = frontUri!!,
                        sideUri = sideUri!!
                    )
                }
            ) {
                Text("Speichern & Hochladen")
            }
        }
    }
}