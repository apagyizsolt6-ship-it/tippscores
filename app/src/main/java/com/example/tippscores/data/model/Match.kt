package com.example.tippscores.data.model

data class Match(
    val id: String,
    val leagueName: String,
    val leagueCountry: String,
    val leagueCountryFlag: String = "",

    // -1 = nem alapértelmezett kiemelt bajnokság, 0-4 = az 5 top
    // bajnokság sorindexe.
    val presetOrder: Int = -1,

    val homeTeam: String,
    val homeTeamLogo: String,
    val awayTeam: String,
    val awayTeamLogo: String,
    val homeScore: Int?,
    val awayScore: Int?,

    // Igaz, ha épp most (az előző frissítéshez képest) született gól
    // az adott csapatnál.
    val homeJustScored: Boolean = false,
    val awayJustScored: Boolean = false,

    val status: String, // pl. "LIVE", "72'", "18:30"
    val isLive: Boolean,

    // Statpal API adatok
    val tipPrediction: String?, // pl. "TÚL 2.5 GÓL"
    val tipConfidence: Int?,    // pl. 85

    // Highlightly API adatok
    val hasVideoHighlight: Boolean = false,
    val videoUrl: String? = null,

    // Csak a UI rétegben számolt mezők (SharedPreferences alapján).
    // Nem kerülnek be a Room adatbázisba.
    val isFavorite: Boolean = false,

    // Követett hazai és vendég csapat állapota.
    // Szintén csak a UI rétegben számolt mezők.
    val isHomeTeamFollowed: Boolean = false,
    val isAwayTeamFollowed: Boolean = false
)
