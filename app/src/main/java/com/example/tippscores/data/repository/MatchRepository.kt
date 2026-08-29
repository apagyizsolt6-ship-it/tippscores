package com.example.tippscores.data.repository

import com.example.tippscores.data.local.ApiPreferences
import com.example.tippscores.data.local.MatchDao
import com.example.tippscores.data.local.MatchEntity
import com.example.tippscores.data.local.toMatch
import com.example.tippscores.data.model.Match
import com.example.tippscores.data.remote.NetworkModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MatchRepository(
    private val matchDao: MatchDao,
    private val apiPreferences: ApiPreferences
) {

    val allMatches: Flow<List<Match>> = matchDao.getAllMatches().map { entities ->
        entities.map { it.toMatch() }
    }

    suspend fun fetchMatchesForOffset(offset: Int = 0) {
        val statpalKey = apiPreferences.statpalApiKey.trim()
        val highlightlyKey = apiPreferences.highlightlyApiKey.trim()

        if (statpalKey.isEmpty()) {
            matchDao.clearMatches()
            return
        }

        try {
            // Ha offset == 0, akkor a live meccseket kérjük, egyébként a daily végpontot offsettel
            val response = if (offset == 0) {
                NetworkModule.statpalApi.getLiveMatches(statpalKey)
            } else {
                NetworkModule.statpalApi.getDailyMatches(offset = offset, accessKey = statpalKey)
            }

            val highlightsMap = if (highlightlyKey.isNotEmpty()) {
                try {
                    NetworkModule.highlightlyApi.getHighlights(apiKey = highlightlyKey)
                        .filter { !it.match_id.isNullOrEmpty() }
                        .associateBy { it.match_id!! }
                } catch (e: Exception) {
                    emptyMap()
                }
            } else emptyMap()

            val matchEntities = mutableListOf<MatchEntity>()

            response.data?.leagues?.forEach { league ->
                val countryAndLeague = league.name ?: "Bajnokság"
                val parts = countryAndLeague.split(":")
                val country = parts.getOrNull(0)?.trim()?.uppercase() ?: "FOCI"
                val leagueName = parts.getOrNull(1)?.trim() ?: countryAndLeague

                league.matches?.forEach { m ->
                    val highlight = highlightsMap[m.mainId]

                    matchEntities.add(
                        MatchEntity(
                            id = m.mainId ?: System.currentTimeMillis().toString(),
                            leagueName = leagueName,
                            leagueCountry = country,
                            homeTeam = m.home?.name ?: "Hazai",
                            homeTeamLogo = "",
                            awayTeam = m.away?.name ?: "Vendég",
                            awayTeamLogo = "",
                            homeScore = m.home?.goals?.toIntOrNull(),
                            awayScore = m.away?.goals?.toIntOrNull(),
                            status = m.status ?: m.time ?: "FT",
                            isLive = m.status != "FT" && m.status != "AET" && offset == 0,
                            tipPrediction = null,
                            tipConfidence = null,
                            hasVideoHighlight = highlight?.url != null,
                            videoUrl = highlight?.url
                        )
                    )
                }
            }

            matchDao.clearMatches()
            if (matchEntities.isNotEmpty()) {
                matchDao.insertMatches(matchEntities)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            matchDao.clearMatches()
        }
    }
}
