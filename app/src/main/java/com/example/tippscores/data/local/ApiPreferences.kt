package com.example.tippscores.data.local

import android.content.Context
import android.content.SharedPreferences

class ApiPreferences(context: Context) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("api_settings", Context.MODE_PRIVATE)

    var statpalApiKey: String
        get() = prefs.getString("statpal_key", "") ?: ""
        set(value) = prefs.edit().putString("statpal_key", value).apply()

    var highlightlyApiKey: String
        get() = prefs.getString("highlightly_key", "") ?: ""
        set(value) = prefs.edit().putString("highlightly_key", value).apply()
}
