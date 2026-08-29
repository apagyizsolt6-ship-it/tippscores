package com.example.tippscores.data.repository

import com.example.tippscores.data.local.ApiPreferences
import com.example.tippscores.data.local.MatchDao
import com.example.tippscores.data.local.MatchEntity
import com.example.tippscores.data.local.toMatch
import com.example.tippscores.data.model.Match
import com.example.tippscores.data.remote.NetworkModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MatchRepository(
    private val matchDao: MatchDao,
    private val apiPreferences: ApiPreferences
) {

    val allMatches: Flow<List<Match>> = matchDao.getAllMatches().map { entities ->
        entities.map { it.toMatch() }
    }

    /**
     * Az adott nap teljes mérkőzéslistája.
     * offset = -7 ... +7 használható a 15 napos naptárhoz.
     *
     * Fontos: offset 0 is DAILY, nem LIVE.
     */
    suspend fun fetchMatchesForOffset(offset: Int = 0) {
        val statpalKey = apiPreferences.statpalApiKey.trim()
        val highlightlyKey = apiPreferences.highlightlyApiKey.trim()

        if (statpalKey.isEmpty()) {
            matchDao.clearMatches()
            throw IllegalStateException("A StatPal API-kulcs nincs megadva.")
        }

        val safeOffset = offset.coerceIn(-7, 7)

        try {
            // Ne mutassuk az előző nap mérkőzéseit az új nap betöltése közben.
            matchDao.clearMatches()

            val response = NetworkModule.statpalApi.getDailyMatches(
                offset = safeOffset,
                accessKey = statpalKey
            )

            val matchEntities = mutableListOf<MatchEntity>()

            response.data?.leagues.orEmpty().forEach { league ->
                val countryAndLeague = league.name?.trim().takeUnless { it.isNullOrEmpty() }
                    ?: "Bajnokság"

                val parts = countryAndLeague.split(":", limit = 2)
                val country = parts.getOrNull(0)?.trim()?.uppercase(Locale.getDefault())
                    ?.takeUnless { it.isEmpty() }
                    ?: league.country?.trim()?.uppercase(Locale.getDefault())
                    ?: "FOCI"

                val leagueName = parts.getOrNull(1)?.trim()
                    ?.takeUnless { it.isEmpty() }
                    ?: countryAndLeague

                league.matches.orEmpty().forEach { match ->
                    val status = displayStatus(match.status, match.time)
                    val isLive = isLiveStatus(match.status)
                    val isFinished = isFinishedStatus(match.status)

                    // Highlightly kiegészítő adat: ha nem érhető el, a meccs attól még megjelenik.
                    // A közvetlen Highlightly hívást itt csak opcionálisan használjuk.
                    val highlight = if (highlightlyKey.isNotEmpty()) null else null

                    matchEntities.add(
                        MatchEntity(
                            id = match.mainId
                                ?: buildFallbackId(match, leagueName),
                            leagueName = leagueName,
                            leagueCountry = country,
                            homeTeam = match.home?.name?.trim().takeUnless { it.isNullOrEmpty() }
                                ?: "Hazai",
                            homeTeamLogo = "",
                            awayTeam = match.away?.name?.trim().takeUnless { it.isNullOrEmpty() }
                                ?: "Vendég",
                            awayTeamLogo = "",
                            homeScore = match.home?.goals?.toIntOrNull(),
                            awayScore = match.away?.goals?.toIntOrNull(),
                            status = status,
                            isLive = isLive,
                            tipPrediction = null,
                            tipConfidence = null,
                            hasVideoHighlight = highlight?.url != null,
                            videoUrl = highlight?.url
                        )
                    )
                }
            }

            // Időrendben, majd bajnokság szerint stabilan.
            val sortedEntities = matchEntities.sortedWith(
                compareBy<MatchEntity> {
                    timeSortKey(it.status, it.isLive)
                }.thenBy { it.leagueCountry }
                    .thenBy { it.leagueName }
                    .thenBy { it.homeTeam }
            )

            if (sortedEntities.isNotEmpty()) {
                matchDao.insertMatches(sortedEntities)
            }

            if (response.data == null) {
                throw IllegalStateException("A StatPal üres választ adott a kiválasztott napra.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            matchDao.clearMatches()
            throw e
        }
    }

    /** Külön élő lekérés az ÉLŐ nézethez. */
    suspend fun fetchLiveMatches() {
        val statpalKey = apiPreferences.statpalApiKey.trim()

        if (statpalKey.isEmpty()) {
            matchDao.clearMatches()
            throw IllegalStateException("A StatPal API-kulcs nincs megadva.")
        }

        try {
            matchDao.clearMatches()

            val response = NetworkModule.statpalApi.getLiveMatches(statpalKey)
            val liveEntities = mutableListOf<MatchEntity>()

            response.data?.leagues.orEmpty().forEach { league ->
                val countryAndLeague = league.name?.trim().takeUnless { it.isNullOrEmpty() }
                    ?: "Bajnokság"
                val parts = countryAndLeague.split(":", limit = 2)
                val country = parts.getOrNull(0)?.trim()?.uppercase(Locale.getDefault())
                    ?.takeUnless { it.isEmpty() }
                    ?: "FOCI"
                val leagueName = parts.getOrNull(1)?.trim()
                    ?.takeUnless { it.isEmpty() }
                    ?: countryAndLeague

                league.matches.orEmpty().forEach { match ->
                    liveEntities.add(
                        MatchEntity(
                            id = match.mainId ?: buildFallbackId(match, leagueName),
                            leagueName = leagueName,
                            leagueCountry = country,
                            homeTeam = match.home?.name?.trim().takeUnless { it.isNullOrEmpty() }
                                ?: "Hazai",
                            homeTeamLogo = "",
                            awayTeam = match.away?.name?.trim().takeUnless { it.isNullOrEmpty() }
                                ?: "Vendég",
                            awayTeamLogo = "",
                            homeScore = match.home?.goals?.toIntOrNull(),
                            awayScore = match.away?.goals?.toIntOrNull(),
                            status = displayStatus(match.status, match.time),
                            isLive = true,
                            tipPrediction = null,
                            tipConfidence = null,
                            hasVideoHighlight = false,
                            videoUrl = null
                        )
                    )
                }
            }

            if (liveEntities.isNotEmpty()) {
                matchDao.insertMatches(liveEntities)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            matchDao.clearMatches()
            throw e
        }
    }

    private fun displayStatus(status: String?, time: String?): String {
        val normalized = status?.trim()?.uppercase(Locale.getDefault()).orEmpty()
        val normalizedTime = time?.trim().orEmpty()

        return when {
            normalized.contains("POST") || normalized.contains("POSTP") -> "Elhalasztva"
            normalized.contains("CANCEL") -> "Törölve"
            normalized == "AET" -> "Hosszabbítás"
            normalized == "PEN" -> "Tizenegyesek"
            normalized == "FT" || normalized == "FINISHED" -> "Vége"
            normalized == "HT" -> "Szünet"
            isLiveStatus(status) -> normalizedTime.ifEmpty { "ÉLŐ" }
            normalizedTime.isNotEmpty() -> normalizedTime
            normalized.isNotEmpty() -> normalized
            else -> "–"
        }
    }

    private fun isLiveStatus(status: String?): Boolean {
        val normalized = status?.trim()?.uppercase(Locale.getDefault()).orEmpty()
        if (normalized.isEmpty()) return false

        return normalized.contains("LIVE") ||
            normalized == "1H" ||
            normalized == "2H" ||
            normalized == "HT" ||
            normalized == "ET" ||
            normalized.matches(Regex("\\d{1,3}'?"))
    }

    private fun isFinishedStatus(status: String?): Boolean {
        val normalized = status?.trim()?.uppercase(Locale.getDefault()).orEmpty()
        return normalized == "FT" ||
            normalized == "FINISHED" ||
            normalized == "AET" ||
            normalized == "PEN" ||
            normalized.contains("POST") ||
            normalized.contains("CANCEL")
    }

    private fun timeSortKey(status: String, isLive: Boolean): Int {
        if (isLive) return 0
        if (status == "Vége" || status == "Hosszabbítás" || status == "Tizenegyesek") return 3000
        val match = Regex("(\\d{1,2}):(\\d{2})").find(status)
        return if (match != null) {
            match.groupValues[1].toInt() * 60 + match.groupValues[2].toInt()
        } else {
            2000
        }
    }

    private fun buildFallbackId(
        match: com.example.tippscores.data.remote.StatpalMatchItem,
        leagueName: String
    ): String {
        return listOf(
            leagueName,
            match.date.orEmpty(),
            match.time.orEmpty(),
            match.home?.id.orEmpty(),
            match.home?.name.orEmpty(),
            match.away?.id.orEmpty(),
            match.away?.name.orEmpty()
        ).joinToString("|").hashCode().toString()
    }
}
