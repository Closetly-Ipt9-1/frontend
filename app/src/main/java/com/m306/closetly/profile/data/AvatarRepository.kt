package com.m306.closetly.profile.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.m306.closetly.profile.model.Avatar
import kotlinx.coroutines.tasks.await

class AvatarRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun getAvatar(): Avatar? {
        val userId = auth.currentUser?.uid ?: return null
        val doc = db.collection("avatars").document(userId).get().await()
        return doc.toObject(Avatar::class.java)
    }

    suspend fun saveAvatar(avatar: Avatar) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("avatars").document(userId).set(avatar).await()
    }
}