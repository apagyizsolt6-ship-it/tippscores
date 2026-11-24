package com.example.tippscores.data.model

/** Egy mérkőzés "Előzetes" adatai: sztorik, modell-tipp, időjárás, szorzók. */
data class MatchPreview(
    val storylines: List<Storyline> = emptyList(),
    val prediction: PredictionInfo? = null,
    val weather: WeatherInfo? = null,
    val odds: OddsInfo? = null
) {
    val isEmpty: Boolean
        get() = storylines.isEmpty() &&
            prediction == null &&
            weather == null &&
            (odds == null || odds.markets.isEmpty())
}

data class Storyline(
    val date: String? = null,
    val tag: String? = null,
    val title: String? = null,
    val text: String
)

data class PredictionInfo(
    val market: String? = null,
    val pick: String,
    val odds: Double? = null,
    val reasoning: String? = null
)

data class WeatherInfo(
    val temperature: String? = null,
    val condition: String? = null,
    val feelsLike: String? = null,
    val wind: String? = null,
    val rainChance: String? = null,
    val humidity: String? = null
)

data class OddsInfo(
    val isLive: Boolean = false,
    val bookmakerCount: Int = 0,
    val markets: List<OddsMarket> = emptyList()
)

data class OddsMarket(
    val title: String,
    val outcomes: List<OddsOutcome>
)

data class OddsOutcome(
    val label: String,
    val average: Double,
    val best: Double,
    val bestBookmaker: String? = null,
    /** A szorzókból számolt (margin nélküli) valószínűség, 0..1. */
    val probability: Double = 0.0
)

// --- Tabella ---

data class StandingRow(
    val position: Int,
    val team: String,
    val teamId: String? = null,
    val played: Int = 0,
    val won: Int = 0,
    val drawn: Int = 0,
    val lost: Int = 0,
    val goalsFor: Int = 0,
    val goalsAgainst: Int = 0,
    val points: Int = 0
) {
    val goalDifference: Int get() = goalsFor - goalsAgainst
}
