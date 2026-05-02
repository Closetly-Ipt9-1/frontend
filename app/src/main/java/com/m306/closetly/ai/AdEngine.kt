package com.m306.closetly.ai

import com.m306.closetly.data.ClothingItem

object AdEngine {

    fun generateAd(clothes: List<ClothingItem>): String {

        val colors = clothes.map { it.color.lowercase() }

        return when {
            colors.contains("black") ->
                "🔥 Black outfits trending — check new drops!"

            colors.contains("blue") ->
                "👖 New denim styles just arrived!"

            else ->
                "✨ Discover new styles for your wardrobe!"
        }

    }
}