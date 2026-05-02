package com.m306.closetly.ai

import com.closetly.myapp.closet.model.ClothingItemUi
import com.closetly.myapp.tags.model.PredefinedTags

data class AdResult(
    val headline: String,
    val body: String,
    val tag: String
)

object AdEngine {

    fun generateAd(clothes: List<ClothingItemUi>): AdResult {
        if (clothes.isEmpty()) return defaultAd()

        val season        = SeasonEngine.currentSeason()
        val colorProfile  = buildColorProfile(clothes)
        val styleProfile  = buildStyleProfile(clothes)
        val categoryGaps  = findCategoryGaps(clothes, season)
        val dominantColor = colorProfile.maxByOrNull { it.value }?.key
        val dominantStyle = styleProfile.maxByOrNull { it.value }?.key

        return when {
            categoryGaps.isNotEmpty() -> gapAd(categoryGaps.first(), season)
            dominantStyle != null     -> styleAd(dominantStyle, dominantColor, season)
            dominantColor != null     -> colorAd(dominantColor, season)
            else                      -> seasonAd(season)
        }
    }

    fun generateAdAsString(clothes: List<ClothingItemUi>): String {
        val ad = generateAd(clothes)
        return "${ad.headline}\n\n${ad.body}"
    }

    private fun buildColorProfile(clothes: List<ClothingItemUi>): Map<String, Int> =
        clothes
            .mapNotNull { it.color?.lowercase()?.trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()

    private fun buildStyleProfile(clothes: List<ClothingItemUi>): Map<String, Int> =
        clothes
            .mapNotNull { it.style?.lowercase()?.trim() }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()

    private fun findCategoryGaps(
        clothes: List<ClothingItemUi>,
        season: Season
    ): List<String> {
        val needed  = SeasonEngine.preferredCategories(season)
        val present = clothes.map { it.category.lowercase().trim() }.toSet()
        return needed.filter { it !in present }
    }

    private fun gapAd(missingCategory: String, season: Season): AdResult {
        val label = when (missingCategory) {
            "top"    -> "tops"
            "bottom" -> "bottoms"
            "shoes"  -> "shoes"
            "jacket" -> "jackets & outerwear"
            else     -> missingCategory
        }
        return AdResult(
            headline = "Complete your ${season.displayName()} wardrobe",
            body     = "${SeasonEngine.seasonTip(season)} Adding $label would unlock more outfit options.",
            tag      = "gap_$missingCategory"
        )
    }

    private fun styleAd(style: String, dominantColor: String?, season: Season): AdResult {
        val styleMessages = mapOf(
            "occasion_casual"  to "Your casual wardrobe is strong — relaxed fits and earthy tones work great.",
            "occasion_formal"  to "Your formal collection is growing — a classic white shirt is always worth it.",
            "occasion_sport"   to "Your sporty style is on point — performance fabrics are trending now.",
            "occasion_party"   to "Your party looks are bold — statement pieces elevate everything.",
            "occasion_work"    to "Your work wardrobe is solid — clean cuts and neutral tones keep it professional.",
            "style_streetwear" to "Your streetwear game is solid — oversized silhouettes are huge right now.",
            "style_classic"    to "Classic never goes out of style — timeless pieces work across every season.",
            "style_minimal"    to "Minimal is your signature — clean lines and quality fabrics speak for themselves.",
            "style_vintage"    to "Vintage is back — retro cuts are dominating right now.",
            "style_business"   to "Sharp and intentional — your business wardrobe means you mean it."
        )
        val colorHint  = if (dominantColor != null) " Your $dominantColor pieces are a great base." else ""
        val seasonHint = SeasonEngine.seasonTip(season)
        val label      = PredefinedTags.findById(style)?.name ?: style.replaceFirstChar { it.uppercase() }
        return AdResult(
            headline = "$label × ${season.displayName()}",
            body     = (styleMessages[style] ?: "Your wardrobe is looking sharp.") + colorHint + " $seasonHint",
            tag      = style
        )
    }

    private fun colorAd(color: String, season: Season): AdResult {
        val colorMessages = mapOf(
            "black"  to "You love black — a clean white piece creates perfect contrast.",
            "white"  to "Your white pieces are clean and minimal — navy or green pairs beautifully.",
            "grey"   to "Grey is timeless — a pop of burgundy adds depth.",
            "gray"   to "Grey is timeless — a pop of burgundy adds depth.",
            "navy"   to "Navy is a wardrobe staple — white and camel complement it perfectly.",
            "blue"   to "Blue tones are versatile — pair with warm browns or crisp whites.",
            "beige"  to "Beige looks great with black, olive, and soft pastels.",
            "brown"  to "Earth tones are trending — olive greens and warm creams go great here.",
            "red"    to "Bold red stands out — keep the rest neutral to let it shine.",
            "green"  to "Green is having a moment — earthy and forest tones especially.",
            "olive"  to "Olive pairs with almost everything — one of the most wearable colors.",
            "pink"   to "Pink works in any wardrobe — from soft blush to bold fuchsia.",
            "yellow" to "Yellow is confident — navy or white keeps it grounded.",
            "orange" to "Orange is bold and energetic — pair with black or navy to ground it.",
            "violet" to "Violet is striking — keep surrounding pieces neutral to let it pop.",
            "purple" to "Purple is regal and versatile — pairs well with grey and white."
        )
        return AdResult(
            headline = "${color.replaceFirstChar { it.uppercase() }} is your ${season.displayName()} color",
            body     = (colorMessages[color] ?: "Strong color identity — lean into it.") + " ${SeasonEngine.seasonTip(season)}",
            tag      = "color_$color"
        )
    }

    private fun seasonAd(season: Season): AdResult = AdResult(
        headline = "${season.displayName()} Style Guide",
        body     = SeasonEngine.seasonTip(season),
        tag      = "season_${season.name.lowercase()}"
    )

    private fun defaultAd(): AdResult = AdResult(
        headline = "Build your perfect wardrobe",
        body     = "Start adding clothes to get personalized style tips and daily outfit suggestions.",
        tag      = "default"
    )
}