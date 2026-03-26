package com.m306.closetly.closet.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m306.closetly.closet.data.ClosetRepository
import com.m306.closetly.closet.model.ClothingItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class ClosetViewModel : ViewModel() {

    private val repository = ClosetRepository()

    private val _clothingItems = MutableStateFlow<List<ClothingItem>>(emptyList())

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message

    // Filter states
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _selectedColor = MutableStateFlow<String?>(null)
    val selectedColor: StateFlow<String?> = _selectedColor

    private val _selectedBrand = MutableStateFlow<String?>(null)
    val selectedBrand: StateFlow<String?> = _selectedBrand

    private val _selectedSize = MutableStateFlow<String?>(null)
    val selectedSize: StateFlow<String?> = _selectedSize

    val filteredClothes: StateFlow<List<ClothingItem>> = combine(
        _clothingItems, _selectedCategory, _selectedColor, _selectedBrand, _selectedSize
    ) { items, category, color, brand, size ->
        items.filter { item ->
            (category == null || item.category == category) &&
            (color == null || item.color == color) &&
            (brand == null || item.brand == brand) &&
            (size == null || item.size == size)
        }
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.Lazily,
        initialValue = emptyList()
    )

    init {
        loadClothes()
    }

    fun loadClothes() {
        repository.getClothingItems(
            onSuccess = { _clothingItems.value = it },
            onError = { _message.value = it.message ?: "Error" }
        )
    }

    fun saveClothingItem(
        imageUri: Uri,
        category: String,
        color: String,
        brand: String,
        size: String
    ) {
        _isBusy.value = true
        _message.value = ""

        repository.saveClothingItem(
            imageUri = imageUri,
            category = category,
            color = color,
            brand = brand,
            size = size,
            onSuccess = { newItem ->
                _clothingItems.value = listOf(newItem) + _clothingItems.value
                _isBusy.value = false
                _message.value = "Saved!"
            },
            onError = { exception ->
                _isBusy.value = false
                _message.value = exception.message ?: "Error"
            }
        )
    }

    fun deleteClothingItem(itemId: String) {
        _isBusy.value = true
        repository.deleteClothingItem(
            itemId = itemId,
            onSuccess = {
                _clothingItems.value = _clothingItems.value.filter { it.id != itemId }
                _isBusy.value = false
                _message.value = "Deleted!"
            },
            onError = { exception ->
                _isBusy.value = false
                _message.value = exception.message ?: "Error"
            }
        )
    }

    fun updateClothingItem(
        itemId: String,
        category: String,
        color: String,
        brand: String,
        size: String
    ) {
        _isBusy.value = true
        repository.updateClothingItem(
            itemId = itemId,
            category = category,
            color = color,
            brand = brand,
            size = size,
            onSuccess = { updatedItem ->
                _clothingItems.value = _clothingItems.value.map {
                    if (it.id == itemId) updatedItem else it
                }
                _isBusy.value = false
                _message.value = "Updated!"
            },
            onError = { exception ->
                _isBusy.value = false
                _message.value = exception.message ?: "Error"
            }
        )
    }

    fun setSelectedCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun setSelectedColor(color: String?) {
        _selectedColor.value = color
    }

    fun setSelectedBrand(brand: String?) {
        _selectedBrand.value = brand
    }

    fun setSelectedSize(size: String?) {
        _selectedSize.value = size
    }

    fun clearFilters() {
        _selectedCategory.value = null
        _selectedColor.value = null
        _selectedBrand.value = null
        _selectedSize.value = null
    }
}



