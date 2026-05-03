package com.m306.closetly.fitcreator.ui

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

fun seedFirestore() {

    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

    val clothes = listOf(
        mapOf(
            "type" to "top",
            "imageUrl" to "https://via.placeholder.com/150",
            "tags" to listOf("black", "hoodie"),
            "color" to "black",
            "category" to "hoodie"
        ),
        mapOf(
            "type" to "bottom",
            "imageUrl" to "https://via.placeholder.com/150",
            "tags" to listOf("blue", "jeans"),
            "color" to "blue",
            "category" to "jeans"
        )
    )

    clothes.forEach {
        db.collection("users")
            .document(userId)
            .collection("clothes")
            .add(it)
    }
}