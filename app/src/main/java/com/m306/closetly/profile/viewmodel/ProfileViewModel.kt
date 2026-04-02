package com.m306.closetly.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m306.closetly.profile.data.AvatarRepository
import com.m306.closetly.profile.model.Avatar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val repo = AvatarRepository()

    private val _avatar = MutableStateFlow<Avatar?>(null)
    val avatar: StateFlow<Avatar?> = _avatar

    init {
        loadAvatar()
    }

    fun loadAvatar() {
        viewModelScope.launch {
            _avatar.value = repo.getAvatar()
        }
    }
}