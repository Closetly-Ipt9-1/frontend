package com.m306.closetly.ai

import com.closetly.myapp.closet.model.ClothingItemUi

object OutfitGenerator {

    // ── Single best outfit ────────────────────────────────────────────────────
    fun getBestOutfit(clothes: List<ClothingItemUi>): GeneratedOutfit? =
        AiEngine.generateBestOutfit(clothes)

    // ── Multiple suggestions ──────────────────────────────────────────────────
    fun getSuggestions(
        clothes: List<ClothingItemUi>,
        count: Int = 3
    ): List<GeneratedOutfit> =
        AiEngine.generateOutfitSuggestions(clothes, count)

    // ── Season-aware best outfit ──────────────────────────────────────────────
    fun getBestOutfitForSeason(
        clothes: List<ClothingItemUi>,
        season: Season
    ): GeneratedOutfit? =
        AiEngine.generateBestOutfit(clothes, season)

    // ── Season-aware suggestions ──────────────────────────────────────────────
    fun getSuggestionsForSeason(
        clothes: List<ClothingItemUi>,
        season: Season,
        count: Int = 3
    ): List<GeneratedOutfit> =
        AiEngine.generateOutfitSuggestions(clothes, count, season)

    // ── Current season ────────────────────────────────────────────────────────
    fun currentSeason(): Season = SeasonEngine.currentSeason()

    // ── Ad for wardrobe ───────────────────────────────────────────────────────
    fun getAd(clothes: List<ClothingItemUi>): AdResult =
        AdEngine.generateAd(clothes)

    // ── Human-readable outfit description ────────────────────────────────────
    fun describe(outfit: GeneratedOutfit): String =
        AiEngine.describeOutfit(outfit)
}