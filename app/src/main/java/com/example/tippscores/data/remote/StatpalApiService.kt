package com.example.tippscores.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface StatpalApiService {
    @GET("soccer/livescores")
    suspend fun getLiveScores(
        @Query("api_key") apiKey: String
    ): List<Any> // Cseréld ki a pontos Statpal válaszmodellre
}
