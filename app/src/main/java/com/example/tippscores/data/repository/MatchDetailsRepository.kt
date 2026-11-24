package com.example.tippscores.data.repository

import com.example.tippscores.data.model.HeadToHeadMatch
import com.example.tippscores.data.model.LineupPlayer
import com.example.tippscores.data.model.MatchDetails
import com.example.tippscores.data.model.MatchEvent
import com.example.tippscores.data.model.MatchLineup
import com.example.tippscores.data.model.MatchHighlight
import com.example.tippscores.data.model.MatchStatistic
import com.example.tippscores.data.model.PlayerProfile
import com.example.tippscores.data.model.PlayerTransfer
import com.example.tippscores.data.model.TeamTrophy
import com.example.tippscores.data.remote.NetworkModule
import com.example.tippscores.data.util.TtlCache
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.async
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import retrofit2.HttpException

private const val LINEUP_TTL_MS = 2 * 60 * 1000L
private const val SLOW_TTL_MS = 30 * 60 * 1000L

/**
 * Mérkőzés részletek – csak StatPal.
 */
class MatchDetailsRepository(
    private val statpalKeyProvider: () -> String
) {

    suspend fun fetch(
        matchId: String,
        homeTeam: String,
        awayTeam: String,
        homeTeamId: String? = null,
        awayTeamId: String? = null
    ): MatchDetails = coroutineScope {
        val statpalStats = async {
            fetchWithQueryFallback(
                matchId, "live-match-stats", "stats", "live-match-stats"
            ) { hasStatsData(it) }
        }
        val statpalEvents = async {
            fetchWithQueryFallback(
                matchId, "live-plays", "plays", "live-plays"
            ) { hasEventData(it) }
        }
        val statpalLineups = async {
            // A megerősített összeállítás ritkán változik: 2 percig gyorsítótárazzuk.
            cached("lineups:$matchId", LINEUP_TTL_MS) { fetchLineups(matchId) }
        }

        val parsed = MatchDetailsParser.parse(
            statisticsJson = statpalStats.await(),
            eventsJson = statpalEvents.await(),
            rostersJson = statpalLineups.await(),
            homeTeam = homeTeam,
            awayTeam = awayTeam
        )

        val h2h = async {
            cached("h2h:$homeTeamId:$awayTeamId", SLOW_TTL_MS) {
                fetchHeadToHead(homeTeamId, awayTeamId).takeIf { it.isNotEmpty() }
            }.orEmpty()
        }
        val prediction = async {
            cached("prediction:$matchId", SLOW_TTL_MS) { fetchPrediction(matchId) }
        }
        val weather = async {
            cached("weather:$matchId", SLOW_TTL_MS) { fetchWeather(matchId) }
        }

        MatchDetails(
            statistics = parsed.statistics,
            events = parsed.events,
            homeLineup = parsed.homeLineup,
            awayLineup = parsed.awayLineup,
            headToHead = h2h.await(),
            highlight = null,
            prediction = prediction.await(),
            weather = weather.await(),
            injuries = parsed.injuries
        )
    }

    /**
     * A StatPal "Get Player" végpontja (v2/soccer/players/{player_id}).
     *
     * A szezon-statisztika a "club_league_statistics" listából jön (a
     * játékos jelenlegi klubjának sora), a pályafutás-összesítés az
     * "overall_club_statistics" blokkból. A kettőt nem keverjük.
     */
    suspend fun fetchPlayerProfile(playerId: String): PlayerProfile? {
        val key = statpalKeyProvider().trim()
        if (key.isEmpty() || playerId.isBlank()) return null

        return cached("player:$playerId", SLOW_TTL_MS) { loadPlayerProfile(playerId, key) }
    }

    private suspend fun loadPlayerProfile(playerId: String, key: String): PlayerProfile? {

        return try {
            val root = NetworkModule.statpalMatchDetailsApi.getDetails(
                url = "https://statpal.io/api/v2/soccer/players/$playerId",
                accessKey = key
            )

            val playerObj = objOrNull(root, "player") ?: return null

            val playerTeamId = firstString(playerObj, "team_id")
            val clubs = objectList(objOrNull(playerObj, "club_league_statistics")?.get("club"))
            val season = clubs.firstOrNull { firstString(it, "team_id") == playerTeamId }
                ?: clubs.firstOrNull()
            val overall = objOrNull(playerObj, "overall_club_statistics")

            fun s(vararg keys: String): String? = season?.let { firstString(it, *keys) }
            fun c(vararg keys: String): String? = overall?.let { firstString(it, *keys) }

            val trophies = objectList(objOrNull(playerObj, "trophies")?.get("trophy"))
                .mapNotNull { t ->
                    val league = firstString(t, "league") ?: return@mapNotNull null
                    TeamTrophy(
                        league = league,
                        country = firstString(t, "country"),
                        status = firstString(t, "status"),
                        count = firstString(t, "count"),
                        seasons = firstString(t, "seasons")?.trim()?.trimEnd(',')?.trim()
                    )
                }

            val transfers = mutableListOf<PlayerTransfer>()
            collectTransfers(playerObj.get("transfers"), transfers)

            val seasonLabel = listOfNotNull(
                s("league"),
                s("season")
            ).joinToString(" ").takeIf { it.isNotBlank() }

            PlayerProfile(
                id = firstString(playerObj, "id") ?: playerId,
                name = firstString(playerObj, "name") ?: "Ismeretlen játékos",
                age = firstString(playerObj, "age"),
                nationality = firstString(playerObj, "nationality"),
                position = firstString(playerObj, "position"),
                height = firstString(playerObj, "height"),
                weight = firstString(playerObj, "weight"),
                preferredFoot = firstString(playerObj, "preferred_foot"),
                team = firstString(playerObj, "team"),
                marketValueEur = firstString(playerObj, "market_value_eur"),
                appearances = s("appearances", "appearences"),
                goals = s("goals"),
                assists = s("assists"),
                minutesPlayed = s("minutes_played"),
                rating = s("rating"),
                yellowCards = s("yellowcards"),
                redCards = s("redcards"),
                birthdate = firstString(playerObj, "birthdate"),
                birthplace = listOfNotNull(
                    firstString(playerObj, "birthplace"),
                    firstString(playerObj, "birthcountry")
                ).distinct().joinToString(", ").takeIf { it.isNotBlank() },
                seasonLabel = seasonLabel,
                startingLineups = s("starting_lineups", "lineups"),
                shotsTotal = s("shots_total"),
                shotsOnTarget = s("shots_on_target", "shots_on"),
                keyPasses = s("key_passes"),
                dribbleAttempts = s("dribble_attempts"),
                dribbleSuccess = s("dribble_success"),
                duelsTotal = s("duels_total"),
                duelsWon = s("duels_won"),
                tackles = s("tackles"),
                interceptions = s("interceptions"),
                foulsCommitted = s("fouls_committed"),
                foulsDrawn = s("fouls_drawn"),
                penScored = s("pen_scored", "penalties_scored"),
                penMissed = s("pen_missed", "penalties_missed"),
                saves = s("saves"),
                goalsConceded = s("goals_conceded"),
                careerAppearances = c("appearances", "appearences"),
                careerMinutes = c("minutes_played"),
                careerAssists = c("assists"),
                careerShotsTotal = c("shots_total"),
                careerShotsOnTarget = c("shots_on_target", "shots_on"),
                careerPenScored = c("pen_scored", "penalties_scored"),
                careerRating = c("rating"),
                trophies = trophies,
                transfers = transfers
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun objOrNull(obj: JsonObject?, name: String): JsonObject? =
        obj?.get(name)?.takeIf { it.isJsonObject }?.asJsonObject

    private fun objectList(element: JsonElement?): List<JsonObject> {
        if (element == null || element.isJsonNull) return emptyList()
        return when {
            element.isJsonArray ->
                element.asJsonArray.filter { it.isJsonObject }.map { it.asJsonObject }
            element.isJsonObject -> listOf(element.asJsonObject)
            else -> emptyList()
        }
    }

    /**
     * A játékos átigazolásainak pontos szerkezetét a StatPal mintában még
     * nem láttuk (üres volt), ezért rugalmasan keressük: minden olyan
     * objektum átigazolás, ahol van dátum és honnan / hova csapat.
     */
    private fun collectTransfers(element: JsonElement?, out: MutableList<PlayerTransfer>) {
        if (element == null || element.isJsonNull) return
        when {
            element.isJsonArray -> element.asJsonArray.forEach { collectTransfers(it, out) }
            element.isJsonObject -> {
                val obj = element.asJsonObject
                val from = firstString(obj, "from", "from_team", "team_from", "from_team_name")
                val to = firstString(obj, "to", "to_team", "team_to", "to_team_name")
                val date = firstString(obj, "date", "transfer_date")
                if (date != null && (from != null || to != null)) {
                    out += PlayerTransfer(
                        date = date,
                        from = from,
                        to = to,
                        type = firstString(obj, "type"),
                        price = firstString(obj, "price", "fee")
                    )
                } else {
                    obj.entrySet().forEach { collectTransfers(it.value, out) }
                }
            }
        }
    }

    /**
     * Kezdőcsapat / cserepad. A StatPal "Team Lineups" végpontja (a
     * /team-lineups, ugyanúgy match_id paraméterrel, mint a predictions és
     * weather-forecast) előzetes ("projected") és megerősített ("confirmed")
     * összeállítást is ad. A korábbi rosters / lineups útvonalak tartalékok.
     * Csak olyan választ fogadunk el, amiben tényleg van játékoslista.
     */
    private suspend fun fetchLineups(matchId: String): JsonObject? {
        val key = statpalKeyProvider().trim()
        if (key.isEmpty() || matchId.isBlank()) return null

        val urls = listOf(
            "https://statpal.io/api/v2/soccer/team-lineups?match_id=$matchId",
            "https://statpal.io/api/v2/soccer/matches/$matchId/team-lineups",
            "https://statpal.io/api/v2/soccer/matches/$matchId/rosters"
        )

        for (url in urls) {
            try {
                val json = NetworkModule.statpalMatchDetailsApi.getDetails(
                    url = url,
                    accessKey = key
                )
                if (hasLineupData(json)) return json
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun hasLineupData(json: JsonObject): Boolean {
        val text = json.toString().lowercase()
        return text.contains("starting_xi") ||
            text.contains("startxi") ||
            text.contains("\"starters\"") ||
            text.contains("\"bench\"") ||
            text.contains("\"substitutes\"")
    }

    private val responseCache = TtlCache<Any>(maxEntries = 120)

    /** Közös, lejárati idős gyorsítótár: csak a nem üres eredményeket tartjuk meg. */
    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : Any> cached(
        key: String,
        ttlMs: Long,
        loader: suspend () -> T?
    ): T? {
        val hit = responseCache.get(key, ttlMs)
        if (hit != null) return hit as T
        val value = loader() ?: return null
        responseCache.put(key, value)
        return value
    }

    /**
     * Először a megszokott `matches/{id}/{resource}` címet próbálja, és ha
     * az nem ad használható adatot, a `{resource}?match_id={id}` formát is
     * (a StatPal többi végpontja is így működik).
     */
    private suspend fun fetchWithQueryFallback(
        matchId: String,
        resource1: String,
        resource2: String,
        queryResource: String,
        isUsable: (JsonObject) -> Boolean
    ): JsonObject? {
        val primary = fetchStatpal(matchId, resource1, resource2)
        if (primary != null && isUsable(primary)) return primary

        val key = statpalKeyProvider().trim()
        if (key.isEmpty()) return primary

        val alternative = try {
            NetworkModule.statpalMatchDetailsApi.getDetails(
                url = "https://statpal.io/api/v2/soccer/$queryResource?match_id=$matchId",
                accessKey = key
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

        return if (alternative != null && isUsable(alternative)) alternative else primary
    }

    private fun hasStatsData(json: JsonObject): Boolean {
        val text = json.toString().lowercase()
        return text.contains("possession") || text.contains("shots") || text.contains("corner")
    }

    private fun hasEventData(json: JsonObject): Boolean {
        val text = json.toString().lowercase()
        return text.contains("minute") || text.contains("\"min\"") || text.contains("player")
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

    private suspend fun fetchPrediction(matchId: String): String? {
        val key = statpalKeyProvider().trim()
        if (key.isEmpty() || matchId.isBlank()) return null
        return try {
            val root = NetworkModule.statpalMatchDetailsApi.getDetails(
                url = "https://statpal.io/api/v2/soccer/predictions?match_id=$matchId",
                accessKey = key
            )
            root.getAsJsonObject("prediction")?.get("outcome")?.asString
                ?: root.get("prediction")?.asString
        } catch (_: Exception) { null }
    }

    private suspend fun fetchWeather(matchId: String): String? {
        val key = statpalKeyProvider().trim()
        if (key.isEmpty() || matchId.isBlank()) return null
        return try {
            val root = NetworkModule.statpalMatchDetailsApi.getDetails(
                url = "https://statpal.io/api/v2/soccer/weather-forecast?match_id=$matchId",
                accessKey = key
            )
            val temp = root.getAsJsonObject("weather")?.get("temperature")?.asString
            val cond = root.getAsJsonObject("weather")?.get("condition")?.asString
            listOfNotNull(temp, cond).joinToString(" • ").ifBlank { null }
        } catch (_: Exception) { null }
    }

    private suspend fun fetchHeadToHead(
        homeTeamId: String?,
        awayTeamId: String?
    ): List<HeadToHeadMatch> {
        if (homeTeamId.isNullOrBlank() || awayTeamId.isNullOrBlank()) return emptyList()
        val key = statpalKeyProvider().trim()
        if (key.isEmpty()) return emptyList()

        return try {
            val root = NetworkModule.statpalMatchDetailsApi.getDetails(
                url = "https://statpal.io/api/v2/soccer/head-to-head?team1_id=$homeTeamId&team2_id=$awayTeamId",
                accessKey = key
            )
            val meetings = root.getAsJsonObject("head-to-head")
                ?.getAsJsonObject("recent_meetings")
                ?.getAsJsonArray("match")
                ?: return emptyList()

            meetings.mapNotNull { el ->
                if (!el.isJsonObject) return@mapNotNull null
                val obj = el.asJsonObject
                HeadToHeadMatch(
                    date = obj.get("date")?.asString.orEmpty(),
                    homeTeam = obj.get("team1_name")?.asString.orEmpty(),
                    awayTeam = obj.get("team2_name")?.asString.orEmpty(),
                    homeScore = obj.get("team1_score")?.asString ?: "-",
                    awayScore = obj.get("team2_score")?.asString ?: "-"
                )
            }.take(10)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
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
        awayLineup = parseLineup(rostersJson, awayTeam, false),
        injuries = parseSidelined(rostersJson, homeTeam, awayTeam)
    )

    /**
     * A rosters válasz "sidelined" listája: az adott meccs két csapatának
     * hiányzói (kiesik / kérdéses, ok: sérülés vagy eltiltás).
     */
    private fun parseSidelined(root: JsonObject?, homeTeam: String, awayTeam: String): List<String> {
        if (root == null) return emptyList()
        val result = mutableListOf<String>()
        listOf(homeTeam to true, awayTeam to false).forEach { (team, preferHome) ->
            val obj = findTeamObject(root, team, preferHome) ?: return@forEach
            for (array in findArrays(obj, setOf("sidelined", "injuries", "missing"))) {
                array.forEach { element ->
                    if (!element.isJsonObject) return@forEach
                    val p = element.asJsonObject
                    val name = firstString(p, "name", "player", "player_name") ?: return@forEach
                    val status = when (firstString(p, "status")?.lowercase()) {
                        "out" -> "kiesik"
                        "doubtful" -> "kérdéses"
                        else -> null
                    }
                    val reason = when (firstString(p, "reason")?.lowercase()) {
                        "injury" -> "sérülés"
                        "suspension", "suspended" -> "eltiltás"
                        else -> null
                    }
                    val details = listOfNotNull(status, reason).joinToString(", ")
                    result += if (details.isBlank()) "$name ($team)" else "$name ($team) – $details"
                }
            }
        }
        return result
    }

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
            formation = firstString(obj, "formation", "team_formation", "system", "shape"),
            isProjected = firstString(root, "status").equals("projected", ignoreCase = true),
            coach = firstString(obj, "coach", "manager", "coach_name", "manager_name")
                ?: coachObjectName(obj),
            coachId = coachObjectId(obj),
            startingPlayers = starters.distinctBy { it.name },
            substitutePlayers = subs.distinctBy { it.name }
        )
    }

    /** A StatPal néha objektumként adja az edzőt: {"name": "...", "id": "..."}. */
    private fun coachObjectName(obj: JsonObject): String? {
        val coach = obj.entrySet()
            .firstOrNull { normalize(it.key) == "coach" || normalize(it.key) == "manager" }
            ?.value
            ?.takeIf { it.isJsonObject }
            ?.asJsonObject
            ?: return null
        return firstString(coach, "name", "full_name", "fullname")
    }

    private fun coachObjectId(obj: JsonObject): String? {
        val coach = obj.entrySet()
            .firstOrNull { normalize(it.key) == "coach" || normalize(it.key) == "manager" }
            ?.value
            ?.takeIf { it.isJsonObject }
            ?.asJsonObject
            ?: return null
        return firstString(coach, "id", "coach_id")
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
            id = firstString(obj, "id", "player_id", "playerId"),
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