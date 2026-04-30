package com.closetly.myapp.fitcreator.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.closetly.myapp.closet.model.ClothingItemUi
import com.closetly.myapp.fitcreator.model.Outfit

class OutfitRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun saveOutfit(
        caption: String,
        imageUrl: String,
        clothingItems: List<ClothingItemUi>,
        isPublic: Boolean,
        username: String,
        tags: List<String> = emptyList(),
        onSuccess: (Outfit) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onError(Exception("User not logged in"))
            return
        }

        val clothingItemsData = clothingItems.map { item ->
            mapOf(
                "id" to item.id,
                "category" to item.category,
                "imageUrl" to item.imageUrl,
                "color" to item.color,
                "brand" to item.brand,
                "size" to item.size
            )
        }

        val createdAt = System.currentTimeMillis()

        val data = hashMapOf(
            "ownerId" to userId,
            "username" to username,
            "caption" to caption,
            "imageUrl" to imageUrl,
            "clothingItems" to clothingItemsData,
            "isPublic" to isPublic,
            "likeCount" to 0,
            "likedBy" to emptyList<String>(),
            "saveCount" to 0,
            "savedBy" to emptyList<String>(),
            "createdAt" to createdAt,
            "tags" to tags
        )

        db.collection("outfits")
            .add(data)
            .addOnSuccessListener { documentRef ->
                onSuccess(
                    Outfit(
                        id = documentRef.id,
                        ownerId = userId,
                        username = username,
                        caption = caption,
                        imageUrl = imageUrl,
                        clothingItems = clothingItems,
                        isPublic = isPublic,
                        likeCount = 0,
                        likedBy = emptyList(),
                        saveCount = 0,
                        savedBy = emptyList(),
                        createdAt = createdAt,
                        tags = tags
                    )
                )
            }
            .addOnFailureListener {
                onError(it)
            }
    }

    fun getMyOutfits(
        onSuccess: (List<Outfit>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onError(Exception("User not logged in"))
            return
        }

        db.collection("outfits")
            .whereEqualTo("ownerId", userId)
            .get()
            .addOnSuccessListener { result ->
                val outfits = result.mapNotNull { document ->
                    try {
                        val clothingItemsData =
                            (document.get("clothingItems") as? List<*>) ?: emptyList<Any>()

                        val clothingItems = clothingItemsData.mapNotNull { item ->
                            if (item is Map<*, *>) {
                                @Suppress("UNCHECKED_CAST")
                                val itemMap = item as Map<String, Any?>

                                ClothingItemUi(
                                    id = itemMap["id"] as? String ?: "",
                                    category = itemMap["category"] as? String ?: "",
                                    imageUrl = itemMap["imageUrl"] as? String ?: "",
                                    color = itemMap["color"] as? String,
                                    brand = itemMap["brand"] as? String,
                                    size = itemMap["size"] as? String
                                )
                            } else {
                                null
                            }
                        }

                        Outfit(
                            id = document.id,
                            ownerId = document.getString("ownerId") ?: "",
                            username = document.getString("username") ?: "Unknown",
                            caption = document.getString("caption") ?: "",
                            imageUrl = document.getString("imageUrl") ?: "",
                            clothingItems = clothingItems,
                            isPublic = document.getBoolean("isPublic") ?: false,
                            likeCount = document.getLong("likeCount")?.toInt() ?: 0,
                            likedBy = (document.get("likedBy") as? List<*>)?.filterIsInstance<String>().orEmpty(),
                            saveCount = document.getLong("saveCount")?.toInt() ?: 0,
                            savedBy = (document.get("savedBy") as? List<*>)?.filterIsInstance<String>().orEmpty(),
                            createdAt = document.getLong("createdAt") ?: 0,
                            tags = (document.get("tags") as? List<*>)?.filterIsInstance<String>().orEmpty()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                onSuccess(outfits)
            }
            .addOnFailureListener {
                onError(it)
            }
    }

    fun updateOutfitVisibility(
        outfitId: String,
        isPublic: Boolean,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("outfits")
            .document(outfitId)
            .update("isPublic", isPublic)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun likeOutfit(
        outfitId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onError(Exception("User not logged in"))
            return
        }

        db.collection("outfits")
            .document(outfitId)
            .get()
            .addOnSuccessListener { doc ->
                val likedBy = (doc.get("likedBy") as? List<*>)?.filterIsInstance<String>()?.toMutableList()
                    ?: mutableListOf()
                val likeCount = doc.getLong("likeCount")?.toInt() ?: 0

                if (userId !in likedBy) {
                    likedBy.add(userId)

                    db.collection("outfits")
                        .document(outfitId)
                        .update(
                            mapOf(
                                "likedBy" to likedBy,
                                "likeCount" to likeCount + 1
                            )
                        )
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { onError(it) }
                } else {
                    onSuccess()
                }
            }
            .addOnFailureListener { onError(it) }
    }

    fun saveOutfitToProfile(
        outfitId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onError(Exception("User not logged in"))
            return
        }

        db.collection("outfits")
            .document(outfitId)
            .get()
            .addOnSuccessListener { doc ->
                val savedBy = (doc.get("savedBy") as? List<*>)?.filterIsInstance<String>()?.toMutableList()
                    ?: mutableListOf()
                val saveCount = doc.getLong("saveCount")?.toInt() ?: 0

                if (userId !in savedBy) {
                    savedBy.add(userId)

                    db.collection("outfits")
                        .document(outfitId)
                        .update(
                            mapOf(
                                "savedBy" to savedBy,
                                "saveCount" to saveCount + 1
                            )
                        )
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { onError(it) }
                } else {
                    onSuccess()
                }
            }
            .addOnFailureListener { onError(it) }
    }

    fun deleteOutfit(
        outfitId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("outfits")
            .document(outfitId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }
}