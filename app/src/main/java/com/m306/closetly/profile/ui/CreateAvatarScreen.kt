package com.m306.closetly.profile.ui


import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.m306.closetly.profile.viewmodel.CreateAvatarViewModel

@Composable
fun CreateAvatarScreen(
    navController: NavController,
    viewModel: CreateAvatarViewModel = viewModel()
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        Text("Create Avatar", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            viewModel.saveDefaultAvatar(
                hair = "brown",
                skin = "light",
                imageUrl = "https://example.com/avatar.png"
            )
            navController.popBackStack()
        }) {
            Text("Standard Avatar wählen")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            navController.navigate("custom_avatar")
        }) {
            Text("Custom Avatar erstellen")
        }
    }
}