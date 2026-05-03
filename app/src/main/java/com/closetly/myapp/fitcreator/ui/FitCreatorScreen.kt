package com.closetly.myapp.fitcreator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.closetly.myapp.auth.func.AuthManager
import com.closetly.myapp.closet.data.ClosetRepository
import com.closetly.myapp.closet.model.ClothingItemUi
import com.closetly.myapp.fitcreator.model.Outfit
import com.closetly.myapp.tags.model.PredefinedTags
import com.closetly.myapp.tags.model.Tag
import com.m306.closetly.ai.AiEngine
import com.m306.closetly.ai.GeneratedOutfit
import com.m306.closetly.ai.SeasonEngine

@Composable
fun FitCreatorScreen() {
    val viewModel: FitCreatorViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val closetRepository = remember { ClosetRepository() }
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(start = 16.dp, top = 18.dp, end = 16.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Outfits erstellen",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Kombiniere deine Kleidung digital und speichere fertige Looks.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Erstellen") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Generator") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Vorschläge") })
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Meine Outfits") })
        }

        when (selectedTab) {
            0 -> CreateOutfitTab(viewModel, closetRepository)
            1 -> DailyOutfitTab(viewModel, onUseOutfit = {
                viewModel.loadSuggestionIntoCreator(it)
                selectedTab = 0
            })
            2 -> SuggestionsTab(viewModel, onUseOutfit = {
                viewModel.loadSuggestionIntoCreator(it)
                selectedTab = 0
            })
            3 -> MyOutfitsTab(viewModel)
        }
    }
}

// ── Daily Outfit Tab ──────────────────────────────────────────────────────────

@Composable
private fun DailyOutfitTab(
    viewModel: FitCreatorViewModel,
    onUseOutfit: (GeneratedOutfit) -> Unit
) {
    val dailyOutfit by viewModel.dailyOutfit.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()
    val aiMessage by viewModel.aiMessage.collectAsState()
    val season = remember { SeasonEngine.currentSeason() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Header in DailyOutfitTab — ersetze den Card-Inhalt:
                    Text(
                        text = "Outfit Generator",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${season.displayName()} · ${SeasonEngine.seasonTip(season)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        if (aiLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (dailyOutfit != null) {
            item {
                GeneratedOutfitCard(
                    outfit = dailyOutfit!!,
                    onUseOutfit = { onUseOutfit(dailyOutfit!!) }
                )
            }
            item {
                OutlinedButton(
                    onClick = { viewModel.regenerateDailyOutfit() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Neues Outfit generieren")
                }
            }
        } else {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = aiMessage ?: "Füge mindestens ein Oberteil und eine Hose hinzu um ein Outfit zu generieren.",
                        modifier = Modifier.padding(18.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── Suggestions Tab ───────────────────────────────────────────────────────────

@Composable
private fun SuggestionsTab(
    viewModel: FitCreatorViewModel,
    onUseOutfit: (GeneratedOutfit) -> Unit
) {
    val suggestions by viewModel.outfitSuggestions.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()
    val season = remember { SeasonEngine.currentSeason() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "KI-Vorschläge für ${season.displayName()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Basierend auf deiner Garderobe und der aktuellen Saison.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        if (aiLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (suggestions.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "Keine Vorschläge verfügbar. Füge mehr Kleidung hinzu.",
                        modifier = Modifier.padding(18.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(suggestions.size) { index ->
                val outfit = suggestions[index]
                GeneratedOutfitCard(
                    outfit = outfit,
                    label = "Vorschlag ${index + 1}",
                    onUseOutfit = { onUseOutfit(outfit) }
                )
            }
        }
    }
}

// ── Generated Outfit Card (shared) ────────────────────────────────────────────

@Composable
private fun GeneratedOutfitCard(
    outfit: GeneratedOutfit,
    label: String = "Outfit",
    onUseOutfit: () -> Unit
) {
    val items = listOfNotNull(outfit.top, outfit.bottom, outfit.jacket, outfit.shoes)
    val description = AiEngine.describeOutfit(outfit)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Score: ${outfit.score}",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (items.size > 2) 220.dp else 110.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = false
            ) {
                items(items) { item ->
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.category,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Button(
                onClick = onUseOutfit,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Im Outfit-Creator verwenden")
            }
        }
    }
}

// ── Existing tabs and components (unchanged) ──────────────────────────────────

@Composable
private fun CreateOutfitTab(viewModel: FitCreatorViewModel, closetRepository: ClosetRepository) {
    val selectedItems by viewModel.selectedItems.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedTagIds by viewModel.selectedTagIds.collectAsState()

    var clothingItems by remember { mutableStateOf<List<ClothingItemUi>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var caption by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var isFetchingItems by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        closetRepository.getClothingItems(
            onSuccess = { items -> clothingItems = items; isFetchingItems = false },
            onError = { isFetchingItems = false }
        )
        onDispose {}
    }

    if (showSaveDialog) {
        SaveOutfitDialog(
            caption = caption,
            isPublic = isPublic,
            isLoading = isLoading,
            selectedTagIds = selectedTagIds,
            onCaptionChange = { caption = it },
            onPublicChange = { isPublic = it },
            onTagToggle = { viewModel.toggleNewOutfitTag(it) },
            onSave = {
                val username = AuthManager.getCurrentUser()?.displayName ?: "Unknown"
                viewModel.saveOutfit(
                    caption = caption,
                    imageUrl = selectedItems.firstOrNull()?.imageUrl ?: "",
                    isPublic = isPublic,
                    username = username,
                    onSuccess = { showSaveDialog = false; caption = ""; isPublic = false },
                    onError = {}
                )
            },
            onDismiss = { showSaveDialog = false }
        )
    }

    val categories = clothingItems.map { it.category }.distinct()
    val visibleItems = selectedCategory?.let { cat -> clothingItems.filter { it.category == cat } } ?: clothingItems

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            OutfitPreviewCard(
                selectedItems = selectedItems,
                onRemove = { viewModel.removeItem(it.id) },
                onClear = { viewModel.clearSelection() }
            )
        }

        if (selectedItems.isNotEmpty()) {
            item {
                if (!viewModel.validateOutfit()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "Wähle mindestens ein Oberteil und eine Hose aus.",
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                } else {
                    Button(
                        onClick = { showSaveDialog = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = MaterialTheme.shapes.large
                    ) { Text("Outfit speichern") }
                }
            }
        }

        errorMessage?.let { message ->
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(text = message, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

        item { Text("Kleidung auswählen", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }

        if (isFetchingItems) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { CategoryChip(label = "Alle", isSelected = selectedCategory == null, onClick = { selectedCategory = null }) }
                    items(categories) { category ->
                        CategoryChip(
                            label = category,
                            isSelected = selectedCategory == category,
                            onClick = { selectedCategory = category }
                        )
                    }
                }
            }

            if (visibleItems.isEmpty()) {
                item { EmptyClosetHint() }
            } else {
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxWidth().height(430.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        userScrollEnabled = true
                    ) {
                        items(visibleItems) { item ->
                            val isSelected = selectedItems.any { it.id == item.id }
                            ClothingItemCard(
                                item = item,
                                isSelected = isSelected,
                                onSelect = { if (isSelected) viewModel.removeItem(item.id) else viewModel.addItem(item) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OutfitPreviewCard(selectedItems: List<ClothingItemUi>, onRemove: (ClothingItemUi) -> Unit, onClear: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Outfit erstellen", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${selectedItems.size} Teile ausgewählt", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (selectedItems.isNotEmpty()) {
                    Button(
                        onClick = onClear,
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                    ) { Text("Leeren") }
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth().height(270.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(Brush.radialGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, Color(0xFF102B35))))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraLarge)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (selectedItems.isEmpty()) {
                    Text("Tippe unten auf Kleidungsstücke, um dein Outfit zu bauen.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(18.dp))
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        userScrollEnabled = false
                    ) {
                        items(selectedItems.take(6)) { item -> SelectedItemCard(item = item, onRemove = { onRemove(item) }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        tonalElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
    }
}

@Composable
private fun EmptyClosetHint() {
    Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface) {
        Text(text = "Noch keine passenden Kleidungsstücke gefunden. Füge zuerst Kleidung im Schrank hinzu.", modifier = Modifier.padding(18.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MyOutfitsTab(viewModel: FitCreatorViewModel) {
    val outfits by viewModel.filteredOutfits.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val filterTagIds by viewModel.filterTagIds.collectAsState()

    DisposableEffect(Unit) { viewModel.loadMyOutfits(); onDispose {} }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { FitTagSelector(selectedTagIds = filterTagIds, onTagToggle = { viewModel.toggleFilterTag(it) }) }
        if (filterTagIds.isNotEmpty()) {
            item { OutlinedButton(onClick = { viewModel.clearFilterTags() }) { Text("Filter zurücksetzen") } }
        }
        if (outfits.isEmpty()) {
            item {
                Text(
                    text = if (filterTagIds.isNotEmpty()) "Keine Outfits für die ausgewählten Tags." else "Du hast noch keine Outfits erstellt.",
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        items(outfits) { outfit ->
            OutfitCard(outfit = outfit, onToggleVisibility = { viewModel.toggleOutfitVisibility(outfit.id, !outfit.isPublic) }, onDelete = { viewModel.deleteOutfit(outfit.id) })
        }
    }
}

@Composable
private fun ClothingItemCard(item: ClothingItemUi, isSelected: Boolean, onSelect: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onSelect() },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 5.dp else 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(model = item.imageUrl, contentDescription = item.category, modifier = Modifier.fillMaxWidth().height(136.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceVariant), contentScale = ContentScale.Crop)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = item.category, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            if (!item.color.isNullOrBlank()) { Text(text = "Color: ${item.color}", style = MaterialTheme.typography.labelSmall, maxLines = 1) }
            if (isSelected) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.padding(4.dp).size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun SelectedItemCard(item: ClothingItemUi, onRemove: () -> Unit) {
    Card(modifier = Modifier.clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.primaryContainer)) {
        Box {
            AsyncImage(model = item.imageUrl, contentDescription = item.category, modifier = Modifier.fillMaxWidth().height(100.dp).clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceVariant), contentScale = ContentScale.Crop)
            IconButton(onClick = onRemove, modifier = Modifier.align(Alignment.TopEnd).background(MaterialTheme.colorScheme.primary, CircleShape).size(32.dp)) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun OutfitCard(outfit: Outfit, onToggleVisibility: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = outfit.caption.ifBlank { "Outfit" }, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (outfit.isPublic) { Text("Public", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                    Switch(checked = outfit.isPublic, onCheckedChange = { onToggleVisibility() })
                }
            }
            Text("${outfit.clothingItems.size} Items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (outfit.tags.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(outfit.tags) { tagId ->
                        val tag = PredefinedTags.findById(tagId)
                        if (tag != null) { FitTagPill(label = tag.name, selected = false, onClick = {}) }
                    }
                }
            }
            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxWidth().height(100.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), userScrollEnabled = true) {
                items(outfit.clothingItems) { item ->
                    AsyncImage(model = item.imageUrl, contentDescription = item.category, modifier = Modifier.fillMaxWidth().height(80.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceVariant), contentScale = ContentScale.Crop)
                }
            }
            Button(onClick = onDelete, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp).padding(end = 4.dp))
                Text("Delete")
            }
        }
    }
}

@Composable
private fun FitTagSelector(selectedTagIds: List<String>, onTagToggle: (String) -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.background.copy(alpha = 0.35f)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Tags", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(text = if (selectedTagIds.isEmpty()) "Optional, hilft beim Filtern" else "${selectedTagIds.size} ausgewählt", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            FitTagGroup(label = "Saison", tags = PredefinedTags.SEASON, selectedTagIds = selectedTagIds, onTagToggle = onTagToggle)
            FitTagGroup(label = "Anlass", tags = PredefinedTags.OCCASION, selectedTagIds = selectedTagIds, onTagToggle = onTagToggle)
            FitTagGroup(label = "Stil", tags = PredefinedTags.STYLE, selectedTagIds = selectedTagIds, onTagToggle = onTagToggle)
        }
    }
}

@Composable
private fun FitTagGroup(label: String, tags: List<Tag>, selectedTagIds: List<String>, onTagToggle: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tags) { tag -> FitTagPill(label = tag.name, selected = tag.id in selectedTagIds, onClick = { onTagToggle(tag.id) }) }
        }
    }
}

@Composable
private fun FitTagPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp))
    }
}

@Composable
private fun SaveOutfitDialog(caption: String, isPublic: Boolean, isLoading: Boolean, selectedTagIds: List<String>, onCaptionChange: (String) -> Unit, onPublicChange: (Boolean) -> Unit, onTagToggle: (String) -> Unit, onSave: () -> Unit, onDismiss: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)).clickable { onDismiss() }, color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(modifier = Modifier.padding(16.dp).clickable(enabled = false) {}, shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Outfit speichern", style = MaterialTheme.typography.headlineSmall)
                    TextField(value = caption, onValueChange = onCaptionChange, label = { Text("Name oder Beschreibung") }, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Öffentlich teilen")
                        Switch(checked = isPublic, onCheckedChange = onPublicChange)
                    }
                    FitTagSelector(selectedTagIds = selectedTagIds, onTagToggle = onTagToggle)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onDismiss, modifier = Modifier.weight(1f).height(40.dp), enabled = !isLoading, shape = MaterialTheme.shapes.large) { Text("Abbrechen") }
                        Button(onClick = onSave, modifier = Modifier.weight(1f).height(40.dp), enabled = !isLoading, shape = MaterialTheme.shapes.large) {
                            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp)) else Text("Speichern")
                        }
                    }
                }
            }
        }
    }
}