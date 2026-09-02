package com.example.tippscores.data.remote

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.http.Url
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
    val goals: String?,

    @SerializedName(
        value = "logo",
        alternate = ["logo_url", "image", "crest", "badge", "team_logo"]
    )
    val logoUrl: String? = null
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
// "OBJEKTUM VAGY TÖMB" FELDOLGOZÁS
// ============================================================

private fun <T> parseObjectOrArrayList(
    element: JsonElement?,
    context: JsonDeserializationContext,
    classOfT: Class<T>
): List<T> {

    return when {

        element == null || element.isJsonNull -> {
            emptyList()
        }

        element.isJsonArray -> {

            element.asJsonArray.mapNotNull { item ->

                try {
                    if (!item.isJsonObject) {
                        null
                    } else {
                        context.deserialize<T>(item, classOfT)
                    }
                } catch (_: Exception) {
                    null
                }
            }
        }

        element.isJsonObject -> {

            try {
                listOf(
                    context.deserialize<T>(element, classOfT)
                )
            } catch (_: Exception) {
                emptyList()
            }
        }

        else -> {
            emptyList()
        }
    }
}

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

        val matches = parseObjectOrArrayList(
            obj.get("match"),
            context,
            StatpalMatchItem::class.java
        )

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

        val matchingKeys = jsonObject.keySet().filter {
            it.startsWith("matches_") || it == "live_matches"
        }

        if (matchingKeys.isEmpty()) {
            return StatpalDailyResponseDto(null)
        }

        val mergedLeagues = mutableListOf<StatpalLeagueItem>()
        var latestUpdated: String? = null

        for (key in matchingKeys) {

            val element = jsonObject.get(key)
                ?.takeIf { it.isJsonObject }
                ?.asJsonObject
                ?: continue

            val leaguesForKey = parseObjectOrArrayList(
                element.get("league"),
                context,
                StatpalLeagueItem::class.java
            )

            mergedLeagues.addAll(leaguesForKey)

            element.get("updated")
                ?.takeIf { !it.isJsonNull }
                ?.let {
                    try {
                        latestUpdated = it.asString
                    } catch (_: Exception) {
                        // előző érték marad
                    }
                }
        }

        if (mergedLeagues.isEmpty()) {
            return StatpalDailyResponseDto(null)
        }

        return StatpalDailyResponseDto(
            StatpalDailyData(
                updated = latestUpdated,
                leagues = mergedLeagues
            )
        )
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
// STATPAL MÉRKŐZÉS RÉSZLETEK
// ============================================================

interface StatpalMatchDetailsApiService {

    @GET
    suspend fun getDetails(
        @Url url: String,
        @Query("access_key") accessKey: String
    ): JsonObject
}

// ============================================================
// HIGHLIGHTLY - HIGHLIGHTS DTO
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
// HIGHLIGHTLY - CSAPAT DTO
//
// A /matches válasz homeTeam és awayTeam objektumai ezt
// a struktúrát használják.
// ============================================================

data class HighlightlyTeamDto(
    @SerializedName("id")
    val id: String?,

    @SerializedName("logo")
    val logo: String?,

    @SerializedName("name")
    val name: String?,

    @SerializedName("type")
    val type: String? = null
)

// ============================================================
// HIGHLIGHTLY - MÉRKŐZÉS DTO
// ============================================================

data class HighlightlyMatchDto(
    @SerializedName("id")
    val id: String?,

    @SerializedName("date")
    val date: String?,

    @SerializedName("homeTeam")
    val homeTeam: HighlightlyTeamDto?,

    @SerializedName("awayTeam")
    val awayTeam: HighlightlyTeamDto?
)

// ============================================================
// HIGHLIGHTLY MATCHES RESPONSE
// ============================================================

data class HighlightlyMatchesResponseDto(
    @SerializedName("data")
    val data: List<HighlightlyMatchDto>?
)

// ============================================================
// HIGHLIGHTLY API
// ============================================================

interface HighlightlyApiService {

    // --------------------------------------------------------
    // MÉRKŐZÉS KERESÉSE CSAPATOK ALAPJÁN
    // --------------------------------------------------------

    @GET("matches")
    suspend fun findMatches(
        @Header("x-rapidapi-key")
        apiKey: String,

        @Query("homeTeamName")
        homeTeamName: String,

        @Query("awayTeamName")
        awayTeamName: String,

        @Query("limit")
        limit: Int = 10,

        @Query("offset")
        offset: Int = 0
    ): HighlightlyMatchesResponseDto

    // --------------------------------------------------------
    // MÉRKŐZÉS RÉSZLETES ADATAI
    // --------------------------------------------------------

    @GET("matches/{id}")
    suspend fun getMatchById(
        @Header("x-rapidapi-key")
        apiKey: String,

        @retrofit2.http.Path("id")
        matchId: String
    ): JsonElement

    // --------------------------------------------------------
    // MÉRKŐZÉS STATISZTIKÁK
    // --------------------------------------------------------

    @GET("statistics/{matchId}")
    suspend fun getMatchStatistics(
        @Header("x-rapidapi-key")
        apiKey: String,

        @retrofit2.http.Path("matchId")
        matchId: String
    ): JsonElement

    // --------------------------------------------------------
    // ÉLŐ ESEMÉNYEK
    // --------------------------------------------------------

    @GET("events/{id}")
    suspend fun getMatchEvents(
        @Header("x-rapidapi-key")
        apiKey: String,

        @retrofit2.http.Path("id")
        matchId: String
    ): JsonElement

    // --------------------------------------------------------
    // JÁTÉKOS ADATOK / BOX SCORE
    // --------------------------------------------------------

    @GET("box-score/{matchId}")
    suspend fun getBoxScore(
        @Header("x-rapidapi-key")
        apiKey: String,
        @retrofit2.http.Path("matchId")
        matchId: String
    ): JsonElement

    // --------------------------------------------------------
    // Videó/highlight lista
    // --------------------------------------------------------

    @GET("highlights")
    suspend fun getHighlights(
        @Header("x-rapidapi-key")
        apiKey: String,

        @Query("date")
        date: String? = null
    ): HighlightlyResponseDto

    // --------------------------------------------------------
    // CSAPATKERESÉS / CSAPATPROFIL
    // --------------------------------------------------------

    @GET("teams")
    suspend fun searchTeams(
        @Header("x-rapidapi-key") apiKey: String,
        @Query("name") name: String,
        @Query("type") type: String = "club",
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 0
    ): JsonElement

    @GET("teams/{id}")
    suspend fun getTeamById(
        @Header("x-rapidapi-key") apiKey: String,
        @retrofit2.http.Path("id") teamId: String
    ): JsonElement

    @GET("teams/statistics/{id}")
    suspend fun getTeamStatistics(
        @Header("x-rapidapi-key") apiKey: String,
        @retrofit2.http.Path("id") teamId: String,
        @Query("fromDate") fromDate: String,
        @Query("timezone") timezone: String = "Europe/Budapest"
    ): JsonElement

    @GET("last-five-games")
    suspend fun getLastFiveGames(
        @Header("x-rapidapi-key") apiKey: String,
        @Query("teamId") teamId: String
    ): JsonElement

    // --------------------------------------------------------
    // EGYEZŐ CSAPATOK - H2H
    // --------------------------------------------------------

    @GET("head-2-head")
    suspend fun getHeadToHead(
        @Header("x-rapidapi-key") apiKey: String,
        @Query("teamIdOne") teamIdOne: String,
        @Query("teamIdTwo") teamIdTwo: String
    ): JsonElement

    // --------------------------------------------------------
    // TELJES NAPI MÉRKŐZÉSLISTA
    //
    // Innen kapjuk:
    // homeTeam.logo
    // awayTeam.logo
    //
    // A limit maximum 100.
    // --------------------------------------------------------

    @GET("matches")
    suspend fun getMatches(
        @Header("x-rapidapi-key")
        apiKey: String,

        @Query("date")
        date: String,

        @Query("timezone")
        timezone: String = "Europe/Budapest",

        @Query("limit")
        limit: Int = 100,

        @Query("offset")
        offset: Int = 0
    ): HighlightlyMatchesResponseDto
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
    // STATPAL MATCH DETAILS
    // ========================================================

    val statpalMatchDetailsApi: StatpalMatchDetailsApiService by lazy {

        Retrofit.Builder()
            .baseUrl(STATPAL_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(StatpalMatchDetailsApiService::class.java)
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
