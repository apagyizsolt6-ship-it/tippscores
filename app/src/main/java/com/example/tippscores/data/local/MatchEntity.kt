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

    // -1 = nem alapértelmezett kiemelt bajnokság, 0-4 = az 5 top
    // bajnokság sorindexe (Anglia, Németország, Franciaország,
    // Olaszország, Spanyolország sorrendben).
    val presetOrder: Int = -1,

    val homeTeam: String,
    val homeTeamLogo: String,
    val awayTeam: String,
    val awayTeamLogo: String,
    val homeScore: Int?,
    val awayScore: Int?,

    // Igaz, ha az ELŐZŐ frissítéshez képest nőtt az adott csapat
    // gólszáma (vagyis épp most született gól nála).
    val homeJustScored: Boolean = false,
    val awayJustScored: Boolean = false,

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
    presetOrder = presetOrder,
    homeTeam = homeTeam,
    homeTeamLogo = homeTeamLogo,
    awayTeam = awayTeam,
    awayTeamLogo = awayTeamLogo,
    homeScore = homeScore,
    awayScore = awayScore,
    homeJustScored = homeJustScored,
    awayJustScored = awayJustScored,
    status = status,
    isLive = isLive,
    tipPrediction = tipPrediction,
    tipConfidence = tipConfidence,
    hasVideoHighlight = hasVideoHighlight,
    videoUrl = videoUrl
)
