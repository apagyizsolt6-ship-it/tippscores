package com.example.tippscores.data.repository

import com.example.tippscores.data.local.ApiPreferences
import com.example.tippscores.data.local.MatchDao
import com.example.tippscores.data.local.MatchEntity
import com.example.tippscores.data.local.toMatch
import com.example.tippscores.data.model.Match
import com.example.tippscores.data.remote.NetworkModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
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

    suspend fun fetchMatchesForOffset(offset: Int = 0) {

        val statpalKey =
            apiPreferences.statpalApiKey.trim()

        val highlightlyKey =
            apiPreferences.highlightlyApiKey.trim()

        if (statpalKey.isEmpty()) {

            throw IllegalStateException(
                "A StatPal API kulcs nincs megadva."
            )
        }

        try {

            // ------------------------------------------------
            // FONTOS:
            // offset 0 = MAI ÖSSZES MÉRKŐZÉS
            //
            // NEM a live endpoint!
            // ------------------------------------------------

            val response =
                NetworkModule.statpalApi.getDailyMatches(
                    offset = offset,
                    accessKey = statpalKey
                )

            // ------------------------------------------------
            // HIGHLIGHTLY
            // ------------------------------------------------

            val highlightsMap =
                if (highlightlyKey.isNotEmpty()) {

                    try {

                        val today =
                            SimpleDateFormat(
                                "yyyy-MM-dd",
                                Locale.US
                            ).format(Date())

                        val highlightResponse =
                            NetworkModule.highlightlyApi
                                .getHighlights(
                                    apiKey = highlightlyKey,
                                    date = today
                                )

                        highlightResponse.data
                            .orEmpty()
                            .mapNotNull { item ->

                                val matchId =
                                    item.match?.id

                                if (!matchId.isNullOrEmpty()) {
                                    matchId to item
                                } else {
                                    null
                                }
                            }
                            .toMap()

                    } catch (e: Exception) {

                        // A Highlightly hibája ne blokkolja
                        // a StatPal mérkőzések megjelenítését.

                        emptyMap()
                    }

                } else {
                    emptyMap()
                }

            // ------------------------------------------------
            // MATCH ENTITIES
            // ------------------------------------------------

            val matchEntities =
                mutableListOf<MatchEntity>()

            response.data
                ?.leagues
                ?.forEach { league ->

                    val countryAndLeague =
                        league.name ?: "Bajnokság"

                    val parts =
                        countryAndLeague.split(":")

                    val country =
                        parts
                            .getOrNull(0)
                            ?.trim()
                            ?.uppercase()
                            ?: "FOCI"

                    val leagueName =
                        parts
                            .getOrNull(1)
                            ?.trim()
                            ?: countryAndLeague

                    league.matches
                        ?.forEach { match ->

                            val highlight =
                                highlightsMap[match.mainId]

                            val status =
                                match.status
                                    ?: match.time
                                    ?: "NS"

                            val normalizedStatus =
                                status.uppercase()

                            val isFinished =
                                normalizedStatus == "FT" ||
                                    normalizedStatus == "AET" ||
                                    normalizedStatus == "PEN" ||
                                    normalizedStatus == "CANCELLED" ||
                                    normalizedStatus == "POSTPONED"

                            val isLive =
                                !isFinished &&
                                    (
                                        normalizedStatus.contains("LIVE") ||
                                            normalizedStatus.contains("1H") ||
                                            normalizedStatus.contains("2H") ||
                                            normalizedStatus.contains("HT") ||
                                            normalizedStatus.contains("ET")
                                    )

                            matchEntities.add(

                                MatchEntity(

                                    id =
                                        match.mainId
                                            ?: "${match.home?.id}_${match.away?.id}_${match.date}_${match.time}",

                                    leagueName =
                                        leagueName,

                                    leagueCountry =
                                        country,

                                    homeTeam =
                                        match.home?.name
                                            ?: "Hazai",

                                    homeTeamLogo =
                                        "",

                                    awayTeam =
                                        match.away?.name
                                            ?: "Vendég",

                                    awayTeamLogo =
                                        "",

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

                                    hasVideoHighlight =
                                        highlight?.url != null,

                                    videoUrl =
                                        highlight?.url
                                )
                            )
                        }
                }

            // ------------------------------------------------
            // DATABASE
            // ------------------------------------------------

            matchDao.clearMatches()

            if (matchEntities.isNotEmpty()) {

                matchDao.insertMatches(
                    matchEntities
                )
            }

        } catch (e: Exception) {

            e.printStackTrace()

            // FONTOS:
            // Nem nyeljük el a hibát!
            // A ViewModel így megkapja a valódi
            // hálózati/API hibaüzenetet.

            throw e
        }
    }

    // ========================================================
    // KÜLÖN LIVE LEKÉRÉS
    // ========================================================

    suspend fun fetchLiveMatches() {

        val statpalKey =
            apiPreferences.statpalApiKey.trim()

        if (statpalKey.isEmpty()) {

            throw IllegalStateException(
                "A StatPal API kulcs nincs megadva."
            )
        }

        try {

            val response =
                NetworkModule.statpalApi
                    .getLiveMatches(
                        accessKey = statpalKey
                    )

            val liveEntities =
                mutableListOf<MatchEntity>()

            response.data
                ?.leagues
                ?.forEach { league ->

                    val countryAndLeague =
                        league.name ?: "Bajnokság"

                    val parts =
                        countryAndLeague.split(":")

                    val country =
                        parts
                            .getOrNull(0)
                            ?.trim()
                            ?.uppercase()
                            ?: "FOCI"

                    val leagueName =
                        parts
                            .getOrNull(1)
                            ?.trim()
                            ?: countryAndLeague

                    league.matches
                        ?.forEach { match ->

                            val status =
                                match.status
                                    ?: match.time
                                    ?: "LIVE"

                            liveEntities.add(

                                MatchEntity(

                                    id =
                                        match.mainId
                                            ?: System.currentTimeMillis()
                                                .toString(),

                                    leagueName =
                                        leagueName,

                                    leagueCountry =
                                        country,

                                    homeTeam =
                                        match.home?.name
                                            ?: "Hazai",

                                    homeTeamLogo =
                                        "",

                                    awayTeam =
                                        match.away?.name
                                            ?: "Vendég",

                                    awayTeamLogo =
                                        "",

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

            matchDao.clearMatches()

            if (liveEntities.isNotEmpty()) {

                matchDao.insertMatches(
                    liveEntities
                )
            }

        } catch (e: Exception) {

            e.printStackTrace()

            throw e
        }
    }
}
