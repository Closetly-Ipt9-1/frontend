package com.m306.closetly.ai

import com.closetly.myapp.closet.model.ClothingItemUi

private val COLOR_COMPATIBILITY: Map<String, List<String>> = mapOf(
    "black"  to listOf("white", "grey", "red", "blue", "beige", "yellow", "pink", "green"),
    "white"  to listOf("black", "navy", "grey", "blue", "beige", "brown", "green"),
    "grey"   to listOf("black", "white", "navy", "blue", "pink", "purple"),
    "navy"   to listOf("white", "grey", "beige", "light blue", "yellow"),
    "blue"   to listOf("white", "grey", "beige", "brown", "navy"),
    "beige"  to listOf("black", "white", "brown", "navy", "blue", "olive"),
    "brown"  to listOf("beige", "white", "olive", "blue", "orange"),
    "red"    to listOf("black", "white", "grey", "navy"),
    "green"  to listOf("beige", "white", "brown", "black", "olive"),
    "olive"  to listOf("beige", "white", "brown", "black", "orange"),
    "pink"   to listOf("white", "grey", "black", "navy"),
    "yellow" to listOf("black", "navy", "white", "grey"),
    "purple" to listOf("white", "grey", "black", "beige"),
    "orange" to listOf("black", "white", "navy", "brown"),
    "gray"   to listOf("black", "white", "navy", "blue", "pink", "purple"),
    "violet" to listOf("white", "grey", "black", "beige")
)

private val STYLE_COMPATIBILITY: Map<String, List<String>> = mapOf(
    "occasion_casual"  to listOf("occasion_casual", "style_streetwear", "style_minimal", "occasion_sport"),
    "occasion_formal"  to listOf("occasion_formal", "style_business", "style_classic"),
    "occasion_sport"   to listOf("occasion_sport", "occasion_casual", "style_streetwear"),
    "occasion_party"   to listOf("occasion_party", "style_streetwear", "style_vintage"),
    "occasion_work"    to listOf("occasion_work", "style_business", "style_classic", "occasion_formal"),
    "style_streetwear" to listOf("style_streetwear", "occasion_casual", "occasion_sport", "style_vintage"),
    "style_classic"    to listOf("style_classic", "occasion_formal", "occasion_work", "style_minimal"),
    "style_minimal"    to listOf("style_minimal", "style_classic", "occasion_casual"),
    "style_vintage"    to listOf("style_vintage", "style_streetwear", "occasion_casual", "occasion_party"),
    "style_business"   to listOf("style_business", "occasion_work", "occasion_formal", "style_classic")
)

private fun normalizeCategory(category: String): String = when (category.lowercase().trim()) {
    "jacket"   -> "jacket"
    "pants"    -> "bottom"
    "pullover" -> "top"
    "shirt"    -> "top"
    "shoes"    -> "shoes"
    "watch"    -> "watch"
    else       -> category.lowercase().trim()
}

data class GeneratedOutfit(
    val top: ClothingItemUi,
    val bottom: ClothingItemUi,
    val jacket: ClothingItemUi? = null,
    val shoes: ClothingItemUi? = null,
    val watch: ClothingItemUi? = null,
    val score: Int = 0
)

object AiEngine {

    fun generateBestOutfit(
        clothes: List<ClothingItemUi>,
        season: Season = SeasonEngine.currentSeason()
    ): GeneratedOutfit? {
        val tops    = clothes.filter { normalizeCategory(it.category) == "top" }
        val bottoms = clothes.filter { normalizeCategory(it.category) == "bottom" }
        val jackets = clothes.filter { normalizeCategory(it.category) == "jacket" }
        val shoes   = clothes.filter { normalizeCategory(it.category) == "shoes" }
        val watches = clothes.filter { normalizeCategory(it.category) == "watch" }

        if (tops.isEmpty() || bottoms.isEmpty()) return null

        val includeJacket = season == Season.WINTER || season == Season.AUTUMN

        var bestOutfit: GeneratedOutfit? = null
        var highestScore = -1

        for (top in tops) {
            for (bottom in bottoms) {
                val topColor    = top.color.orEmpty()
                val bottomColor = bottom.color.orEmpty()
                val topStyle    = top.style.orEmpty()

                val baseScore = scoreColorMatch(topColor, bottomColor) +
                        scoreStyleMatch(topStyle, bottom.style.orEmpty()) +
                        SeasonEngine.seasonColorBonus(topColor, season) +
                        SeasonEngine.seasonColorBonus(bottomColor, season) +
                        SeasonEngine.seasonStyleBonus(topStyle, season)

                val bestJacket = if (includeJacket) {
                    jackets.maxByOrNull {
                        scoreColorMatch(it.color.orEmpty(), topColor) +
                                SeasonEngine.seasonColorBonus(it.color.orEmpty(), season) +
                                SeasonEngine.seasonCategoryBonus("jacket", season)
                    }
                } else null

                val bestShoes = shoes.maxByOrNull {
                    scoreColorMatch(it.color.orEmpty(), bottomColor) +
                            SeasonEngine.seasonColorBonus(it.color.orEmpty(), season)
                }

                val bestWatch = watches.firstOrNull()

                val jacketScore = bestJacket?.let {
                    scoreColorMatch(it.color.orEmpty(), topColor) +
                            SeasonEngine.seasonCategoryBonus("jacket", season)
                } ?: 0

                val shoesScore = bestShoes?.let {
                    scoreColorMatch(it.color.orEmpty(), bottomColor)
                } ?: 0

                val totalScore = baseScore + shoesScore + jacketScore

                if (totalScore > highestScore) {
                    highestScore = totalScore
                    bestOutfit = GeneratedOutfit(
                        top    = top,
                        bottom = bottom,
                        jacket = bestJacket,
                        shoes  = bestShoes,
                        watch  = bestWatch,
                        score  = totalScore
                    )
                }
            }
        }
        return bestOutfit
    }

    fun generateOutfitSuggestions(
        clothes: List<ClothingItemUi>,
        count: Int = 3,
        season: Season = SeasonEngine.currentSeason()
    ): List<GeneratedOutfit> {
        val tops    = clothes.filter { normalizeCategory(it.category) == "top" }.shuffled()
        val bottoms = clothes.filter { normalizeCategory(it.category) == "bottom" }.shuffled()
        val jackets = clothes.filter { normalizeCategory(it.category) == "jacket" }
        val shoes   = clothes.filter { normalizeCategory(it.category) == "shoes" }
        val watches = clothes.filter { normalizeCategory(it.category) == "watch" }

        if (tops.isEmpty() || bottoms.isEmpty()) return emptyList()

        val includeJacket = season == Season.WINTER || season == Season.AUTUMN

        val allCombinations = mutableListOf<GeneratedOutfit>()

        for (top in tops) {
            for (bottom in bottoms) {
                val topColor    = top.color.orEmpty()
                val bottomColor = bottom.color.orEmpty()
                val topStyle    = top.style.orEmpty()

                val score = scoreColorMatch(topColor, bottomColor) +
                        scoreStyleMatch(topStyle, bottom.style.orEmpty()) +
                        SeasonEngine.seasonColorBonus(topColor, season) +
                        SeasonEngine.seasonColorBonus(bottomColor, season) +
                        SeasonEngine.seasonStyleBonus(topStyle, season)

                val bestJacket = if (includeJacket) {
                    jackets.maxByOrNull {
                        scoreColorMatch(it.color.orEmpty(), topColor) +
                                SeasonEngine.seasonCategoryBonus(it.category, season)
                    }
                } else null

                val bestShoes = shoes.maxByOrNull {
                    scoreColorMatch(it.color.orEmpty(), bottomColor)
                }

                val bestWatch = watches.firstOrNull()

                allCombinations.add(
                    GeneratedOutfit(
                        top    = top,
                        bottom = bottom,
                        jacket = bestJacket,
                        shoes  = bestShoes,
                        watch  = bestWatch,
                        score  = score
                    )
                )
            }
        }

        val sorted = allCombinations.sortedByDescending { it.score }

        val result    = mutableListOf<GeneratedOutfit>()
        val usedTopIds = mutableSetOf<String>()

        for (outfit in sorted) {
            if (outfit.top.id !in usedTopIds) {
                result.add(outfit)
                usedTopIds.add(outfit.top.id)
            }
            if (result.size >= count) break
        }

        if (result.size < count) {
            for (outfit in sorted) {
                if (outfit !in result) result.add(outfit)
                if (result.size >= count) break
            }
        }

        return result
    }

    fun generateWeatherOutfit(
        clothes: List<ClothingItemUi>,
        rules: WeatherOutfitRules,
        season: Season = SeasonEngine.currentSeason()
    ): GeneratedOutfit? {
        val tops = clothes.filter { normalizeCategory(it.category) == "top" }
            .filter { item ->
                val cat = item.category.lowercase()
                when {
                    rules.needsPullover && !rules.allowsShirt -> cat == "pullover"
                    rules.allowsShirt && !rules.needsPullover -> cat == "shirt"
                    else -> cat == "pullover" || cat == "shirt"
                }
            }
            .shuffled() // ← neu
            .ifEmpty {
                clothes.filter { normalizeCategory(it.category) == "top" }.shuffled()
            }

        val bottoms = clothes.filter { normalizeCategory(it.category) == "bottom" }.shuffled() // ← neu
        val jackets = clothes.filter { normalizeCategory(it.category) == "jacket" }
        val shoes   = clothes.filter { normalizeCategory(it.category) == "shoes" }
        val watches = clothes.filter { normalizeCategory(it.category) == "watch" }

        if (tops.isEmpty() || bottoms.isEmpty()) return null
        if (shoes.isEmpty()) return null

        // Anstatt bestes Outfit — zufällige valide Kombination zurückgeben
        // damit Regenerate immer etwas anderes zeigt
        val allCombinations = mutableListOf<GeneratedOutfit>()

        for (top in tops) {
            for (bottom in bottoms) {
                val topColor    = top.color.orEmpty()
                val bottomColor = bottom.color.orEmpty()
                val topStyle    = top.style.orEmpty()

                val baseScore = scoreColorMatch(topColor, bottomColor) +
                        scoreStyleMatch(topStyle, bottom.style.orEmpty()) +
                        SeasonEngine.seasonColorBonus(topColor, season) +
                        SeasonEngine.seasonColorBonus(bottomColor, season) +
                        SeasonEngine.seasonStyleBonus(topStyle, season)

                val bestJacket = if (rules.needsJacket) {
                    jackets.maxByOrNull {
                        scoreColorMatch(it.color.orEmpty(), topColor) +
                                SeasonEngine.seasonColorBonus(it.color.orEmpty(), season)
                    }
                } else null

                val bestShoes = shoes.maxByOrNull {
                    scoreColorMatch(it.color.orEmpty(), bottomColor)
                }

                val bestWatch = watches.firstOrNull()

                val jacketScore = bestJacket?.let { scoreColorMatch(it.color.orEmpty(), topColor) } ?: 0
                val shoesScore  = bestShoes?.let  { scoreColorMatch(it.color.orEmpty(), bottomColor) } ?: 0
                val totalScore  = baseScore + jacketScore + shoesScore

                allCombinations.add(
                    GeneratedOutfit(
                        top    = top,
                        bottom = bottom,
                        jacket = bestJacket,
                        shoes  = bestShoes,
                        watch  = bestWatch,
                        score  = totalScore
                    )
                )
            }
        }

        if (allCombinations.isEmpty()) return null

        // Nicht immer das beste — rotiere durch Top-3 um Variety zu garantieren
        val top3 = allCombinations.sortedByDescending { it.score }.take(3)
        return top3.random()
    }

    fun scoreColorMatch(color1: String, color2: String): Int {
        val c1 = color1.lowercase().trim()
        val c2 = color2.lowercase().trim()
        if (c1 == c2) return 2
        val compatible = COLOR_COMPATIBILITY[c1] ?: return 0
        return if (c2 in compatible) 10 else 0
    }

    fun scoreStyleMatch(style1: String, style2: String): Int {
        val s1 = style1.lowercase().trim()
        val s2 = style2.lowercase().trim()
        if (s1.isEmpty() || s2.isEmpty()) return 0
        if (s1 == s2) return 10
        val compatible = STYLE_COMPATIBILITY[s1] ?: return 0
        return if (s2 in compatible) 6 else 0
    }

    fun describeOutfit(outfit: GeneratedOutfit): String {
        val sb = StringBuilder()
        sb.append("${outfit.top.color.orEmpty().replaceFirstChar { it.uppercase() }} ${outfit.top.brand.orEmpty().ifEmpty { outfit.top.category }}")
        sb.append(" + ${outfit.bottom.color.orEmpty().replaceFirstChar { it.uppercase() }} ${outfit.bottom.brand.orEmpty().ifEmpty { outfit.bottom.category }}")
        outfit.jacket?.let { sb.append(" + ${it.color.orEmpty().replaceFirstChar { it.uppercase() }} Jacket") }
        outfit.shoes?.let  { sb.append(" + ${it.color.orEmpty().replaceFirstChar { it.uppercase() }} Shoes") }
        outfit.watch?.let  { sb.append(" + ${it.brand.orEmpty().ifEmpty { "Watch" }}") }
        return sb.toString()
    }
}