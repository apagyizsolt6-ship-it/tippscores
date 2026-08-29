package com.example.tippscores.data.remote

import retrofit2.http.GET
import retrofit2.http.Header

data class HighlightlyDto(
    val match_id: String,
    val video_url: String?
)

interface HighlightlyApiService {
    @GET("football-highlights")
    suspend fun getHighlights(
        @Header("X-RapidAPI-Key") apiKey: String
    ): List<HighlightlyDto>
}
