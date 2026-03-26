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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.m306.closetly.closet.model.ClothingItemUi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import com.m306.closetly.closet.func.getColorFromName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClosetScreen() {

    val viewModel: ClosetViewModel = viewModel()

    val clothes by viewModel.clothes.collectAsState()
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


        items(
            items = filteredClothes,
            key = { it.id }
        ) { item ->
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        item(key = "bottom_space") {
            Spacer(modifier = Modifier.height(24.dp))
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
    val isColorSelector = label == "Color"

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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isColorSelector && selectedValue.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(
                                    color = getColorFromName(selectedValue),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                        )

                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        if (isColorSelector) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(
                                            color = getColorFromName(option),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                                )

                                Spacer(modifier = Modifier.width(8.dp))
                                Text(option)
                            }
                        } else {
                            Text(option)
                        }
                    },
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
    val isColorFilter = label == "Color"

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
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isColorFilter && selectedValue != null) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(
                                    color = getColorFromName(selectedValue),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                        )

                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
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
                    text = {
                        if (isColorFilter) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(
                                            color = getColorFromName(option),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                                )

                                Spacer(modifier = Modifier.width(8.dp))
                                Text(option)
                            }
                        } else {
                            Text(option)
                        }
                    },
                    onClick = {
                        onValueSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
