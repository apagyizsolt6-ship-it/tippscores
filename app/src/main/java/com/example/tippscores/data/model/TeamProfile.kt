package com.example.tippscores.data.model

data class TeamProfile(
    val id: String,
    val name: String,
    val logoUrl: String = "",
    val type: String? = null,
    val country: String? = null,
    val league: String? = null,
    val statistics: List<TeamProfileStatistic> = emptyList(),
    val recentMatches: List<TeamRecentMatch> = emptyList()
)

data class TeamProfileStatistic(
    val label: String,
    val value: String
)

data class TeamRecentMatch(
    val date: String = "",
    val opponent: String,
    val opponentLogoUrl: String = "",
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: String = "-",
    val awayScore: String = "-",
    val result: String = "-"
)
