package com.example.tippscores.data.repository

import com.example.tippscores.data.local.ApiPreferences
import com.example.tippscores.data.local.MatchDao
import com.example.tippscores.data.local.MatchEntity
import com.example.tippscores.data.local.toMatch
import com.example.tippscores.data.localization.CountryLocalizer
import com.example.tippscores.data.localization.LeagueLocalizer
import com.example.tippscores.data.model.Match
import com.example.tippscores.data.remote.NetworkModule
import com.example.tippscores.data.remote.StatpalLeagueItem
import com.example.tippscores.data.remote.StatpalMatchItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class MatchRepository(
    private val matchDao: MatchDao,
    private val apiPreferences: ApiPreferences
) {

    val allMatches: Flow<List<Match>> =
        matchDao.getAllMatches().map { entities -> entities.map { it.toMatch() } }

    // ========================================================
    // NAPI MÉRKŐZÉSEK (StatPal DAILY végpont)
    //
    // -7 = 7 nappal korábban, 0 = ma, +7 = 7 nappal később.
    // Minden meccset visszaadunk, amit a StatPal az adott napra
    // ad - itt nincs semmilyen mesterséges darabszám-korlátozás.
    // ========================================================

    suspend fun fetchMatchesForOffset(offset: Int = 0) {
        val statpalKey = apiPreferences.statpalApiKey.trim()

        if (statpalKey.isEmpty()) {
            matchDao.clearMatches()
            throw IllegalStateException("A StatPal API-kulcs nincs megadva.")
        }

        val safeOffset = offset.coerceIn(-7, 7)

        try {
            matchDao.clearMatches()

            val response = NetworkModule.statpalApi.getDailyMatches(
                offset = safeOffset,
                accessKey = statpalKey
            )

            val entities = buildEntities(
                leagues = response.data?.leagues.orEmpty(),
                offsetDays = safeOffset,
                forceLive = false
            )

            val sortedEntities = entities.sortedWith(
                compareBy<MatchEntity> { timeSortKey(it.status, it.isLive) }
                    .thenBy { it.leagueCountry }
                    .thenBy { it.leagueName }
                    .thenBy { it.homeTeam }
            )

            if (sortedEntities.isNotEmpty()) {
                matchDao.insertMatches(sortedEntities)
            }

            if (response.data == null) {
                throw IllegalStateException("A StatPal üres választ adott a kiválasztott napra.")
            }

        } catch (e: HttpException) {
            e.printStackTrace()
            matchDao.clearMatches()
            val detail = try { e.response()?.errorBody()?.string() } catch (ex: Exception) { null }
            throw Exception(
                "Statpal API hiba (${e.code()}): " +
                    when (e.code()) {
                        401, 403 -> "érvénytelen vagy inaktív API kulcs."
                        429 -> "túllépted a napi/perces lekérési limitet."
                        else -> detail?.takeIf { it.isNotBlank() } ?: e.message()
                    }
            )
        } catch (e: IOException) {
            e.printStackTrace()
            matchDao.clearMatches()
            throw Exception("Nincs internetkapcsolat, vagy a szerver nem elérhető: ${e.message}")
        } catch (e: Exception) {
            e.printStackTrace()
            matchDao.clearMatches()
            throw e
        }
    }

    // ========================================================
    // ÉLŐ MÉRKŐZÉSEK (StatPal LIVE végpont)
    // ========================================================

    suspend fun fetchLiveMatches() {
        val statpalKey = apiPreferences.statpalApiKey.trim()

        if (statpalKey.isEmpty()) {
            matchDao.clearMatches()
            throw IllegalStateException("A StatPal API-kulcs nincs megadva.")
        }

        try {
            matchDao.clearMatches()

            val response = NetworkModule.statpalApi.getLiveMatches(statpalKey)

            val entities = buildEntities(
                leagues = response.data?.leagues.orEmpty(),
                offsetDays = 0,
                forceLive = true
            )

            if (entities.isNotEmpty()) {
                matchDao.insertMatches(entities)
            }

        } catch (e: HttpException) {
            e.printStackTrace()
            matchDao.clearMatches()
            throw Exception(
                "Statpal API hiba (${e.code()}): " +
                    when (e.code()) {
                        401, 403 -> "érvénytelen vagy inaktív API kulcs."
                        429 -> "túllépted a napi/perces lekérési limitet."
                        else -> e.message()
                    }
            )
        } catch (e: IOException) {
            e.printStackTrace()
            matchDao.clearMatches()
            throw Exception("Nincs internetkapcsolat, vagy a szerver nem elérhető: ${e.message}")
        } catch (e: Exception) {
            e.printStackTrace()
            matchDao.clearMatches()
            throw e
        }
    }

    // ========================================================
    // KÖZÖS FELDOLGOZÁS (napi + élő lekérés közös logikája)
    // ========================================================

    private fun buildEntities(
        leagues: List<StatpalLeagueItem>,
        offsetDays: Int,
        forceLive: Boolean
    ): List<MatchEntity> {

        val result = mutableListOf<MatchEntity>()

        leagues.forEach { league ->
            val (country, flag, leagueName) = resolveLeagueInfo(league)

            league.matches.orEmpty().forEach { match ->
                val isLive = if (forceLive) true else isLiveStatus(match.status)
                val localTime = convertUtcTimeToLocal(match.time, offsetDays)
                val status = displayStatus(match.status, localTime)

                val matchId = match.mainId
                    ?.takeIf { it.isNotBlank() }
                    ?: buildFallbackId(match, leagueName)

                result.add(
                    MatchEntity(
                        id = matchId,
                        leagueName = leagueName,
                        leagueCountry = country,
                        leagueCountryFlag = flag,
                        homeTeam = match.home?.name?.trim()?.takeUnless { it.isEmpty() } ?: "Hazai",
                        homeTeamLogo = "",
                        awayTeam = match.away?.name?.trim()?.takeUnless { it.isEmpty() } ?: "Vendég",
                        awayTeamLogo = "",
                        homeScore = match.home?.goals?.toIntOrNull(),
                        awayScore = match.away?.goals?.toIntOrNull(),
                        status = status,
                        isLive = isLive,
                        tipPrediction = null,
                        tipConfidence = null,
                        hasVideoHighlight = false,
                        videoUrl = null
                    )
                )
            }
        }

        return result
    }

    // ========================================================
    // BAJNOKSÁG / ORSZÁG FELOLDÁSA + MAGYARÍTÁS + ZÁSZLÓ
    // ========================================================

    private fun resolveLeagueInfo(league: StatpalLeagueItem): Triple<String, String, String> {
        val countryAndLeague = league.name?.trim()?.takeUnless { it.isEmpty() } ?: "Bajnokság"

        val parts = countryAndLeague.split(":", limit = 2)

        val rawCountry = parts.getOrNull(0)
            ?.trim()
            ?.uppercase(Locale.getDefault())
            ?.takeUnless { it.isEmpty() }
            ?: league.country?.trim()?.uppercase(Locale.getDefault())?.takeUnless { it.isEmpty() }
            ?: "FOCI"

        val rawLeagueName = parts.getOrNull(1)
            ?.trim()
            ?.takeUnless { it.isEmpty() }
            ?: countryAndLeague

        val country = CountryLocalizer.hungarianName(rawCountry)
        val flag = CountryLocalizer.flagEmoji(rawCountry)
        val leagueName = LeagueLocalizer.hungarianLeagueName(rawLeagueName)

        return Triple(country, flag, leagueName)
    }

    // ========================================================
    // IDŐZÓNA: UTC KEZDÉSI IDŐ -> ESZKÖZ HELYI IDEJE
    //
    // Feltételezés: a StatPal UTC-ben adja a kezdési időt (ez a
    // legelterjedtebb konvenció sportadat API-knál, és ez magyarázza
    // pontosan a jelentett "2 óra csúszást" is nyáron, CEST idő
    // szerint). Ha ez a feltételezés mégsem lenne pontos, ezen az
    // egyetlen függvényen kell módosítani.
    //
    // A tényleges naptári napot (offsetDays) használjuk az eltolás
    // kiszámításához, így a nyári/téli időszámítás (DST) is helyesen
    // kezelve van, nem egy fix +2 órás eltolással dolgozunk.
    // ========================================================

    private fun convertUtcTimeToLocal(rawTime: String?, offsetDays: Int): String? {
        val trimmed = rawTime?.trim().orEmpty()
        val match = Regex("^(\\d{1,2}):(\\d{2})").find(trimmed) ?: return rawTime

        val hh = match.groupValues[1].toIntOrNull() ?: return rawTime
        val mm = match.groupValues[2].toIntOrNull() ?: return rawTime

        val dayMillis = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, offsetDays)
        }.timeInMillis

        val offsetMinutes = TimeZone.getDefault().getOffset(dayMillis) / 60000

        val total = (((hh * 60 + mm) + offsetMinutes) % 1440 + 1440) % 1440

        return "%02d:%02d".format(total / 60, total % 60)
    }

    // ========================================================
    // STÁTUSZ MAGYARÍTÁS
    // ========================================================

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

    // ========================================================
    // ÉLŐ / BEFEJEZETT STÁTUSZ FELISMERÉS
    // ========================================================

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

    // ========================================================
    // RENDEZÉSI KULCS
    // ========================================================

    private fun timeSortKey(status: String, isLive: Boolean): Int {
        if (isLive) return 0

        if (status == "Vége" || status == "Hosszabbítás" || status == "Tizenegyesek") {
            return 3000
        }

        val match = Regex("(\\d{1,2}):(\\d{2})").find(status)

        return if (match != null) {
            match.groupValues[1].toInt() * 60 + match.groupValues[2].toInt()
        } else {
            2000
        }
    }

    // ========================================================
    // FALLBACK ID (ha a StatPal nem ad mainId-t)
    // ========================================================

    private fun buildFallbackId(match: StatpalMatchItem, leagueName: String): String {
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
