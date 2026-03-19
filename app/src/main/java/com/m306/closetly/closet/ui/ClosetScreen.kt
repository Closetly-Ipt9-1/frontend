package com.m306.closetly.closet.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import com.m306.closetly.closet.func.Images

@Composable
fun ClosetScreen() {
    val images = Images()
    var isBusy by remember { mutableStateOf(false) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var uploadMessage by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        uploadMessage = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Closet", style = MaterialTheme.typography.headlineLarge)
        Text("Your wardrobe items", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(24.dp))

        if (selectedImageUri == null) {
            Button(
                onClick = {
                    launcher.launch("image/*")
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.height(0.dp))
                Text("Chose image")
            }
        } else {
            Text(
                text = "Preview",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                shape = RoundedCornerShape(16.dp)
            ) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "Preview",
                    modifier = Modifier
                        .size(250.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                enabled = !isBusy,
                onClick = {
                    isBusy = true
                    val uri = selectedImageUri
                    if (uri != null) {
                        images.uploadClothes(
                            imageUri = uri,
                            onSuccess = { url ->
                                selectedImageUri = null
                                isBusy = false

                            },
                            onError = { error ->
                                uploadMessage = "Error: ${error.message}"
                                isBusy = false

                            }
                        )
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null
                )
                Text("Accept")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                enabled = !isBusy,
                onClick = {
                    selectedImageUri = null
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = null
                )
                Text("Cancel")
            }
        }



    }
}

@Preview(showBackground = true)
@Composable
fun ClosetScreenPreview() {
    ClosetScreen()
}