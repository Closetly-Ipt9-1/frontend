package com.m306.closetly.closet.model

data class ClothingItemInput(
    val category: String,
    val imageUrl: String,
    val color: String?,
    val brand: String?,
    val size: String?
)