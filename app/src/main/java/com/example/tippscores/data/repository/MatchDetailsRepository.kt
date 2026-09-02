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

/**
 * Mérkőzés részletek.
 *
 * Forrás sorrend:
 *  1. Highlightly – ha van kulcs és a mérkőzés megtalálható.
 *  2. StatPal – visszaesési lehetőség a jelenlegi projekt működésének megőrzéséhez.
 *
 * A Highlightly dokumentáció szerint a /matches/{id} válasz közvetlenül tartalmazhat
 * statistics és events mezőket, ezért a 3. modulnál ezt használjuk elsődleges forrásként.
 */
class MatchDetailsRepository(
    private val statpalKeyProvider: () -> String,
    private val highlightlyKeyProvider: () -> String
) {

    suspend fun fetch(
        matchId: String,
        homeTeam: String,
        awayTeam: String
    ): MatchDetails = coroutineScope {
        val statpalStats = async { fetchStatpal(matchId, "live-match-stats", "stats") }
        val statpalEvents = async { fetchStatpal(matchId, "live-plays", "plays") }
        val statpalLineups = async { fetchStatpal(matchId, "rosters", "lineups") }

        val highlightly = async {
            fetchHighlightlyMatch(homeTeam, awayTeam)
        }

        val h = highlightly.await()

        val boxScoreElement = h?.id?.let { fetchHighlightlyBoxScore(it) }

        val highlightlyStatistics = h?.details?.let { parseHighlightlyStatistics(it) }
            .orEmpty()
            .ifEmpty {
                h?.details?.let { parseHighlightlyStatisticsFromArray(it) }.orEmpty()
            }
            .ifEmpty {
                h?.statistics?.let { parseHighlightlyStatisticsElement(it) }.orEmpty()
            }

        val highlightlyEvents = h?.events?.let {
            parseHighlightlyEvents(it, homeTeam, awayTeam)
        }.orEmpty().ifEmpty {
            h?.details?.let {
                parseEvents(it, homeTeam, awayTeam)
            }.orEmpty()
        }

        val highlightlyLineups = h?.details?.let {
            Pair(
                enrichLineupPhotos(parseLineupFromMatchObject(it, homeTeam, true), boxScoreElement),
                enrichLineupPhotos(parseLineupFromMatchObject(it, awayTeam, false), boxScoreElement)
            )
        }

        MatchDetails(
            statistics = highlightlyStatistics.ifEmpty {
                MatchDetailsParser.parse(
                    statisticsJson = statpalStats.await(),
                    eventsJson = null,
                    rostersJson = null,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam
                ).statistics
            },
            events = highlightlyEvents.ifEmpty {
                MatchDetailsParser.parse(
                    statisticsJson = null,
                    eventsJson = statpalEvents.await(),
                    rostersJson = null,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam
                ).events
            },
            homeLineup = highlightlyLineups?.first?.takeIf { it.hasPlayers() }
                ?: MatchDetailsParser.parse(
                    statisticsJson = null,
                    eventsJson = null,
                    rostersJson = statpalLineups.await(),
                    homeTeam = homeTeam,
                    awayTeam = awayTeam
                ).homeLineup,
            awayLineup = highlightlyLineups?.second?.takeIf { it.hasPlayers() }
                ?: MatchDetailsParser.parse(
                    statisticsJson = null,
                    eventsJson = null,
                    rostersJson = statpalLineups.await(),
                    homeTeam = homeTeam,
                    awayTeam = awayTeam
                ).awayLineup
        )
    }

    private data class HighlightlyMatchDetails(
        val id: String,
        val details: JsonObject?,
        val statistics: JsonElement?,
        val events: JsonElement?
    )

    private suspend fun fetchHighlightlyMatch(
        homeTeam: String,
        awayTeam: String
    ): HighlightlyMatchDetails? {
        val key = highlightlyKeyProvider().trim()
        if (key.isEmpty()) return null

        return try {
            val response = NetworkModule.highlightlyApi.findMatches(
                apiKey = key,
                homeTeamName = homeTeam,
                awayTeamName = awayTeam,
                limit = 10
            )

            val candidate = response.data.orEmpty().firstOrNull { match ->
                normalize(match.homeTeam?.name.orEmpty()) == normalize(homeTeam) &&
                    normalize(match.awayTeam?.name.orEmpty()) == normalize(awayTeam)
            } ?: response.data.orEmpty().firstOrNull()

            val id = candidate?.id ?: return null

            val element = try {
                NetworkModule.highlightlyApi.getMatchById(key, id)
            } catch (_: Exception) {
                null
            }

            val details = when {
                element?.isJsonArray == true ->
                    element.asJsonArray.firstOrNull()?.takeIf { it.isJsonObject }?.asJsonObject
                element?.isJsonObject == true -> element.asJsonObject
                else -> null
            }

            val statistics = try {
                NetworkModule.highlightlyApi.getMatchStatistics(key, id)
            } catch (_: Exception) {
                details?.get("statistics")
            }

            val events = try {
                NetworkModule.highlightlyApi.getMatchEvents(key, id)
            } catch (_: Exception) {
                details?.get("events")
            }

            HighlightlyMatchDetails(
                id = id,
                details = details,
                statistics = statistics,
                events = events
            )
        } catch (_: HttpException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun fetchHighlightlyBoxScore(matchId: String): JsonElement? {
        val key = highlightlyKeyProvider().trim()
        if (key.isEmpty()) return null
        return try { NetworkModule.highlightlyApi.getBoxScore(key, matchId) } catch (_: Exception) { null }
    }

    private fun enrichLineupPhotos(lineup: MatchLineup, boxScore: JsonElement?): MatchLineup {
        if (!lineup.hasPlayers() || boxScore == null) return lineup
        val photos = mutableMapOf<String, String>()
        collectPlayerPhotos(boxScore, photos)
        if (photos.isEmpty()) return lineup
        fun enrich(list: List<LineupPlayer>) = list.map { p -> p.copy(photoUrl = p.photoUrl?.takeIf { it.isNotBlank() } ?: photos[normalize(p.name)]) }
        return lineup.copy(startingPlayers = enrich(lineup.startingPlayers), substitutePlayers = enrich(lineup.substitutePlayers))
    }

    private fun collectPlayerPhotos(element: JsonElement?, out: MutableMap<String, String>) {
        if (element == null || element.isJsonNull) return
        if (element.isJsonArray) { element.asJsonArray.forEach { collectPlayerPhotos(it, out) }; return }
        if (!element.isJsonObject) return
        val obj = element.asJsonObject
        val player = obj.get("player")?.takeIf { it.isJsonObject }?.asJsonObject
        val name = firstString(obj, "name", "player_name", "playerName", "full_name") ?: player?.let { firstString(it, "name", "full_name", "fullName") }
        val photo = firstString(obj, "logo", "photo", "photo_url", "photoUrl", "image", "image_url", "avatar") ?: player?.let { firstString(it, "logo", "photo", "photo_url", "photoUrl", "image", "image_url", "avatar") }
        if (!name.isNullOrBlank() && !photo.isNullOrBlank()) out[normalize(name)] = photo
        obj.entrySet().forEach { (_, child) -> collectPlayerPhotos(child, out) }
    }

    private fun parseHighlightlyStatistics(root: JsonObject): List<MatchStatistic> {
        val statistics = root.getAsJsonArray("statistics") ?: return emptyList()
        return parseHighlightlyStatisticsArray(statistics)
    }

    private fun parseHighlightlyStatisticsFromArray(root: JsonObject): List<MatchStatistic> {
        val candidates = findArrays(root, setOf("statistics"))
        return candidates.firstOrNull()?.let { parseHighlightlyStatisticsArray(it) }.orEmpty()
    }

    private fun parseHighlightlyStatisticsElement(element: JsonElement): List<MatchStatistic> {
        return when {
            element.isJsonArray -> parseHighlightlyStatisticsArray(element.asJsonArray)
            element.isJsonObject -> {
                val obj = element.asJsonObject
                val array = obj.getAsJsonArray("data") ?: obj.getAsJsonArray("statistics")
                if (array != null) parseHighlightlyStatisticsArray(array) else emptyList()
            }
            else -> emptyList()
        }
    }

    /** Highlightly séma: [{ team:{name}, statistics:[{value,displayName}] }, ...] */
    private fun parseHighlightlyStatisticsArray(array: JsonArray): List<MatchStatistic> {
        data class TeamStats(val team: String, val values: Map<String, String>)

        val teams = array.mapNotNull { item ->
            if (!item.isJsonObject) return@mapNotNull null
            val obj = item.asJsonObject
            val teamObj = obj.getAsJsonObject("team") ?: return@mapNotNull null
            val team = string(teamObj, "name") ?: return@mapNotNull null
            val stats = obj.getAsJsonArray("statistics") ?: return@mapNotNull null
            val values = stats.mapNotNull { stat ->
                if (!stat.isJsonObject) return@mapNotNull null
                val so = stat.asJsonObject
                val name = string(so, "displayName", "name") ?: return@mapNotNull null
                val value = valueString(so.get("value")) ?: return@mapNotNull null
                name to value
            }.toMap()
            TeamStats(team, values)
        }

        if (teams.isEmpty()) return emptyList()
        val home = teams.first()
        val away = teams.getOrNull(1)

        val labels = linkedSetOf<String>()
        home.values.keys.forEach { labels += it }
        away?.values?.keys?.forEach { labels += it }

        return labels.map { raw ->
            MatchStatistic(
                label = translateStatLabel(raw),
                home = home.values[raw] ?: "–",
                away = away?.values?.get(raw) ?: "–"
            )
        }
    }

    private fun parseHighlightlyEvents(
        element: JsonElement,
        homeTeam: String,
        awayTeam: String
    ): List<MatchEvent> {
        val array = when {
            element.isJsonArray -> element.asJsonArray
            element.isJsonObject -> {
                element.asJsonObject.getAsJsonArray("data")
                    ?: element.asJsonObject.getAsJsonArray("events")
                    ?: JsonArray()
            }
            else -> JsonArray()
        }

        return array.mapNotNull { item ->
            if (!item.isJsonObject) return@mapNotNull null
            val obj = item.asJsonObject

            val teamObject = obj.getAsJsonObject("team")
            val team = teamObject?.let { firstString(it, "name") }
                ?: firstString(obj, "teamName", "team_name", "team", "side")
                ?: inferTeam(obj, homeTeam, awayTeam)

            val rawType = firstString(obj, "type", "eventType", "kind") ?: "Esemény"
            val rawMinute = firstString(obj, "time", "minute", "elapsed", "clock") ?: "–"
            val player = firstString(obj, "player", "playerName", "player_name", "name")
                ?: "Ismeretlen játékos"
            val assist = firstString(obj, "assist", "assistingPlayer", "assistName", "assist_name")

            MatchEvent(
                minute = normalizeMinute(rawMinute),
                team = team,
                player = player,
                assist = assist,
                type = translateEventType(rawType),
                detail = firstString(obj, "detail", "description", "comments")
            )
        }.sortedBy { minuteSortKey(it.minute) }
    }

    private fun parseEvents(
        root: JsonObject,
        homeTeam: String,
        awayTeam: String
    ): List<MatchEvent> {
        val array = root.getAsJsonArray("events") ?: return emptyList()
        return array.mapNotNull { item ->
            if (!item.isJsonObject) return@mapNotNull null
            val obj = item.asJsonObject
            val team = obj.getAsJsonObject("team")?.let { string(it, "name") }
                ?: string(obj, "team", "teamName")
                ?: "–"
            val type = string(obj, "type") ?: "Esemény"
            val player = string(obj, "player") ?: "Ismeretlen játékos"
            val minute = string(obj, "time", "minute") ?: "–"
            MatchEvent(
                minute = normalizeMinute(minute),
                team = if (team == "–") inferTeam(obj, homeTeam, awayTeam) else team,
                player = player,
                assist = string(obj, "assist"),
                type = translateEventType(type),
                detail = string(obj, "detail", "description")
            )
        }.sortedBy { minuteSortKey(it.minute) }
    }

    private fun parseLineupFromMatchObject(
        root: JsonObject,
        teamName: String,
        preferHome: Boolean
    ): MatchLineup {
        // A /matches/{id} endpoint egyes csomagoknál csak általános adatot ad.
        // Ha nincs lineup mező, üresen térünk vissza és a StatPal fallback marad.
        val lineupRoot = root.getAsJsonObject(if (preferHome) "homeTeam" else "awayTeam") ?: return MatchLineup()
        val name = string(lineupRoot, "name")
        if (name != null && !sameTeam(name, teamName)) return MatchLineup()

        val starters = mutableListOf<LineupPlayer>()
        val subs = mutableListOf<LineupPlayer>()

        for (array in findArrays(lineupRoot, setOf("initialLineup", "startxi", "startingxi", "starters", "startingPlayers"))) {
            array.forEach { parsePlayer(it)?.let(starters::add) }
        }
        for (array in findArrays(lineupRoot, setOf("substitutes", "subs", "bench"))) {
            array.forEach { parsePlayer(it)?.let(subs::add) }
        }

        return MatchLineup(
            formation = firstString(lineupRoot, "formation", "system", "shape"),
            coach = firstString(lineupRoot, "coach", "manager", "coach_name", "manager_name"),
            startingPlayers = starters.distinctBy { it.name },
            substitutePlayers = subs.distinctBy { it.name }
        )
    }

    private fun parsePlayer(element: JsonElement): LineupPlayer? {
        if (!element.isJsonObject) return null
        val obj = element.asJsonObject
        val playerObj = obj.getAsJsonObject("player")
        val source = playerObj ?: obj
        val name = firstString(source, "name", "fullName", "full_name", "player", "playerName") ?: return null
        return LineupPlayer(
            number = firstString(obj, "shirtNumber", "number", "shirt_number", "jersey"),
            name = name,
            position = firstString(obj, "position", "pos", "role") ?: firstString(source, "position"),
            photoUrl = firstString(source, "logo", "photo", "photoUrl", "image", "image_url", "avatar"),
            isCaptain = firstBoolean(obj, "isCaptain", "captain", "is_captain")
        )
    }

    private suspend fun fetchStatpal(
        matchId: String,
        resource1: String,
        resource2: String
    ): JsonObject? {
        val key = statpalKeyProvider().trim()
        if (key.isEmpty()) return null
        for (resource in listOf(resource1, resource2)) {
            try {
                return NetworkModule.statpalMatchDetailsApi.getDetails(
                    url = "https://statpal.io/api/v2/soccer/matches/$matchId/$resource",
                    accessKey = key
                )
            } catch (_: HttpException) {
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun MatchLineup.hasPlayers(): Boolean =
        startingPlayers.isNotEmpty() || substitutePlayers.isNotEmpty()

    private fun sameTeam(a: String, b: String): Boolean {
        val x = normalize(a)
        val y = normalize(b)
        return x == y || x.contains(y) || y.contains(x)
    }

    private fun inferTeam(obj: JsonObject, home: String, away: String): String {
        val raw = string(obj, "side", "participant", "team_id")
        return when {
            raw.equals("home", true) -> home
            raw.equals("away", true) -> away
            else -> "–"
        }
    }

    private fun findArrays(root: JsonObject, wanted: Set<String>): List<JsonArray> {
        val result = mutableListOf<JsonArray>()
        fun visit(element: JsonElement) {
            when {
                element.isJsonObject -> {
                    element.asJsonObject.entrySet().forEach { (key, child) ->
                        if (wanted.contains(normalize(key)) && child.isJsonArray) result += child.asJsonArray
                        visit(child)
                    }
                }
                element.isJsonArray -> element.asJsonArray.forEach(::visit)
            }
        }
        visit(root)
        return result
    }

    private fun firstString(obj: JsonObject, vararg keys: String): String? {
        for (key in keys) {
            obj.entrySet().firstOrNull { normalize(it.key) == normalize(key) }
                ?.value?.let { valueString(it) }?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return null
    }

    private fun string(obj: JsonObject, vararg keys: String): String? = firstString(obj, *keys)

    private fun firstBoolean(obj: JsonObject, vararg keys: String): Boolean {
        for (key in keys) {
            val value = obj.entrySet().firstOrNull { normalize(it.key) == normalize(key) }?.value ?: continue
            if (value.isJsonPrimitive) {
                val p = value.asJsonPrimitive
                if (p.isBoolean) return p.asBoolean
                p.asString.toBooleanStrictOrNull()?.let { return it }
            }
        }
        return false
    }

    private fun valueString(value: JsonElement?): String? {
        if (value == null || value.isJsonNull) return null
        return try {
            if (value.isJsonPrimitive) value.asString else null
        } catch (_: Exception) {
            null
        }
    }

    private fun normalize(value: String): String =
        value.lowercase()
            .replace("á", "a").replace("é", "e").replace("í", "i")
            .replace("ó", "o").replace("ö", "o").replace("ő", "o")
            .replace("ú", "u").replace("ü", "u").replace("ű", "u")
            .replace("_", "").replace("-", "").replace(" ", "")

    private fun normalizeMinute(value: String): String {
        val clean = value.trim()
        if (clean.isEmpty()) return "–"
        if (clean.matches(Regex("\\d{1,3}"))) return "$clean'"
        return clean
    }

    private fun minuteSortKey(value: String): Int =
        Regex("\\d{1,3}").find(value)?.value?.toIntOrNull() ?: 999

    private fun translateStatLabel(raw: String): String = when (normalize(raw)) {
        "possession" -> "Labdabirtoklás"
        "shotsontarget" -> "Kaput eltaláló lövések"
        "shots" -> "Lövések"
        "corners", "cornerkicks" -> "Szögletek"
        "fouls" -> "Szabálytalanságok"
        "yellowcards" -> "Sárga lapok"
        "redcards" -> "Piros lapok"
        "offsides" -> "Lesek"
        "passes" -> "Passzok"
        "passesaccuracy", "passaccuracy" -> "Passzpontosság"
        "expectedgoals", "xg" -> "Várható gólok (xG)"
        "shotsaccuracy" -> "Lövési pontosság"
        else -> raw
    }

    private fun translateEventType(raw: String): String = when (normalize(raw)) {
        "goal", "goals", "score" -> "Gól"
        "owngoal" -> "Öngól"
        "yellowcard", "yellow" -> "Sárga lap"
        "redcard", "red" -> "Piros lap"
        "substitution", "substitute", "sub" -> "Csere"
        "penalty" -> "Tizenegyes"
        "missedpenalty" -> "Kihagyott tizenegyes"
        "vargoalconfirmed" -> "VAR – gól megerősítve"
        "vargoalcancelled" -> "VAR – gól törölve"
        "varpenalty" -> "VAR – tizenegyes"
        "varpenaltycancelled" -> "VAR – tizenegyes törölve"
        "vargoalcancelledoffside" -> "VAR – gól törölve (les)"
        else -> raw
    }
}

/** A meglévő StatPal parser marad fallbackként. */
private object MatchDetailsParser {

    fun parse(
        statisticsJson: JsonObject?,
        eventsJson: JsonObject?,
        rostersJson: JsonObject?,
        homeTeam: String,
        awayTeam: String
    ): MatchDetails = MatchDetails(
        statistics = parseStatistics(statisticsJson),
        events = parseEvents(eventsJson, homeTeam, awayTeam),
        homeLineup = parseLineup(rostersJson, homeTeam, true),
        awayLineup = parseLineup(rostersJson, awayTeam, false)
    )

    private fun parseStatistics(root: JsonObject?): List<MatchStatistic> {
        if (root == null) return emptyList()
        val result = mutableListOf<MatchStatistic>()
        for (array in findArrays(root, setOf("statistics", "stats", "match_statistics"))) {
            array.forEach { item ->
                if (!item.isJsonObject) return@forEach
                val obj = item.asJsonObject
                val label = firstString(obj, "name", "label", "type", "statistic", "title") ?: return@forEach
                val home = firstString(obj, "home", "home_value", "homeValue", "local", "value_home") ?: nestedValue(obj, "home")
                val away = firstString(obj, "away", "away_value", "awayValue", "visitor", "value_away") ?: nestedValue(obj, "away")
                if (home != null || away != null) result += MatchStatistic(label, home ?: "–", away ?: "–")
            }
        }
        return result.distinctBy { it.label }
    }

    private fun parseEvents(root: JsonObject?, homeTeam: String, awayTeam: String): List<MatchEvent> {
        if (root == null) return emptyList()
        val result = mutableListOf<MatchEvent>()
        for (array in findArrays(root, setOf("events", "plays", "live_plays", "incidents", "timeline"))) {
            array.forEach { item ->
                if (!item.isJsonObject) return@forEach
                val obj = item.asJsonObject
                result += MatchEvent(
                    minute = normalizeMinute(firstString(obj, "minute", "min", "time", "elapsed", "clock") ?: "–"),
                    team = firstString(obj, "team", "team_name", "teamName", "side") ?: inferTeam(obj, homeTeam, awayTeam),
                    player = firstString(obj, "player", "player_name", "playerName", "name", "athlete") ?: "Ismeretlen játékos",
                    assist = firstString(obj, "assist", "assist_name", "assistName", "secondary_player"),
                    type = firstString(obj, "type", "event_type", "eventType", "kind", "action") ?: "Esemény",
                    detail = firstString(obj, "detail", "description", "comments", "result")
                )
            }
        }
        return result.sortedBy { minuteSortKey(it.minute) }
    }

    private fun parseLineup(root: JsonObject?, teamName: String, preferHome: Boolean): MatchLineup {
        if (root == null) return MatchLineup()
        val obj = findTeamObject(root, teamName, preferHome) ?: root
        val starters = mutableListOf<LineupPlayer>()
        val subs = mutableListOf<LineupPlayer>()
        for (array in findArrays(obj, setOf("startxi", "startingxi", "starting_xi", "starters", "starting_players", "starting"))) array.forEach { parsePlayer(it)?.let(starters::add) }
        for (array in findArrays(obj, setOf("substitutes", "subs", "bench", "sub_players"))) array.forEach { parsePlayer(it)?.let(subs::add) }
        return MatchLineup(
            formation = firstString(obj, "formation", "system", "shape"),
            coach = firstString(obj, "coach", "manager", "coach_name", "manager_name"),
            startingPlayers = starters.distinctBy { it.name },
            substitutePlayers = subs.distinctBy { it.name }
        )
    }

    private fun findTeamObject(root: JsonObject, teamName: String, preferHome: Boolean): JsonObject? {
        val normalizedTeam = normalize(teamName)
        fun visit(element: JsonElement): JsonObject? {
            if (element.isJsonObject) {
                val obj = element.asJsonObject
                val objectName = firstString(obj, "team", "team_name", "teamName", "name")
                if (objectName != null && (normalize(objectName) == normalizedTeam || normalizedTeam.contains(normalize(objectName)) || normalize(objectName).contains(normalizedTeam))) return obj
                for ((key, child) in obj.entrySet()) {
                    if (normalize(key).contains(if (preferHome) "home" else "away") && child.isJsonObject) return child.asJsonObject
                    visit(child)?.let { return it }
                }
            } else if (element.isJsonArray) element.asJsonArray.forEach { visit(it)?.let { return it } }
            return null
        }
        return visit(root)
    }

    private fun parsePlayer(element: JsonElement): LineupPlayer? {
        if (!element.isJsonObject) return null
        val obj = element.asJsonObject
        val name = firstString(obj, "name", "player", "player_name", "playerName", "full_name", "fullname") ?: return null
        return LineupPlayer(
            number = firstString(obj, "number", "shirt_number", "shirtNumber", "jersey"),
            name = name,
            position = firstString(obj, "position", "pos", "role"),
            photoUrl = firstString(obj, "photo", "photo_url", "photoUrl", "image", "image_url", "avatar"),
            isCaptain = firstBoolean(obj, "captain", "is_captain", "isCaptain")
        )
    }

    private fun findArrays(root: JsonObject, wanted: Set<String>): List<JsonArray> {
        val result = mutableListOf<JsonArray>()
        fun visit(element: JsonElement) {
            if (element.isJsonObject) {
                element.asJsonObject.entrySet().forEach { (key, child) ->
                    if (wanted.contains(normalize(key)) && child.isJsonArray) result += child.asJsonArray
                    visit(child)
                }
            } else if (element.isJsonArray) element.asJsonArray.forEach(::visit)
        }
        visit(root)
        return result
    }

    private fun firstString(obj: JsonObject, vararg keys: String): String? {
        for (key in keys) obj.entrySet().firstOrNull { normalize(it.key) == normalize(key) }?.value?.let { valueString(it) }?.takeIf { it.isNotBlank() }?.let { return it }
        return null
    }

    private fun firstBoolean(obj: JsonObject, vararg keys: String): Boolean {
        for (key in keys) {
            val value = obj.entrySet().firstOrNull { normalize(it.key) == normalize(key) }?.value ?: continue
            if (value.isJsonPrimitive) {
                val p = value.asJsonPrimitive
                if (p.isBoolean) return p.asBoolean
                p.asString.toBooleanStrictOrNull()?.let { return it }
            }
        }
        return false
    }

    private fun nestedValue(obj: JsonObject, key: String): String? {
        val child = obj.entrySet().firstOrNull { normalize(it.key) == normalize(key) }?.value
        return if (child?.isJsonObject == true) firstString(child.asJsonObject, "value", "display", "text", "total") else valueString(child)
    }

    private fun inferTeam(obj: JsonObject, home: String, away: String): String {
        val raw = firstString(obj, "team_id", "side", "participant")
        return when {
            raw.equals("home", true) -> home
            raw.equals("away", true) -> away
            else -> "–"
        }
    }

    private fun normalizeMinute(value: String): String = if (value.matches(Regex("\\d{1,3}"))) "$value'" else value.trim().ifEmpty { "–" }
    private fun minuteSortKey(value: String): Int = Regex("\\d{1,3}").find(value)?.value?.toIntOrNull() ?: 999
    private fun normalize(value: String): String = value.lowercase().replace("á","a").replace("é","e").replace("í","i").replace("ó","o").replace("ö","o").replace("ő","o").replace("ú","u").replace("ü","u").replace("ű","u").replace("_","").replace("-","").replace(" ","")
    private fun valueString(value: JsonElement?): String? = if (value?.isJsonPrimitive == true) value.asString else null
}
