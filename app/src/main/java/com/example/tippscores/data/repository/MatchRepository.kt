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

    suspend fun fetchRealMatchesFromNetwork() {
        val statpalKey = apiPreferences.statpalApiKey
        val highlightlyKey = apiPreferences.highlightlyApiKey

        if (statpalKey.isNotBlank()) {
            try {
                // 1. Statpal meccsek és tippek lekérése
                val remoteMatches = NetworkModule.statpalApi.getMatches(statpalKey)

                // 2. Highlightly videó meglét lekérése (ha van kulcs)
                val highlightsMap = if (highlightlyKey.isNotBlank()) {
                    try {
                        NetworkModule.highlightlyApi.getHighlights(highlightlyKey)
                            .associateBy { it.match_id }
                    } catch (e: Exception) {
                        emptyMap()
                    }
                } else emptyMap()

                // 3. Adatok összefésülése és mentése a Room adatbázisba
                val entities = remoteMatches.map { dto ->
                    val highlight = highlightsMap[dto.id]
                    MatchEntity(
                        id = dto.id,
                        leagueName = dto.league_name ?: "Liga",
                        leagueCountry = dto.country_name ?: "Ország",
                        homeTeam = dto.home_team ?: "Hazai",
                        homeTeamLogo = dto.home_team_logo ?: "",
                        awayTeam = dto.away_team ?: "Vendég",
                        awayTeamLogo = dto.away_team_logo ?: "",
                        homeScore = dto.home_score,
                        awayScore = dto.away_score,
                        status = dto.status ?: "18:00",
                        isLive = dto.is_live,
                        tipPrediction = dto.prediction,
                        tipConfidence = dto.confidence,
                        hasVideoHighlight = highlight?.video_url != null,
                        videoUrl = highlight?.video_url
                    )
                }

                matchDao.clearMatches()
                matchDao.insertMatches(entities)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
