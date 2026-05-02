package com.m306.closetly.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class ClothingItem(
    val id: String = "",
    val category: String = "",
    val color: String = "",
    val imageUrl: String = "",
    val brand: String = "",
    val size: String = ""
)

class ClothingRepository {

    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    suspend fun getUserClothes(): List<ClothingItem> {
        if (userId == null) return emptyList()

        val snapshot = db.collection("clothingItems")
            .whereEqualTo("userId", userId)
            .get()
            .await()

        return snapshot.documents.map {
            ClothingItem(
                id = it.id,
                category = it.getString("category") ?: "",
                color = it.getString("color") ?: "",
                imageUrl = it.getString("imageUrl") ?: "",
                brand = it.getString("brand") ?: "",
                size = it.getString("size") ?: ""
            )
        }
    }
}