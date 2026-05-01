package com.closetly.myapp.fitcreator.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.closetly.myapp.closet.model.ClothingItemUi
import com.closetly.myapp.fitcreator.data.OutfitRepository
import com.closetly.myapp.fitcreator.model.Outfit
import com.closetly.myapp.premium.data.PremiumAccessRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

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

    private val _selectedTagIds = MutableStateFlow<List<String>>(emptyList())
    val selectedTagIds: StateFlow<List<String>> = _selectedTagIds

    private val _filterTagIds = MutableStateFlow<List<String>>(emptyList())
    val filterTagIds: StateFlow<List<String>> = _filterTagIds

    val filteredOutfits: StateFlow<List<Outfit>> = combine(
        _outfits,
        _filterTagIds
    ) { outfits, filterTags ->
        if (filterTags.isEmpty()) outfits
        else outfits.filter { outfit -> filterTags.all { it in outfit.tags } }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

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
                tags = _selectedTagIds.value,
                onSuccess = {
                    _isLoading.value = false
                    _selectedItems.value = emptyList()
                    _selectedTagIds.value = emptyList()
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

    fun toggleNewOutfitTag(tagId: String) {
        val current = _selectedTagIds.value.toMutableList()
        if (tagId in current) current.remove(tagId) else current.add(tagId)
        _selectedTagIds.value = current
    }

    fun toggleFilterTag(tagId: String) {
        val current = _filterTagIds.value.toMutableList()
        if (tagId in current) current.remove(tagId) else current.add(tagId)
        _filterTagIds.value = current
    }

    fun clearFilterTags() {
        _filterTagIds.value = emptyList()
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
        _selectedTagIds.value = emptyList()
        _errorMessage.value = null
    }
}
