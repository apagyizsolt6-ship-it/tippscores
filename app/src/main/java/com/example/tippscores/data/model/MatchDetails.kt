package com.example.tippscores.data.model

data class MatchDetails(
    val statistics: List<MatchStatistic> = emptyList(),
    val events: List<MatchEvent> = emptyList(),
    val homeLineup: MatchLineup = MatchLineup(),
    val awayLineup: MatchLineup = MatchLineup(),
    val headToHead: List<HeadToHeadMatch> = emptyList(),
    val highlight: MatchHighlight? = null,
    val prediction: String? = null,
    val weather: String? = null,
    val injuries: List<String> = emptyList()
) {
    val hasStatistics: Boolean get() = statistics.isNotEmpty()
    val hasEvents: Boolean get() = events.isNotEmpty()
    val hasLineups: Boolean get() =
        homeLineup.startingPlayers.isNotEmpty() ||
            homeLineup.substitutePlayers.isNotEmpty() ||
            awayLineup.startingPlayers.isNotEmpty() ||
            awayLineup.substitutePlayers.isNotEmpty()
    val hasPrediction: Boolean get() = !prediction.isNullOrBlank()
    val hasWeather: Boolean get() = !weather.isNullOrBlank()
    val hasInjuries: Boolean get() = injuries.isNotEmpty()
}

data class HeadToHeadMatch(
    val date: String = "",
    val homeTeam: String,
    val awayTeam: String,
    val homeScore: String = "-",
    val awayScore: String = "-",
    val homeLogoUrl: String = "",
    val awayLogoUrl: String = ""
)

data class MatchStatistic(
    val label: String,
    val home: String,
    val away: String
)

data class MatchEvent(
    val minute: String,
    val team: String,
    val player: String,
    val assist: String? = null,
    val type: String,
    val detail: String? = null
)

data class MatchLineup(
    val formation: String? = null,
    val coach: String? = null,
    val coachId: String? = null,
    val startingPlayers: List<LineupPlayer> = emptyList(),
    val substitutePlayers: List<LineupPlayer> = emptyList()
)

data class LineupPlayer(
    val id: String? = null,
    val number: String? = null,
    val name: String,
    val position: String? = null,
    val photoUrl: String? = null,
    val isCaptain: Boolean = false
)

data class MatchHighlight(
    val title: String = "",
    val url: String = ""
)

data class PlayerProfile(
    val id: String,
    val name: String,
    val age: String? = null,
    val nationality: String? = null,
    val position: String? = null,
    val height: String? = null,
    val weight: String? = null,
    val preferredFoot: String? = null,
    val team: String? = null,
    val marketValueEur: String? = null,

    // Aktuális szezon (a játékos bajnokságának statisztikája)
    val appearances: String? = null,
    val goals: String? = null,
    val assists: String? = null,
    val minutesPlayed: String? = null,
    val rating: String? = null,
    val yellowCards: String? = null,
    val redCards: String? = null,

    val birthdate: String? = null,
    val birthplace: String? = null,
    val seasonLabel: String? = null,
    val startingLineups: String? = null,
    val shotsTotal: String? = null,
    val shotsOnTarget: String? = null,
    val keyPasses: String? = null,
    val dribbleAttempts: String? = null,
    val dribbleSuccess: String? = null,
    val duelsTotal: String? = null,
    val duelsWon: String? = null,
    val tackles: String? = null,
    val interceptions: String? = null,
    val foulsCommitted: String? = null,
    val foulsDrawn: String? = null,
    val penScored: String? = null,
    val penMissed: String? = null,
    val saves: String? = null,
    val goalsConceded: String? = null,

    // Klubkarrier (összes szezon együtt)
    val careerAppearances: String? = null,
    val careerMinutes: String? = null,
    val careerAssists: String? = null,
    val careerShotsTotal: String? = null,
    val careerShotsOnTarget: String? = null,
    val careerPenScored: String? = null,
    val careerRating: String? = null,

    val trophies: List<TeamTrophy> = emptyList(),
    val transfers: List<PlayerTransfer> = emptyList()
)

data class PlayerTransfer(
    val date: String = "",
    val from: String? = null,
    val to: String? = null,
    val type: String? = null,
    val price: String? = null
)
