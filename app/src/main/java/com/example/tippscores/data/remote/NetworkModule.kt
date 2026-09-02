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
import retrofit2.http.Url
import com.google.gson.JsonObject
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
// KÖZÖS SEGÉDFÜGGVÉNY: "OBJEKTUM VAGY TÖMB" FELOLDÁS
//
// A StatPal válaszaiban több helyen is előfordul ugyanaz a minta:
// ha egy gyűjteményben csak 1 elem van, sima objektumként küldi
// ("match": {...} / "league": {...}), ha több, akkor tömbként
// ("match": [{...}, {...}]). Ez a függvény mindkét alakot ugyanúgy
// egy listává alakítja, hogy sehol ne vesszen el emiatt elem.
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
//
// A válasz tetején egy vagy több "matches_..." (vagy "live_matches")
// kulcs is szerepelhet. Korábban csak az elsőt dolgoztuk fel - most
// mindegyiket összefésüljük, hogy egyetlen bajnokság se maradjon ki.
// A "league" mezőre is ráteszünk ugyanazt az objektum/tömb védelmet,
// ami eddig csak a "match" mezőnél volt meg.
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
                        // marad az előző érték
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
//
// A StatPal külön végpontokat biztosít az élő statisztikákhoz,
// play-by-play eseményekhez és keretekhez. A részletes JSON-t itt
// JsonObjectként vesszük át, mert a szolgáltató többféle mezőalakot
// használhat ligától / adatkörtől függően. A UI-réteg egy közös,
// stabil modellt kap.
// ============================================================

interface StatpalMatchDetailsApiService {

    @GET
    suspend fun getDetails(
        @Url url: String,
        @Query("access_key") accessKey: String
    ): JsonObject
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
