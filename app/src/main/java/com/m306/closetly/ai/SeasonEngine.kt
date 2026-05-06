package com.m306.closetly.ai

import java.util.Calendar

enum class Season {
    SPRING, SUMMER, AUTUMN, WINTER;

    fun displayName(): String = when (this) {
        SPRING -> "Spring"
        SUMMER -> "Summer"
        AUTUMN -> "Autumn"
        WINTER -> "Winter"
    }
}

object SeasonEngine {

    fun currentSeason(): Season = when (Calendar.getInstance().get(Calendar.MONTH)) {
        Calendar.MARCH, Calendar.APRIL, Calendar.MAY            -> Season.SPRING
        Calendar.JUNE, Calendar.JULY, Calendar.AUGUST           -> Season.SUMMER
        Calendar.SEPTEMBER, Calendar.OCTOBER, Calendar.NOVEMBER -> Season.AUTUMN
        else                                                      -> Season.WINTER
    }

    fun preferredCategories(season: Season): List<String> = when (season) {
        Season.SPRING -> listOf("top", "bottom", "jacket")
        Season.SUMMER -> listOf("top", "bottom", "shoes")
        Season.AUTUMN -> listOf("top", "bottom", "jacket", "shoes")
        Season.WINTER -> listOf("top", "bottom", "jacket", "shoes")
    }

    fun seasonColorBonus(color: String, season: Season): Int {
        val preferred = preferredColors(season)
        return if (color.lowercase().trim() in preferred) 5 else 0
    }

    private fun preferredColors(season: Season): List<String> = when (season) {
        Season.SPRING -> listOf("white", "pink", "light blue", "beige", "green", "yellow", "lavender")
        Season.SUMMER -> listOf("white", "yellow", "orange", "red", "light blue", "coral", "turquoise")
        Season.AUTUMN -> listOf("brown", "olive", "orange", "burgundy", "beige", "rust", "camel")
        Season.WINTER -> listOf("black", "navy", "grey", "white", "dark green", "burgundy", "camel")
    }

    fun seasonCategoryBonus(category: String, season: Season): Int {
        val cat = category.lowercase().trim()
        return when (season) {
            Season.WINTER -> if (cat == "jacket") 8 else 0
            Season.AUTUMN -> if (cat == "jacket") 5 else 0
            Season.SUMMER -> if (cat == "jacket") -3 else 0
            Season.SPRING -> if (cat == "jacket") 2 else 0
        }
    }

    fun seasonStyleBonus(style: String, season: Season): Int {
        val preferred = preferredStyles(season)
        return if (style.lowercase().trim() in preferred) 4 else 0
    }

    private fun preferredStyles(season: Season): List<String> = when (season) {
        Season.SPRING -> listOf("occasion_casual", "style_minimal", "style_classic")
        Season.SUMMER -> listOf("occasion_casual", "occasion_sport", "style_streetwear", "style_minimal")
        Season.AUTUMN -> listOf("style_classic", "style_streetwear", "occasion_casual", "style_vintage")
        Season.WINTER -> listOf("occasion_formal", "style_business", "style_classic", "style_minimal")
    }

    fun seasonTip(season: Season): String = when (season) {
        Season.SPRING -> "Spring is here - light layers and fresh colors work best."
        Season.SUMMER -> "Keep it light and bright - breathable fabrics and bold tones."
        Season.AUTUMN -> "Autumn calls for earthy tones and layering with jackets."
        Season.WINTER -> "Stay warm in style - dark tones and outerwear are key."
    }
}
