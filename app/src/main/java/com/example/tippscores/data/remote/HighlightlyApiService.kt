package com.example.tippscores.data.remote

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface HighlightlyApiService {
    @GET("football-highlights")
    suspend fun getHighlights(
        @Header("X-RapidAPI-Key") apiKey: String,
        @Query("match_id") matchId: String
    ): List<Any> // Cseréld ki a pontos Highlightly válaszmodellre
}
