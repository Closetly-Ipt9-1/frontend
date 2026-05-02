package com.m306.closetly.ai

import com.m306.closetly.data.ClothingItem
import kotlin.random.Random

data class Outfit(
    val top: ClothingItem?,
    val bottom: ClothingItem?
)

object OutfitGenerator {

    fun generateOutfit(clothes: List<ClothingItem>): Outfit {

        val tops = clothes.filter {
            it.category.lowercase().contains("shirt") ||
                    it.category.lowercase().contains("hoodie") ||
                    it.category.lowercase().contains("top")
        }

        val bottoms = clothes.filter {
            it.category.lowercase().contains("pants") ||
                    it.category.lowercase().contains("jeans")
        }

        val randomTop = if (tops.isNotEmpty()) tops.random() else null
        val randomBottom = if (bottoms.isNotEmpty()) bottoms.random() else null

        return Outfit(randomTop, randomBottom)
    }
}