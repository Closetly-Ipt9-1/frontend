package com.closetly.myapp.explore.ui

import com.google.firebase.firestore.FirebaseFirestore

internal fun seedPlaceholderOutfitsIfNeeded(firestore: FirebaseFirestore) {
    firestore.collection("outfits")
        .limit(1)
        .get()
        .addOnSuccessListener { result ->
            if (!result.isEmpty) return@addOnSuccessListener

            val batch = firestore.batch()

            val first = firestore.collection("outfits").document()
            batch.set(
                first,
                mapOf(
                    "ownerId" to "placeholder-user-1",
                    "username" to "Closetly",
                    "caption" to "Minimaler Schwarz-Weiß-Look",
                    "imageUrl" to "",
                    "isPublic" to true,
                    "likeCount" to 0,
                    "likedBy" to emptyList<String>(),
                    "saveCount" to 0,
                    "savedBy" to emptyList<String>(),
                    "createdAt" to System.currentTimeMillis()
                )
            )

            val second = firestore.collection("outfits").document()
            batch.set(
                second,
                mapOf(
                    "ownerId" to "placeholder-user-2",
                    "username" to "Closetly",
                    "caption" to "Streetwear Look für den Alltag",
                    "imageUrl" to "",
                    "isPublic" to true,
                    "likeCount" to 0,
                    "likedBy" to emptyList<String>(),
                    "saveCount" to 0,
                    "savedBy" to emptyList<String>(),
                    "createdAt" to System.currentTimeMillis() - 1
                )
            )

            batch.commit()
        }
}

internal fun toggleLike(
    firestore: FirebaseFirestore,
    outfitId: String,
    userId: String
) {
    val docRef = firestore.collection("outfits").document(outfitId)

    firestore.runTransaction { transaction ->
        val snapshot = transaction.get(docRef)
        val likedBy = (snapshot.get("likedBy") as? List<*>)?.filterIsInstance<String>()?.toMutableList()
            ?: mutableListOf()
        var likeCount = snapshot.getLong("likeCount")?.toInt() ?: 0

        if (likedBy.contains(userId)) {
            likedBy.remove(userId)
            if (likeCount > 0) likeCount -= 1
        } else {
            likedBy.add(userId)
            likeCount += 1
        }

        transaction.update(docRef, mapOf("likedBy" to likedBy, "likeCount" to likeCount))
    }
}

internal fun toggleSave(
    firestore: FirebaseFirestore,
    outfitId: String,
    userId: String
) {
    val docRef = firestore.collection("outfits").document(outfitId)

    firestore.runTransaction { transaction ->
        val snapshot = transaction.get(docRef)
        val savedBy = (snapshot.get("savedBy") as? List<*>)?.filterIsInstance<String>()?.toMutableList()
            ?: mutableListOf()
        var saveCount = snapshot.getLong("saveCount")?.toInt() ?: 0

        if (savedBy.contains(userId)) {
            savedBy.remove(userId)
            if (saveCount > 0) saveCount -= 1
        } else {
            savedBy.add(userId)
            saveCount += 1
        }

        transaction.update(docRef, mapOf("savedBy" to savedBy, "saveCount" to saveCount))
    }
}

internal fun addComment(
    firestore: FirebaseFirestore,
    outfitId: String,
    userId: String,
    username: String,
    text: String
) {
    firestore
        .collection("outfits")
        .document(outfitId)
        .collection("comments")
        .add(
            mapOf(
                "userId" to userId,
                "username" to username,
                "text" to text,
                "likeCount" to 0,
                "likedBy" to emptyList<String>(),
                "createdAt" to System.currentTimeMillis()
            )
        )
}

internal fun toggleCommentLike(
    firestore: FirebaseFirestore,
    outfitId: String,
    commentId: String,
    userId: String
) {
    val docRef = firestore
        .collection("outfits")
        .document(outfitId)
        .collection("comments")
        .document(commentId)

    firestore.runTransaction { transaction ->
        val snapshot = transaction.get(docRef)
        val likedBy = (snapshot.get("likedBy") as? List<*>)?.filterIsInstance<String>()?.toMutableList()
            ?: mutableListOf()
        var likeCount = snapshot.getLong("likeCount")?.toInt() ?: 0

        if (likedBy.contains(userId)) {
            likedBy.remove(userId)
            if (likeCount > 0) likeCount -= 1
        } else {
            likedBy.add(userId)
            likeCount += 1
        }

        transaction.update(docRef, mapOf("likedBy" to likedBy, "likeCount" to likeCount))
    }
}
