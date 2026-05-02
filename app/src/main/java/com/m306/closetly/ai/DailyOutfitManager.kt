package com.m306.closetly.ai

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.m306.closetly.data.ClothingRepository
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class DailyOutfitManager {

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    suspend fun generateTodayOutfit() {

        if (userId == null) return

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Date())

        val docRef = db.collection("users")
            .document(userId)
            .collection("dailyOutfit")
            .document("today")

        val existing = docRef.get().await()

        if (existing.exists() && existing.getString("date") == today) {
            return
        }

        val clothes = ClothingRepository().getUserClothes()
        val outfit = OutfitGenerator.generateOutfit(clothes)

        val data = mapOf(
            "date" to today,
            "topImage" to outfit.top?.imageUrl,
            "bottomImage" to outfit.bottom?.imageUrl
        )

        docRef.set(data)
    }
}