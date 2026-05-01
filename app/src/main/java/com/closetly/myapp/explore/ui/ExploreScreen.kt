package com.closetly.myapp.explore.ui

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.closetly.myapp.auth.func.AuthManager

data class ExploreOutfit(
    val id: String,
    val ownerId: String,
    val username: String,
    val caption: String,
    val imageUrl: String,
    val isPublic: Boolean,
    val likeCount: Int,
    val likedBy: List<String>,
    val saveCount: Int,
    val savedBy: List<String>,
    val createdAt: Long
)

data class OutfitComment(
    val id: String,
    val userId: String,
    val username: String,
    val text: String,
    val likeCount: Int,
    val likedBy: List<String>,
    val createdAt: Long
)

@Composable
fun ExploreScreen() {
    val firestore = remember { FirebaseFirestore.getInstance() }
    val currentUserId = AuthManager.getCurrentUserId()
    val currentUsername = AuthManager.getCurrentUser()?.displayName ?: "Anonymous"

    var outfits by remember { mutableStateOf<List<ExploreOutfit>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        seedPlaceholderOutfitsIfNeeded(firestore)
    }

    DisposableEffect(Unit) {
        val listener = firestore
            .collection("outfits")
            .whereEqualTo("isPublic", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    errorText = error.message
                    isLoading = false
                    return@addSnapshotListener
                }

                val loadedOutfits = snapshot
                    ?.documents
                    ?.mapNotNull { document ->
                        ExploreOutfit(
                            id = document.id,
                            ownerId = document.getString("ownerId") ?: "",
                            username = document.getString("username") ?: "Unknown",
                            caption = document.getString("caption") ?: "",
                            imageUrl = document.getString("imageUrl") ?: "",
                            isPublic = document.getBoolean("isPublic") ?: false,
                            likeCount = document.getLong("likeCount")?.toInt() ?: 0,
                            likedBy = (document.get("likedBy") as? List<*>)?.filterIsInstance<String>().orEmpty(),
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
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No public outfits yet",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        else -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 4.dp)
                            .clip(MaterialTheme.shapes.extraLarge)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surface,
                                        MaterialTheme.colorScheme.surfaceVariant
                                    )
                                )
                            )
                            .padding(22.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Explore",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Discover public outfits",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(outfits, key = { it.id }) { outfit ->
                    ExploreOutfitCard(
                        outfit = outfit,
                        currentUserId = currentUserId,
                        currentUsername = currentUsername,
                        firestore = firestore,
                        onLikeClick = {
                            if (currentUserId != null) {
                                toggleLike(
                                    firestore = firestore,
                                    outfitId = outfit.id,
                                    userId = currentUserId
                                )
                            }
                        },
                        onSaveClick = {
                            if (currentUserId != null) {
                                toggleSave(
                                    firestore = firestore,
                                    outfitId = outfit.id,
                                    userId = currentUserId
                                )
                            }
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
fun ExploreOutfitCard(
    outfit: ExploreOutfit,
    currentUserId: String?,
    currentUsername: String,
    firestore: FirebaseFirestore,
    onLikeClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    val isLiked = currentUserId != null && outfit.likedBy.contains(currentUserId)
    val isSaved = currentUserId != null && outfit.savedBy.contains(currentUserId)

    var commentsExpanded by remember { mutableStateOf(false) }
    var comments by remember { mutableStateOf<List<OutfitComment>>(emptyList()) }
    var commentText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    DisposableEffect(outfit.id) {
        val listener = firestore
            .collection("outfits")
            .document(outfit.id)
            .collection("comments")
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, _ ->
                comments = snapshot?.documents?.mapNotNull { doc ->
                    OutfitComment(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        username = doc.getString("username") ?: "Unknown",
                        text = doc.getString("text") ?: "",
                        likeCount = doc.getLong("likeCount")?.toInt() ?: 0,
                        likedBy = (doc.get("likedBy") as? List<*>)?.filterIsInstance<String>().orEmpty(),
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                }.orEmpty()
            }
        onDispose { listener.remove() }
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
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
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (outfit.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = outfit.imageUrl,
                        contentDescription = outfit.caption.ifBlank { "Outfit" },
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
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
                    }
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
                        onClick = onLikeClick
                    ) {
                        Icon(
                            imageVector = if (isLiked) {
                                Icons.Default.Favorite
                            } else {
                                Icons.Default.FavoriteBorder
                            },
                            contentDescription = "Like",
                            modifier = Modifier.size(28.dp),
                            tint = if (isLiked) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    Text(
                        text = "${outfit.likeCount} likes",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = onSaveClick
                    ) {
                        Icon(
                            imageVector = if (isSaved) {
                                Icons.Default.Check
                            } else {
                                Icons.Default.CheckCircle
                            },
                            contentDescription = "Save",
                            modifier = Modifier.size(28.dp),
                            tint = if (isSaved) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    Text(
                        text = "${outfit.saveCount} saves",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (comments.isEmpty()) "Kommentare" else "${comments.size} Kommentar${if (comments.size != 1) "e" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { commentsExpanded = !commentsExpanded }) {
                    Icon(
                        imageVector = if (commentsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (commentsExpanded) "Ausblenden" else "Anzeigen"
                    )
                }
            }

            if (commentsExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                ) {
                    if (comments.isEmpty()) {
                        Text(
                            text = "Noch keine Kommentare. Sei der Erste!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        comments.forEach { comment ->
                            CommentItem(
                                comment = comment,
                                currentUserId = currentUserId,
                                onLikeClick = {
                                    if (currentUserId != null) {
                                        toggleCommentLike(firestore, outfit.id, comment.id, currentUserId)
                                    }
                                }
                            )
                        }
                    }

                    if (currentUserId != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            OutlinedTextField(
                                value = commentText,
                                onValueChange = { commentText = it },
                                placeholder = { Text("Kommentar schreiben...") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (commentText.isNotBlank()) {
                                            addComment(firestore, outfit.id, currentUserId, currentUsername, commentText.trim())
                                            commentText = ""
                                            keyboardController?.hide()
                                        }
                                    }
                                ),
                                maxLines = 3,
                                shape = RoundedCornerShape(16.dp)
                            )
                            IconButton(
                                onClick = {
                                    if (commentText.isNotBlank()) {
                                        addComment(firestore, outfit.id, currentUserId, currentUsername, commentText.trim())
                                        commentText = ""
                                        keyboardController?.hide()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Senden",
                                    tint = if (commentText.isNotBlank()) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentItem(
    comment: OutfitComment,
    currentUserId: String?,
    onLikeClick: () -> Unit
) {
    val isLiked = currentUserId != null && comment.likedBy.contains(currentUserId)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = comment.username,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (comment.likeCount > 0) {
                Text(
                    text = "${comment.likeCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onLikeClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like Kommentar",
                    modifier = Modifier.size(16.dp),
                    tint = if (isLiked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

private fun seedPlaceholderOutfitsIfNeeded(firestore: FirebaseFirestore) {
    firestore.collection("outfits")
        .limit(1)
        .get()
        .addOnSuccessListener { result ->
            if (!result.isEmpty) return@addOnSuccessListener

            val batch = firestore.batch()

            val first = firestore.collection("outfits").document()
            batch.set(
                first,
                mapOf(
                    "ownerId" to "placeholder-user-1",
                    "username" to "Closetly",
                    "caption" to "Minimal black and white placeholder outfit",
                    "imageUrl" to "",
                    "isPublic" to true,
                    "likeCount" to 0,
                    "likedBy" to emptyList<String>(),
                    "saveCount" to 0,
                    "savedBy" to emptyList<String>(),
                    "createdAt" to System.currentTimeMillis()
                )
            )

            val second = firestore.collection("outfits").document()
            batch.set(
                second,
                mapOf(
                    "ownerId" to "placeholder-user-2",
                    "username" to "Closetly",
                    "caption" to "Streetwear placeholder outfit",
                    "imageUrl" to "",
                    "isPublic" to true,
                    "likeCount" to 0,
                    "likedBy" to emptyList<String>(),
                    "saveCount" to 0,
                    "savedBy" to emptyList<String>(),
                    "createdAt" to System.currentTimeMillis() - 1
                )
            )

            batch.commit()
        }
}

private fun toggleLike(
    firestore: FirebaseFirestore,
    outfitId: String,
    userId: String
) {
    val docRef = firestore.collection("outfits").document(outfitId)

    firestore.runTransaction { transaction ->
        val snapshot = transaction.get(docRef)
        val likedBy = (snapshot.get("likedBy") as? List<*>)?.filterIsInstance<String>()?.toMutableList()
            ?: mutableListOf()
        var likeCount = snapshot.getLong("likeCount")?.toInt() ?: 0

        if (likedBy.contains(userId)) {
            likedBy.remove(userId)
            if (likeCount > 0) {
                likeCount -= 1
            }
        } else {
            likedBy.add(userId)
            likeCount += 1
        }

        transaction.update(
            docRef,
            mapOf(
                "likedBy" to likedBy,
                "likeCount" to likeCount
            )
        )
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

private fun addComment(
    firestore: FirebaseFirestore,
    outfitId: String,
    userId: String,
    username: String,
    text: String
) {
    firestore
        .collection("outfits")
        .document(outfitId)
        .collection("comments")
        .add(
            mapOf(
                "userId" to userId,
                "username" to username,
                "text" to text,
                "likeCount" to 0,
                "likedBy" to emptyList<String>(),
                "createdAt" to System.currentTimeMillis()
            )
        )
}

private fun toggleCommentLike(
    firestore: FirebaseFirestore,
    outfitId: String,
    commentId: String,
    userId: String
) {
    val docRef = firestore
        .collection("outfits")
        .document(outfitId)
        .collection("comments")
        .document(commentId)

    firestore.runTransaction { transaction ->
        val snapshot = transaction.get(docRef)
        val likedBy = (snapshot.get("likedBy") as? List<*>)?.filterIsInstance<String>()?.toMutableList()
            ?: mutableListOf()
        var likeCount = snapshot.getLong("likeCount")?.toInt() ?: 0

        if (likedBy.contains(userId)) {
            likedBy.remove(userId)
            if (likeCount > 0) likeCount -= 1
        } else {
            likedBy.add(userId)
            likeCount += 1
        }

        transaction.update(
            docRef,
            mapOf(
                "likedBy" to likedBy,
                "likeCount" to likeCount
            )
        )
    }
}
