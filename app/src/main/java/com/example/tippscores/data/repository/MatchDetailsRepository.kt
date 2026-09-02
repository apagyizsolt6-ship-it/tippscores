package com.example.tippscores.data.repository

import com.example.tippscores.data.model.LineupPlayer
import com.example.tippscores.data.model.MatchDetails
import com.example.tippscores.data.model.MatchEvent
import com.example.tippscores.data.model.MatchLineup
import com.example.tippscores.data.model.MatchStatistic
import com.example.tippscores.data.remote.NetworkModule
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import retrofit2.HttpException

class MatchDetailsRepository(
    private val accessKeyProvider: () -> String
) {

    suspend fun fetch(matchId: String, homeTeam: String, awayTeam: String): MatchDetails =
        coroutineScope {
            val stats = async { fetchFirstAvailable(matchId, "live-match-stats", "stats") }
            val events = async { fetchFirstAvailable(matchId, "live-plays", "plays") }
            val rosters = async { fetchFirstAvailable(matchId, "rosters", "lineups") }

            MatchDetailsParser.parse(
                statisticsJson = stats.await(),
                eventsJson = events.await(),
                rostersJson = rosters.await(),
                homeTeam = homeTeam,
                awayTeam = awayTeam
            )
        }

    private suspend fun fetchFirstAvailable(
        matchId: String,
        vararg resources: String
    ): JsonObject? {
        for (resource in resources) {
            try {
                return NetworkModule.statpalMatchDetailsApi.getDetails(
                    url = "https://statpal.io/api/v2/soccer/matches/$matchId/$resource",
                    accessKey = accessKeyProvider().trim()
                )
            } catch (_: HttpException) {
                // Ha az első dokumentált név nem érhető el ezen a csomagon,
                // megpróbáljuk a kompatibilis második útvonalat.
            } catch (_: Exception) {
                // Részletek hiányánál a többi blokk ettől még megjelenhet.
            }
        }
        return null
    }


}

private object MatchDetailsParser {

    fun parse(
        statisticsJson: JsonObject?,
        eventsJson: JsonObject?,
        rostersJson: JsonObject?,
        homeTeam: String,
        awayTeam: String
    ): MatchDetails {
        return MatchDetails(
            statistics = parseStatistics(statisticsJson),
            events = parseEvents(eventsJson, homeTeam, awayTeam),
            homeLineup = parseLineup(rostersJson, homeTeam, preferHome = true),
            awayLineup = parseLineup(rostersJson, awayTeam, preferHome = false)
        )
    }

    private fun parseStatistics(root: JsonObject?): List<MatchStatistic> {
        if (root == null) return emptyList()

        val result = mutableListOf<MatchStatistic>()
        val candidates = findArrays(root, setOf("statistics", "stats", "match_statistics"))

        for (array in candidates) {
            for (item in array) {
                if (!item.isJsonObject) continue
                val obj = item.asJsonObject

                val label = firstString(obj, "name", "label", "type", "statistic", "title")
                    ?: continue

                val home = valueString(
                    obj,
                    "home", "home_value", "homeValue", "local", "value_home"
                ) ?: nestedValue(obj, "home")

                val away = valueString(
                    obj,
                    "away", "away_value", "awayValue", "visitor", "value_away"
                ) ?: nestedValue(obj, "away")

                if (home != null || away != null) {
                    result += MatchStatistic(
                        label = translateStatLabel(label),
                        home = home ?: "–",
                        away = away ?: "–"
                    )
                }
            }
        }

        // Néhány API-válaszban a statisztika nem tömb, hanem kulcs-érték
        // objektum. Ezt is kezeljük.
        if (result.isEmpty()) {
            val objects = findObjects(root, setOf("statistics", "stats", "match_statistics"))
            for (obj in objects) {
                for ((key, value) in obj.entrySet()) {
                    if (!value.isJsonPrimitive) continue
                    result += MatchStatistic(
                        label = translateStatLabel(key),
                        home = valueString(value) ?: "–",
                        away = "–"
                    )
                }
            }
        }

        return result.distinctBy { it.label }
    }

    private fun parseEvents(
        root: JsonObject?,
        homeTeam: String,
        awayTeam: String
    ): List<MatchEvent> {
        if (root == null) return emptyList()

        val result = mutableListOf<MatchEvent>()
        val arrays = findArrays(
            root,
            setOf("events", "plays", "live_plays", "incidents", "timeline")
        )

        for (array in arrays) {
            for (item in array) {
                if (!item.isJsonObject) continue
                val obj = item.asJsonObject

                val player = firstString(
                    obj, "player", "player_name", "playerName", "name", "athlete"
                ) ?: "Ismeretlen játékos"

                val minute = firstString(
                    obj, "minute", "min", "time", "elapsed", "clock"
                ) ?: "–"

                val rawType = firstString(
                    obj, "type", "event_type", "eventType", "kind", "action"
                ) ?: "Esemény"

                val detail = firstString(
                    obj, "detail", "description", "comments", "result"
                )

                val assist = firstString(
                    obj, "assist", "assist_name", "assistName", "secondary_player"
                )

                val team = firstString(
                    obj, "team", "team_name", "teamName", "side"
                ) ?: inferTeam(obj, homeTeam, awayTeam)

                result += MatchEvent(
                    minute = normalizeMinute(minute),
                    team = team,
                    player = player,
                    assist = assist,
                    type = translateEventType(rawType),
                    detail = detail
                )
            }
        }

        return result.sortedWith(
            compareBy<MatchEvent> { minuteSortKey(it.minute) }
        )
    }

    private fun parseLineup(
        root: JsonObject?,
        teamName: String,
        preferHome: Boolean
    ): MatchLineup {
        if (root == null) return MatchLineup()

        val teamObject = findTeamObject(root, teamName, preferHome)
        if (teamObject != null) {
            return parseLineupObject(teamObject)
        }

        // Ha nincs külön home/away objektum, a teljes válaszban keressük
        // a kezdő és csere játékoslistákat.
        return parseLineupObject(root)
    }

    private fun findTeamObject(
        root: JsonObject,
        teamName: String,
        preferHome: Boolean
    ): JsonObject? {
        val normalizedTeam = normalize(teamName)

        fun visit(element: JsonElement, key: String? = null): JsonObject? {
            if (element.isJsonObject) {
                val obj = element.asJsonObject
                val objectName = firstString(obj, "team", "team_name", "teamName", "name")
                if (objectName != null &&
                    (normalize(objectName) == normalizedTeam ||
                        normalizedTeam.contains(normalize(objectName)) ||
                        normalize(objectName).contains(normalizedTeam))
                ) {
                    return obj
                }

                for ((childKey, child) in obj.entrySet()) {
                    if (normalize(childKey).contains(if (preferHome) "home" else "away")) {
                        if (child.isJsonObject) return child.asJsonObject
                    }
                    visit(child, childKey)?.let { return it }
                }
            } else if (element.isJsonArray) {
                for (child in element.asJsonArray) {
                    visit(child, key)?.let { return it }
                }
            }
            return null
        }

        return visit(root)
    }

    private fun parseLineupObject(root: JsonObject): MatchLineup {
        val formation = firstString(root, "formation", "system", "shape")
        val coach = firstString(root, "coach", "manager", "coach_name", "manager_name")

        val starters = mutableListOf<LineupPlayer>()
        val substitutes = mutableListOf<LineupPlayer>()

        for (array in findArrays(
            root,
            setOf("startxi", "startingxi", "starting_xi", "starters", "starting_players", "starting")
        )) {
            array.forEach { element ->
                parsePlayer(element)?.let { starters += it }
            }
        }

        for (array in findArrays(
            root,
            setOf("substitutes", "subs", "bench", "sub_players")
        )) {
            array.forEach { element ->
                parsePlayer(element)?.let { substitutes += it }
            }
        }

        // Egyes sémák egyetlen "players" listát küldenek.
        if (starters.isEmpty() && substitutes.isEmpty()) {
            for (array in findArrays(root, setOf("players", "lineup_players", "roster"))) {
                array.forEach { element ->
                    parsePlayer(element)?.let { player ->
                        if (isStarter(element)) starters += player else substitutes += player
                    }
                }
            }
        }

        return MatchLineup(
            formation = formation,
            coach = coach,
            startingPlayers = starters.distinctBy { it.name },
            substitutePlayers = substitutes.distinctBy { it.name }
        )
    }

    private fun parsePlayer(element: JsonElement): LineupPlayer? {
        if (!element.isJsonObject) return null
        val obj = element.asJsonObject

        val name = firstString(
            obj, "name", "player", "player_name", "playerName", "full_name", "fullname"
        ) ?: return null

        return LineupPlayer(
            number = firstString(obj, "number", "shirt_number", "shirtNumber", "jersey"),
            name = name,
            position = firstString(obj, "position", "pos", "role"),
            photoUrl = firstString(
                obj, "photo", "photo_url", "photoUrl", "image", "image_url", "avatar"
            ),
            isCaptain = firstBoolean(obj, "captain", "is_captain", "isCaptain")
        )
    }

    private fun isStarter(element: JsonElement): Boolean {
        if (!element.isJsonObject) return false
        return firstBoolean(
            element.asJsonObject,
            "starter", "starting", "is_starter", "isStarter"
        )
    }

    private fun findArrays(root: JsonObject, wanted: Set<String>): List<JsonArray> {
        val result = mutableListOf<JsonArray>()
        fun visit(element: JsonElement) {
            if (element.isJsonObject) {
                for ((key, child) in element.asJsonObject.entrySet()) {
                    if (wanted.contains(normalize(key)) && child.isJsonArray) {
                        result += child.asJsonArray
                    }
                    visit(child)
                }
            } else if (element.isJsonArray) {
                element.forEach(::visit)
            }
        }
        visit(root)
        return result
    }

    private fun findObjects(root: JsonObject, wanted: Set<String>): List<JsonObject> {
        val result = mutableListOf<JsonObject>()
        fun visit(element: JsonElement) {
            if (element.isJsonObject) {
                for ((key, child) in element.asJsonObject.entrySet()) {
                    if (wanted.contains(normalize(key)) && child.isJsonObject) {
                        result += child.asJsonObject
                    }
                    visit(child)
                }
            } else if (element.isJsonArray) {
                element.forEach(::visit)
            }
        }
        visit(root)
        return result
    }

    private fun firstString(obj: JsonObject, vararg keys: String): String? {
        for (key in keys) {
            val value = obj.entrySet().firstOrNull {
                normalize(it.key) == normalize(key)
            }?.value
            valueString(value)?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return null
    }

    private fun firstBoolean(obj: JsonObject, vararg keys: String): Boolean {
        for (key in keys) {
            val value = obj.entrySet().firstOrNull {
                normalize(it.key) == normalize(key)
            }?.value ?: continue
            if (value.isJsonPrimitive) {
                val primitive = value.asJsonPrimitive
                if (primitive.isBoolean) return primitive.asBoolean
                primitive.asString.toBooleanStrictOrNull()?.let { return it }
            }
        }
        return false
    }

    private fun valueString(obj: JsonObject, vararg keys: String): String? =
        firstString(obj, *keys)

    private fun valueString(value: JsonElement?): String? {
        if (value == null || value.isJsonNull) return null
        return try {
            if (value.isJsonPrimitive) value.asString else null
        } catch (_: Exception) {
            null
        }
    }

    private fun nestedValue(obj: JsonObject, key: String): String? {
        val child = obj.entrySet().firstOrNull { normalize(it.key) == normalize(key) }?.value
        return if (child?.isJsonObject == true) {
            firstString(child.asJsonObject, "value", "display", "text", "total")
        } else {
            valueString(child)
        }
    }

    private fun inferTeam(obj: JsonObject, home: String, away: String): String {
        val raw = firstString(obj, "team_id", "side", "participant")
        return when {
            raw?.equals("home", true) == true -> home
            raw?.equals("away", true) == true -> away
            else -> "–"
        }
    }

    private fun normalizeMinute(value: String): String {
        val clean = value.trim()
        if (clean.isEmpty()) return "–"
        if (clean.matches(Regex("\\d{1,3}"))) return "$clean'"
        return clean
    }

    private fun minuteSortKey(value: String): Int {
        return Regex("\\d{1,3}").find(value)?.value?.toIntOrNull() ?: 999
    }

    private fun normalize(value: String): String =
        value.lowercase()
            .replace("_", "")
            .replace("-", "")
            .replace(" ", "")

    private fun translateStatLabel(raw: String): String {
        return when (normalize(raw)) {
            "possession" -> "Labdabirtoklás"
            "shotsontarget" -> "Kaput eltaláló lövések"
            "shots" -> "Lövések"
            "cornerkicks", "corners" -> "Szögletek"
            "fouls" -> "Szabálytalanságok"
            "yellowcards" -> "Sárga lapok"
            "redcards" -> "Piros lapok"
            "offsides" -> "Lesek"
            "passes" -> "Passzok"
            "passesaccuracy", "passaccuracy" -> "Passzpontosság"
            "expectedgoals", "xg" -> "Várható gólok (xG)"
            else -> raw
        }
    }

    private fun translateEventType(raw: String): String {
        return when (normalize(raw)) {
            "goal", "goals", "score" -> "Gól"
            "yellowcard", "yellow" -> "Sárga lap"
            "redcard", "red" -> "Piros lap"
            "substitution", "substitute", "sub" -> "Csere"
            "penalty" -> "Tizenegyes"
            "owngoal" -> "Öngól"
            else -> raw
        }
    }
}
