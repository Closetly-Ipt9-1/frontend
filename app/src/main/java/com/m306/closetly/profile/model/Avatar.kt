package com.m306.closetly.profile.model

data class Avatar(
    val id: String = "",
    val userId: String = "",
    val type: String = "",
    val imageUrl: String = "",
    val hairColor: String? = null,
    val skinColor: String? = null,
    val frontImageUrl: String? = null,
    val sideImageUrl: String? = null
)