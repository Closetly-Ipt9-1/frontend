package com.closetly.myapp.fitcreator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.closetly.myapp.auth.func.AuthManager
import com.closetly.myapp.closet.data.ClosetRepository
import com.closetly.myapp.closet.model.ClothingItemUi
import com.closetly.myapp.fitcreator.model.Outfit

@Composable
fun FitCreatorScreen() {
    val viewModel = remember { FitCreatorViewModel() }
    val closetRepository = remember { ClosetRepository() }
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Create outfit") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("My Outfits") }
            )
        }

        when (selectedTab) {
            0 -> CreateOutfitTab(viewModel, closetRepository)
            1 -> MyOutfitsTab(viewModel)
        }
    }
}

@Composable
private fun CreateOutfitTab(
    viewModel: FitCreatorViewModel,
    closetRepository: ClosetRepository
) {
    val selectedItems by viewModel.selectedItems.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var clothingItems by remember { mutableStateOf<List<ClothingItemUi>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var caption by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var isFetchingItems by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        closetRepository.getClothingItems(
            onSuccess = { items ->
                clothingItems = items
                isFetchingItems = false
            },
            onError = {
                isFetchingItems = false
            }
        )
        onDispose {}
    }

    if (showSaveDialog) {
        SaveOutfitDialog(
            caption = caption,
            isPublic = isPublic,
            isLoading = isLoading,
            onCaptionChange = { caption = it },
            onPublicChange = { isPublic = it },
            onSave = {
                val username = AuthManager.getCurrentUser()?.displayName ?: "Unknown"
                viewModel.saveOutfit(
                    caption = caption,
                    imageUrl = selectedItems.firstOrNull()?.imageUrl ?: "",
                    isPublic = isPublic,
                    username = username,
                    onSuccess = {
                        showSaveDialog = false
                        caption = ""
                        isPublic = false
                    },
                    onError = {}
                )
            },
            onDismiss = { showSaveDialog = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Selected items: (${selectedItems.size})",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        if (selectedItems.isNotEmpty()) {
            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    userScrollEnabled = true
                ) {
                    items(selectedItems) { item ->
                        SelectedItemCard(
                            item = item,
                            onRemove = { viewModel.removeItem(item.id) }
                        )
                    }
                }
            }

            item {
                if (!viewModel.validateOutfit()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp)),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "Please select at least one top and one bottom item.",
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                } else {
                    Button(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Outfit")
                    }
                }
            }
        }

        if (errorMessage != null) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = errorMessage ?: "",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        item {
            Text(
                text = "Categories",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        if (isFetchingItems) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        } else {
            val categories = clothingItems.map { it.category }.distinct()

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categories.forEach { category ->
                        CategoryButton(
                            category = category,
                            isSelected = selectedCategory == category,
                            onClick = {
                                selectedCategory =
                                    if (selectedCategory == category) null else category
                            }
                        )
                    }
                }
            }
        }

        if (selectedCategory != null) {
            val itemsInCategory = clothingItems.filter { it.category == selectedCategory }

            item {
                Text(
                    text = "Items in $selectedCategory",
                    style = MaterialTheme.typography.titleSmall
                )
            }

            if (itemsInCategory.isEmpty()) {
                item {
                    Text(
                        text = "No items found in this category.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        userScrollEnabled = true
                    ) {
                        items(itemsInCategory) { item ->
                            ClothingItemCard(
                                item = item,
                                isSelected = selectedItems.any { it.id == item.id },
                                onSelect = { viewModel.addItem(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyOutfitsTab(viewModel: FitCreatorViewModel) {
    val outfits by viewModel.outfits.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    DisposableEffect(Unit) {
        viewModel.loadMyOutfits()
        onDispose {}
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (outfits.isEmpty()) {
            item {
                Text(
                    text = "You haven't created any outfits yet. Start by creating your first outfit!",
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        items(outfits) { outfit ->
            OutfitCard(
                outfit = outfit,
                onToggleVisibility = { viewModel.toggleOutfitVisibility(outfit.id, !outfit.isPublic) },
                onDelete = { viewModel.deleteOutfit(outfit.id) }
            )
        }
    }
}

@Composable
private fun ClothingItemCard(
    item: ClothingItemUi,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.category,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.category,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )

            if (!item.color.isNullOrBlank()) {
                Text(
                    text = "Color: ${item.color}",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun SelectedItemCard(
    item: ClothingItemUi,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        Box {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.category,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun OutfitCard(
    outfit: Outfit,
    onToggleVisibility: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = outfit.caption.ifBlank { "Outfit" },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (outfit.isPublic) {
                        Text(
                            text = "Public",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Switch(
                        checked = outfit.isPublic,
                        onCheckedChange = { onToggleVisibility() }
                    )
                }
            }

            Text(
                text = "${outfit.clothingItems.size} Items",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = true
            ) {
                items(outfit.clothingItems) { item ->
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.category,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Button(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier
                        .size(18.dp)
                        .padding(end = 4.dp)
                )
                Text("Delete")
            }
        }
    }
}

@Composable
private fun CategoryButton(
    category: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    ) {
        Text(category)
    }
}

@Composable
private fun SaveOutfitDialog(
    caption: String,
    isPublic: Boolean,
    isLoading: Boolean,
    onCaptionChange: (String) -> Unit,
    onPublicChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
            .clickable { onDismiss() },
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .clickable(enabled = false) {}
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Save outfit",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    TextField(
                        value = caption,
                        onValueChange = onCaptionChange,
                        label = { Text("Caption (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Make outfit public")
                        Switch(
                            checked = isPublic,
                            onCheckedChange = onPublicChange
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            enabled = !isLoading
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = onSave,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            } else {
                                Text("Save")
                            }
                        }
                    }
                }
            }
        }
    }
}