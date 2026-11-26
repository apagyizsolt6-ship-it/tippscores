package com.example.tippscores.data.model

data class TeamProfile(
    val id: String,
    val name: String,
    val logoUrl: String = "",
    val type: String? = null,
    val country: String? = null,
    val league: String? = null,
    val statistics: List<TeamProfileStatistic> = emptyList(),
    val recentMatches: List<TeamRecentMatch> = emptyList(),
    val form: List<TeamFormItem> = emptyList(),

    // StatPal "Get Team" (v2/soccer/teams/{team_id}) adatok
    val founded: String? = null,
    val venueName: String? = null,
    val venueCity: String? = null,
    val venueCapacity: String? = null,
    val venueSurface: String? = null,
    val coachName: String? = null,
    val coachId: String? = null,
    val leagueRecords: List<TeamLeagueRecord> = emptyList(),
    val squad: List<TeamSquadPlayer> = emptyList(),
    val transfers: List<TeamTransfer> = emptyList(),
    val trophies: List<TeamTrophy> = emptyList()
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


data class TeamFormItem(
    val result: String,
    val label: String,
    val score: String,
    val opponent: String
)

/** Egy bajnokság szezonmérlege (győzelem / döntetlen / vereség). */
data class TeamLeagueRecord(
    val leagueName: String,
    val season: String = "",
    val leagueId: String = "",
    val winsTotal: Int = 0,
    val winsHome: Int = 0,
    val winsAway: Int = 0,
    val drawsTotal: Int = 0,
    val drawsHome: Int = 0,
    val drawsAway: Int = 0,
    val lossesTotal: Int = 0,
    val lossesHome: Int = 0,
    val lossesAway: Int = 0,
    val scoringMinutes: List<TeamMinuteBucket> = emptyList(),
    val concededMinutes: List<TeamMinuteBucket> = emptyList()
) {
    val played: Int get() = winsTotal + drawsTotal + lossesTotal
}

data class TeamMinuteBucket(
    val range: String,
    val percent: Int = 0,
    val count: Int = 0
)

data class TeamSquadPlayer(
    val id: String? = null,
    val name: String,
    val number: String? = null,
    val age: String? = null,
    val position: String? = null,
    val isCaptain: Boolean = false,
    val injured: Boolean = false
)

data class TeamTransfer(
    val playerName: String,
    val date: String = "",
    val otherTeam: String? = null,
    val type: String? = null,
    val price: String? = null,
    val isIncoming: Boolean = true
)

data class TeamTrophy(
    val league: String,
    val country: String? = null,
    val status: String? = null,
    val count: String? = null,
    val seasons: String? = null
)

/** Egy játékos szezonstatisztikája a bajnoki (leagues/{id}/stats) válaszból. */
data class LeaguePlayerStat(
    val playerId: String? = null,
    val name: String,
    val teamId: String? = null,
    val teamName: String = "",
    val number: String? = null,
    val position: String? = null,
    val appearances: Int = 0,
    val minutes: Int = 0,
    val goals: Int = 0,
    val assists: Int = 0,
    val yellowCards: Int = 0,
    val redCards: Int = 0,
    val rating: Double? = null,
    val age: String? = null,
    val shotsTotal: Int = 0,
    val shotsOnTarget: Int = 0,
    val keyPasses: Int = 0,
    val dribbleAttempts: Int = 0,
    val dribbleSuccess: Int = 0,
    val duelsTotal: Int = 0,
    val duelsWon: Int = 0,
    val tackles: Int = 0,
    val interceptions: Int = 0,
    val clearances: Int = 0,
    val passAttempts: Int = 0,
    val passSuccess: Int = 0,
    val saves: Int = 0,
    val goalsConceded: Int = 0,
    val foulsCommitted: Int = 0
)

data class LeagueStats(
    val leagueId: String,
    val leagueName: String,
    val players: List<LeaguePlayerStat> = emptyList()
)
