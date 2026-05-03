package com.closetly.myapp.closet.model

data class ClothingItem(
    val id: String = "",
    val userId: String = "",
    val imageUrl: String = "",
    val category: String = "",
    val color: String = "",
    val brand: String = "",
    val size: String = "",
    val purchaseLink: String = "",
    val style: String = "",
    val tags: List<String> = emptyList()
)