package com.example.tippscores.data.repository

import com.example.tippscores.data.local.MatchDao
import com.example.tippscores.data.local.MatchEntity
import com.example.tippscores.data.local.toMatch
import com.example.tippscores.data.model.Match
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MatchRepository(private val matchDao: MatchDao) {

    val allMatches: Flow<List<Match>> = matchDao.getAllMatches().map { entities ->
        entities.map { it.toMatch() }
    }

    // Előre betöltött teszt adatok (Offline / Első indításhoz)
    suspend fun refreshMatches() {
        val dummyNetworkData = listOf(
            MatchEntity(
                id = "1",
                leagueName = "Premier League",
                leagueCountry = "ANGLIA",
                homeTeam = "Liverpool",
                homeTeamLogo = "https://example.com/liverpool.png",
                awayTeam = "Nottingham",
                awayTeamLogo = "https://example.com/nottingham.png",
                homeScore = 2,
                awayScore = 2,
                status = "FT",
                isLive = false,
                tipPrediction = "TÚL 2.5",
                tipConfidence = 88,
                hasVideoHighlight = true,
                videoUrl = null
            ),
            MatchEntity(
                id = "2",
                leagueName = "Premier League",
                leagueCountry = "ANGLIA",
                homeTeam = "Bournemouth",
                homeTeamLogo = "https://example.com/bournemouth.png",
                awayTeam = "Everton",
                awayTeamLogo = "https://example.com/everton.png",
                homeScore = 1,
                awayScore = 0,
                status = "72'",
                isLive = true,
                tipPrediction = "HAZAI",
                tipConfidence = 75,
                hasVideoHighlight = true,
                videoUrl = null
            ),
            MatchEntity(
                id = "3",
                leagueName = "La Liga",
                leagueCountry = "SPANYOLORSZÁG",
                homeTeam = "Real Madrid",
                homeTeamLogo = "https://example.com/real.png",
                awayTeam = "Barcelona",
                awayTeamLogo = "https://example.com/barca.png",
                homeScore = 0,
                awayScore = 0,
                status = "21:00",
                isLive = false,
                tipPrediction = "BTTS YES",
                tipConfidence = 82,
                hasVideoHighlight = false,
                videoUrl = null
            )
        )
        matchDao.insertMatches(dummyNetworkData)
    }
}
