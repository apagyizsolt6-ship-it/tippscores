package com.example.tippscores.data.remote

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.lang.reflect.Type

// ============================================================
// STATPAL V2 DTO
// ============================================================

data class StatpalTeamInfo(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("goals") val goals: String?
)

data class StatpalMatchItem(
    @SerializedName("main_id") val mainId: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("date") val date: String?,
    @SerializedName("time") val time: String?,
    @SerializedName("home") val home: StatpalTeamInfo?,
    @SerializedName("away") val away: StatpalTeamInfo?
)

data class StatpalLeagueItem(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("country") val country: String?,
    @SerializedName("match") val matches: List<StatpalMatchItem>?
)

data class StatpalDailyData(
    @SerializedName("updated") val updated: String?,
    @SerializedName("league") val leagues: List<StatpalLeagueItem>?
)

data class StatpalDailyResponseDto(
    val data: StatpalDailyData?
)

// ============================================================
// STATPAL LEAGUE DESERIALIZER
// ============================================================

/**
 * A StatPal bizonyos ligáknál tömbként, más ligáknál pedig
 * egyetlen objektumként küldheti a "match" mezőt.
 * Ez az adapter mindkét formátumot kezeli.
 */
class StatpalLeagueItemDeserializer : JsonDeserializer<StatpalLeagueItem> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): StatpalLeagueItem {
        if (!json.isJsonObject) {
            return StatpalLeagueItem(null, null, null, emptyList())
        }

        val obj = json.asJsonObject

        val id = obj.get("id")?.takeIf { !it.isJsonNull }?.safeString()
        val name = obj.get("name")?.takeIf { !it.isJsonNull }?.safeString()
        val country = obj.get("country")?.takeIf { !it.isJsonNull }?.safeString()
        val matchElement = obj.get("match")

        val matches = when {
            matchElement == null || matchElement.isJsonNull -> emptyList()

            matchElement.isJsonArray -> {
                matchElement.asJsonArray.mapNotNull { element ->
                    try {
                        if (!element.isJsonObject) null
                        else context.deserialize(element, StatpalMatchItem::class.java)
                    } catch (_: Exception) {
                        null
                    }
                }
            }

            matchElement.isJsonObject -> {
                try {
                    listOf(
                        context.deserialize(
                            matchElement,
                            StatpalMatchItem::class.java
                        )
                    )
                } catch (_: Exception) {
                    emptyList()
                }
            }

            else -> emptyList()
        }

        return StatpalLeagueItem(
            id = id,
            name = name,
            country = country,
            matches = matches
        )
    }
}

private fun JsonElement.safeString(): String? = try {
    asString
} catch (_: Exception) {
    null
}

// ============================================================
// STATPAL DAILY RESPONSE DESERIALIZER
// ============================================================

class StatpalDailyDeserializer : JsonDeserializer<StatpalDailyResponseDto> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): StatpalDailyResponseDto {
        if (!json.isJsonObject) {
            return StatpalDailyResponseDto(null)
        }

        val jsonObject = json.asJsonObject
        val key = jsonObject.keySet().firstOrNull {
            it.startsWith("matches_") || it == "live_matches"
        }

        return if (key != null) {
            try {
                val dailyData = context.deserialize<StatpalDailyData>(
                    jsonObject.get(key),
                    StatpalDailyData::class.java
                )
                StatpalDailyResponseDto(dailyData)
            } catch (_: Exception) {
                StatpalDailyResponseDto(null)
            }
        } else {
            // Egyes API-válaszok közvetlenül "data" alatt adhatják vissza.
            val dataElement = jsonObject.get("data")
            if (dataElement != null && dataElement.isJsonObject) {
                try {
                    StatpalDailyResponseDto(
                        context.deserialize(
                            dataElement,
                            StatpalDailyData::class.java
                        )
                    )
                } catch (_: Exception) {
                    StatpalDailyResponseDto(null)
                }
            } else {
                StatpalDailyResponseDto(null)
            }
        }
    }
}

// ============================================================
// STATPAL API
// ============================================================

interface StatpalApiService {

    @GET("api/v2/soccer/matches/live")
    suspend fun getLiveMatches(
        @Query("access_key") accessKey: String
    ): StatpalDailyResponseDto

    @GET("api/v2/soccer/matches/daily")
    suspend fun getDailyMatches(
        @Query("offset") offset: Int,
        @Query("access_key") accessKey: String
    ): StatpalDailyResponseDto
}

// ============================================================
// HIGHLIGHTLY DTO
// ============================================================

data class HighlightlyMatchRefDto(
    @SerializedName("id") val id: String?
)

data class HighlightlyItemDto(
    @SerializedName("id") val id: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("url") val url: String?,
    @SerializedName("match") val match: HighlightlyMatchRefDto?
)

data class HighlightlyResponseDto(
    @SerializedName("data") val data: List<HighlightlyItemDto>?
)

// ============================================================
// HIGHLIGHTLY API
// ============================================================

interface HighlightlyApiService {

    @GET("highlights")
    suspend fun getHighlights(
        @Header("x-rapidapi-key") apiKey: String,
        @Query("date") date: String? = null
    ): HighlightlyResponseDto
}

// ============================================================
// RETROFIT NETWORK MODULE
// ============================================================

object NetworkModule {

    private const val STATPAL_BASE_URL = "https://statpal.io/"

    // Közvetlen Highlightly végpont – nem RapidAPI host.
    private const val HIGHLIGHTLY_BASE_URL = "https://soccer.highlightly.net/"

    private val customGson = GsonBuilder()
        .registerTypeAdapter(
            StatpalDailyResponseDto::class.java,
            StatpalDailyDeserializer()
        )
        .registerTypeAdapter(
            StatpalLeagueItem::class.java,
            StatpalLeagueItemDeserializer()
        )
        .create()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val statpalApi: StatpalApiService by lazy {
        Retrofit.Builder()
            .baseUrl(STATPAL_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(customGson))
            .build()
            .create(StatpalApiService::class.java)
    }

    val highlightlyApi: HighlightlyApiService by lazy {
        Retrofit.Builder()
            .baseUrl(HIGHLIGHTLY_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HighlightlyApiService::class.java)
    }
}
