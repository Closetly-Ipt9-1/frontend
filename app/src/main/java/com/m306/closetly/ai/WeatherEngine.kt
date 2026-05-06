package com.m306.closetly.ai

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.coroutines.resume

private const val OPENWEATHER_API_KEY = "5ab318c9cb4867fc413e7963cbc0d962"
data class WeatherInfo(
    val tempCelsius: Double,
    val description: String,
    val city: String
)

enum class TempRange {
    FREEZING,
    COLD,
    COOL,
    MILD,
    HOT;

    fun displayName(): String = when (this) {
        FREEZING -> "Freezing"
        COLD     -> "Cold"
        COOL     -> "Cool"
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
        }

        // Falls GPS null → Fallback auf Luzern
        val (lat, lon) = location ?: Pair(47.0502, 8.3093)

        return fetchWeather(lat, lon)
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

    private suspend fun fetchWeather(lat: Double, lon: Double): WeatherInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://api.openweathermap.org/data/2.5/weather" +
                        "?lat=$lat&lon=$lon&appid=$OPENWEATHER_API_KEY&units=metric"

                android.util.Log.d("WEATHER_GEN", "Fetching URL: $url")

                val request  = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body     = response.body?.string() ?: return@withContext null


                val json        = JSONObject(body)
                val temp        = json.getJSONObject("main").getDouble("temp")
                val description = json.getJSONArray("weather").getJSONObject(0).getString("description")
                val city        = json.getString("name")

                WeatherInfo(
                    tempCelsius = temp,
                    description = description,
                    city        = city
                )
            } catch (e: Exception) {
                android.util.Log.e("WEATHER_GEN", "fetchWeather error: ${e.message}", e)
                null
            }
        }
    }

    // ── Temp range from celsius ───────────────────────────────────────────────
    fun getTempRange(celsius: Double): TempRange = when {
        celsius < 5  -> TempRange.FREEZING
        celsius < 12 -> TempRange.COLD
        celsius < 18 -> TempRange.COOL
        celsius < 25 -> TempRange.MILD
        else         -> TempRange.HOT
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
                tip           = "Freezing - a jacket and pullover are essential."
            )
            TempRange.COLD -> WeatherOutfitRules(
                needsJacket   = true,
                needsPullover = true,
                allowsShirt   = false,
                needsShoes    = true,
                allowsWatch   = true,
                tip           = "Cold - do not forget a jacket."
            )
            TempRange.COOL -> WeatherOutfitRules(
                needsJacket   = season == Season.AUTUMN || season == Season.WINTER,
                needsPullover = true,
                allowsShirt   = false,
                needsShoes    = true,
                allowsWatch   = true,
                tip           = "Cool - a pullover is usually enough."
            )
            TempRange.MILD -> WeatherOutfitRules(
                needsJacket   = false,
                needsPullover = true,
                allowsShirt   = true,
                needsShoes    = true,
                allowsWatch   = true,
                tip           = "Pleasantly mild - a shirt or light pullover works well."
            )
            TempRange.HOT -> WeatherOutfitRules(
                needsJacket   = false,
                needsPullover = false,
                allowsShirt   = true,
                needsShoes    = true,
                allowsWatch   = true,
                tip           = "Warm - light clothes are recommended."
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
