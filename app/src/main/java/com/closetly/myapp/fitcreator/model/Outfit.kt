package com.closetly.myapp.fitcreator.model

import com.closetly.myapp.closet.model.ClothingItemUi

data class Outfit(
    val id: String = "",
    val ownerId: String = "",
    val username: String = "",
    val caption: String = "",
    val imageUrl: String = "",
    val clothingItems: List<ClothingItemUi> = emptyList(),
    val isPublic: Boolean = false,
    val likeCount: Int = 0,
    val likedBy: List<String> = emptyList(),
    val saveCount: Int = 0,
    val savedBy: List<String> = emptyList(),
    val createdAt: Long = 0
)

