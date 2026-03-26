package com.m306.closetly.closet.data

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.m306.closetly.closet.func.Images
import com.m306.closetly.closet.model.ClothingItemUi

class ClosetRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val images = Images()

    fun saveClothingItem(
        imageUri: Uri,
        category: String,
        color: String,
        brand: String,
        size: String,
        onSuccess: (ClothingItemUi) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onError(Exception("User not logged in"))
            return
        }

        images.uploadClothesImage(
            imageUri = imageUri,
            onSuccess = { imageUrl ->

                val data = hashMapOf(
                    "userId" to userId,
                    "category" to category,
                    "imageUrl" to imageUrl,
                    "color" to color.ifBlank { null },
                    "brand" to brand.ifBlank { null },
                    "size" to size.ifBlank { null },
                    "createdAt" to System.currentTimeMillis()
                )

                db.collection("clothingItems")
                    .add(data)
                    .addOnSuccessListener { documentRef ->
                        onSuccess(
                            ClothingItemUi(
                                id = documentRef.id,
                                category = category,
                                imageUrl = imageUrl,
                                color = color.ifBlank { null },
                                brand = brand.ifBlank { null },
                                size = size.ifBlank { null }
                            )
                        )
                    }
                    .addOnFailureListener {
                        onError(it)
                    }
            },
            onError = onError
        )
    }

    fun getClothingItems(
        onSuccess: (List<ClothingItemUi>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onError(Exception("User not logged in"))
            return
        }

        db.collection("clothingItems")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                val items = result.map {
                    ClothingItemUi(
                        id = it.id,
                        category = it.getString("category") ?: "",
                        imageUrl = it.getString("imageUrl") ?: "",
                        color = it.getString("color"),
                        brand = it.getString("brand"),
                        size = it.getString("size")
                    )
                }
                onSuccess(items)
            }
            .addOnFailureListener {
                onError(it)
            }
    }
}