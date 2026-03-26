package com.m306.closetly.closet.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.m306.closetly.closet.model.ClothingItem
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClosetScreen() {

    val viewModel: ClosetViewModel = viewModel()

    val filteredClothes by viewModel.filteredClothes.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()
    val message by viewModel.message.collectAsState()

    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedColor by viewModel.selectedColor.collectAsState()
    val selectedBrand by viewModel.selectedBrand.collectAsState()
    val selectedSize by viewModel.selectedSize.collectAsState()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var category by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("") }

    var editingItemId by remember { mutableStateOf<String?>(null) }
    var editCategory by remember { mutableStateOf("") }
    var editColor by remember { mutableStateOf("") }
    var editBrand by remember { mutableStateOf("") }
    var editSize by remember { mutableStateOf("") }

    val categoryOptions = listOf("Jacket", "Pants", "Pullover", "Shirt", "Shoes", "Watch")
    val colorOptions = listOf("Black", "White", "Blue", "Red", "Green", "Gray", "Beige", "Yellow", "Orange", "Violet", "Purple")
    val brandOptions = listOf("Nike", "Adidas", "Zara", "H&M", "Puma", "Levi's", "Ralph Lauren", "Jack&Jones", "Louis Vuitton", "Gucci", "Prada")
    val sizeOptions = listOf("XS", "S", "M", "L", "XL", "XXL")

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "choose_button") {
            Button(onClick = { launcher.launch("image/*") }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Choose Image")
            }
        }

        item(key = "filters") {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Filters", style = MaterialTheme.typography.titleMedium)
                FilterDropdown(
                    label = "Category",
                    options = categoryOptions,
                    selectedValue = selectedCategory,
                    onValueSelected = { viewModel.setSelectedCategory(it) },
                    modifier = Modifier.weight(1f)
                )

                FilterDropdown(
                    label = "Color",
                    options = colorOptions,
                    selectedValue = selectedColor,
                    onValueSelected = { viewModel.setSelectedColor(it) },
                    modifier = Modifier.weight(1f)
                )



                FilterDropdown(
                    label = "Brand",
                    options = brandOptions,
                    selectedValue = selectedBrand,
                    onValueSelected = { viewModel.setSelectedBrand(it) },
                    modifier = Modifier.weight(1f)
                )

                FilterDropdown(
                    label = "Size",
                    options = sizeOptions,
                    selectedValue = selectedSize,
                    onValueSelected = { viewModel.setSelectedSize(it) },
                    modifier = Modifier.weight(1f)
                )


                OutlinedButton(onClick = { viewModel.clearFilters() }) {
                    Text("Clear Filters")
                }
            }
        }

        if (selectedImageUri != null) {
            item(key = "upload_form") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
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

                    Button(
                        enabled = !isBusy &&
                                category.isNotBlank() &&
                                color.isNotBlank() &&
                                brand.isNotBlank() &&
                                size.isNotBlank(),
                        onClick = {
                            val image = selectedImageUri ?: return@Button

                            val savedCategory = category
                            val savedColor = color
                            val savedBrand = brand
                            val savedSize = size

                            viewModel.saveClothingItem(
                                imageUri = image,
                                category = savedCategory,
                                color = savedColor,
                                brand = savedBrand,
                                size = savedSize
                            )

                            selectedImageUri = null
                            category = ""
                            color = ""
                            brand = ""
                            size = ""
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save")
                    }

                    OutlinedButton(onClick = {
                        selectedImageUri = null
                        category = ""
                        color = ""
                        brand = ""
                        size = ""
                    }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Cancel")
                    }
                }
            }
        }

        if (message.isNotBlank()) {
            item(key = "message") {
                Text(message)
            }
        }

        if (editingItemId != null) {
            item(key = "edit_form") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Edit Item", style = MaterialTheme.typography.titleMedium)

                        DropdownSelector(
                            label = "Category",
                            options = categoryOptions,
                            selectedValue = editCategory,
                            onValueSelected = { editCategory = it }
                        )

                        DropdownSelector(
                            label = "Color",
                            options = colorOptions,
                            selectedValue = editColor,
                            onValueSelected = { editColor = it }
                        )

                        DropdownSelector(
                            label = "Brand",
                            options = brandOptions,
                            selectedValue = editBrand,
                            onValueSelected = { editBrand = it }
                        )

                        DropdownSelector(
                            label = "Size",
                            options = sizeOptions,
                            selectedValue = editSize,
                            onValueSelected = { editSize = it }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                enabled = !isBusy &&
                                        editCategory.isNotBlank() &&
                                        editColor.isNotBlank() &&
                                        editBrand.isNotBlank() &&
                                        editSize.isNotBlank(),
                                onClick = {
                                    viewModel.updateClothingItem(
                                        itemId = editingItemId!!,
                                        category = editCategory,
                                        color = editColor,
                                        brand = editBrand,
                                        size = editSize
                                    )
                                    editingItemId = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Save")
                            }

                            OutlinedButton(
                                onClick = {
                                    editingItemId = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
        }

        items(
            items = filteredClothes,
            key = { it.id }
        ) { item ->
            ClothingItemCard(
                item = item,
                onEdit = {
                    editingItemId = item.id
                    editCategory = item.category
                    editColor = item.color
                    editBrand = item.brand
                    editSize = item.size
                },
                onDelete = {
                    viewModel.deleteClothingItem(item.id)
                }
            )
        }

        item(key = "bottom_space") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ClothingItemCard(
    item: ClothingItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                ) {
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = Color.White
                        )
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
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(
    label: String,
    options: List<String>,
    selectedValue: String,
    onValueSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdown(
    label: String,
    options: List<String>,
    selectedValue: String?,
    onValueSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedValue ?: "All",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = modifier
                .fillMaxWidth()
                .menuAnchor(),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("All") },
                onClick = {
                    onValueSelected(null)
                    expanded = false
                }
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}