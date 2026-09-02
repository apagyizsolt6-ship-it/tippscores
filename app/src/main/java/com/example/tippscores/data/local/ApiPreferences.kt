package com.example.tippscores.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ApiPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("api_settings", Context.MODE_PRIVATE)

    private val gson = Gson()

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

    // ========================================================
    // KEDVENC CSAPATOK
    //
    // A csapat NEVE a kulcs (nem az API id-je, mert az gyakran
    // üres alacsonyabb szintű bajnokságoknál). Ha egy követett
    // csapat játszik - bármelyik napon, bármelyik meccsén -,
    // az a "Kedvencek" fülön megjelenik.
    // ========================================================

    var favoriteTeamNames: Set<String>
        get() = prefs.getStringSet("favorite_teams", emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet("favorite_teams", value).apply()

    fun toggleFavoriteTeam(teamName: String): Set<String> {
        val updated = favoriteTeamNames.toMutableSet()
        if (!updated.add(teamName)) {
            updated.remove(teamName)
        }
        favoriteTeamNames = updated
        return updated
    }

    // ========================================================
    // KIEMELT BAJNOKSÁGOK
    //
    // Az 5 alapértelmezett (top) bajnokság kiemelése kódban van
    // eldöntve (lásd FeaturedLeagues.kt), NEM itt van tárolva - ide
    // csak azt mentjük, hogy a felhasználó min változtatott ehhez
    // képest: mit vett fel pluszban ("additions"), és mit vett le
    // az 5 alapértelmezett közül ("removals"). Így bármelyik
    // bajnokság kiemelhető és a kiemelés visszavonható is,
    // az 5 alapértelmezettet is beleértve.
    // ========================================================

    var featuredLeagueAdditions: Set<String>
        get() = prefs.getStringSet("featured_additions", emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet("featured_additions", value).apply()

    var featuredLeagueRemovals: Set<String>
        get() = prefs.getStringSet("featured_removals", emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet("featured_removals", value).apply()

    fun toggleFeaturedLeague(leagueKey: String, isPreset: Boolean, currentlyFeatured: Boolean) {

        if (currentlyFeatured) {
            if (isPreset) {
                featuredLeagueRemovals = featuredLeagueRemovals + leagueKey
            } else {
                featuredLeagueAdditions = featuredLeagueAdditions - leagueKey
            }
        } else {
            if (isPreset) {
                featuredLeagueRemovals = featuredLeagueRemovals - leagueKey
            } else {
                featuredLeagueAdditions = featuredLeagueAdditions + leagueKey
            }
        }
    }

    // ========================================================
    // SÖTÉT MÓD
    // ========================================================

    var darkModeEnabled: Boolean
        get() = prefs.getBoolean("dark_mode", false)
        set(value) = prefs.edit().putBoolean("dark_mode", value).apply()

    // ========================================================
    // PUSH ÉRTESÍTÉS GÓLNÁL
    // ========================================================

    var goalNotificationsEnabled: Boolean
        get() = prefs.getBoolean("goal_notifications", false)
        set(value) = prefs.edit().putBoolean("goal_notifications", value).apply()

    // Az utoljára ismert állás meccsenként ("matchId" -> "hazaiGól-vendégGól"),
    // hogy a háttérellenőrzés fel tudja ismerni, mikor NŐTT egy gólszám
    // (vagyis mikor kell értesítést küldeni). Csak a háttérfolyamat
    // (GoalCheckWorker) használja, nem a UI.
    var lastKnownScores: Map<String, String>
        get() {
            val raw = prefs.getString("last_known_scores", null) ?: return emptyMap()
            return try {
                val type = object : TypeToken<Map<String, String>>() {}.type
                gson.fromJson(raw, type) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }
        }
        set(value) = prefs.edit().putString("last_known_scores", gson.toJson(value)).apply()
}
