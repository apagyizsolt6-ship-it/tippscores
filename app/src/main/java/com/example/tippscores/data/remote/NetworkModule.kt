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

// ============================================================
// STATPAL LEAGUE DTO
// ============================================================

/*
 * FONTOS:
 *
 * A StatPal API nem mindig ugyanabban a JSON-formában küldi
 * a "match" mezőt.
 *
 * Több mérkőzés esetén:
 *
 * "match": [
 *     {...},
 *     {...}
 * ]
 *
 * Egyetlen mérkőzés esetén viszont előfordulhat:
 *
 * "match": {
 *     ...
 * }
 *
 * Ezért a tényleges feldolgozást a
 * StatpalLeagueItemDeserializer végzi.
 */

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

// ============================================================
// STATPAL DAILY DATA
// ============================================================

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

/*
 * Ez az adapter kezeli a következő két formátumot:
 *
 * 1. Tömb:
 *
 * "match": [
 *   {...},
 *   {...}
 * ]
 *
 * 2. Egyetlen objektum:
 *
 * "match": {
 *   ...
 * }
 *
 * Így nem omlik össze a Gson akkor sem,
 * ha egy ligában csak egy mérkőzés van.
 */

class StatpalLeagueItemDeserializer :
    JsonDeserializer<StatpalLeagueItem> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): StatpalLeagueItem {

        // Biztonsági ellenőrzés
        if (!json.isJsonObject) {
            return StatpalLeagueItem(
                id = null,
                name = null,
                country = null,
                matches = emptyList()
            )
        }

        val jsonObject = json.asJsonObject

        // ----------------------------------------------------
        // ID
        // ----------------------------------------------------

        val id =
            jsonObject
                .get("id")
                ?.takeIf { !it.isJsonNull }
                ?.let {
                    try {
                        it.asString
                    } catch (_: Exception) {
                        null
                    }
                }

        // ----------------------------------------------------
        // NAME
        // ----------------------------------------------------

        val name =
            jsonObject
                .get("name")
                ?.takeIf { !it.isJsonNull }
                ?.let {
                    try {
                        it.asString
                    } catch (_: Exception) {
                        null
                    }
                }

        // ----------------------------------------------------
        // COUNTRY
        // ----------------------------------------------------

        val country =
            jsonObject
                .get("country")
                ?.takeIf { !it.isJsonNull }
                ?.let {
                    try {
                        it.asString
                    } catch (_: Exception) {
                        null
                    }
                }

        // ----------------------------------------------------
        // MATCH
        // ----------------------------------------------------

        val matchElement =
            jsonObject.get("match")

        val matches: List<StatpalMatchItem> =
            when {

                // Nincs match
                matchElement == null ||
                    matchElement.isJsonNull -> {

                    emptyList()
                }

                // ------------------------------------------------
                // MATCH = ARRAY
                // ------------------------------------------------

                matchElement.isJsonArray -> {

                    matchElement
                        .asJsonArray
                        .mapNotNull { element ->

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

                // ------------------------------------------------
                // MATCH = OBJECT
                // ------------------------------------------------

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

                // ------------------------------------------------
                // ISMERETLEN FORMÁTUM
                // ------------------------------------------------

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
// STATPAL DAILY RESPONSE DESERIALIZER
// ============================================================

class StatpalDailyDeserializer :
    JsonDeserializer<StatpalDailyResponseDto> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): StatpalDailyResponseDto {

        // ----------------------------------------------------
        // Biztonsági ellenőrzés
        // ----------------------------------------------------

        if (!json.isJsonObject) {
            return StatpalDailyResponseDto(null)
        }

        val jsonObject =
            json.asJsonObject

        // ----------------------------------------------------
        // STATPAL KULCS KERESÉSE
        // ----------------------------------------------------
        //
        // Például:
        //
        // matches_2026-08-29
        //
        // vagy:
        //
        // live_matches
        //
        // ----------------------------------------------------

        val key =
            jsonObject.keySet()
                .firstOrNull {
                    it.startsWith("matches_") ||
                        it == "live_matches"
                }

        // ----------------------------------------------------
        // ADAT FELDOLGOZÁSA
        // ----------------------------------------------------

        return if (key != null) {

            try {

                val dailyData =
                    context.deserialize<StatpalDailyData>(
                        jsonObject.get(key),
                        StatpalDailyData::class.java
                    )

                StatpalDailyResponseDto(
                    dailyData
                )

            } catch (_: Exception) {

                StatpalDailyResponseDto(null)
            }

        } else {

            StatpalDailyResponseDto(null)
        }
    }
}

// ============================================================
// STATPAL API
// ============================================================

interface StatpalApiService {

    // --------------------------------------------------------
    // LIVE MÉRKŐZÉSEK
    // --------------------------------------------------------

    @GET("api/v2/soccer/matches/live")
    suspend fun getLiveMatches(
        @Query("access_key")
        accessKey: String
    ): StatpalDailyResponseDto

    // --------------------------------------------------------
    // NAPI MÉRKŐZÉSEK
    // --------------------------------------------------------

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

    // ========================================================
    // STATPAL
    // ========================================================

    private const val STATPAL_BASE_URL =
        "https://statpal.io/"

    // ========================================================
    // HIGHLIGHTLY
    // ========================================================

    private const val HIGHLIGHTLY_BASE_URL =
        "https://soccer.highlightly.net/"

    // ========================================================
    // CUSTOM GSON
    // ========================================================

    /*
     * Itt regisztráljuk a két fontos saját deserializert:
     *
     * 1. StatpalDailyResponseDto
     *    -> megtalálja a matches_... / live_matches blokkot
     *
     * 2. StatpalLeagueItem
     *    -> kezeli a match tömb / objektum eltérést
     */

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

    // ========================================================
    // HTTP LOGGING
    // ========================================================

    private val loggingInterceptor =
        HttpLoggingInterceptor().apply {

            /*
             * BASIC:
             * URL + HTTP státusz + alap információk.
             *
             * Nem használunk BODY logolást, hogy az API-válasz
             * teljes tartalma ne kerüljön feleslegesen a logba.
             */

            level =
                HttpLoggingInterceptor.Level.BASIC
        }

    // ========================================================
    // OKHTTP CLIENT
    // ========================================================

    private val okHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

    // ========================================================
    // STATPAL RETROFIT
    // ========================================================

    val statpalApi: StatpalApiService by lazy {

        Retrofit.Builder()

            .baseUrl(
                STATPAL_BASE_URL
            )

            .client(
                okHttpClient
            )

            .addConverterFactory(
                GsonConverterFactory.create(
                    customGson
                )
            )

            .build()

            .create(
                StatpalApiService::class.java
            )
    }

    // ========================================================
    // HIGHLIGHTLY RETROFIT
    // ========================================================

    val highlightlyApi: HighlightlyApiService by lazy {

        Retrofit.Builder()

            .baseUrl(
                HIGHLIGHTLY_BASE_URL
            )

            .client(
                okHttpClient
            )

            .addConverterFactory(
                GsonConverterFactory.create()
            )

            .build()

            .create(
                HighlightlyApiService::class.java
            )
    }
}
