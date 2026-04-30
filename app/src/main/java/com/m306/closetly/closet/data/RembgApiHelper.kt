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
        if (apiKey.isBlank()) {
            onError("API Key fehlt. Prüfe RMBG_API_KEY in gradle.properties")
            return
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "image_file",
                imageFile.name,
                imageFile.asRequestBody("image/*".toMediaTypeOrNull())
            )
            .addFormDataPart("size", "auto")
            .build()

        val request = Request.Builder()
            .url("https://api.remove.bg/v1.0/removebg")
            .addHeader("X-Api-Key", apiKey)
            .post(requestBody)
            .build()

        Thread {
            try {
                client.newCall(request).execute().use { response ->
                    val body = response.body

                    if (!response.isSuccessful) {
                        val errorText = body?.string() ?: "No error body"
                        onError("remove.bg Error ${response.code}: $errorText")
                        return@use
                    }

                    val bytes = body?.bytes()

                    if (bytes == null || bytes.isEmpty()) {
                        onError("remove.bg returned empty image")
                        return@use
                    }

                    onSuccess(bytes)
                }
            } catch (e: Exception) {
                onError(e.message ?: "Unknown remove.bg error")
            }
        }.start()
    }
}