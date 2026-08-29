package com.example.tippscores.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

// Statpal API válaszmodell
data class StatpalMatchDto(
    val id: String,
    val league_name: String?,
    val country_name: String?,
    val home_team: String?,
    val home_team_logo: String?,
    val away_team: String?,
    val away_team_logo: String?,
    val home_score: Int?,
    val away_score: Int?,
    val status: String?, // "LIVE", "FT", "18:30"
    val is_live: Boolean = false,
    val prediction: String?, // pl. "TÚL 2.5"
    val confidence: Int?     // pl. 88
)

interface StatpalApiService {
    @GET("soccer/matches")
    suspend fun getMatches(
        @Query("api_key") apiKey: String
    ): List<StatpalMatchDto>
}
