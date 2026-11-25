package com.example.tippscores.data.repository

import com.example.tippscores.data.model.*
import com.example.tippscores.data.remote.NetworkModule
import com.example.tippscores.data.remote.StatpalImages
import com.example.tippscores.data.util.TtlCache
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlayerProfileRepository {
    private val cache = TtlCache<PlayerProfile>(ttlMillis = 15 * 60 * 1000L) // 15 perc cache

    suspend fun getPlayerProfile(playerId: String, accessKey: String): Result<PlayerProfile> =
        withContext(Dispatchers.IO) {
            cache.get(playerId)?.let { return@withContext Result.success(it) }

            runCatching {
                val url = "https://statpal.io"
                val rawJson = NetworkModule.getString(url)
                val root = JSONObject(rawJson)
                val data = root.optJSONObject("data")
                    ?: root.optJSONObject("player")
                    ?: root

                val profile = parsePlayer(playerId, data, accessKey)
                cache.put(playerId, profile)
                profile
            }
        }

    private fun parsePlayer(fallbackId: String, obj: JSONObject, accessKey: String): PlayerProfile {
        val id = obj.optString("id", fallbackId).ifBlank { fallbackId }
        val teamObj = obj.optJSONObject("team")
        val statsArray = obj.optJSONArray("stats")
            ?: obj.optJSONArray("statistics")
            ?: JSONArray()
        val matchesArray = obj.optJSONArray("matches")
            ?: obj.optJSONArray("recent_matches")
            ?: JSONArray()
        val trophiesArray = obj.optJSONArray("trophies") ?: JSONArray()

        val seasonStats = buildList {
            for (i in 0 until statsArray.length()) {
                val s = statsArray.optJSONObject(i) ?: continue
                add(
                    PlayerSeasonStat(
                        leagueName = s.optString("league_name", s.optString("league", "Bajnokság")),
                        season = s.optString("season", ""),
                        appearances = s.optInt("appearances", s.optInt("matches", 0)),
                        lineups = s.optInt("lineups", s.optInt("starting", 0)),
                        minutesPlayed = s.optInt("minutes", s.optInt("minutes_played", 0)),
                        goals = s.optInt("goals", 0),
                        assists = s.optInt("assists", 0),
                        yellowCards = s.optInt("yellow_cards", s.optInt("yellowcards", 0)),
                        redCards = s.optInt("red_cards", s.optInt("redcards", 0)),
                        shotsTotal = s.optInt("shots", 0),
                        shotsOnTarget = s.optInt("shots_on_target", 0),
                        keyPasses = s.optInt("key_passes", 0),
                        passAccuracy = s.optDouble("pass_accuracy", 0.0).toFloat(),
                        rating = s.optDouble("rating", 0.0).toFloat()
                    )
                )
            }
        }

        val recentMatches = buildList {
            for (i in 0 until matchesArray.length()) {
                val m = matchesArray.optJSONObject(i) ?: continue
                add(
                    PlayerMatchLog(
                        matchId = m.optString("match_id", m.optString("id", "")),
                        date = m.optString("date", ""),
                        opponent = m.optString("opponent", m.optString("against", "")),
                        score = m.optString("score", m.optString("result", "-:-")),
                        minutes = m.optInt("minutes", 0),
                        goals = m.optInt("goals", 0),
                        assists = m.optInt("assists", 0),
                        yellowCard = m.optInt("yellow_cards", 0) > 0,
                        redCard = m.optInt("red_cards", 0) > 0,
                        rating = m.optDouble("rating", 0.0).toFloat()
                    )
                )
            }
        }

        val trophies = buildList {
            for (i in 0 until trophiesArray.length()) {
                val t = trophiesArray.optJSONObject(i) ?: continue
                add(
                    PlayerTrophy(
                        title = t.optString("title", t.optString("name", "")),
                        season = t.optString("season", ""),
                        teamName = t.optString("team", "")
                    )
                )
            }
        }

        val resolvedPhoto = obj.optString("image").takeIf { it.isNotBlank() }
            ?: "https://statpal.io"

        return PlayerProfile(
            id = id,
            name = obj.optString("name", obj.optString("common_name", "Ismeretlen játékos")),
            fullName = obj.optString("fullname", obj.optString("full_name", "")),
            nationality = obj.optString("nationality", obj.optString("country", "")),
            birthDate = obj.optString("birthdate", obj.optString("date_of_birth", "")),
            age = obj.optInt("age", -1).takeIf { it > 0 },
            height = obj.optString("height", ""),
            weight = obj.optString("weight", ""),
            preferredFoot = obj.optString("foot", obj.optString("preferred_foot", "")),
            position = obj.optString("position", ""),
            jerseyNumber = obj.optString("number", obj.optString("jersey_number", "")),
            marketValue = obj.optString("market_value", ""),
            contractUntil = obj.optString("contract_until", ""),
            teamId = teamObj?.optString("id") ?: obj.optString("team_id", ""),
            teamName = teamObj?.optString("name") ?: obj.optString("team_name", ""),
            photoUrl = resolvedPhoto,
            seasonStats = seasonStats,
            recentMatches = recentMatches,
            trophies = trophies
        )
    }
}
