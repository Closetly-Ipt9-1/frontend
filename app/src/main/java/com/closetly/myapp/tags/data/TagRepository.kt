package com.closetly.myapp.tags.data

import com.google.firebase.firestore.FirebaseFirestore

class TagRepository {

    private val db = FirebaseFirestore.getInstance()

    fun updateItemTags(
        itemId: String,
        tagIds: List<String>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("clothingItems")
            .document(itemId)
            .update("tags", tagIds)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    fun updateOutfitTags(
        outfitId: String,
        tagIds: List<String>,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("outfits")
            .document(outfitId)
            .update("tags", tagIds)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }
}
