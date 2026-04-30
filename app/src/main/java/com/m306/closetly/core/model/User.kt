package com.m306.closetly.core.model
import com.m306.closetly.avatar.AvatarConfig

data class User(
    val id: String = "",
    val username: String = "",
    val avatar: AvatarConfig = AvatarConfig()
)