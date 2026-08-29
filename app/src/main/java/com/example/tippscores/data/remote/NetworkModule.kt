package com.example.tippscores.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {
    
    // Statpal API Base URL
    private const val STATPAL_BASE_URL = "https://statpal.io/docs/#/" // API endpoint URL

    // Highlightly API Base URL
    private const val HIGHLIGHTLY_BASE_URL = "https://highlightly.net/football-api/" // API endpoint URL

    val statpalApi: StatpalApiService by lazy {
        Retrofit.Builder()
            .baseUrl(STATPAL_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(StatpalApiService::class.java)
    }

    val highlightlyApi: HighlightlyApiService by lazy {
        Retrofit.Builder()
            .baseUrl(HIGHLIGHTLY_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HighlightlyApiService::class.java)
    }
}
