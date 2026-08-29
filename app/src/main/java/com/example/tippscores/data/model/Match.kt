package com.example.tippscores.data.model

data class Match(
    val id: String,
    val leagueName: String,
    val leagueCountry: String,
    val homeTeam: String,
    val homeTeamLogo: String,
    val awayTeam: String,
    val awayTeamLogo: String,
    val homeScore: Int?,
    val awayScore: Int?,
    val status: String, // pl. "LIVE", "72'", "18:30"
    val isLive: Boolean,
    
    // Statpal API adatok
    val tipPrediction: String?, // pl. "TÚL 2.5 GÓL"
    val tipConfidence: Int?,    // pl. 85
    
    // Highlightly API adatok
    val hasVideoHighlight: Boolean = false,
    val videoUrl: String? = null
)
