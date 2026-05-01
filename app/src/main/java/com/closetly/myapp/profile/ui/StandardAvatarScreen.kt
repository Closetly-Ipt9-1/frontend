package com.closetly.myapp.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.closetly.myapp.navigation.Routes
import com.closetly.myapp.profile.viewmodel.CreateAvatarViewModel

private val skinOptions = listOf(
    "hell" to Color(0xFFFFDBAC),
    "mittel" to Color(0xFFC68642),
    "dunkel" to Color(0xFF4A2912)
)

private val hairOptions = listOf(
    "hellblond" to Color(0xFFFFE680),
    "dunkelblond" to Color(0xFFD4A843),
    "hellbraun" to Color(0xFFA0674A),
    "kastanienbraun" to Color(0xFF6B2D0E),
    "dunkelbraun" to Color(0xFF3B1C0C),
    "schwarz" to Color(0xFF1A1A1A)
)

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
    var selectedHair by remember { mutableStateOf("hellbraun") }

    LaunchedEffect(existingAvatar) {
        existingAvatar?.let { avatar ->
            avatar.gender?.let { selectedGender = it }
            avatar.skinColor?.let { selectedSkin = it }
            avatar.hairColor?.let { selectedHair = it }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("Avatar", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Passe deinen Avatar an",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text("Geschlecht", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("männlich", "weiblich").forEach { option ->
                FilterChip(
                    selected = selectedGender == option,
                    onClick = { selectedGender = option },
                    label = { Text(option) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Hautfarbe", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        ColorSwatchRow(
            options = skinOptions,
            selected = selectedSkin,
            onSelect = { selectedSkin = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("Haarfarbe", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        ColorSwatchRow(
            options = hairOptions,
            selected = selectedHair,
            onSelect = { selectedHair = it }
        )

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
private fun ColorSwatchRow(
    options: List<Pair<String, Color>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (label, color) ->
            ColorSwatch(
                color = color,
                selected = selected == label,
                onSelect = { onSelect(label) }
            )
        }
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    selected: Boolean,
    onSelect: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f)
    val borderWidth = if (selected) 3.dp else 1.dp

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .clickable { onSelect() }
    )
}
