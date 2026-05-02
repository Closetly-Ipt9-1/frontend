package com.closetly.myapp.closet.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.closetly.myapp.closet.data.ClosetRepository
import com.closetly.myapp.closet.model.ClothingItemUi
import com.closetly.myapp.premium.data.PremiumAccessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class ClosetViewModel : ViewModel() {

    private val repository = ClosetRepository()
    private val premiumAccessRepository = PremiumAccessRepository()

    private val _clothes = MutableStateFlow<List<ClothingItemUi>>(emptyList())
    val clothes: StateFlow<List<ClothingItemUi>> = _clothes

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _selectedColor = MutableStateFlow<String?>(null)
    val selectedColor: StateFlow<String?> = _selectedColor

    private val _selectedBrand = MutableStateFlow<String?>(null)
    val selectedBrand: StateFlow<String?> = _selectedBrand

    private val _selectedSize = MutableStateFlow<String?>(null)
    val selectedSize: StateFlow<String?> = _selectedSize

    val filteredClothes: StateFlow<List<ClothingItemUi>> = combine(
        _clothes,
        _selectedCategory,
        _selectedColor,
        _selectedBrand,
        _selectedSize
    ) { clothes, category, color, brand, size ->
        clothes.filter { item ->
            (category == null || item.category == category) &&
                    (color == null || item.color == color) &&
                    (brand == null || item.brand == brand) &&
                    (size == null || item.size == size)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        loadClothes()
    }

    fun loadClothes() {
        repository.getClothingItems(
            onSuccess = { _clothes.value = it },
            onError = { _message.value = it.message ?: "Error" }
        )
    }

    fun saveClothingItemWithRemovedBackground(
        context: Context,
        imageUri: Uri,
        category: String,
        color: String,
        brand: String,
        size: String,
        purchaseLink: String
    ) {
        _isBusy.value = true
        _message.value = "Removing background..."

        premiumAccessRepository.canAddClothingItem { canAdd, reason ->
            if (!canAdd) {
                _isBusy.value = false
                _message.value = reason ?: "Upgrade to Premium to save more clothing items."
                return@canAddClothingItem
            }

            repository.saveClothingItemWithRemovedBackground(
                context = context,
                imageUri = imageUri,
                category = category,
                color = color,
                brand = brand,
                size = size,
                purchaseLink = purchaseLink,
                onSuccess = { newItem ->
                    _clothes.value = listOf(newItem) + _clothes.value
                    _isBusy.value = false
                    _message.value = "Saved with removed background!"
                },
                onError = { exception ->
                    _isBusy.value = false
                    _message.value = exception.message ?: "Error"
                }
            )
        }
    }

    fun deleteClothingItem(itemId: String) {
        _isBusy.value = true
        _message.value = ""

        repository.deleteClothingItem(
            itemId = itemId,
            onSuccess = {
                _clothes.value = _clothes.value.filter { it.id != itemId }
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
        size: String,
        purchaseLink: String
    ) {
        _isBusy.value = true
        _message.value = ""

        repository.updateClothingItem(
            itemId = itemId,
            category = category,
            color = color,
            brand = brand,
            size = size,
            purchaseLink = purchaseLink,
            onSuccess = { updatedItem ->
                _clothes.value = _clothes.value.map {
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
