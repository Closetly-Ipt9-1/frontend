package com.m306.closetly.closet.data

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

object RembgApiHelper {

    private val client = OkHttpClient()

    fun removeBackground(
        imageFile: File,
        apiKey: String,
        onSuccess: (ByteArray) -> Unit,
        onError: (String) -> Unit
    ) {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                imageFile.name,
                imageFile.asRequestBody("image/*".toMediaTypeOrNull())
            )
            .build()

        val request = Request.Builder()
            .url("https://www.rembg.com/api/remove")
            .addHeader("X-API-Key", apiKey)
            .post(requestBody)
            .build()

        Thread {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        onError("API Error: ${response.code}")
                        return@use
                    }

                    val bytes = response.body?.bytes()
                    if (bytes != null) {
                        onSuccess(bytes)
                    } else {
                        onError("Empty response")
                    }
                }
            } catch (e: Exception) {
                onError(e.message ?: "Unknown error")
            }
        }.start()
    }
}