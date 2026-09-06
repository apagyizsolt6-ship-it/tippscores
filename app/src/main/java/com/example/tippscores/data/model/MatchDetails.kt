package com.example.tippscores.data.model

data class MatchDetails(
    val statistics: List<MatchStatistic> = emptyList(),
    val events: List<MatchEvent> = emptyList(),
    val homeLineup: MatchLineup = MatchLineup(),
    val awayLineup: MatchLineup = MatchLineup(),
    val headToHead: List<HeadToHeadMatch> = emptyList(),
    val highlight: MatchHighlight? = null
) {
    val hasStatistics: Boolean get() = statistics.isNotEmpty()
    val hasEvents: Boolean get() = events.isNotEmpty()
    val hasLineups: Boolean get() =
        homeLineup.startingPlayers.isNotEmpty() ||
            homeLineup.substitutePlayers.isNotEmpty() ||
            awayLineup.startingPlayers.isNotEmpty() ||
            awayLineup.substitutePlayers.isNotEmpty()
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

/** A StatPal "Get Player" végpontból (v2/soccer/players/{id}) összeállított, kurált adatlap. */
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
    val appearances: String? = null,
    val goals: String? = null,
    val assists: String? = null,
    val minutesPlayed: String? = null,
    val rating: String? = null,
    val yellowCards: String? = null,
    val redCards: String? = null
)

data class MatchHighlight(
    val id: String = "",
    val title: String = "",
    val url: String? = null,
    val embedUrl: String? = null,
    val imageUrl: String? = null,
    val description: String? = null,
    val type: String? = null
)
