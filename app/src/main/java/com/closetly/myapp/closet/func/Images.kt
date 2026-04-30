package com.closetly.myapp.closet.func

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class Images {

    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    fun uploadClothesImage(
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            onError(Exception("User not logged in"))
            return
        }

        val fileName = UUID.randomUUID().toString()
        val ref = storage.reference.child("users/$userId/clothes/$fileName.jpg")

        ref.putFile(imageUri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    onSuccess(uri.toString())
                }
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }
}