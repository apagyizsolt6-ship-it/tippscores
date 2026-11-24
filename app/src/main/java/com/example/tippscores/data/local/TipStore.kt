package com.example.tippscores.data.local

import android.content.Context
import com.example.tippscores.data.model.Match
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Saját tippjáték: a tippek helyben (SharedPreferences) tárolódnak, és
 * a mérkőzés végén automatikusan kiértékelődnek.
 *
 * Pontozás:
 *  - pontos eredmény: 5 pont
 *  - jó kimenetel és jó gólkülönbség: 3 pont
 *  - csak a jó kimenetel (győztes vagy döntetlen): 2 pont
 */
data class Tip(
    val matchId: String,
    val homeTeam: String,
    val awayTeam: String,
    val league: String = "",
    val predictedHome: Int,
    val predictedAway: Int,
    val createdAt: Long,
    val finalHome: Int? = null,
    val finalAway: Int? = null,
    val points: Int? = null
) {
    val isSettled: Boolean get() = points != null
}

data class TipStats(
    val total: Int = 0,
    val settled: Int = 0,
    val points: Int = 0,
    val exactHits: Int = 0,
    val outcomeHits: Int = 0,
    val currentStreak: Int = 0
) {
    val hitRatePercent: Int
        get() = if (settled == 0) 0 else (outcomeHits * 100) / settled
}

class TipStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val gson = Gson()

    fun all(): List<Tip> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Tip>>() {}.type
            val list: List<Tip>? = gson.fromJson(raw, type)
            list.orEmpty().sortedByDescending { it.createdAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun get(matchId: String): Tip? = all().firstOrNull { it.matchId == matchId }

    fun save(tip: Tip) {
        val updated = all().filterNot { it.matchId == tip.matchId } + tip
        write(updated)
    }

    fun remove(matchId: String) {
        write(all().filterNot { it.matchId == matchId })
    }

    /**
     * A lejátszott mérkőzések eredményéből kiértékeli a függő tippeket.
     * Igaz, ha legalább egy tipp változott.
     */
    fun settleFrom(matches: List<Match>): Boolean {
        val current = all()
        if (current.none { !it.isSettled }) return false

        var changed = false
        val updated = current.map { tip ->
            if (tip.isSettled) return@map tip

            val match = matches.firstOrNull { it.id == tip.matchId } ?: return@map tip
            val home = match.homeScore
            val away = match.awayScore
            if (!isFinishedStatus(match.status) || home == null || away == null) {
                return@map tip
            }

            changed = true
            tip.copy(
                finalHome = home,
                finalAway = away,
                points = pointsFor(tip.predictedHome, tip.predictedAway, home, away)
            )
        }

        if (changed) write(updated)
        return changed
    }

    fun stats(): TipStats {
        val tips = all()
        val settled = tips.filter { it.isSettled }

        val exact = settled.count {
            it.predictedHome == it.finalHome && it.predictedAway == it.finalAway
        }
        val outcome = settled.count { (it.points ?: 0) > 0 }

        // Folyamatban lévő nyerő sorozat: a legutóbbi kiértékelt tippektől visszafelé.
        var streak = 0
        for (tip in settled) {
            if ((tip.points ?: 0) > 0) streak++ else break
        }

        return TipStats(
            total = tips.size,
            settled = settled.size,
            points = settled.sumOf { it.points ?: 0 },
            exactHits = exact,
            outcomeHits = outcome,
            currentStreak = streak
        )
    }

    private fun write(list: List<Tip>) {
        prefs.edit().putString(KEY, gson.toJson(list)).apply()
    }

    companion object {
        private const val PREFS = "tippscores_tips"
        private const val KEY = "tips"

        fun pointsFor(
            predictedHome: Int,
            predictedAway: Int,
            actualHome: Int,
            actualAway: Int
        ): Int {
            val predictedDiff = predictedHome - predictedAway
            val actualDiff = actualHome - actualAway

            return when {
                predictedHome == actualHome && predictedAway == actualAway -> 5
                predictedDiff.compareTo(0) != actualDiff.compareTo(0) -> 0
                predictedDiff == actualDiff -> 3
                else -> 2
            }
        }

        fun isFinishedStatus(status: String): Boolean =
            status == "Vége" || status == "Hosszabbítás" || status == "Tizenegyesek"

        /** Tippelni csak a kezdés előtt lehet. */
        fun canTip(match: Match): Boolean =
            !match.isLive &&
                !isFinishedStatus(match.status) &&
                match.status != "Elhalasztva" &&
                match.status != "Törölve"
    }
}
