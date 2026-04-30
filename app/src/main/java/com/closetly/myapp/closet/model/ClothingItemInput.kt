package com.closetly.myapp.closet.model

data class ClothingItemInput(
    val category: String,
    val imageUrl: String,
    val color: String?,
    val brand: String?,
    val size: String?
)