package com.closetly.myapp.closet.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.closetly.myapp.closet.func.getColorFromName
import com.closetly.myapp.closet.model.ClothingItemUi
import com.closetly.myapp.tags.model.PredefinedTags
import com.m306.closetly.ai.ShoppingEngine
import java.io.File
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun ClosetScreen() {
    val viewModel: ClosetViewModel = viewModel()
    val context = LocalContext.current

    val filteredClothes by viewModel.filteredClothes.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()
    val message by viewModel.message.collectAsState()

    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedColor by viewModel.selectedColor.collectAsState()
    val selectedBrand by viewModel.selectedBrand.collectAsState()
    val selectedSize by viewModel.selectedSize.collectAsState()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var category by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("") }
    var purchaseLink by remember { mutableStateOf("") }
    var purchaseLinkError by remember { mutableStateOf(false) }
    var selectedOccasion by remember { mutableStateOf<String?>(null) }
    var selectedStyle by remember { mutableStateOf<String?>(null) }

    var editingItemId by remember { mutableStateOf<String?>(null) }
    var editCategory by remember { mutableStateOf("") }
    var editColor by remember { mutableStateOf("") }
    var editBrand by remember { mutableStateOf("") }
    var editSize by remember { mutableStateOf("") }
    var editPurchaseLink by remember { mutableStateOf("") }
    var editPurchaseLinkError by remember { mutableStateOf(false) }
    var editOccasion by remember { mutableStateOf<String?>(null) }
    var editStyle by remember { mutableStateOf<String?>(null) }

    // ── Shopping Popup ────────────────────────────────────────────────────────
    var showShoppingPopup by remember { mutableStateOf(false) }
    val shoppingRecommendation = remember(filteredClothes) {
        ShoppingEngine.generateRecommendation(filteredClothes)
    }

    LaunchedEffect(shoppingRecommendation) {
        if (shoppingRecommendation != null) {
            if (Random.nextInt(100) < 20) {
                showShoppingPopup = true
            }
        }
    }

    if (showShoppingPopup && shoppingRecommendation != null) {
        AlertDialog(
            onDismissRequest = { showShoppingPopup = false },
            shape = MaterialTheme.shapes.extraLarge,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Style Tip",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = shoppingRecommendation.headline,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = shoppingRecommendation.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            shoppingRecommendation.shoppingUrl.toUri()
                        )
                        context.startActivity(intent)
                        showShoppingPopup = false
                    },
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Shop on Google")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showShoppingPopup = false },
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("Later")
                }
            }
        )
    }
    // ─────────────────────────────────────────────────────────────────────────

    val categoryOptions = listOf("Jacket", "Pants", "Pullover", "Shirt", "Shoes", "Watch")
    val colorOptions = listOf("Black", "White", "Blue", "Red", "Green", "Gray", "Beige", "Yellow", "Orange", "Violet", "Purple", "Other")
    val brandOptions = listOf("No Brand", "Nike", "Adidas", "Zara", "H&M", "Puma", "Levi's", "Ralph Lauren", "Jack&Jones", "Louis Vuitton", "Gucci", "Prada")
    val sizeOptions = listOf("XS", "S", "M", "L", "XL", "XXL")

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageUri != null) {
            selectedImageUri = cameraImageUri
        }
    }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Add clothing") },
            text = { Text("Choose an image from your gallery or take a new photo.") },
            confirmButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    imagePickerLauncher.launch("image/*")
                }) { Text("Gallery") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    val newImageUri = createClothingImageUri(context)
                    cameraImageUri = newImageUri
                    cameraLauncher.launch(newImageUri)
                }) { Text("Camera") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "header") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF173742), MaterialTheme.colorScheme.surfaceVariant)
                        )
                    )
                    .padding(22.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Closet",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(
                            text = "${filteredClothes.size} items found",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        item(key = "choose_button") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Add clothing",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Upload a photo and set category, color, brand, and size right away.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { showImageSourceDialog = true },
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add")
                    }
                }
            }
        }

        if (selectedImageUri != null) {
            item(key = "upload_form") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(MaterialTheme.shapes.large),
                            contentScale = ContentScale.Crop
                        )

                        DropdownSelector(
                            label = "Category",
                            options = categoryOptions,
                            selectedValue = category,
                            onValueSelected = { category = it }
                        )

                        DropdownSelector(
                            label = "Color",
                            options = colorOptions,
                            selectedValue = color,
                            onValueSelected = { color = it }
                        )

                        DropdownSelector(
                            label = "Brand",
                            options = brandOptions,
                            selectedValue = brand,
                            onValueSelected = { brand = it }
                        )

                        DropdownSelector(
                            label = "Size",
                            options = sizeOptions,
                            selectedValue = size,
                            onValueSelected = { size = it }
                        )

                        OutlinedTextField(
                            value = purchaseLink,
                            onValueChange = {
                                purchaseLink = it
                                purchaseLinkError = it.isNotBlank() && !android.util.Patterns.WEB_URL.matcher(it).matches()
                            },
                            label = { Text("Purchase link (optional)") },
                            placeholder = { Text("https://...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = purchaseLinkError,
                            supportingText = if (purchaseLinkError) {
                                { Text("Please enter a valid link (e.g. https://...)") }
                            } else null
                        )

                        Text(
                            text = "Occasion",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(PredefinedTags.OCCASION) { tag ->
                                FilterChip(
                                    selected = selectedOccasion == tag.id,
                                    onClick = { selectedOccasion = if (selectedOccasion == tag.id) null else tag.id },
                                    label = { Text(tag.name) }
                                )
                            }
                        }

                        Text(
                            text = "Style",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(PredefinedTags.STYLE) { tag ->
                                FilterChip(
                                    selected = selectedStyle == tag.id,
                                    onClick = { selectedStyle = if (selectedStyle == tag.id) null else tag.id },
                                    label = { Text(tag.name) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                enabled = !isBusy &&
                                        category.isNotBlank() &&
                                        color.isNotBlank() &&
                                        brand.isNotBlank() &&
                                        size.isNotBlank() &&
                                        !purchaseLinkError,
                                onClick = {
                                    val uri = selectedImageUri ?: return@Button
                                    val combinedTags = listOfNotNull(selectedOccasion, selectedStyle)
                                    val styleValue = selectedOccasion ?: selectedStyle ?: ""
                                    viewModel.saveClothingItemWithRemovedBackground(
                                        context = context,
                                        imageUri = uri,
                                        category = category,
                                        color = color,
                                        brand = brand,
                                        size = size,
                                        purchaseLink = purchaseLink,
                                        style = styleValue,
                                        tags = combinedTags
                                    )

                                    selectedImageUri = null
                                    category = ""
                                    color = ""
                                    brand = ""
                                    size = ""
                                    purchaseLink = ""
                                    purchaseLinkError = false
                                    selectedOccasion = null
                                    selectedStyle = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Save")
                            }
                            OutlinedButton(
                                onClick = {
                                    selectedImageUri = null
                                    category = ""
                                    color = ""
                                    brand = ""
                                    size = ""
                                    purchaseLink = ""
                                    purchaseLinkError = false
                                    selectedOccasion = null
                                    selectedStyle = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
        }

        item(key = "filters") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Category & Filter",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            ClosetFilterChip(
                                label = "All",
                                isSelected = selectedCategory == null,
                                onClick = { viewModel.setSelectedCategory(null) }
                            )
                        }
                        items(categoryOptions) { option ->
                            ClosetFilterChip(
                                label = option,
                                isSelected = selectedCategory == option,
                                onClick = { viewModel.setSelectedCategory(if (selectedCategory == option) null else option) }
                            )
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            ClosetFilterChip(
                                label = "All Colors",
                                isSelected = selectedColor == null,
                                onClick = { viewModel.setSelectedColor(null) }
                            )
                        }
                        items(colorOptions.take(7)) { option ->
                            ClosetColorChip(
                                label = option,
                                isSelected = selectedColor == option,
                                onClick = { viewModel.setSelectedColor(if (selectedColor == option) null else option) }
                            )
                        }
                    }

                    FilterDropdown(label = "Brand", options = brandOptions, selectedValue = selectedBrand, onValueSelected = { viewModel.setSelectedBrand(it) })
                    FilterDropdown(label = "Size", options = sizeOptions, selectedValue = selectedSize, onValueSelected = { viewModel.setSelectedSize(it) })

                    OutlinedButton(
                        onClick = { viewModel.clearFilters() },
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset filters")
                    }
                }
            }
        }

        if (message.isNotBlank()) {
            item(key = "message") {
                Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(text = message, modifier = Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }

        if (editingItemId != null) {
            item(key = "edit_form") {
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.extraLarge),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Edit Item", style = MaterialTheme.typography.titleMedium)

                        DropdownSelector(label = "Category", options = categoryOptions, selectedValue = editCategory, onValueSelected = { editCategory = it })
                        DropdownSelector(label = "Color", options = colorOptions, selectedValue = editColor, onValueSelected = { editColor = it })
                        DropdownSelector(label = "Brand", options = brandOptions, selectedValue = editBrand, onValueSelected = { editBrand = it })
                        DropdownSelector(label = "Size", options = sizeOptions, selectedValue = editSize, onValueSelected = { editSize = it })

                        OutlinedTextField(
                            value = editPurchaseLink,
                            onValueChange = {
                                editPurchaseLink = it
                                editPurchaseLinkError = it.isNotBlank() && !android.util.Patterns.WEB_URL.matcher(it).matches()
                            },
                            label = { Text("Purchase link (optional)") },
                            placeholder = { Text("https://...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = editPurchaseLinkError,
                            supportingText = if (editPurchaseLinkError) {
                                { Text("Please enter a valid link (e.g. https://...)") }
                            } else null
                        )

                        Text(
                            text = "Occasion",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(PredefinedTags.OCCASION) { tag ->
                                FilterChip(
                                    selected = editOccasion == tag.id,
                                    onClick = { editOccasion = if (editOccasion == tag.id) null else tag.id },
                                    label = { Text(tag.name) }
                                )
                            }
                        }

                        Text(
                            text = "Style",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(PredefinedTags.STYLE) { tag ->
                                FilterChip(
                                    selected = editStyle == tag.id,
                                    onClick = { editStyle = if (editStyle == tag.id) null else tag.id },
                                    label = { Text(tag.name) }
                                )
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                enabled = !isBusy &&
                                        editCategory.isNotBlank() &&
                                        editColor.isNotBlank() &&
                                        editBrand.isNotBlank() &&
                                        editSize.isNotBlank() &&
                                        !editPurchaseLinkError,
                                onClick = {
                                    val itemId = editingItemId ?: return@Button
                                    val combinedTags = listOfNotNull(editOccasion, editStyle)
                                    val styleValue = editOccasion ?: editStyle ?: ""
                                    viewModel.updateClothingItem(
                                        itemId = itemId,
                                        category = editCategory,
                                        color = editColor,
                                        brand = editBrand,
                                        size = editSize,
                                        purchaseLink = editPurchaseLink,
                                        style = styleValue,
                                        tags = combinedTags
                                    )
                                    editingItemId = null
                                    editPurchaseLinkError = false
                                    editOccasion = null
                                    editStyle = null
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Save") }

                            OutlinedButton(
                                onClick = {
                                    editingItemId = null
                                    editPurchaseLinkError = false
                                    editOccasion = null
                                    editStyle = null
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Cancel") }
                        }
                    }
                }
            }
        }

        item(key = "clothes_grid") {
            if (filteredClothes.isEmpty()) {
                Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface) {
                    Text(
                        text = "No clothing found yet. Add your first item above.",
                        modifier = Modifier.padding(18.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val rows = (filteredClothes.size + 1) / 2
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth().height((rows * 254).dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    userScrollEnabled = false
                ) {
                    items(items = filteredClothes, key = { it.id }) { item ->
                        ClosetGridItemCard(
                            item = item,
                            onEdit = {
                                editingItemId = item.id
                                editCategory = item.category
                                editColor = item.color ?: ""
                                editBrand = item.brand ?: ""
                                editSize = item.size ?: ""
                                editPurchaseLink = item.purchaseLink ?: ""
                                editPurchaseLinkError = false
                                editOccasion = item.tags.firstOrNull { it.startsWith("occasion_") }
                                editStyle = item.tags.firstOrNull { it.startsWith("style_") }
                            },
                            onDelete = { viewModel.deleteClothingItem(item.id) }
                        )
                    }
                }
            }
        }

        item(key = "bottom_space") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── Components ────────────────────────────────────────────────────────────────

@Composable
private fun ClosetFilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp))
    }
}

@Composable
private fun ClosetColorChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(modifier = Modifier.size(13.dp).background(getColorFromName(label), CircleShape).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun ClosetGridItemCard(item: ClothingItemUi, onEdit: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.category,
                    modifier = Modifier.fillMaxWidth().height(142.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(32.dp).background(Color.Black.copy(alpha = 0.38f), CircleShape)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White, modifier = Modifier.size(18.dp))
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            onEdit()
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                        onClick = {
                            onDelete()
                            expanded = false
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = item.category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item.color?.takeIf { it.isNotBlank() }?.let {
                    Box(modifier = Modifier.size(10.dp).background(getColorFromName(it), CircleShape).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
                }
                Text(
                    text = listOfNotNull(item.brand, item.color, item.size).joinToString(" | ").ifBlank { "No details" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun ClothingItemCard(item: ClothingItemUi, onEdit: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(220.dp).clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
                Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.46f), MaterialTheme.shapes.medium)) {
                    IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        DropdownMenuItem(text = { Text("Edit") }, leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }, onClick = { onEdit(); expanded = false })
                        DropdownMenuItem(text = { Text("Delete") }, leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }, onClick = { onDelete(); expanded = false })
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(text = listOfNotNull(item.brand, item.color, item.size).joinToString(" | ").ifBlank { "No details" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item.color?.takeIf { it.isNotBlank() }?.let {
                    Box(modifier = Modifier.size(28.dp).background(getColorFromName(it), CircleShape).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape))
                }
            }
        }
    }
}

private fun createClothingImageUri(context: Context): Uri {
    val imageFile = File.createTempFile(
        "clothing_photo_${System.currentTimeMillis()}",
        ".jpg",
        context.cacheDir
    )
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(label: String, options: List<String>, selectedValue: String, onValueSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val isColorSelector = label == "Color"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isColorSelector && selectedValue.isNotBlank()) {
                        Box(modifier = Modifier.size(20.dp).background(getColorFromName(selectedValue), RoundedCornerShape(4.dp)).border(1.dp, Color.Gray, RoundedCornerShape(4.dp)))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        if (isColorSelector) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(20.dp).background(getColorFromName(option), RoundedCornerShape(4.dp)).border(1.dp, Color.Gray, RoundedCornerShape(4.dp)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(option)
                            }
                        } else { Text(option) }
                    },
                    onClick = { onValueSelected(option); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdown(label: String, options: List<String>, selectedValue: String?, onValueSelected: (String?) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val isColorFilter = label == "Color"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selectedValue ?: "All",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = modifier.fillMaxWidth().menuAnchor(),
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isColorFilter && selectedValue != null) {
                        Box(modifier = Modifier.size(20.dp).background(getColorFromName(selectedValue), RoundedCornerShape(4.dp)).border(1.dp, Color.Gray, RoundedCornerShape(4.dp)))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("All") }, onClick = { onValueSelected(null); expanded = false })
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        if (isColorFilter) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(20.dp).background(getColorFromName(option), RoundedCornerShape(4.dp)).border(1.dp, Color.Gray, RoundedCornerShape(4.dp)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(option)
                            }
                        } else { Text(option) }
                    },
                    onClick = { onValueSelected(option); expanded = false }
                )
            }
        }
    }
}
