package com.closetly.myapp.premium.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.closetly.myapp.premium.model.SubscriptionStatus

class PremiumAccessRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun getSubscriptionStatus(
        onSuccess: (SubscriptionStatus) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onError(Exception("User is not logged in."))
            return
        }

        db.collection("users")
            .document(userId)
            .collection("private")
            .document("subscription")
            .get()
            .addOnSuccessListener { document ->
                onSuccess(
                    SubscriptionStatus(
                        isPremium = document.getBoolean("isPremium") ?: false,
                        planBaseId = document.getString("planBaseId"),
                        productId = document.getString("productId"),
                        purchaseToken = document.getString("purchaseToken"),
                        purchaseTimeMillis = document.getLong("purchaseTimeMillis"),
                        estimatedExpiryMillis = document.getLong("estimatedExpiryMillis"),
                        isAutoRenewing = document.getBoolean("isAutoRenewing") ?: false,
                        lastUpdatedMillis = document.getLong("lastUpdatedMillis")
                            ?: System.currentTimeMillis()
                    )
                )
            }
            .addOnFailureListener(onError)
    }

    fun canAddClothingItem(
        onResult: (Boolean, String?) -> Unit
    ) {
        getSubscriptionStatus(
            onSuccess = { status ->
                if (status.isPremium) {
                    onResult(true, null)
                    return@getSubscriptionStatus
                }
                countUserDocuments(
                    collection = "clothingItems",
                    ownerField = "userId",
                    onSuccess = { count ->
                        onResult(
                            count < FREE_CLOTHING_LIMIT,
                            "Free accounts can save up to $FREE_CLOTHING_LIMIT clothing items."
                        )
                    },
                    onError = { onResult(false, it.message) }
                )
            },
            onError = { onResult(false, it.message) }
        )
    }

    fun canAddOutfit(
        onResult: (Boolean, String?) -> Unit
    ) {
        getSubscriptionStatus(
            onSuccess = { status ->
                if (status.isPremium) {
                    onResult(true, null)
                    return@getSubscriptionStatus
                }
                countUserDocuments(
                    collection = "outfits",
                    ownerField = "ownerId",
                    onSuccess = { count ->
                        onResult(
                            count < FREE_OUTFIT_LIMIT,
                            "Free accounts can save up to $FREE_OUTFIT_LIMIT outfits."
                        )
                    },
                    onError = { onResult(false, it.message) }
                )
            },
            onError = { onResult(false, it.message) }
        )
    }

    fun enforceFreeLimits(
        onComplete: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        getSubscriptionStatus(
            onSuccess = { status ->
                if (status.isPremium) {
                    onComplete()
                    return@getSubscriptionStatus
                }

                trimCollection(
                    collection = "clothingItems",
                    ownerField = "userId",
                    keepCount = FREE_CLOTHING_LIMIT,
                    onComplete = {
                        trimCollection(
                            collection = "outfits",
                            ownerField = "ownerId",
                            keepCount = FREE_OUTFIT_LIMIT,
                            onComplete = onComplete,
                            onError = onError
                        )
                    },
                    onError = onError
                )
            },
            onError = onError
        )
    }

    private fun countUserDocuments(
        collection: String,
        ownerField: String,
        onSuccess: (Int) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onError(Exception("User is not logged in."))
            return
        }

        db.collection(collection)
            .whereEqualTo(ownerField, userId)
            .get()
            .addOnSuccessListener { onSuccess(it.size()) }
            .addOnFailureListener(onError)
    }

    private fun trimCollection(
        collection: String,
        ownerField: String,
        keepCount: Int,
        onComplete: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onError(Exception("User is not logged in."))
            return
        }

        db.collection(collection)
            .whereEqualTo(ownerField, userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val documentsToDelete = snapshot.documents
                    .sortedByDescending { it.getLong("createdAt") ?: 0L }
                    .drop(keepCount)

                if (documentsToDelete.isEmpty()) {
                    onComplete()
                    return@addOnSuccessListener
                }

                val batch = db.batch()
                documentsToDelete.forEach { batch.delete(it.reference) }
                batch.commit()
                    .addOnSuccessListener { onComplete() }
                    .addOnFailureListener(onError)
            }
            .addOnFailureListener(onError)
    }

    companion object {
        const val FREE_CLOTHING_LIMIT = 6
        const val FREE_OUTFIT_LIMIT = 3
    }
}
