package com.m306.closetly.avatar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.m306.closetly.R
import com.m306.closetly.avatar.AvatarConfig

// -----------------------------
// DATA MODEL
// -----------------------------
data class AvatarConfig(
    val skin: String = "light",
    val hair: String = "short_black",
    val eyes: String = "default",
    val mouth: String = "smile"
)

// -----------------------------
// RESOURCE MAPPING
// -----------------------------
fun getSkinRes(skin: String): Int {
    return when (skin) {
        "light" -> R.drawable.skin_light
        "dark" -> R.drawable.skin_dark
        else -> R.drawable.skin_light
    }
}

fun getHairRes(hair: String): Int {
    return when (hair) {
        "short_black" -> R.drawable.hair_short_black
        "long_blonde" -> R.drawable.hair_long_blonde
        else -> R.drawable.hair_short_black
    }
}

fun getEyesRes(eyes: String): Int {
    return when (eyes) {
        "default" -> R.drawable.eyes_default
        else -> R.drawable.eyes_default
    }
}

fun getMouthRes(mouth: String): Int {
    return when (mouth) {
        "smile" -> R.drawable.mouth_smile
        else -> R.drawable.mouth_smile
    }
}

// -----------------------------
// AVATAR VIEW (RENDER)
// -----------------------------
@Composable
fun AvatarView(
    config: AvatarConfig,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(Color(0xFFF5F5F5))
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
    ) {

        // Reihenfolge wichtig!
        Image(
            painter = painterResource(getSkinRes(config.skin)),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )

        Image(
            painter = painterResource(getEyesRes(config.eyes)),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )

        Image(
            painter = painterResource(getMouthRes(config.mouth)),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )

        Image(
            painter = painterResource(getHairRes(config.hair)),
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    }
}

// -----------------------------
// AVATAR EDITOR
// -----------------------------
@Composable
fun AvatarEditor(
    initialConfig: AvatarConfig = AvatarConfig(),
    onSave: (AvatarConfig) -> Unit
) {
    var config by remember { mutableStateOf(initialConfig) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Preview
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AvatarView(config)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // SKIN
        Text("Skin", style = MaterialTheme.typography.titleMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("light", "dark").forEach { skin ->
                OptionButton(
                    text = skin,
                    selected = config.skin == skin
                ) {
                    config = config.copy(skin = skin)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // HAIR
        Text("Hair", style = MaterialTheme.typography.titleMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("short_black", "long_blonde").forEach { hair ->
                OptionButton(
                    text = hair,
                    selected = config.hair == hair
                ) {
                    config = config.copy(hair = hair)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // MOUTH
        Text("Mouth", style = MaterialTheme.typography.titleMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("smile").forEach { mouth ->
                OptionButton(
                    text = mouth,
                    selected = config.mouth == mouth
                ) {
                    config = config.copy(mouth = mouth)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onSave(config) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Avatar")
        }
    }
}

// -----------------------------
// REUSABLE BUTTON
// -----------------------------
@Composable
fun OptionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) Color.Black else Color.LightGray)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color.Black
        )
    }
}