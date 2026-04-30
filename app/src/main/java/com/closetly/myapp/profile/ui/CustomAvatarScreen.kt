package com.closetly.myapp.profile.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.closetly.myapp.navigation.Routes
import com.closetly.myapp.profile.viewmodel.CreateAvatarViewModel

@Composable
fun CustomAvatarScreen(
    navController: NavController,
    viewModel: CreateAvatarViewModel = viewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.resetSaved()
    }

    LaunchedEffect(isSaved) {
        if (isSaved) {
            navController.popBackStack(Routes.PROFILE, false)
        }
    }

    var frontUri by remember { mutableStateOf<Uri?>(null) }
    var sideUri by remember { mutableStateOf<Uri?>(null) }

    var cameraTargetUri by remember { mutableStateOf<Uri?>(null) }
    var cameraTarget by remember { mutableStateOf("") }

    val frontGalleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) frontUri = uri }

    val sideGalleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) sideUri = uri }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraTargetUri != null) {
            if (cameraTarget == "front") frontUri = cameraTargetUri
            else sideUri = cameraTargetUri
        }
    }

    val context = LocalContext.current

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Custom Avatar", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Lade ein Foto von vorne und von der Seite hoch",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text("Foto von vorne", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (frontUri != null) {
            AsyncImage(
                model = frontUri,
                contentDescription = "Vorderes Foto",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { frontGalleryLauncher.launch("image/*") }) {
                Text("Galerie")
            }
            OutlinedButton(onClick = { launchCamera("front") }) {
                Text("Kamera")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Foto von der Seite", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (sideUri != null) {
            AsyncImage(
                model = sideUri,
                contentDescription = "Seitliches Foto",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { sideGalleryLauncher.launch("image/*") }) {
                Text("Galerie")
            }
            OutlinedButton(onClick = { launchCamera("side") }) {
                Text("Kamera")
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

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
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Speichern & Hochladen")
            }
        }
    }
}
