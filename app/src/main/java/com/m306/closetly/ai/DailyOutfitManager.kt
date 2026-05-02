package com.m306.closetly.ai

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.closetly.myapp.closet.model.ClothingItemUi
import com.closetly.myapp.closet.data.ClosetRepository
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class DailyOutfitResult {
    data class Generated(val outfit: GeneratedOutfit)     : DailyOutfitResult()
    data class AlreadyExists(val outfit: GeneratedOutfit) : DailyOutfitResult()
    data class Error(val message: String)                 : DailyOutfitResult()
}

class DailyOutfitManager {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val DATE_FORMAT = "yyyy-MM-dd"
    }

    suspend fun generateTodayOutfit(clothes: List<ClothingItemUi>): DailyOutfitResult {
        val userId = auth.currentUser?.uid
            ?: return DailyOutfitResult.Error("User not logged in")

        val today = todayDateString()
        val existing = loadTodayOutfit(userId, today)
        if (existing != null) return DailyOutfitResult.AlreadyExists(existing)

        if (clothes.isEmpty()) return DailyOutfitResult.Error("No clothes in wardrobe")

        val outfit = AiEngine.generateBestOutfit(clothes)
            ?: return DailyOutfitResult.Error("Need at least 1 top and 1 bottom")

        saveTodayOutfit(userId, today, outfit)
        return DailyOutfitResult.Generated(outfit)
    }

    suspend fun regenerateTodayOutfit(clothes: List<ClothingItemUi>): DailyOutfitResult {
        val userId = auth.currentUser?.uid
            ?: return DailyOutfitResult.Error("User not logged in")

        if (clothes.isEmpty()) return DailyOutfitResult.Error("No clothes in wardrobe")

        val today   = todayDateString()
        val current = loadTodayOutfit(userId, today)

        val suggestions = AiEngine.generateOutfitSuggestions(clothes, count = 5)
        val picked = suggestions.firstOrNull {
            it.top.id != current?.top?.id || it.bottom.id != current.bottom.id
        } ?: suggestions.firstOrNull()
        ?: return DailyOutfitResult.Error("Could not generate a new outfit")

        saveTodayOutfit(userId, today, picked)
        return DailyOutfitResult.Generated(picked)
    }

    suspend fun getTodayOutfit(): GeneratedOutfit? {
        val userId = auth.currentUser?.uid ?: return null
        return loadTodayOutfit(userId, todayDateString())
    }

    private suspend fun saveTodayOutfit(userId: String, date: String, outfit: GeneratedOutfit) {
        val data = mapOf(
            "date"        to date,
            "score"       to outfit.score,
            "topId"       to outfit.top.id,
            "bottomId"    to outfit.bottom.id,
            "jacketId"    to (outfit.jacket?.id ?: ""),
            "shoesId"     to (outfit.shoes?.id ?: ""),
            "topData"     to outfit.top.toMap(),
            "bottomData"  to outfit.bottom.toMap(),
            "jacketData"  to (outfit.jacket?.toMap() ?: emptyMap<String, Any>()),
            "shoesData"   to (outfit.shoes?.toMap() ?: emptyMap<String, Any>()),
            "description" to AiEngine.describeOutfit(outfit),
            "generatedAt" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(userId)
            .collection("dailyOutfit")
            .document(date)
            .set(data)
            .await()
    }

    private suspend fun loadTodayOutfit(userId: String, date: String): GeneratedOutfit? {
        return try {
            val doc = db.collection("users")
                .document(userId)
                .collection("dailyOutfit")
                .document(date)
                .get()
                .await()

            if (!doc.exists()) return null

            val topData    = doc.get("topData")    as? Map<*, *> ?: return null
            val bottomData = doc.get("bottomData") as? Map<*, *> ?: return null
            val jacketData = doc.get("jacketData") as? Map<*, *>
            val shoesData  = doc.get("shoesData")  as? Map<*, *>

            GeneratedOutfit(
                top    = topData.toClothingItemUi(),
                bottom = bottomData.toClothingItemUi(),
                jacket = jacketData?.takeIf { it["id"] != "" }?.toClothingItemUi(),
                shoes  = shoesData?.takeIf  { it["id"] != "" }?.toClothingItemUi(),
                score  = (doc.getLong("score") ?: 0).toInt()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun todayDateString(): String =
        SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(Date())
}

// ── Extension functions ───────────────────────────────────────────────────────

fun ClothingItemUi.toMap(): Map<String, Any?> = mapOf(
    "id"          to id,
    "category"    to category,
    "imageUrl"    to imageUrl,
    "color"       to color,
    "brand"       to brand,
    "size"        to size,
    "purchaseLink" to purchaseLink,
    "style"       to style,
    "tags"        to tags
)

@Suppress("UNCHECKED_CAST")
fun Map<*, *>.toClothingItemUi() = ClothingItemUi(
    id          = this["id"]          as? String ?: "",
    category    = this["category"]    as? String ?: "",
    imageUrl    = this["imageUrl"]    as? String ?: "",
    color       = this["color"]       as? String,
    brand       = this["brand"]       as? String,
    size        = this["size"]        as? String,
    purchaseLink = this["purchaseLink"] as? String,
    style       = this["style"]       as? String,
    tags        = (this["tags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
)