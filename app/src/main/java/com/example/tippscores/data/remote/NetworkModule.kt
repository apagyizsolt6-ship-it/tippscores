package com.example.tippscores.data.remote

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.lang.reflect.Type

// --- STATPAL V2 JSON DTO MODELL ---

data class StatpalTeamInfo(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("goals") val goals: String?
)

data class StatpalMatchItem(
    @SerializedName("main_id") val mainId: String?,
    @SerializedName("status") val status: String?, // "FT", "72'", "18:30"
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

// Burkoló osztály a dinamikus matches_DD_MM_YYYY kulcs kezelésére
data class StatpalDailyResponseDto(
    val data: StatpalDailyData?
)

// Dinamikus Gson Deserializer a "matches_15_12_2025" típusú kulcsok kibontásához
class StatpalDailyDeserializer : JsonDeserializer<StatpalDailyResponseDto> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): StatpalDailyResponseDto {
        val jsonObject = json.asJsonObject
        // Megkeressük az első olyan kulcsot, ami matches_-el kezdődik, vagy a live_matches-t
        val key = jsonObject.keySet().firstOrNull { it.startsWith("matches_") || it == "live_matches" }
        return if (key != null) {
            val dailyData = context.deserialize<StatpalDailyData>(jsonObject.get(key), StatpalDailyData::class.java)
            StatpalDailyResponseDto(dailyData)
        } else {
            StatpalDailyResponseDto(null)
        }
    }
}

interface StatpalApiService {
    // ÉLŐ MECCSEK (MAI NAP)
    @GET("api/v2/soccer/matches/live")
    suspend fun getLiveMatches(
        @Query("access_key") accessKey: String
    ): StatpalDailyResponseDto

    // ELMÚLT ÉS KÖVETKEZŐ MECCSEK (-7 ÉS +7 NAP KÖZÖTT)
    @GET("api/v2/soccer/matches/daily")
    suspend fun getDailyMatches(
        @Query("offset") offset: Int, // -7 ... 7
        @Query("access_key") accessKey: String
    ): StatpalDailyResponseDto
}

// --- HIGHLIGHTLY DTO STRUCT (RapidAPI) ---
data class HighlightlyItemDto(
    val match_id: String?,
    val match_name: String?,
    val url: String?
)

interface HighlightlyApiService {
    @GET("highlights")
    suspend fun getHighlights(
        @Header("X-RapidAPI-Key") apiKey: String,
        @Header("X-RapidAPI-Host") host: String = "football-highlights-api.p.rapidapi.com"
    ): List<HighlightlyItemDto>
}

// --- RETROFIT KLIENSEK ---
object NetworkModule {

    private const val STATPAL_BASE_URL = "https://statpal.io/"
    private const val HIGHLIGHTLY_BASE_URL = "https://football-highlights-api.p.rapidapi.com/"

    private val customGson = GsonBuilder()
        .registerTypeAdapter(StatpalDailyResponseDto::class.java, StatpalDailyDeserializer())
        .create()

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
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
