package com.m306.closetly.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m306.closetly.profile.data.AvatarRepository
import com.m306.closetly.profile.model.Avatar
import kotlinx.coroutines.launch

class CreateAvatarViewModel : ViewModel() {

    private val repo = AvatarRepository()

    fun saveDefaultAvatar(hair: String, skin: String, imageUrl: String) {
        viewModelScope.launch {
            repo.saveAvatar(
                Avatar(
                    type = "default",
                    hairColor = hair,
                    skinColor = skin,
                    imageUrl = imageUrl
                )
            )
        }
    }

    fun saveCustomAvatar(front: String, side: String) {
        viewModelScope.launch {
            repo.saveAvatar(
                Avatar(
                    type = "custom",
                    frontImageUrl = front,
                    sideImageUrl = side
                )
            )
        }
    }
}