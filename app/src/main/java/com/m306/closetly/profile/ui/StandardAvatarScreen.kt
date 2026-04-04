package com.m306.closetly.profile.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.m306.closetly.navigation.Routes
import com.m306.closetly.profile.viewmodel.CreateAvatarViewModel

@Composable
fun StandardAvatarScreen(
    navController: NavController,
    viewModel: CreateAvatarViewModel = viewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val existingAvatar by viewModel.existingAvatar.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.resetSaved()
        viewModel.loadExistingAvatar()
    }

    LaunchedEffect(isSaved) {
        if (isSaved) {
            navController.popBackStack(Routes.PROFILE, false)
        }
    }

    var selectedGender by remember { mutableStateOf("männlich") }
    var selectedSkin by remember { mutableStateOf("hell") }
    var selectedHair by remember { mutableStateOf("braun") }

    LaunchedEffect(existingAvatar) {
        existingAvatar?.let { avatar ->
            avatar.gender?.let { selectedGender = it }
            avatar.skinColor?.let { selectedSkin = it }
            avatar.hairColor?.let { selectedHair = it }
        }
    }

    val genderOptions = listOf("männlich", "weiblich", "divers")
    val skinOptions = listOf("hell", "mittel", "dunkel")
    val hairOptions = listOf("blond", "braun", "schwarz", "rot", "grau")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Standard Avatar", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Passe deinen Avatar an",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text("Geschlecht", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        AvatarOptionRow(
            options = genderOptions,
            selected = selectedGender,
            onSelect = { selectedGender = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("Hautfarbe", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        AvatarOptionRow(
            options = skinOptions,
            selected = selectedSkin,
            onSelect = { selectedSkin = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("Haarfarbe", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            hairOptions.forEach { option ->
                FilterChip(
                    selected = selectedHair == option,
                    onClick = { selectedHair = option },
                    label = { Text(option) }
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    viewModel.saveDefaultAvatar(
                        gender = selectedGender,
                        hair = selectedHair,
                        skin = selectedSkin
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Speichern")
            }
        }
    }
}

@Composable
private fun AvatarOptionRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(option) }
            )
        }
    }
}
