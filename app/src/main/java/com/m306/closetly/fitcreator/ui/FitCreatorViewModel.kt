package com.m306.closetly.fitcreator.ui

import androidx.lifecycle.ViewModel
import com.m306.closetly.closet.model.ClothingItemUi
import com.m306.closetly.fitcreator.data.OutfitRepository
import com.m306.closetly.fitcreator.model.Outfit
import com.m306.closetly.premium.data.PremiumAccessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FitCreatorViewModel : ViewModel() {

    private val repository = OutfitRepository()
    private val premiumAccessRepository = PremiumAccessRepository()

    private val _selectedItems = MutableStateFlow<List<ClothingItemUi>>(emptyList())
    val selectedItems: StateFlow<List<ClothingItemUi>> = _selectedItems

    private val _outfits = MutableStateFlow<List<Outfit>>(emptyList())
    val outfits: StateFlow<List<Outfit>> = _outfits

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun addItem(item: ClothingItemUi) {
        val currentItems = _selectedItems.value
        val isCategorySelected = currentItems.any { it.category == item.category }

        if (isCategorySelected) {
            _errorMessage.value = "Sie können nur ein Item pro Kategorie auswählen"
            return
        }

        if (hasConflictingCategory(currentItems, item)) {
            _errorMessage.value = "Sie können nicht gleichzeitig ${item.category} und ein anderes Oberteil wählen"
            return
        }

        _selectedItems.value = currentItems + item
        _errorMessage.value = null
    }

    fun removeItem(itemId: String) {
        _selectedItems.value = _selectedItems.value.filter { it.id != itemId }
    }

    fun validateOutfit(): Boolean {
        val selectedItems = _selectedItems.value
        
        val hasBottomWear = selectedItems.any { 
            it.category in listOf("Hose", "Pants", "Rock", "Skirt", "Shorts")
        }

        val hasTopWear = selectedItems.any {
            it.category in listOf("Oberteil", "Top", "Shirt", "T-Shirt", "Bluse", "Blouse", "Pullover", "Jumper")
        }

        return hasBottomWear && hasTopWear
    }

    fun saveOutfit(
        caption: String,
        imageUrl: String,
        isPublic: Boolean,
        username: String,
        onSuccess: (Outfit) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!validateOutfit()) {
            onError("Bitte wählen Sie mindestens eine Hose und ein Oberteil")
            return
        }

        _isLoading.value = true

        premiumAccessRepository.canAddOutfit { canAdd, reason ->
            if (!canAdd) {
                _isLoading.value = false
                val message = reason ?: "Upgrade to Premium to save more outfits."
                _errorMessage.value = message
                onError(message)
                return@canAddOutfit
            }

            repository.saveOutfit(
                caption = caption,
                imageUrl = imageUrl,
                clothingItems = _selectedItems.value,
                isPublic = isPublic,
                username = username,
                onSuccess = {
                    _isLoading.value = false
                    _selectedItems.value = emptyList()
                    onSuccess(it)
                },
                onError = {
                    _isLoading.value = false
                    val errorMsg = it.message ?: "Error while saving"
                    _errorMessage.value = errorMsg
                    onError(errorMsg)
                }
            )
        }
    }

    fun loadMyOutfits() {
        _isLoading.value = true
        repository.getMyOutfits(
            onSuccess = {
                _outfits.value = it
                _isLoading.value = false
            },
            onError = {
                _isLoading.value = false
                _errorMessage.value = it.message
            }
        )
    }

    fun toggleOutfitVisibility(outfitId: String, isPublic: Boolean) {
        repository.updateOutfitVisibility(outfitId, isPublic, {}, { _errorMessage.value = it.message })
    }

    fun deleteOutfit(outfitId: String) {
        repository.deleteOutfit(outfitId, {
            _outfits.value = _outfits.value.filter { it.id != outfitId }
        }, { _errorMessage.value = it.message })
    }

    private fun hasConflictingCategory(currentItems: List<ClothingItemUi>, newItem: ClothingItemUi): Boolean {
        val conflicts = mapOf(
            "Shirt" to listOf("Pullover", "Jumper"),
            "T-Shirt" to listOf("Pullover", "Jumper")
        )
        for ((cat, conflicted) in conflicts) {
            if (currentItems.any { it.category == cat } && newItem.category in conflicted) return true
            if (newItem.category == cat && currentItems.any { it.category in conflicted }) return true
        }
        return false
    }

    fun clearSelection() {
        _selectedItems.value = emptyList()
        _errorMessage.value = null
    }
}

