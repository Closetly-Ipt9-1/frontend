package com.closetly.myapp.profile.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.closetly.myapp.auth.func.AuthManager

data class SavedOutfit(
    val id: String,
    val ownerId: String,
    val username: String,
    val caption: String,
    val imageUrl: String,
    val isPublic: Boolean,
    val saveCount: Int,
    val savedBy: List<String>,
    val createdAt: Long
)

@Composable
fun SavedOutfitsScreen(onBack: () -> Unit = {}) {
    val firestore = remember { FirebaseFirestore.getInstance() }
    val currentUserId = AuthManager.getCurrentUserId()

    var outfits by remember { mutableStateOf<List<SavedOutfit>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }

    if (currentUserId == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "You are not logged in",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    DisposableEffect(Unit) {
        val listener = firestore
            .collection("outfits")
            .whereArrayContains("savedBy", currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    errorText = error.message
                    isLoading = false
                    return@addSnapshotListener
                }

                val loadedOutfits = snapshot
                    ?.documents
                    ?.mapNotNull { document ->
                        SavedOutfit(
                            id = document.id,
                            ownerId = document.getString("ownerId") ?: "",
                            username = document.getString("username") ?: "Unknown",
                            caption = document.getString("caption") ?: "",
                            imageUrl = document.getString("imageUrl") ?: "",
                            isPublic = document.getBoolean("isPublic") ?: false,
                            saveCount = document.getLong("saveCount")?.toInt() ?: 0,
                            savedBy = (document.get("savedBy") as? List<*>)?.filterIsInstance<String>().orEmpty(),
                            createdAt = document.getLong("createdAt") ?: 0L
                        )
                    }
                    ?.sortedByDescending { it.createdAt }
                    .orEmpty()

                outfits = loadedOutfits
                isLoading = false
                errorText = null
            }

        onDispose {
            listener.remove()
        }
    }

    when {
        isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        errorText != null -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = errorText ?: "Something went wrong",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        outfits.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No saved outfits yet",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        else -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Saved Outfits",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your saved outfit collection",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(outfits, key = { it.id }) { outfit ->
                    SavedOutfitCard(
                        outfit = outfit,
                        onUnsaveClick = {
                            toggleSave(
                                firestore = firestore,
                                outfitId = outfit.id,
                                userId = currentUserId
                            )
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun SavedOutfitCard(
    outfit: SavedOutfit,
    onUnsaveClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    Text(
                        text = outfit.username,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = if (outfit.isPublic) "Public outfit" else "Private outfit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Outfit",
                        modifier = Modifier.size(42.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Outfit preview",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Real image comes later",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = outfit.caption,
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.height(12.dp))

                HorizontalDivider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onUnsaveClick
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Unsave",
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "${outfit.saveCount} saves",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

private fun toggleSave(
    firestore: FirebaseFirestore,
    outfitId: String,
    userId: String
) {
    val docRef = firestore.collection("outfits").document(outfitId)

    firestore.runTransaction { transaction ->
        val snapshot = transaction.get(docRef)
        val savedBy = (snapshot.get("savedBy") as? List<*>)?.filterIsInstance<String>()?.toMutableList()
            ?: mutableListOf()
        var saveCount = snapshot.getLong("saveCount")?.toInt() ?: 0

        if (savedBy.contains(userId)) {
            savedBy.remove(userId)
            if (saveCount > 0) {
                saveCount -= 1
            }
        } else {
            savedBy.add(userId)
            saveCount += 1
        }

        transaction.update(
            docRef,
            mapOf(
                "savedBy" to savedBy,
                "saveCount" to saveCount
            )
        )
    }
}