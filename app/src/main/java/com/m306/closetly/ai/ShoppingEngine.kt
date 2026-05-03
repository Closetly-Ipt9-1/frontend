package com.m306.closetly.ai

import com.closetly.myapp.closet.model.ClothingItemUi
import java.net.URLEncoder

data class ShoppingRecommendation(
    val headline: String,
    val reason: String,
    val searchQuery: String,
    val shoppingUrl: String,
    val tag: String
)

object ShoppingEngine {

    fun generateRecommendation(clothes: List<ClothingItemUi>): ShoppingRecommendation? {
        if (clothes.isEmpty()) return null

        val season       = SeasonEngine.currentSeason()
        val colorProfile = buildColorProfile(clothes)
        val categoryGaps = findCategoryGaps(clothes, season)
        val dominantColor = colorProfile.maxByOrNull { it.value }?.key

        return when {
            categoryGaps.isNotEmpty() -> gapRecommendation(categoryGaps.first(), dominantColor, season)
            dominantColor != null     -> colorRecommendation(dominantColor, season)
            else                      -> seasonRecommendation(season)
        }
    }

    // ── Gap: missing category ─────────────────────────────────────────────────
    private fun gapRecommendation(
        missingCategory: String,
        dominantColor: String?,
        season: Season
    ): ShoppingRecommendation {
        val colorHint = dominantColor ?: "black"
        val seasonLabel = season.displayName().lowercase()

        val (label, query) = when (missingCategory) {
            "top"    -> "Top"    to "$colorHint top $seasonLabel outfit"
            "bottom" -> "Bottom" to "$colorHint pants $seasonLabel"
            "shoes"  -> "Shoes"  to "$colorHint shoes $seasonLabel style"
            "jacket" -> "Jacket" to "$colorHint jacket $seasonLabel outerwear"
            else     -> missingCategory to "$colorHint $missingCategory $seasonLabel"
        }

        return ShoppingRecommendation(
            headline   = "Add a $label to complete your look",
            reason     = "Your wardrobe is missing $label — a $colorHint one would match ${colorProfile_hint(dominantColor)} of your existing pieces.",
            searchQuery = query,
            shoppingUrl = buildGoogleShoppingUrl(query),
            tag        = "gap_$missingCategory"
        )
    }

    // ── Dominant color: suggest complementary item ────────────────────────────
    private fun colorRecommendation(
        dominantColor: String,
        season: Season
    ): ShoppingRecommendation {
        val complement = complementaryColor(dominantColor)
        val seasonLabel = season.displayName().lowercase()
        val query = "$complement ${seasonLabel} outfit clothing"

        return ShoppingRecommendation(
            headline    = "Try $complement to complement your $dominantColor pieces",
            reason      = "$complement pairs perfectly with $dominantColor — a classic combination for ${season.displayName()}.",
            searchQuery = query,
            shoppingUrl = buildGoogleShoppingUrl(query),
            tag         = "color_$dominantColor"
        )
    }

    // ── Season fallback ───────────────────────────────────────────────────────
    private fun seasonRecommendation(season: Season): ShoppingRecommendation {
        val (query, headline, reason) = when (season) {
            Season.SPRING -> Triple(
                "spring casual outfit light layers",
                "Refresh your wardrobe for Spring",
                "Light layers and fresh colors are perfect for Spring."
            )
            Season.SUMMER -> Triple(
                "summer outfit breathable clothing",
                "Stay cool this Summer",
                "Breathable fabrics and bright tones are trending this season."
            )
            Season.AUTUMN -> Triple(
                "autumn outfit earthy tones jacket",
                "Layer up for Autumn",
                "Earthy tones and outerwear define the Autumn look."
            )
            Season.WINTER -> Triple(
                "winter outfit warm dark tones",
                "Stay warm in style this Winter",
                "Dark tones and quality outerwear are key for Winter."
            )
        }

        return ShoppingRecommendation(
            headline    = headline,
            reason      = reason,
            searchQuery = query,
            shoppingUrl = buildGoogleShoppingUrl(query),
            tag         = "season_${season.name.lowercase()}"
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun buildGoogleShoppingUrl(query: String): String {
        val encoded = URLEncoder.encode(query, "UTF-8")
        return "https://www.google.com/search?tbm=shop&q=$encoded"
    }

    private fun complementaryColor(color: String): String = when (color.lowercase()) {
        "black"  -> "white"
        "white"  -> "navy"
        "grey", "gray" -> "burgundy"
        "navy"   -> "camel"
        "blue"   -> "white"
        "beige"  -> "olive"
        "brown"  -> "cream"
        "red"    -> "black"
        "green"  -> "beige"
        "olive"  -> "white"
        "pink"   -> "grey"
        "yellow" -> "navy"
        "orange" -> "navy"
        "purple", "violet" -> "grey"
        else     -> "white"
    }

    private fun colorProfile_hint(color: String?): String =
        if (color != null) "most" else "many"

    private fun buildColorProfile(clothes: List<ClothingItemUi>): Map<String, Int> =
        clothes
            .mapNotNull { it.color?.lowercase()?.trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()

    private fun findCategoryGaps(clothes: List<ClothingItemUi>, season: Season): List<String> {
        val needed  = SeasonEngine.preferredCategories(season)
        val present = clothes.map { it.category.lowercase().trim() }.toSet()
        return needed.filter { it !in present }
    }
}