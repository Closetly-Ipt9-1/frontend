package com.closetly.myapp.fitcreator.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.closetly.myapp.closet.data.ClosetRepository
import com.closetly.myapp.closet.model.ClothingItemUi
import com.closetly.myapp.fitcreator.data.OutfitRepository
import com.closetly.myapp.fitcreator.model.Outfit
import com.closetly.myapp.premium.data.PremiumAccessRepository
import com.m306.closetly.ai.AiEngine
import com.m306.closetly.ai.DailyOutfitManager
import com.m306.closetly.ai.DailyOutfitResult
import com.m306.closetly.ai.GeneratedOutfit
import com.m306.closetly.ai.SeasonEngine
import com.m306.closetly.ai.WeatherEngine
import com.m306.closetly.ai.WeatherInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FitCreatorViewModel : ViewModel() {

    private val repository              = OutfitRepository()
    private val premiumAccessRepository = PremiumAccessRepository()
    private val closetRepository        = ClosetRepository()
    private val dailyOutfitManager      = DailyOutfitManager()

    companion object {
        private var cachedWardrobe: List<ClothingItemUi>? = null
    }

    // ── Outfit creator state ──────────────────────────────────────────────────
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
        _outfits, _filterTagIds
    ) { outfits, filterTags ->
        if (filterTags.isEmpty()) outfits
        else outfits.filter { outfit -> filterTags.all { it in outfit.tags } }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // ── AI state ──────────────────────────────────────────────────────────────
    private val _dailyOutfit = MutableStateFlow<GeneratedOutfit?>(null)
    val dailyOutfit: StateFlow<GeneratedOutfit?> = _dailyOutfit

    private val _outfitSuggestions = MutableStateFlow<List<GeneratedOutfit>>(emptyList())
    val outfitSuggestions: StateFlow<List<GeneratedOutfit>> = _outfitSuggestions

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading

    private val _aiMessage = MutableStateFlow<String?>(null)
    val aiMessage: StateFlow<String?> = _aiMessage

    private val _wardrobeItems = MutableStateFlow<List<ClothingItemUi>>(emptyList())

    // ── Weather state ─────────────────────────────────────────────────────────
    private val _weatherInfo = MutableStateFlow<WeatherInfo?>(null)
    val weatherInfo: StateFlow<WeatherInfo?> = _weatherInfo

    private val _weatherOutfit = MutableStateFlow<GeneratedOutfit?>(null)
    val weatherOutfit: StateFlow<GeneratedOutfit?> = _weatherOutfit

    // ── Init ──────────────────────────────────────────────────────────────────
    init {
        loadWardrobeAndGenerateAi()
    }

    private fun loadWardrobeAndGenerateAi() {
        val cached = cachedWardrobe
        if (cached != null) {
            _wardrobeItems.value = cached
            generateSuggestions(cached)
            viewModelScope.launch {
                generateDailyOutfit(cached)
                _aiLoading.value = false
            }
            return
        }

        _aiLoading.value = true
        closetRepository.getClothingItems(
            onSuccess = { items ->
                cachedWardrobe = items
                _wardrobeItems.value = items
                generateSuggestions(items)
                viewModelScope.launch {
                    generateDailyOutfit(items)
                    _aiLoading.value = false
                }
            },
            onError = {
                _aiLoading.value = false
                _aiMessage.value = "Konnte Garderobe nicht laden."
            }
        )
    }

    private suspend fun generateDailyOutfit(items: List<ClothingItemUi>) {
        android.util.Log.d("OOTD", "Jackets in wardrobe: ${items.filter { it.category.lowercase() == "jacket" }}")
        val result = dailyOutfitManager.generateTodayOutfit(items)
        _dailyOutfit.value = when (result) {
            is DailyOutfitResult.Generated     -> result.outfit
            is DailyOutfitResult.AlreadyExists -> result.outfit
            is DailyOutfitResult.Error         -> null
        }
        if (result is DailyOutfitResult.Error) {
            _aiMessage.value = result.message
        }
    }

    private fun generateSuggestions(items: List<ClothingItemUi>) {
        val shuffled = items.shuffled()
        _outfitSuggestions.value = AiEngine.generateOutfitSuggestions(
            clothes = shuffled,
            count   = 5,
            season  = SeasonEngine.currentSeason()
        )
    }

    // ── Weather outfit ────────────────────────────────────────────────────────
    fun loadWeatherOutfit(context: Context) {
        viewModelScope.launch {
            _aiLoading.value = true
            val items = _wardrobeItems.value.ifEmpty { cachedWardrobe ?: emptyList() }

            android.util.Log.d("WEATHER_GEN", "Items count: ${items.size}")
            android.util.Log.d("WEATHER_GEN", "Items: ${items.map { "${it.category} ${it.color}" }}")

            if (items.isEmpty()) {
                _aiLoading.value = false
                return@launch
            }

            val weather = WeatherEngine.getCurrentWeather(context)
            _weatherInfo.value = weather

            val season   = SeasonEngine.currentSeason()
            val shuffled = items.shuffled()

            android.util.Log.d("WEATHER_GEN", "Shuffled: ${shuffled.map { "${it.category} ${it.color}" }}")
            android.util.Log.d("WEATHER_GEN", "Temp: ${weather?.tempCelsius}°C")

            if (weather != null) {
                val tempRange = WeatherEngine.getTempRange(weather.tempCelsius)
                val rules     = WeatherEngine.getRecommendedCategories(tempRange, season)
                _weatherOutfit.value = AiEngine.generateWeatherOutfit(shuffled, rules, season)
                    ?: AiEngine.generateBestOutfit(shuffled, season)
            } else {
                // Fallback: kein GPS → trotzdem verschiedene Outfits zeigen
                val suggestions = AiEngine.generateOutfitSuggestions(shuffled, count = 6, season = season)
                _weatherOutfit.value = suggestions.randomOrNull()
                    ?: AiEngine.generateBestOutfit(shuffled, season)
            }

            _aiLoading.value = false
        }
    }

    // ── Regenerate ────────────────────────────────────────────────────────────
    fun regenerateDailyOutfit() {
        viewModelScope.launch {
            _aiLoading.value = true

            val items = _wardrobeItems.value.ifEmpty {
                cachedWardrobe ?: emptyList()
            }

            if (items.isEmpty()) {
                _aiMessage.value = "No clothes found"
                _aiLoading.value = false
                return@launch
            }

            val result = dailyOutfitManager.regenerateTodayOutfit(items)
            _dailyOutfit.value = when (result) {
                is DailyOutfitResult.Generated     -> result.outfit
                is DailyOutfitResult.AlreadyExists -> result.outfit
                is DailyOutfitResult.Error         -> {
                    _aiMessage.value = result.message
                    null
                }
            }
            _aiLoading.value = false
        }
    }

    fun loadSuggestionIntoCreator(outfit: GeneratedOutfit) {
        _selectedItems.value = listOfNotNull(
            outfit.top, outfit.bottom, outfit.jacket, outfit.shoes
        )
        _errorMessage.value = null
    }

    // ── Wardrobe actions ──────────────────────────────────────────────────────
    fun addItem(item: ClothingItemUi) {
        val currentItems = _selectedItems.value
        if (currentItems.any { it.category == item.category }) {
            _errorMessage.value = "You can only select one item per category"
            return
        }
        if (hasConflictingCategory(currentItems, item)) {
            _errorMessage.value = "You cannot select ${item.category} and another top at the same time"
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
            onError("Please select at least one pair of pants and one top")
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
                caption       = caption,
                imageUrl      = imageUrl,
                clothingItems = _selectedItems.value,
                isPublic      = isPublic,
                username      = username,
                tags          = _selectedTagIds.value,
                onSuccess     = {
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

    // ── Outfit management ─────────────────────────────────────────────────────
    fun loadMyOutfits() {
        _isLoading.value = true
        repository.getMyOutfits(
            onSuccess = { _outfits.value = it; _isLoading.value = false },
            onError   = { _isLoading.value = false; _errorMessage.value = it.message }
        )
    }

    fun toggleOutfitVisibility(outfitId: String, isPublic: Boolean) {
        repository.updateOutfitVisibility(outfitId, isPublic, onSuccess = {}, onError = { _errorMessage.value = it.message })
    }

    fun deleteOutfit(outfitId: String) {
        repository.deleteOutfit(
            outfitId,
            onSuccess = { _outfits.value = _outfits.value.filter { it.id != outfitId } },
            onError   = { _errorMessage.value = it.message }
        )
    }

    // ── Tag management ────────────────────────────────────────────────────────
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

    fun clearFilterTags() { _filterTagIds.value = emptyList() }

    fun clearSelection() {
        _selectedItems.value = emptyList()
        _selectedTagIds.value = emptyList()
        _errorMessage.value = null
    }

    private fun hasConflictingCategory(currentItems: List<ClothingItemUi>, newItem: ClothingItemUi): Boolean {
        val conflicts = mapOf(
            "Shirt"   to listOf("Pullover", "Jumper"),
            "T-Shirt" to listOf("Pullover", "Jumper")
        )
        for ((cat, conflicted) in conflicts) {
            if (currentItems.any { it.category == cat } && newItem.category in conflicted) return true
            if (newItem.category == cat && currentItems.any { it.category in conflicted }) return true
        }
        return false
    }
}
