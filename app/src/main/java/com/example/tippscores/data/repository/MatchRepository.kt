package com.example.tippscores.data.repository

import com.example.tippscores.data.local.ApiPreferences
import com.example.tippscores.data.local.MatchDao
import com.example.tippscores.data.local.MatchEntity
import com.example.tippscores.data.local.toMatch
import com.example.tippscores.data.model.Match
import com.example.tippscores.data.remote.NetworkModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

class MatchRepository(
    private val matchDao: MatchDao,
    private val apiPreferences: ApiPreferences
) {

    val allMatches: Flow<List<Match>> =
        matchDao.getAllMatches().map { entities ->
            entities.map { it.toMatch() }
        }

    // ========================================================
    // NAPI MÉRKŐZÉSEK
    // ========================================================

    /**
     * Az adott nap teljes mérkőzéslistája.
     *
     * -7 = 7 nappal korábban
     *  0 = ma
     * +7 = 7 nappal később
     *
     * Az offsetet a StatPal DAILY végpont kapja.
     */
    suspend fun fetchMatchesForOffset(
        offset: Int = 0
    ) {

        val statpalKey =
            apiPreferences.statpalApiKey.trim()

        if (statpalKey.isEmpty()) {

            matchDao.clearMatches()

            throw IllegalStateException(
                "A StatPal API-kulcs nincs megadva."
            )
        }

        val safeOffset =
            offset.coerceIn(-7, 7)

        try {

            // ------------------------------------------------
            // Régi napi adatok törlése
            // ------------------------------------------------

            matchDao.clearMatches()

            // ------------------------------------------------
            // STATPAL DAILY
            // ------------------------------------------------

            val response =
                NetworkModule.statpalApi.getDailyMatches(
                    offset = safeOffset,
                    accessKey = statpalKey
                )

            val matchEntities =
                mutableListOf<MatchEntity>()

            // ------------------------------------------------
            // LIGÁK
            // ------------------------------------------------

            response.data
                ?.leagues
                .orEmpty()
                .forEach { league ->

                    val countryAndLeague =
                        league.name
                            ?.trim()
                            ?.takeUnless {
                                it.isEmpty()
                            }
                            ?: "Bajnokság"

                    val parts =
                        countryAndLeague.split(
                            ":",
                            limit = 2
                        )

                    val country =
                        parts
                            .getOrNull(0)
                            ?.trim()
                            ?.uppercase(
                                Locale.getDefault()
                            )
                            ?.takeUnless {
                                it.isEmpty()
                            }
                            ?: league.country
                                ?.trim()
                                ?.uppercase(
                                    Locale.getDefault()
                                )
                                ?.takeUnless {
                                    it.isEmpty()
                                }
                            ?: "FOCI"

                    val leagueName =
                        parts
                            .getOrNull(1)
                            ?.trim()
                            ?.takeUnless {
                                it.isEmpty()
                            }
                            ?: countryAndLeague

                    // ------------------------------------------------
                    // MÉRKŐZÉSEK
                    // ------------------------------------------------

                    league.matches
                        .orEmpty()
                        .forEach { match ->

                            val status =
                                displayStatus(
                                    match.status,
                                    match.time
                                )

                            val isLive =
                                isLiveStatus(
                                    match.status
                                )

                            val matchId =
                                match.mainId
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                    ?: buildFallbackId(
                                        match,
                                        leagueName
                                    )

                            matchEntities.add(

                                MatchEntity(

                                    id = matchId,

                                    leagueName =
                                        leagueName,

                                    leagueCountry =
                                        country,

                                    homeTeam =
                                        match.home
                                            ?.name
                                            ?.trim()
                                            ?.takeUnless {
                                                it.isEmpty()
                                            }
                                            ?: "Hazai",

                                    homeTeamLogo = "",

                                    awayTeam =
                                        match.away
                                            ?.name
                                            ?.trim()
                                            ?.takeUnless {
                                                it.isEmpty()
                                            }
                                            ?: "Vendég",

                                    awayTeamLogo = "",

                                    homeScore =
                                        match.home
                                            ?.goals
                                            ?.toIntOrNull(),

                                    awayScore =
                                        match.away
                                            ?.goals
                                            ?.toIntOrNull(),

                                    status =
                                        status,

                                    isLive =
                                        isLive,

                                    tipPrediction =
                                        null,

                                    tipConfidence =
                                        null,

                                    // Highlightlyt most nem
                                    // kapcsoljuk a meccslistához.
                                    // A mérkőzéslista önállóan működik.
                                    hasVideoHighlight =
                                        false,

                                    videoUrl =
                                        null
                                )
                            )
                        }
                }

            // ------------------------------------------------
            // IDŐRENDI RENDEZÉS
            // ------------------------------------------------

            val sortedEntities =
                matchEntities.sortedWith(

                    compareBy<MatchEntity> {

                        timeSortKey(
                            it.status,
                            it.isLive
                        )

                    }.thenBy {

                        it.leagueCountry

                    }.thenBy {

                        it.leagueName

                    }.thenBy {

                        it.homeTeam
                    }
                )

            // ------------------------------------------------
            // ADATBÁZIS
            // ------------------------------------------------

            if (sortedEntities.isNotEmpty()) {

                matchDao.insertMatches(
                    sortedEntities
                )
            }

            // ------------------------------------------------
            // ÜRES VÁLASZ ELLENŐRZÉSE
            // ------------------------------------------------

            if (response.data == null) {

                throw IllegalStateException(
                    "A StatPal üres választ adott a kiválasztott napra."
                )
            }

        } catch (e: Exception) {

            e.printStackTrace()

            matchDao.clearMatches()

            throw e
        }
    }

    // ========================================================
    // ÉLŐ MÉRKŐZÉSEK
    // ========================================================

    /**
     * Külön LIVE lekérés.
     *
     * Ezt nem használjuk az Összes nézethez.
     */
    suspend fun fetchLiveMatches() {

        val statpalKey =
            apiPreferences.statpalApiKey.trim()

        if (statpalKey.isEmpty()) {

            matchDao.clearMatches()

            throw IllegalStateException(
                "A StatPal API-kulcs nincs megadva."
            )
        }

        try {

            matchDao.clearMatches()

            val response =
                NetworkModule.statpalApi.getLiveMatches(
                    statpalKey
                )

            val liveEntities =
                mutableListOf<MatchEntity>()

            response.data
                ?.leagues
                .orEmpty()
                .forEach { league ->

                    val countryAndLeague =
                        league.name
                            ?.trim()
                            ?.takeUnless {
                                it.isEmpty()
                            }
                            ?: "Bajnokság"

                    val parts =
                        countryAndLeague.split(
                            ":",
                            limit = 2
                        )

                    val country =
                        parts
                            .getOrNull(0)
                            ?.trim()
                            ?.uppercase(
                                Locale.getDefault()
                            )
                            ?.takeUnless {
                                it.isEmpty()
                            }
                            ?: league.country
                                ?.trim()
                                ?.uppercase(
                                    Locale.getDefault()
                                )
                                ?.takeUnless {
                                    it.isEmpty()
                                }
                            ?: "FOCI"

                    val leagueName =
                        parts
                            .getOrNull(1)
                            ?.trim()
                            ?.takeUnless {
                                it.isEmpty()
                            }
                            ?: countryAndLeague

                    league.matches
                        .orEmpty()
                        .forEach { match ->

                            val matchId =
                                match.mainId
                                    ?.takeIf {
                                        it.isNotBlank()
                                    }
                                    ?: buildFallbackId(
                                        match,
                                        leagueName
                                    )

                            liveEntities.add(

                                MatchEntity(

                                    id = matchId,

                                    leagueName =
                                        leagueName,

                                    leagueCountry =
                                        country,

                                    homeTeam =
                                        match.home
                                            ?.name
                                            ?.trim()
                                            ?.takeUnless {
                                                it.isEmpty()
                                            }
                                            ?: "Hazai",

                                    homeTeamLogo = "",

                                    awayTeam =
                                        match.away
                                            ?.name
                                            ?.trim()
                                            ?.takeUnless {
                                                it.isEmpty()
                                            }
                                            ?: "Vendég",

                                    awayTeamLogo = "",

                                    homeScore =
                                        match.home
                                            ?.goals
                                            ?.toIntOrNull(),

                                    awayScore =
                                        match.away
                                            ?.goals
                                            ?.toIntOrNull(),

                                    status =
                                        displayStatus(
                                            match.status,
                                            match.time
                                        ),

                                    isLive =
                                        true,

                                    tipPrediction =
                                        null,

                                    tipConfidence =
                                        null,

                                    hasVideoHighlight =
                                        false,

                                    videoUrl =
                                        null
                                )
                            )
                        }
                }

            if (liveEntities.isNotEmpty()) {

                matchDao.insertMatches(
                    liveEntities
                )
            }

        } catch (e: Exception) {

            e.printStackTrace()

            matchDao.clearMatches()

            throw e
        }
    }

    // ========================================================
    // STÁTUSZ MAGYARÍTÁS
    // ========================================================

    private fun displayStatus(
        status: String?,
        time: String?
    ): String {

        val normalized =
            status
                ?.trim()
                ?.uppercase(
                    Locale.getDefault()
                )
                .orEmpty()

        val normalizedTime =
            time
                ?.trim()
                .orEmpty()

        return when {

            normalized.contains("POST") ||
                normalized.contains("POSTP") ->
                "Elhalasztva"

            normalized.contains("CANCEL") ->
                "Törölve"

            normalized == "AET" ->
                "Hosszabbítás"

            normalized == "PEN" ->
                "Tizenegyesek"

            normalized == "FT" ||
                normalized == "FINISHED" ->
                "Vége"

            normalized == "HT" ->
                "Szünet"

            isLiveStatus(status) ->
                normalizedTime.ifEmpty {
                    "ÉLŐ"
                }

            normalizedTime.isNotEmpty() ->
                normalizedTime

            normalized.isNotEmpty() ->
                normalized

            else ->
                "–"
        }
    }

    // ========================================================
    // ÉLŐ STÁTUSZ
    // ========================================================

    private fun isLiveStatus(
        status: String?
    ): Boolean {

        val normalized =
            status
                ?.trim()
                ?.uppercase(
                    Locale.getDefault()
                )
                .orEmpty()

        if (normalized.isEmpty()) {
            return false
        }

        return normalized.contains("LIVE") ||
            normalized == "1H" ||
            normalized == "2H" ||
            normalized == "HT" ||
            normalized == "ET" ||
            normalized.matches(
                Regex("\\d{1,3}'?")
            )
    }

    // ========================================================
    // BEFEJEZETT STÁTUSZ
    // ========================================================

    private fun isFinishedStatus(
        status: String?
    ): Boolean {

        val normalized =
            status
                ?.trim()
                ?.uppercase(
                    Locale.getDefault()
                )
                .orEmpty()

        return normalized == "FT" ||
            normalized == "FINISHED" ||
            normalized == "AET" ||
            normalized == "PEN" ||
            normalized.contains("POST") ||
            normalized.contains("CANCEL")
    }

    // ========================================================
    // RENDEZÉSI KULCS
    // ========================================================

    private fun timeSortKey(
        status: String,
        isLive: Boolean
    ): Int {

        if (isLive) {
            return 0
        }

        if (
            status == "Vége" ||
            status == "Hosszabbítás" ||
            status == "Tizenegyesek"
        ) {
            return 3000
        }

        val match =
            Regex(
                "(\\d{1,2}):(\\d{2})"
            ).find(status)

        return if (match != null) {

            match.groupValues[1]
                .toInt() * 60 +
                match.groupValues[2]
                    .toInt()

        } else {

            2000
        }
    }

    // ========================================================
    // FALLBACK ID
    // ========================================================

    private fun buildFallbackId(
        match: com.example.tippscores.data.remote.StatpalMatchItem,
        leagueName: String
    ): String {

        return listOf(

            leagueName,

            match.date
                .orEmpty(),

            match.time
                .orEmpty(),

            match.home
                ?.id
                .orEmpty(),

            match.home
                ?.name
                .orEmpty(),

            match.away
                ?.id
                .orEmpty(),

            match.away
                ?.name
                .orEmpty()

        ).joinToString("|")
            .hashCode()
            .toString()
    }
}
