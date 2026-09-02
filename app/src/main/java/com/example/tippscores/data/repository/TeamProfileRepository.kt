package com.example.tippscores.data.repository

import com.example.tippscores.data.model.TeamProfile
import com.example.tippscores.data.model.TeamProfileStatistic
import com.example.tippscores.data.model.TeamRecentMatch
import com.example.tippscores.data.remote.NetworkModule
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.time.LocalDate

class TeamProfileRepository(
    private val highlightlyKeyProvider: () -> String
) {

    suspend fun fetch(teamName: String): TeamProfile? {
        val key = highlightlyKeyProvider().trim()
        if (key.isEmpty() || teamName.isBlank()) return null

        return try {
            val search = NetworkModule.highlightlyApi.searchTeams(
                apiKey = key,
                name = teamName,
                type = "club",
                limit = 10
            )

            val candidates = asArray(search)
            val team = candidates.firstOrNull { sameTeam(firstString(it, "name"), teamName) }
                ?: candidates.firstOrNull()
                ?: return null

            val id = firstString(team, "id") ?: return null
            val profileElement = try {
                NetworkModule.highlightlyApi.getTeamById(key, id)
            } catch (_: Exception) {
                team
            }
            val profile = firstObject(profileElement) ?: team

            val stats = try {
                NetworkModule.highlightlyApi.getTeamStatistics(
                    apiKey = key,
                    teamId = id,
                    fromDate = LocalDate.now().minusMonths(12).toString(),
                    timezone = "Europe/Budapest"
                )
            } catch (_: Exception) {
                null
            }

            val lastFive = try {
                NetworkModule.highlightlyApi.getLastFiveGames(key, id)
            } catch (_: Exception) {
                null
            }

            TeamProfile(
                id = id,
                name = firstString(profile, "name", "displayName") ?: teamName,
                logoUrl = firstString(profile, "logo", "logoUrl", "logo_url")
                    ?: firstString(team, "logo", "logoUrl", "logo_url")
                    ?: "",
                type = firstString(profile, "type"),
                country = countryName(profile),
                league = firstString(profile, "league", "leagueName"),
                statistics = parseStatistics(stats),
                recentMatches = parseRecentMatches(lastFive, teamName)
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun parseStatistics(element: JsonElement?): List<TeamProfileStatistic> {
        if (element == null || element.isJsonNull) return emptyList()
        val objects = collectObjects(element)
        val result = mutableListOf<TeamProfileStatistic>()
        val skip = setOf("id", "team", "league", "season", "fromDate", "toDate", "updated")

        for (obj in objects) {
            for ((key, value) in obj.entrySet()) {
                if (key in skip) continue
                if (value.isJsonPrimitive && !value.asJsonPrimitive.isBoolean) {
                    val text = try { value.asString } catch (_: Exception) { continue }
                    if (text.isNotBlank()) {
                        result += TeamProfileStatistic(prettyLabel(key), text)
                    }
                }
            }
        }

        return result
            .distinctBy { "${it.label}|${it.value}" }
            .take(16)
    }

    private fun parseRecentMatches(element: JsonElement?, teamName: String): List<TeamRecentMatch> {
        if (element == null || element.isJsonNull) return emptyList()
        return asArray(element).mapNotNull { obj ->
            val home = firstString(obj, "homeTeam.name", "homeTeam.displayName", "homeTeam")
                ?: nestedName(obj, "homeTeam")
                ?: return@mapNotNull null
            val away = firstString(obj, "awayTeam.name", "awayTeam.displayName", "awayTeam")
                ?: nestedName(obj, "awayTeam")
                ?: return@mapNotNull null

            val homeScore = scoreValue(obj, "homeTeam")
            val awayScore = scoreValue(obj, "awayTeam")
            val isHome = sameTeam(home, teamName)
            val isAway = sameTeam(away, teamName)
            if (!isHome && !isAway) return@mapNotNull null

            val hs = homeScore ?: "-"
            val ascore = awayScore ?: "-"
            val result = when {
                hs.toIntOrNull() == null || ascore.toIntOrNull() == null -> "-"
                hs.toInt() == ascore.toInt() -> "D"
                (isHome && hs.toInt() > ascore.toInt()) || (isAway && ascore.toInt() > hs.toInt()) -> "GY"
                else -> "V"
            }

            TeamRecentMatch(
                date = firstString(obj, "date", "startDate", "startTime") ?: "",
                opponent = if (isHome) away else home,
                opponentLogoUrl = if (isHome) nestedString(obj, "awayTeam", "logo") else nestedString(obj, "homeTeam", "logo"),
                homeTeam = home,
                awayTeam = away,
                homeScore = hs,
                awayScore = ascore,
                result = result
            )
        }.take(5)
    }

    private fun scoreValue(obj: JsonObject, teamKey: String): String? {
        val team = obj.getAsJsonObject(teamKey)
        val direct = team?.let { firstString(it, "score", "goals", "points") }
        if (direct != null) return direct
        return firstString(obj, "$teamKey.score", "$teamKey.goals")
    }

    private fun countryName(obj: JsonObject): String? {
        val country = obj.getAsJsonObject("country")
        return country?.let { firstString(it, "name", "code") }
            ?: firstString(obj, "countryName", "country")
    }

    private fun nestedName(obj: JsonObject, key: String): String? =
        obj.getAsJsonObject(key)?.let { firstString(it, "name", "displayName") }

    private fun nestedString(obj: JsonObject, parent: String, key: String): String {
        return obj.getAsJsonObject(parent)?.let { firstString(it, key) } ?: ""
    }

    private fun firstObject(element: JsonElement?): JsonObject? {
        if (element == null || element.isJsonNull) return null
        return when {
            element.isJsonObject -> element.asJsonObject
            element.isJsonArray -> element.asJsonArray.firstOrNull { it.isJsonObject }?.asJsonObject
            else -> null
        }
    }

    private fun asArray(element: JsonElement): List<JsonObject> {
        if (element.isJsonArray) return element.asJsonArray.mapNotNull { it.takeIf { e -> e.isJsonObject }?.asJsonObject }
        if (element.isJsonObject) {
            val obj = element.asJsonObject
            for (key in listOf("data", "teams", "matches", "games", "results")) {
                val child = obj.get(key)
                if (child != null && child.isJsonArray) {
                    return child.asJsonArray.mapNotNull { it.takeIf { e -> e.isJsonObject }?.asJsonObject }
                }
            }
            return listOf(obj)
        }
        return emptyList()
    }

    private fun collectObjects(element: JsonElement): List<JsonObject> {
        val out = mutableListOf<JsonObject>()
        fun visit(e: JsonElement) {
            when {
                e.isJsonObject -> {
                    val o = e.asJsonObject
                    out += o
                    o.entrySet().forEach { visit(it.value) }
                }
                e.isJsonArray -> e.asJsonArray.forEach { visit(it) }
            }
        }
        visit(element)
        return out
    }

    private fun firstString(obj: JsonObject, vararg keys: String): String? {
        for (key in keys) {
            val direct = obj.get(key)
            if (direct != null && direct.isJsonPrimitive) {
                val value = try { direct.asString } catch (_: Exception) { null }
                if (!value.isNullOrBlank()) return value
            }
            if (key.contains('.')) {
                val parts = key.split('.')
                var current: JsonElement? = obj
                for (part in parts) {
                    current = current?.takeIf { it.isJsonObject }?.asJsonObject?.get(part)
                }
                if (current?.isJsonPrimitive == true) {
                    val value = try { current.asString } catch (_: Exception) { null }
                    if (!value.isNullOrBlank()) return value
                }
            }
        }
        return null
    }

    private fun sameTeam(a: String?, b: String): Boolean =
        normalize(a.orEmpty()) == normalize(b)

    private fun normalize(value: String): String = value
        .lowercase()
        .replace("á", "a").replace("é", "e").replace("í", "i")
        .replace("ó", "o").replace("ö", "o").replace("ő", "o")
        .replace("ú", "u").replace("ü", "u").replace("ű", "u")
        .replace("-", "").replace("_", "").replace(" ", "")

    private fun prettyLabel(value: String): String = value
        .replace(Regex("([a-z])([A-Z])"), "$1 $2")
        .replace('_', ' ')
        .replaceFirstChar { it.uppercase() }
}
