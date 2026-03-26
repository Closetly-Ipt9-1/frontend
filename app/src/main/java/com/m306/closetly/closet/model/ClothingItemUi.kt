package com.m306.closetly.closet.model

data class ClothingItemUi(
    val id: String = "",
    val category: String = "",
    val imageUrl: String = "",
    val color: String? = null,
    val brand: String? = null,
    val size: String? = null
)