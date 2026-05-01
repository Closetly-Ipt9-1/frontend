package com.closetly.myapp.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.closetly.myapp.profile.data.AvatarRepository
import com.closetly.myapp.profile.model.Avatar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CreateAvatarViewModel : ViewModel() {

    private val repo = AvatarRepository()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved

    private val _existingAvatar = MutableStateFlow<Avatar?>(null)
    val existingAvatar: StateFlow<Avatar?> = _existingAvatar

    fun loadExistingAvatar() {
        viewModelScope.launch {
            _existingAvatar.value = repo.getAvatar()
        }
    }

    fun resetSaved() {
        _isSaved.value = false
    }

    fun saveDefaultAvatar(gender: String, hair: String, skin: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val imageUrl = repo.getDefaultAvatarUrl(gender, skin, hair)
            repo.saveAvatar(
                Avatar(
                    gender = gender,
                    hairColor = hair,
                    skinColor = skin,
                    imageUrl = imageUrl
                )
            )
            _isLoading.value = false
            _isSaved.value = true
        }
    }

}
