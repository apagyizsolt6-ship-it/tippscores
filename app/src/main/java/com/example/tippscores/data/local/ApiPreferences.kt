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

    // ========================================================
    // KEDVENC MECCSEK (a csillag a mérkőzéssoron)
    //
    // Ez a lista NEM a Room "matches" táblájában van (az minden
    // frissítéskor törlődik), hanem itt, a SharedPreferences-ben -
    // így a kedvenc jelölés túléli a nap-/frissítés-váltásokat.
    // ========================================================

    var favoriteMatchIds: Set<String>
        get() = prefs.getStringSet("favorite_matches", emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet("favorite_matches", value).apply()

    /** Be- vagy kikapcsolja egy meccs kedvenc állapotát, és visszaadja az új listát. */
    fun toggleFavoriteMatch(matchId: String): Set<String> {
        val updated = favoriteMatchIds.toMutableSet()
        if (!updated.add(matchId)) {
            updated.remove(matchId)
        }
        favoriteMatchIds = updated
        return updated
    }
}
