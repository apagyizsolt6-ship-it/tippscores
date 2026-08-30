package com.example.tippscores.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.tippscores.data.model.Match

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey val id: String,
    val leagueName: String,
    val leagueCountry: String,
    val leagueCountryFlag: String = "",
    val homeTeam: String,
    val homeTeamLogo: String,
    val awayTeam: String,
    val awayTeamLogo: String,
    val homeScore: Int?,
    val awayScore: Int?,
    val status: String,
    val isLive: Boolean,
    val tipPrediction: String?,
    val tipConfidence: Int?,
    val hasVideoHighlight: Boolean,
    val videoUrl: String?
)

// Konverter függvény a UI modellhez
fun MatchEntity.toMatch() = Match(
    id = id,
    leagueName = leagueName,
    leagueCountry = leagueCountry,
    leagueCountryFlag = leagueCountryFlag,
    homeTeam = homeTeam,
    homeTeamLogo = homeTeamLogo,
    awayTeam = awayTeam,
    awayTeamLogo = awayTeamLogo,
    homeScore = homeScore,
    awayScore = awayScore,
    status = status,
    isLive = isLive,
    tipPrediction = tipPrediction,
    tipConfidence = tipConfidence,
    hasVideoHighlight = hasVideoHighlight,
    videoUrl = videoUrl
)
