package com.example.tippscores.data.model

data class PlayerProfile(
    val id: String,
    val name: String,
    val fullName: String = "",
    val nationality: String = "",
    val birthDate: String = "",
    val age: Int? = null,
    val height: String = "",
    val weight: String = "",
    val preferredFoot: String = "",
    val position: String = "",
    val jerseyNumber: String = "",
    val marketValue: String = "",
    val contractUntil: String = "",
    val teamId: String = "",
    val teamName: String = "",
    val photoUrl: String? = null,
    val seasonStats: List<PlayerSeasonStat> = emptyList(),
    val recentMatches: List<PlayerMatchLog> = emptyList(),
    val trophies: List<PlayerTrophy> = emptyList()
)

data class PlayerSeasonStat(
    val leagueName: String,
    val season: String,
    val appearances: Int = 0,
    val lineups: Int = 0,
    val minutesPlayed: Int = 0,
    val goals: Int = 0,
    val assists: Int = 0,
    val yellowCards: Int = 0,
    val redCards: Int = 0,
    val shotsTotal: Int = 0,
    val shotsOnTarget: Int = 0,
    val keyPasses: Int = 0,
    val passAccuracy: Float = 0f,
    val rating: Float = 0f
)

data class PlayerMatchLog(
    val matchId: String,
    val date: String,
    val opponent: String,
    val score: String,
    val minutes: Int,
    val goals: Int,
    val assists: Int,
    val yellowCard: Boolean = false,
    val redCard: Boolean = false,
    val rating: Float = 0f
)

data class PlayerTrophy(
    val title: String,
    val season: String,
    val teamName: String
)
