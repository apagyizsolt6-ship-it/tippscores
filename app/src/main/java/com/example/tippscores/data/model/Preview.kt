package com.example.tippscores.data.model

/** Egy mérkőzés "Előzetes" adatai: sztorik, modell-tipp, időjárás, szorzók. */
data class MatchPreview(
    val storylines: List<Storyline> = emptyList(),
    val prediction: PredictionInfo? = null,
    val weather: WeatherInfo? = null,
    val odds: OddsInfo? = null,
    val rivalry: String? = null
) {
    val isEmpty: Boolean
        get() = storylines.isEmpty() &&
            rivalry == null &&
            prediction == null &&
            weather == null &&
            (odds == null || odds.markets.isEmpty())
}

/** Szerkesztői történetszál: elemzés (insight) vagy idézet (quote). */
data class Storyline(
    /** "Mérkőzés" vagy a csapat neve. */
    val section: String,
    val isQuote: Boolean = false,
    val text: String,
    val speaker: String? = null,
    val context: String? = null
)

data class PredictionInfo(
    val market: String? = null,
    val selection: String? = null,
    val pick: String,
    val odds: Double? = null,
    val reasoning: String? = null
)

data class WeatherInfo(
    val place: String? = null,
    val localTime: String? = null,
    val temperature: String? = null,
    val condition: String? = null,
    val feelsLike: String? = null,
    val wind: String? = null,
    val rainChance: String? = null,
    val humidity: String? = null,
    val cloudCover: String? = null,
    val uvIndex: String? = null
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
    val points: Int = 0,
    /** Utolsó meccsek betűkkel, pl. "LWWWWW" (W = győzelem, D = döntetlen, L = vereség). */
    val form: String? = null,
    /** Pl. "Promotion - Premier League" vagy "Relegation". */
    val description: String? = null
) {
    val goalDifference: Int get() = goalsFor - goalsAgainst
}
