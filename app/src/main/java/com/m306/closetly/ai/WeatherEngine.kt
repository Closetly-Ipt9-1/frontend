package com.m306.closetly.ai

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.coroutines.resume

private const val OPENWEATHER_API_KEY = "1a857f11487b2ffec5f4a64d7791b20b"

data class WeatherInfo(
    val tempCelsius: Double,
    val description: String,
    val city: String
)

enum class TempRange {
    FREEZING,   // < 5°C
    COLD,       // 5–12°C
    COOL,       // 12–18°C
    MILD,       // 18–25°C
    HOT;        // > 25°C

    fun displayName(): String = when (this) {
        FREEZING -> "Sehr kalt"
        COLD     -> "Kalt"
        COOL     -> "Kühl"
        MILD     -> "Mild"
        HOT      -> "Warm"
    }
}

object WeatherEngine {

    private val client = OkHttpClient()

    // ── Get current location + weather ───────────────────────────────────────
    @SuppressLint("MissingPermission")
    suspend fun getCurrentWeather(context: Context): WeatherInfo? {
        val location = withTimeoutOrNull(5000) {
            getLocation(context)
        } ?: return null

        return fetchWeather(location.first, location.second)
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLocation(context: Context): Pair<Double, Double>? =
        suspendCancellableCoroutine { cont ->
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        cont.resume(Pair(location.latitude, location.longitude))
                    } else {
                        cont.resume(null)
                    }
                }
                .addOnFailureListener {
                    cont.resume(null)
                }
        }

    private fun fetchWeather(lat: Double, lon: Double): WeatherInfo? {
        return try {
            val url = "https://api.openweathermap.org/data/2.5/weather" +
                    "?lat=$lat&lon=$lon&appid=$OPENWEATHER_API_KEY&units=metric"

            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val body     = response.body?.string() ?: return null
            val json     = JSONObject(body)

            val temp        = json.getJSONObject("main").getDouble("temp")
            val description = json.getJSONArray("weather").getJSONObject(0).getString("description")
            val city        = json.getString("name")

            WeatherInfo(
                tempCelsius = temp,
                description = description,
                city        = city
            )
        } catch (e: Exception) {
            null
        }
    }

    // ── Temp range from celsius ───────────────────────────────────────────────
    fun getTempRange(celsius: Double): TempRange = when {
        celsius < 5   -> TempRange.FREEZING
        celsius < 12  -> TempRange.COLD
        celsius < 18  -> TempRange.COOL
        celsius < 25  -> TempRange.MILD
        else          -> TempRange.HOT
    }

    // ── What categories to include based on temp + season ────────────────────
    fun getRecommendedCategories(
        tempRange: TempRange,
        season: Season
    ): WeatherOutfitRules {
        return when (tempRange) {
            TempRange.FREEZING -> WeatherOutfitRules(
                needsJacket   = true,
                needsPullover = true,
                allowsShirt   = false,
                needsShoes    = true,
                allowsWatch   = true,
                tip           = "Sehr kalt — Jacke und Pullover sind ein Muss."
            )
            TempRange.COLD -> WeatherOutfitRules(
                needsJacket   = true,
                needsPullover = true,
                allowsShirt   = false,
                needsShoes    = true,
                allowsWatch   = true,
                tip           = "Kalt — Jacke nicht vergessen."
            )
            TempRange.COOL -> WeatherOutfitRules(
                needsJacket   = season == Season.AUTUMN || season == Season.WINTER,
                needsPullover = true,
                allowsShirt   = false,
                needsShoes    = true,
                allowsWatch   = true,
                tip           = "Kühl — ein Pullover reicht meistens."
            )
            TempRange.MILD -> WeatherOutfitRules(
                needsJacket   = false,
                needsPullover = season == Season.AUTUMN || season == Season.WINTER,
                allowsShirt   = true,
                needsShoes    = true,
                allowsWatch   = true,
                tip           = "Angenehm mild — Shirt oder leichter Pullover."
            )
            TempRange.HOT -> WeatherOutfitRules(
                needsJacket   = false,
                needsPullover = false,
                allowsShirt   = true,
                needsShoes    = true,
                allowsWatch   = true,
                tip           = "Warm — leichte Kleidung empfohlen."
            )
        }
    }
}

data class WeatherOutfitRules(
    val needsJacket: Boolean,
    val needsPullover: Boolean,
    val allowsShirt: Boolean,
    val needsShoes: Boolean,
    val allowsWatch: Boolean,
    val tip: String
)