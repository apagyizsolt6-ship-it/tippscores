package com.example.tippscores.data.repository

import com.example.tippscores.data.model.LeagueStats
import com.example.tippscores.data.model.TeamFormItem
import com.example.tippscores.data.model.TeamRecentMatch
import com.example.tippscores.data.util.JsonX
import com.example.tippscores.data.model.LeaguePlayerStat
import com.example.tippscores.data.model.TeamLeagueRecord
import com.example.tippscores.data.model.TeamMinuteBucket
import com.example.tippscores.data.model.TeamProfile
import com.example.tippscores.data.model.TeamSquadPlayer
import com.example.tippscores.data.model.TeamTransfer
import com.example.tippscores.data.model.TeamTrophy
import com.example.tippscores.data.remote.NetworkModule
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException

/**
 * Csapatprofil a StatPal "Get Team" végpontjából
 * (v2/soccer/teams/{team_id}).
 *
 * A végpont csapat-AZONOSÍTÓT vár, ezért a meccslistából érkező
 * homeTeamId / awayTeamId értéket használjuk. Az eredményt 10 percig
 * memóriában tartjuk, hogy a profil újranyitása ne költsön API-hívást.
 */
class TeamProfileRepository(
    private val statpalKeyProvider: () -> String
) {

    private data class CacheEntry(
        val savedAt: Long,
        val profile: TeamProfile
    )

    private data class LeagueCacheEntry(
        val savedAt: Long,
        val stats: LeagueStats
    )

    private val cache = mutableMapOf<String, CacheEntry>()
    private val leagueCache = mutableMapOf<String, LeagueCacheEntry>()

    suspend fun fetch(teamId: String?, teamName: String): TeamProfile? {
        val id = teamId?.trim().orEmpty()
        val key = statpalKeyProvider().trim()
        if (id.isEmpty() || key.isEmpty()) return null

        val cached = cache[id]
        if (cached != null &&
            System.currentTimeMillis() - cached.savedAt < CACHE_MS
        ) {
            return cached.profile
        }

        return try {
            val root = NetworkModule.statpalMatchDetailsApi.getDetails(
                url = "https://statpal.io/api/v2/soccer/teams/$id",
                accessKey = key
            )
            val profile = parse(root, id, teamName)
            if (profile != null) {
                cache[id] = CacheEntry(System.currentTimeMillis(), profile)
            }
            profile
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * StatPal "Get League Stats" (v2/soccer/leagues/{league_id}/stats):
     * a bajnokság összes csapata és keretük játékos-statisztikái egy
     * hívásban. 30 percig memóriában tartjuk (nagy válasz).
     */
    suspend fun fetchLeagueStats(leagueId: String?): LeagueStats? {
        val id = leagueId?.trim().orEmpty()
        val key = statpalKeyProvider().trim()
        if (id.isEmpty() || key.isEmpty()) return null

        val cached = leagueCache[id]
        if (cached != null &&
            System.currentTimeMillis() - cached.savedAt < LEAGUE_CACHE_MS
        ) {
            return cached.stats
        }

        return try {
            val root = NetworkModule.statpalMatchDetailsApi.getDetails(
                url = "https://statpal.io/api/v2/soccer/leagues/$id/stats",
                accessKey = key
            )
            val stats = parseLeagueStats(root, id)
            if (stats != null) {
                leagueCache[id] = LeagueCacheEntry(System.currentTimeMillis(), stats)
            }
            stats
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseLeagueStats(root: JsonObject, id: String): LeagueStats? {
        val league = root.obj("league_stats")
            ?.get("league")
            .items()
            .firstOrNull()
            ?: return null

        val players = league.get("team")
            .items()
            .flatMap { team ->
                val teamId = team.text("id")
                val teamName = team.text("name").orEmpty()
                team.obj("squad")
                    ?.get("player")
                    .items()
                    .mapNotNull { p ->
                        val name = p.text("name") ?: return@mapNotNull null
                        LeaguePlayerStat(
                            playerId = p.text("id"),
                            name = name,
                            teamId = teamId,
                            teamName = teamName,
                            number = p.text("number"),
                            position = p.text("position"),
                            appearances = (p.text("appearences") ?: p.text("appearances"))
                                ?.toIntOrNull() ?: 0,
                            minutes = p.text("minutes_played")?.toIntOrNull() ?: 0,
                            goals = p.text("goals")?.toIntOrNull() ?: 0,
                            assists = p.text("assists")?.toIntOrNull() ?: 0,
                            yellowCards = p.text("yellowcards")?.toIntOrNull() ?: 0,
                            redCards = p.text("redcards")?.toIntOrNull() ?: 0,
                            rating = p.text("rating")?.toDoubleOrNull()?.takeIf { it > 0.0 }
                        )
                    }
            }

        return LeagueStats(
            leagueId = league.text("id") ?: id,
            leagueName = league.text("name") ?: "Bajnokság",
            players = players
        )
    }

    private fun parse(root: JsonObject, id: String, fallbackName: String): TeamProfile? {
        val team = root.obj("team") ?: return null

        val records = team.obj("league_stats")
            ?.get("league")
            .items()
            .map { parseLeagueRecord(it) }

        val coach = team.obj("coach")

        val squad = team.obj("squad")
            ?.get("player")
            .items()
            .mapNotNull { p ->
                val name = p.text("name") ?: return@mapNotNull null
                TeamSquadPlayer(
                    id = p.text("id"),
                    name = name,
                    number = p.text("number"),
                    age = p.text("age"),
                    position = p.text("position"),
                    isCaptain = p.text("is_captain").isTruthy(),
                    injured = p.text("injured").isTruthy()
                )
            }
            .sortedWith(
                compareBy<TeamSquadPlayer> { positionOrder(it.position) }
                    .thenBy { it.number?.toIntOrNull() ?: Int.MAX_VALUE }
                    .thenBy { it.name }
            )

        val transfersObj = team.obj("transfers")
        val transfers =
            parseTransfers(transfersObj?.obj("in"), incoming = true) +
                parseTransfers(transfersObj?.obj("out"), incoming = false)

        val trophies = team.obj("trophies")
            ?.get("trophy")
            .items()
            .mapNotNull { t ->
                val league = t.text("league") ?: return@mapNotNull null
                TeamTrophy(
                    league = league,
                    country = t.text("country"),
                    status = t.text("status"),
                    count = t.text("count"),
                    seasons = t.text("seasons")?.trim()?.trimEnd(',')?.trim()
                )
            }

        return TeamProfile(
            id = team.text("id") ?: id,
            name = team.text("name") ?: fallbackName,
            country = team.text("country"),
            league = records.firstOrNull()?.leagueName,
            founded = team.text("founded"),
            venueName = team.text("venue_name"),
            venueCity = team.text("venue_city"),
            venueCapacity = team.text("venue_capacity"),
            venueSurface = team.text("venue_surface"),
            coachName = coach.text("name"),
            coachId = coach.text("id"),
            leagueRecords = records,
            squad = squad,
            transfers = transfers,
            trophies = trophies
        )
    }

    private fun parseLeagueRecord(lg: JsonObject): TeamLeagueRecord {
        val full = lg.obj("fulltime")

        fun value(result: String, split: String): Int =
            full.obj(result).text(split)?.toIntOrNull() ?: 0

        return TeamLeagueRecord(
            leagueName = lg.text("name") ?: "Bajnokság",
            season = lg.text("season").orEmpty(),
            leagueId = lg.text("id").orEmpty(),
            winsTotal = value("win", "total"),
            winsHome = value("win", "home"),
            winsAway = value("win", "away"),
            drawsTotal = value("draw", "total"),
            drawsHome = value("draw", "home"),
            drawsAway = value("draw", "away"),
            lossesTotal = value("lost", "total"),
            lossesHome = value("lost", "home"),
            lossesAway = value("lost", "away"),
            scoringMinutes = parseBuckets(lg.obj("scoring_minutes")),
            concededMinutes = parseBuckets(lg.obj("goals_conceded_minutes"))
        )
    }

    private fun parseBuckets(obj: JsonObject?): List<TeamMinuteBucket> =
        obj?.get("period")
            .items()
            .mapNotNull { b ->
                val range = b.text("min") ?: return@mapNotNull null
                TeamMinuteBucket(
                    range = range,
                    percent = b.text("pct")
                        ?.filter { it.isDigit() }
                        ?.toIntOrNull() ?: 0,
                    count = b.text("count")?.toIntOrNull() ?: 0
                )
            }

    private fun parseTransfers(obj: JsonObject?, incoming: Boolean): List<TeamTransfer> =
        obj?.get("player")
            .items()
            .mapNotNull { t ->
                val name = t.text("name") ?: return@mapNotNull null
                TeamTransfer(
                    playerName = name,
                    date = t.text("date").orEmpty(),
                    otherTeam = if (incoming) t.text("from") else t.text("to"),
                    type = t.text("type"),
                    price = t.text("price"),
                    isIncoming = incoming
                )
            }

    private fun positionOrder(position: String?): Int =
        when (position?.trim()?.uppercase()) {
            "G", "GK", "GOALKEEPER" -> 0
            "D", "DEFENDER" -> 1
            "M", "MIDFIELDER" -> 2
            "F", "A", "FORWARD", "ATTACKER" -> 3
            else -> 4
        }

    private fun String?.isTruthy(): Boolean =
        this != null && (equals("true", ignoreCase = true) || this == "1")

    // --- Biztonságos JSON segédek (a StatPal néha objektumot ad tömb helyett) ---

    private fun JsonElement?.asObj(): JsonObject? =
        if (this != null && this.isJsonObject) this.asJsonObject else null

    private fun JsonObject?.obj(name: String): JsonObject? =
        this?.get(name).asObj()

    private fun JsonObject?.text(name: String): String? {
        val element = this?.get(name) ?: return null
        if (!element.isJsonPrimitive) return null
        return element.asString.trim().takeIf { it.isNotEmpty() }
    }

    private fun JsonElement?.items(): List<JsonObject> {
        val element = this
        if (element == null || element.isJsonNull) return emptyList()
        return when {
            element.isJsonArray -> element.asJsonArray.mapNotNull { it.asObj() }
            element.isJsonObject -> listOf(element.asJsonObject)
            else -> emptyList()
        }
    }

    private companion object {
        const val CACHE_MS = 10 * 60 * 1000L
        const val LEAGUE_CACHE_MS = 30 * 60 * 1000L
    }
}
