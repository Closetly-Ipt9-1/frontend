package com.m306.closetly.core.data

import com.google.firebase.firestore.FirebaseFirestore
import com.m306.closetly.avatar.AvatarConfig

class UserRepository {

    private val db = FirebaseFirestore.getInstance()

    fun updateAvatar(userId: String, avatar: AvatarConfig) {
        db.collection("users")
            .document(userId)
            .update(
                mapOf(
                    "avatar" to mapOf(
                        "skin" to avatar.skin,
                        "hair" to avatar.hair,
                        "eyes" to avatar.eyes,
                        "mouth" to avatar.mouth
                    )
                )
            )
    }
}