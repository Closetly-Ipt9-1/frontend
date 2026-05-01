package com.closetly.myapp.closet.model

data class ClothingItemUi(
    val id: String = "",
    val category: String = "",
    val imageUrl: String = "",
    val color: String? = null,
    val brand: String? = null,
    val size: String? = null,
    val tags: List<String> = emptyList()
)