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
            it.startsWith("matches_") ||
                it == "live_matches"
        }

        return if (key != null) {
            val dailyData = context.deserialize<StatpalDailyData>(
                jsonObject.get(key),
                StatpalDailyData::class.java
            )

            StatpalDailyResponseDto(dailyData)
        } else {
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
// RETROFIT NETWORK MODULE
// ============================================================

object NetworkModule {

    private const val STATPAL_BASE_URL =
        "https://statpal.io/"

    private const val HIGHLIGHTLY_BASE_URL =
        "https://soccer.highlightly.net/"

    private val customGson = GsonBuilder()
        .registerTypeAdapter(
            StatpalDailyResponseDto::class.java,
            StatpalDailyDeserializer()
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

    // ========================================================
    // STATPAL
    // ========================================================

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

    // ========================================================
    // HIGHLIGHTLY
    // ========================================================

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
