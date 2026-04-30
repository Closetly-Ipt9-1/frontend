package com.closetly.myapp.profile.data

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.closetly.myapp.profile.model.Avatar
import kotlinx.coroutines.tasks.await

class AvatarRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    suspend fun getAvatar(): Avatar? {
        val userId = auth.currentUser?.uid ?: return null
        val doc = db.collection("avatars").document(userId).get().await()
        return doc.toObject(Avatar::class.java)
    }

    suspend fun saveAvatar(avatar: Avatar) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("avatars").document(userId).set(avatar).await()
    }

    suspend fun getDefaultAvatarUrl(gender: String, skin: String, hair: String): String {
        val ref = storage.reference.child("avatars/default/${gender}_${skin}_${hair}.png")
        return ref.downloadUrl.await().toString()
    }

    suspend fun uploadImage(uri: Uri, filename: String): String {
        val userId = auth.currentUser?.uid ?: error("Nicht eingeloggt")
        val ref = storage.reference.child("avatars/$userId/$filename")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }
}