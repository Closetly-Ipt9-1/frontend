package com.m306.closetly.fitcreator.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

@Composable

fun FitCreatorScreen() {

    var result by remember { mutableStateOf("No outfit yet") }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Button(onClick = {
            generateOutfit { res ->
                result = res
            }
        }) {
            Text("Generate Outfit")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(result)
    }

}



// =============================
// 🔥 CALL BACKEND
// =============================
fun generateOutfit(onResult: (String) -> Unit) {

    val client = OkHttpClient()

    // 🔥 TEMP TEST DATA (will replace later with Firestore)
    val clothes = JSONArray().apply {
        put(JSONObject().apply {
            put("id", "1")
            put("type", "top")
            put("tags", JSONArray(listOf("hoodie", "black")))
        })
        put(JSONObject().apply {
            put("id", "2")
            put("type", "bottom")
            put("tags", JSONArray(listOf("jeans", "blue")))
        })
    }

    val json = JSONObject().apply {
        put("weather", "cold")
        put("season", "winter")
        put("style", "streetwear")
        put("clothes", clothes)
    }

    // ✅ FIXED BODY (modern OkHttp)
    val body = json.toString().toRequestBody(
        "application/json".toMediaTypeOrNull()
    )

    val request = Request.Builder()
        .url("https://us-central1-closetly-7e54f.cloudfunctions.net/recommendOutfit")
        .post(body)
        .build()

    client.newCall(request).enqueue(object : Callback {

        override fun onFailure(call: Call, e: IOException) {
            onResult("Error: ${e.message}")
        }

        override fun onResponse(call: Call, response: Response) {
            val res = response.body?.string() ?: "No response"
            onResult(res)
        }
    })
}