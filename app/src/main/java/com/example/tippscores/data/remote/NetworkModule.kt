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
    @SerializedName("id")
    val id: String?,

    @SerializedName("name")
    val name: String?,

    @SerializedName("goals")
    val goals: String?
)

data class StatpalMatchItem(
    @SerializedName("main_id")
    val mainId: String?,

    @SerializedName("status")
    val status: String?,

    @SerializedName("date")
    val date: String?,

    @SerializedName("time")
    val time: String?,

    @SerializedName("home")
    val home: StatpalTeamInfo?,

    @SerializedName("away")
    val away: StatpalTeamInfo?
)

data class StatpalLeagueItem(
    @SerializedName("id")
    val id: String?,

    @SerializedName("name")
    val name: String?,

    @SerializedName("country")
    val country: String?,

    @SerializedName("match")
    val matches: List<StatpalMatchItem>?
)

data class StatpalDailyData(
    @SerializedName("updated")
    val updated: String?,

    @SerializedName("league")
    val leagues: List<StatpalLeagueItem>?
)

data class StatpalDailyResponseDto(
    val data: StatpalDailyData?
)

// ============================================================
// STATPAL LEAGUE DESERIALIZER
// ============================================================

class StatpalLeagueItemDeserializer :
    JsonDeserializer<StatpalLeagueItem> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): StatpalLeagueItem {

        if (!json.isJsonObject) {
            return StatpalLeagueItem(
                id = null,
                name = null,
                country = null,
                matches = emptyList()
            )
        }

        val obj = json.asJsonObject

        val id = obj.get("id")
            ?.takeIf { !it.isJsonNull }
            ?.let {
                try {
                    it.asString
                } catch (_: Exception) {
                    null
                }
            }

        val name = obj.get("name")
            ?.takeIf { !it.isJsonNull }
            ?.let {
                try {
                    it.asString
                } catch (_: Exception) {
                    null
                }
            }

        val country = obj.get("country")
            ?.takeIf { !it.isJsonNull }
            ?.let {
                try {
                    it.asString
                } catch (_: Exception) {
                    null
                }
            }

        val matchElement = obj.get("match")

        val matches = when {

            matchElement == null ||
                matchElement.isJsonNull -> {
                emptyList()
            }

            matchElement.isJsonArray -> {

                matchElement.asJsonArray.mapNotNull { element ->

                    try {
                        if (!element.isJsonObject) {
                            null
                        } else {
                            context.deserialize(
                                element,
                                StatpalMatchItem::class.java
                            )
                        }
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

            else -> {
                emptyList()
            }
        }

        return StatpalLeagueItem(
            id = id,
            name = name,
            country = country,
            matches = matches
        )
    }
}

// ============================================================
// STATPAL DAILY DESERIALIZER
// ============================================================

class StatpalDailyDeserializer :
    JsonDeserializer<StatpalDailyResponseDto> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): StatpalDailyResponseDto {

        if (!json.isJsonObject) {
            return StatpalDailyResponseDto(null)
        }

        val jsonObject = json.asJsonObject

        val key = jsonObject.keySet()
            .firstOrNull {
                it.startsWith("matches_") ||
                    it == "live_matches"
            }

        if (key == null) {
            return StatpalDailyResponseDto(null)
        }

        return try {

            val dailyData =
                context.deserialize<StatpalDailyData>(
                    jsonObject.get(key),
                    StatpalDailyData::class.java
                )

            StatpalDailyResponseDto(dailyData)

        } catch (_: Exception) {

            StatpalDailyResponseDto(null)
        }
    }
}

// ============================================================
// STATPAL API
// ============================================================

interface StatpalApiService {

    @GET("api/v2/soccer/matches/live")
    suspend fun getLiveMatches(
        @Query("access_key")
        accessKey: String
    ): StatpalDailyResponseDto

    @GET("api/v2/soccer/matches/daily")
    suspend fun getDailyMatches(
        @Query("offset")
        offset: Int,

        @Query("access_key")
        accessKey: String
    ): StatpalDailyResponseDto
}

// ============================================================
// HIGHLIGHTLY DTO
// ============================================================

data class HighlightlyMatchRefDto(
    @SerializedName("id")
    val id: String?
)

data class HighlightlyItemDto(
    @SerializedName("id")
    val id: String?,

    @SerializedName("title")
    val title: String?,

    @SerializedName("url")
    val url: String?,

    @SerializedName("match")
    val match: HighlightlyMatchRefDto?
)

data class HighlightlyResponseDto(
    @SerializedName("data")
    val data: List<HighlightlyItemDto>?
)

// ============================================================
// HIGHLIGHTLY API
// ============================================================

interface HighlightlyApiService {

    @GET("highlights")
    suspend fun getHighlights(
        @Header("x-rapidapi-key")
        apiKey: String,

        @Query("date")
        date: String? = null
    ): HighlightlyResponseDto
}

// ============================================================
// NETWORK MODULE
// ============================================================

object NetworkModule {

    private const val STATPAL_BASE_URL =
        "https://statpal.io/"

    private const val HIGHLIGHTLY_BASE_URL =
        "https://soccer.highlightly.net/"

    private val customGson =
        GsonBuilder()
            .registerTypeAdapter(
                StatpalDailyResponseDto::class.java,
                StatpalDailyDeserializer()
            )
            .registerTypeAdapter(
                StatpalLeagueItem::class.java,
                StatpalLeagueItemDeserializer()
            )
            .create()

    private val loggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

    private val okHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

    val statpalApi: StatpalApiService by lazy {

        Retrofit.Builder()
            .baseUrl(STATPAL_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(
                GsonConverterFactory.create(customGson)
            )
            .build()
            .create(StatpalApiService::class.java)
    }

    val highlightlyApi: HighlightlyApiService by lazy {

        Retrofit.Builder()
            .baseUrl(HIGHLIGHTLY_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(HighlightlyApiService::class.java)
    }
}
