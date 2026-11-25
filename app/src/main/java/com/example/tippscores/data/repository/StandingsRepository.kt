package com.example.tippscores.data.repository

import com.example.tippscores.data.model.StandingRow
import com.example.tippscores.data.remote.NetworkModule
import com.example.tippscores.data.util.JsonX
import com.example.tippscores.data.util.TtlCache
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException

/**
 * Bajnoki tabella a StatPal "Get Standings" végpontjából:
 * v2/soccer/leagues/{league_id}/standings
 *
 * A válasz pontos mezőnevei bajnokságonként eltérhetnek, ezért a
 * mezőket több lehetséges néven keressük.
 */
class StandingsRepository(
    private val statpalKeyProvider: () -> String
) {

    private val cache = TtlCache<List<StandingRow>>(maxEntries = 30)

    suspend fun fetchStandings(leagueId: String?): List<StandingRow> {
        val id = leagueId?.trim().orEmpty()
        val key = statpalKeyProvider().trim()
        if (id.isEmpty() || key.isEmpty()) return emptyList()

        cache.get(id, CACHE_MS)?.let { return it }

        return try {
            val root = NetworkModule.statpalMatchDetailsApi.getDetails(
                url = "https://statpal.io/api/v2/soccer/leagues/$id/standings",
                accessKey = key
            )
            val rows = parse(root)
            if (rows.isNotEmpty()) cache.put(id, rows)
            rows
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parse(root: JsonObject): List<StandingRow> {
        val rows = mutableListOf<StandingRow>()

        JsonX.walkObjects(root) { o ->
            val points = JsonX.toInt(
                JsonX.deepText(o, 1, "points", "pts")
            ) ?: return@walkObjects

            val teamObject = JsonX.obj(o.get("team"))
            val name = JsonX.text(o, "team_name", "club", "team")
                ?: JsonX.text(teamObject, "name", "team_name")
                ?: JsonX.text(o, "name")
                ?: return@walkObjects

            fun number(vararg keys: String): Int =
                JsonX.toInt(JsonX.deepText(o, 2, *keys)) ?: 0

            rows += StandingRow(
                position = JsonX.toInt(JsonX.deepText(o, 1, "position", "pos", "rank", "place")) ?: 0,
                team = name,
                teamId = JsonX.text(o, "team_id") ?: JsonX.text(teamObject, "id"),
                played = number("played", "games_played", "gp", "overall_gp", "mp", "matches"),
                won = number("won", "wins", "win", "w", "overall_w"),
                drawn = number("draw", "draws", "drawn", "d", "overall_d"),
                lost = number("lost", "losses", "loss", "l", "overall_l"),
                goalsFor = number("goals_for", "gf", "scored", "goals_scored", "overall_gs", "gs"),
                goalsAgainst = number(
                    "goals_allowed", "goals_against", "ga", "conceded",
                    "goals_conceded", "overall_ga"
                ),
                points = points,
                form = JsonX.text(o, "recent_form", "form"),
                description = JsonX.text(JsonX.obj(o.get("description")), "value")
            )
        }

        val unique = rows.distinctBy { it.team }
        val hasPositions = unique.isNotEmpty() && unique.all { it.position > 0 }

        return if (hasPositions) {
            unique.sortedBy { it.position }
        } else {
            unique.sortedWith(
                compareByDescending<StandingRow> { it.points }
                    .thenByDescending { it.goalDifference }
                    .thenByDescending { it.goalsFor }
            ).mapIndexed { index, row -> row.copy(position = index + 1) }
        }
    }

    private companion object {
        const val CACHE_MS = 10 * 60 * 1000L
    }
}
