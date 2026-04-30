package com.m306.closetly.closet.data

import android.content.Context
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.m306.closetly.BuildConfig
import com.m306.closetly.closet.func.Images
import com.m306.closetly.closet.model.ClothingItemUi
import java.io.File

class ClosetRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val images = Images()

    fun saveClothingItemWithRemovedBackground(
        context: Context,
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

        val inputStream = context.contentResolver.openInputStream(imageUri)
        if (inputStream == null) {
            onError(Exception("Cannot read image"))
            return
        }

        val tempFile = File.createTempFile("clothing_upload", ".jpg", context.cacheDir)

        tempFile.outputStream().use { output ->
            inputStream.use { input ->
                input.copyTo(output)
            }
        }

        RembgApiHelper.removeBackground(
            imageFile = tempFile,
            apiKey = BuildConfig.RMBG_API_KEY,
            onSuccess = { pngBytes ->
                images.uploadClothesImageBytes(
                    imageBytes = pngBytes,
                    onSuccess = { imageUrl ->
                        saveClothingData(
                            userId = userId,
                            imageUrl = imageUrl,
                            category = category,
                            color = color,
                            brand = brand,
                            size = size,
                            onSuccess = onSuccess,
                            onError = onError
                        )
                    },
                    onError = onError
                )
            },
            onError = { error ->
                onError(Exception(error))
            }
        )
    }

    private fun saveClothingData(
        userId: String,
        imageUrl: String,
        category: String,
        color: String,
        brand: String,
        size: String,
        onSuccess: (ClothingItemUi) -> Unit,
        onError: (Exception) -> Unit
    ) {
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

    fun deleteClothingItem(
        itemId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("clothingItems")
            .document(itemId)
            .delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onError(it)
            }
    }

    fun updateClothingItem(
        itemId: String,
        category: String,
        color: String,
        brand: String,
        size: String,
        onSuccess: (ClothingItemUi) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val data = mapOf(
            "category" to category,
            "color" to color.ifBlank { null },
            "brand" to brand.ifBlank { null },
            "size" to size.ifBlank { null }
        )

        db.collection("clothingItems")
            .document(itemId)
            .update(data)
            .addOnSuccessListener {
                db.collection("clothingItems")
                    .document(itemId)
                    .get()
                    .addOnSuccessListener { doc ->
                        onSuccess(
                            ClothingItemUi(
                                id = doc.id,
                                category = doc.getString("category") ?: "",
                                imageUrl = doc.getString("imageUrl") ?: "",
                                color = doc.getString("color"),
                                brand = doc.getString("brand"),
                                size = doc.getString("size")
                            )
                        )
                    }
                    .addOnFailureListener {
                        onError(it)
                    }
            }
            .addOnFailureListener {
                onError(it)
            }
    }
}