package com.example.tippscores.data.repository

import com.example.tippscores.data.local.ApiPreferences
import com.example.tippscores.data.local.MatchDao
import com.example.tippscores.data.local.MatchEntity
import com.example.tippscores.data.local.toMatch
import com.example.tippscores.data.localization.CountryLocalizer
import com.example.tippscores.data.localization.FeaturedLeagues
import com.example.tippscores.data.localization.LeagueLocalizer
import com.example.tippscores.data.model.Match
import com.example.tippscores.data.remote.NetworkModule
import com.example.tippscores.data.remote.StatpalLeagueItem
import com.example.tippscores.data.remote.StatpalMatchItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import java.text.Normalizer
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class MatchRepository(
    private val matchDao: MatchDao,
    private val apiPreferences: ApiPreferences
) {

    val allMatches: Flow<List<Match>> =
        matchDao.getAllMatches().map { entities ->
            entities.map { it.toMatch() }
        }

    // ========================================================
    // NAPI MÉRKŐZÉSEK
    // ========================================================

    suspend fun fetchMatchesForOffset(offset: Int = 0) {

        val statpalKey =
            apiPreferences.statpalApiKey.trim()

        if (statpalKey.isEmpty()) {
            matchDao.clearMatches()
            throw IllegalStateException(
                "A StatPal API-kulcs nincs megadva."
            )
        }

        val safeOffset =
            offset.coerceIn(-7, 7)

        try {

            val previousById =
                snapshotPreviousMatches()

            matchDao.clearMatches()

            // ------------------------------------------------
            // 1. StatPal mérkőzések
            // ------------------------------------------------

            val response =
                NetworkModule.statpalApi.getDailyMatches(
                    offset = safeOffset,
                    accessKey = statpalKey
                )

            // ------------------------------------------------
            // 2. Highlightly csapatlogók
            //
            // A logók csak akkor kerülnek lekérésre, ha van
            // Highlightly API-kulcs.
            // ------------------------------------------------

            val logoMap =
                fetchHighlightlyTeamLogos(
                    offsetDays = safeOffset
                )

            // ------------------------------------------------
            // 3. StatPal + Highlightly összekapcsolása
            // ------------------------------------------------

            val entities =
                buildEntities(
                    leagues = response.data?.leagues.orEmpty(),
                    offsetDays = safeOffset,
                    forceLive = false,
                    previousById = previousById,
                    teamLogos = logoMap
                )

            val sortedEntities =
                entities.sortedWith(
                    compareBy<MatchEntity> {
                        timeSortKey(
                            it.status,
                            it.isLive
                        )
                    }
                        .thenBy {
                            it.leagueCountry
                        }
                        .thenBy {
                            it.leagueName
                        }
                        .thenBy {
                            it.homeTeam
                        }
                )

            if (sortedEntities.isNotEmpty()) {
                matchDao.insertMatches(
                    sortedEntities
                )
            }

            if (response.data == null) {
                throw IllegalStateException(
                    "A StatPal üres választ adott a kiválasztott napra."
                )
            }

        } catch (e: HttpException) {

            e.printStackTrace()

            matchDao.clearMatches()

            val detail =
                try {
                    e.response()
                        ?.errorBody()
                        ?.string()
                } catch (_: Exception) {
                    null
                }

            throw Exception(
                "Statpal API hiba (${e.code()}): " +
                    when (e.code()) {

                        401, 403 ->
                            "érvénytelen vagy inaktív API kulcs."

                        429 ->
                            "túllépted a napi/perces lekérési limitet."

                        else ->
                            detail?.takeIf {
                                it.isNotBlank()
                            } ?: e.message()
                    }
            )

        } catch (e: IOException) {

            e.printStackTrace()

            matchDao.clearMatches()

            throw Exception(
                "Nincs internetkapcsolat, vagy a szerver nem elérhető: ${e.message}"
            )

        } catch (e: Exception) {

            e.printStackTrace()

            matchDao.clearMatches()

            throw e
        }
    }

    // ========================================================
    // ÉLŐ MÉRKŐZÉSEK
    // ========================================================

    suspend fun fetchLiveMatches() {

        val statpalKey =
            apiPreferences.statpalApiKey.trim()

        if (statpalKey.isEmpty()) {
            matchDao.clearMatches()

            throw IllegalStateException(
                "A StatPal API-kulcs nincs megadva."
            )
        }

        try {

            val previousById =
                snapshotPreviousMatches()

            matchDao.clearMatches()

            // ------------------------------------------------
            // StatPal LIVE
            // ------------------------------------------------

            val response =
                NetworkModule.statpalApi.getLiveMatches(
                    statpalKey
                )

            // ------------------------------------------------
            // Highlightly logók
            //
            // Élő meccseknél a mai nap logóit kérjük le.
            // ------------------------------------------------

            val logoMap =
                fetchHighlightlyTeamLogos(
                    offsetDays = 0
                )

            val entities =
                buildEntities(
                    leagues = response.data?.leagues.orEmpty(),
                    offsetDays = 0,
                    forceLive = true,
                    previousById = previousById,
                    teamLogos = logoMap
                )

            if (entities.isNotEmpty()) {

                matchDao.insertMatches(
                    entities
                )
            }

        } catch (e: HttpException) {

            e.printStackTrace()

            matchDao.clearMatches()

            throw Exception(
                "Statpal API hiba (${e.code()}): " +
                    when (e.code()) {

                        401, 403 ->
                            "érvénytelen vagy inaktív API kulcs."

                        429 ->
                            "túllépted a napi/perces lekérési limitet."

                        else ->
                            e.message()
                    }
            )

        } catch (e: IOException) {

            e.printStackTrace()

            matchDao.clearMatches()

            throw Exception(
                "Nincs internetkapcsolat, vagy a szerver nem elérhető: ${e.message}"
            )

        } catch (e: Exception) {

            e.printStackTrace()

            matchDao.clearMatches()

            throw e
        }
    }

    // ========================================================
    // HIGHLIGHTLY LOGÓK BETÖLTÉSE
    // ========================================================
    //
    // A Highlightly /matches végpontja dátum szerint adja vissza
    // a meccseket.
    //
    // Egy oldal maximum 100 meccs lehet, ezért több oldalt is
    // lekérünk, amíg elfogynak a mérkőzések.
    //
    // A kulcs a normalizált csapatnév.
    //
    // Példa:
    //
    // "QPR"
    // "Queens Park Rangers"
    //
    // -> normalizált név alapján próbáljuk összekapcsolni.
    //
    // ========================================================

    private suspend fun fetchHighlightlyTeamLogos(
        offsetDays: Int
    ): Map<String, String> {

        val highlightlyKey =
            apiPreferences.highlightlyApiKey.trim()

        if (highlightlyKey.isEmpty()) {
            return emptyMap()
        }

        val date =
            dateForOffset(offsetDays)

        val result =
            mutableMapOf<String, String>()

        try {

            var offset = 0

            val pageSize = 100

            // Biztonsági korlát:
            // maximum 10 oldal / 1000 meccs.
            //
            // Egy napra ez bőven elegendő, és megakadályozza,
            // hogy hibás pagination esetén végtelen ciklus legyen.
            var pageCount = 0

            while (pageCount < 10) {

                val response =
                    NetworkModule.highlightlyApi.getMatches(
                        apiKey = highlightlyKey,
                        date = date,
                        timezone = "Europe/Budapest",
                        limit = pageSize,
                        offset = offset
                    )

                val matches =
                    response.data.orEmpty()

                if (matches.isEmpty()) {
                    break
                }

                for (highlightMatch in matches) {

                    val home =
                        highlightMatch.homeTeam

                    val away =
                        highlightMatch.awayTeam

                    val homeName =
                        home?.name?.trim().orEmpty()

                    val homeLogo =
                        home?.logo?.trim().orEmpty()

                    val awayName =
                        away?.name?.trim().orEmpty()

                    val awayLogo =
                        away?.logo?.trim().orEmpty()

                    if (
                        homeName.isNotEmpty() &&
                        homeLogo.isNotEmpty()
                    ) {

                        result[
                            normalizeTeamName(homeName)
                        ] = homeLogo
                    }

                    if (
                        awayName.isNotEmpty() &&
                        awayLogo.isNotEmpty()
                    ) {

                        result[
                            normalizeTeamName(awayName)
                        ] = awayLogo
                    }
                }

                // Ha kevesebb érkezett, mint a maximális
                // oldalméret, nincs több oldal.
                if (matches.size < pageSize) {
                    break
                }

                offset += pageSize
                pageCount++
            }

        } catch (e: Exception) {

            // A logó ne állítsa meg a teljes meccslistát.
            //
            // Ha a Highlightly hibázik, a StatPal meccsek
            // továbbra is megjelennek.
            e.printStackTrace()
        }

        return result
    }

    // ========================================================
    // DÁTUM OFFSET
    // ========================================================

    private fun dateForOffset(
        offsetDays: Int
    ): String {

        val calendar =
            Calendar.getInstance()

        calendar.add(
            Calendar.DAY_OF_YEAR,
            offsetDays
        )

        return String.format(
            Locale.US,
            "%04d-%02d-%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    // ========================================================
    // CSAPATNÉV NORMALIZÁLÁS
    // ========================================================

    private fun normalizeTeamName(
        value: String
    ): String {

        var text =
            value.trim().lowercase(Locale.getDefault())

        // Ékezetek eltávolítása
        text =
            Normalizer
                .normalize(
                    text,
                    Normalizer.Form.NFD
                )
                .replace(
                    Regex("\\p{InCombiningDiacriticalMarks}+"),
                    ""
                )

        // Gyakori jelek eltávolítása
        text =
            text.replace(
                Regex("[^a-z0-9]+"),
                ""
            )

        // Néhány gyakori elnevezési eltérés
        text =
            text.replace(
                "footballclub",
                ""
            )

        text =
            text.replace(
                "fc",
                ""
            )

        text =
            text.replace(
                "cf",
                ""
            )

        text =
            text.replace(
                "afc",
                ""
            )

        text =
            text.replace(
                "sc",
                ""
            )

        text =
            text.replace(
                "club",
                ""
            )

        return text
    }

    // ========================================================
    // LOGÓ FELKERESÉSE
    // ========================================================

    private fun findTeamLogo(
        teamName: String?,
        teamLogos: Map<String, String>
    ): String {

        val name =
            teamName?.trim().orEmpty()

        if (name.isEmpty()) {
            return ""
        }

        val normalized =
            normalizeTeamName(name)

        if (normalized.isEmpty()) {
            return ""
        }

        // ----------------------------------------------------
        // 1. Pontos egyezés
        // ----------------------------------------------------

        teamLogos[normalized]
            ?.takeIf { it.isNotBlank() }
            ?.let {
                return it
            }

        // ----------------------------------------------------
        // 2. Tartalmazásos egyezés
        //
        // Pl. kisebb névváltozás esetén.
        // ----------------------------------------------------

        val candidate =
            teamLogos.entries.firstOrNull { entry ->

                val key =
                    entry.key

                key.length >= 4 &&
                    normalized.length >= 4 &&
                    (
                        key.contains(normalized) ||
                            normalized.contains(key)
                        )
            }

        return candidate?.value?.takeIf {
            it.isNotBlank()
        } ?: ""
    }

    // ========================================================
    // ELŐZŐ ÁLLAPOT
    // ========================================================

    private suspend fun snapshotPreviousMatches():
        Map<String, MatchEntity> {

        return try {

            matchDao
                .getAllMatchesSnapshot()
                .associateBy {
                    it.id
                }

        } catch (e: Exception) {

            e.printStackTrace()

            emptyMap()
        }
    }

    // ========================================================
    // KÖZÖS FELDOLGOZÁS
    // ========================================================

    private fun buildEntities(
        leagues: List<StatpalLeagueItem>,
        offsetDays: Int,
        forceLive: Boolean,
        previousById: Map<String, MatchEntity>,
        teamLogos: Map<String, String>
    ): List<MatchEntity> {

        val result =
            mutableListOf<MatchEntity>()

        for (league in leagues) {

            val info =
                resolveLeagueInfo(league)

            for (match in league.matches.orEmpty()) {

                val isLive =
                    if (forceLive) {
                        true
                    } else {
                        isLiveStatus(
                            match.status
                        )
                    }

                val localTime =
                    convertUtcTimeToLocal(
                        match.time,
                        offsetDays
                    )

                val status =
                    displayStatus(
                        rawStatus = match.status,
                        localTime = localTime,
                        rawDate = match.date,
                        rawTime = match.time,
                        isLive = isLive
                    )

                val matchId =
                    match.mainId
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: buildFallbackId(
                            match,
                            info.leagueName
                        )

                val newHomeScore =
                    match.home
                        ?.goals
                        ?.toIntOrNull()

                val newAwayScore =
                    match.away
                        ?.goals
                        ?.toIntOrNull()

                val previous =
                    previousById[matchId]

                val homeJustScored =
                    previous != null &&
                        previous.homeScore != null &&
                        newHomeScore != null &&
                        newHomeScore >
                        previous.homeScore

                val awayJustScored =
                    previous != null &&
                        previous.awayScore != null &&
                        newAwayScore != null &&
                        newAwayScore >
                        previous.awayScore

                // ------------------------------------------------
                // LOGÓ
                //
                // Elsőként a StatPal saját logóját használjuk,
                // ha van.
                //
                // Ha nincs:
                // Highlightly alapján keressük.
                // ------------------------------------------------

                val homeLogo =
                    match.home
                        ?.logoUrl
                        ?.trim()
                        ?.takeIf {
                            it.isNotEmpty()
                        }
                        ?: findTeamLogo(
                            match.home?.name,
                            teamLogos
                        )

                val awayLogo =
                    match.away
                        ?.logoUrl
                        ?.trim()
                        ?.takeIf {
                            it.isNotEmpty()
                        }
                        ?: findTeamLogo(
                            match.away?.name,
                            teamLogos
                        )

                result.add(
                    MatchEntity(
                        id = matchId,

                        leagueName =
                            info.leagueName,

                        leagueCountry =
                            info.country,

                        leagueCountryFlag =
                            info.flag,

                        presetOrder =
                            info.presetOrder,

                        homeTeam =
                            match.home
                                ?.name
                                ?.trim()
                                ?.takeUnless {
                                    it.isEmpty()
                                }
                                ?: "Hazai",

                        homeTeamLogo =
                            homeLogo,

                        awayTeam =
                            match.away
                                ?.name
                                ?.trim()
                                ?.takeUnless {
                                    it.isEmpty()
                                }
                                ?: "Vendég",

                        awayTeamLogo =
                            awayLogo,

                        homeScore =
                            newHomeScore,

                        awayScore =
                            newAwayScore,

                        homeJustScored =
                            homeJustScored,

                        awayJustScored =
                            awayJustScored,

                        status =
                            status,

                        isLive =
                            isLive,

                        tipPrediction =
                            null,

                        tipConfidence =
                            null,

                        hasVideoHighlight =
                            false,

                        videoUrl =
                            null
                    )
                )
            }
        }

        return result
    }

    // ========================================================
    // BAJNOKSÁG / ORSZÁG
    // ========================================================

    private data class ResolvedLeagueInfo(
        val country: String,
        val flag: String,
        val leagueName: String,
        val presetOrder: Int
    )

    private fun resolveLeagueInfo(
        league: StatpalLeagueItem
    ): ResolvedLeagueInfo {

        val countryAndLeague =
            league.name
                ?.trim()
                ?.takeUnless {
                    it.isEmpty()
                }
                ?: "Bajnokság"

        val parts =
            countryAndLeague.split(
                ":",
                limit = 2
            )

        val rawCountry =
            parts.getOrNull(0)
                ?.trim()
                ?.uppercase(
                    Locale.getDefault()
                )
                ?.takeUnless {
                    it.isEmpty()
                }
                ?: league.country
                    ?.trim()
                    ?.uppercase(
                        Locale.getDefault()
                    )
                    ?.takeUnless {
                        it.isEmpty()
                    }
                ?: "FOCI"

        val rawLeagueName =
            parts.getOrNull(1)
                ?.trim()
                ?.takeUnless {
                    it.isEmpty()
                }
                ?: countryAndLeague

        val country =
            CountryLocalizer.hungarianName(
                rawCountry
            )

        val flag =
            CountryLocalizer.flagEmoji(
                rawCountry
            )

        val leagueName =
            FeaturedLeagues.displayNameOverride(
                rawCountry,
                rawLeagueName
            )
                ?: LeagueLocalizer.hungarianLeagueName(
                    rawLeagueName
                )

        val presetOrder =
            FeaturedLeagues.presetOrder(
                rawCountry,
                rawLeagueName
            )

        return ResolvedLeagueInfo(
            country = country,
            flag = flag,
            leagueName = leagueName,
            presetOrder = presetOrder
        )
    }

    // ========================================================
    // UTC -> HELYI IDŐ
    // ========================================================

    private fun convertUtcTimeToLocal(
        rawTime: String?,
        offsetDays: Int
    ): String? {

        val trimmed =
            rawTime?.trim().orEmpty()

        val match =
            Regex(
                "^(\\d{1,2}):(\\d{2})"
            ).find(trimmed)
                ?: return rawTime

        val hh =
            match.groupValues[1]
                .toIntOrNull()
                ?: return rawTime

        val mm =
            match.groupValues[2]
                .toIntOrNull()
                ?: return rawTime

        val dayMillis =
            Calendar.getInstance().apply {
                add(
                    Calendar.DAY_OF_YEAR,
                    offsetDays
                )
            }.timeInMillis

        val offsetMinutes =
            TimeZone
                .getDefault()
                .getOffset(dayMillis) / 60000

        val total =
            (
                (
                    hh * 60 +
                        mm +
                        offsetMinutes
                    ) % 1440 + 1440
                ) % 1440

        return "%02d:%02d".format(
            total / 60,
            total % 60
        )
    }

    // ========================================================
    // STÁTUSZ
    // ========================================================

    private fun displayStatus(
        rawStatus: String?,
        localTime: String?,
        rawDate: String?,
        rawTime: String?,
        isLive: Boolean
    ): String {

        val normalized =
            rawStatus
                ?.trim()
                ?.uppercase(
                    Locale.getDefault()
                )
                .orEmpty()

        val normalizedTime =
            localTime
                ?.trim()
                .orEmpty()

        return when {

            normalized.contains("POST") ||
                normalized.contains("POSTP") ->
                "Elhalasztva"

            normalized.contains("CANCEL") ->
                "Törölve"

            normalized == "AET" ->
                "Hosszabbítás"

            normalized == "PEN" ->
                "Tizenegyesek"

            normalized == "FT" ||
                normalized == "FINISHED" ->
                "Vége"

            normalized == "HT" ->
                "SZÜNET"

            isLive -> {

                extractLiveMinute(
                    normalized
                )?.let {
                    "$it'"
                }
                    ?: calculateLiveMinute(
                        normalizedTime
                    )
                    ?: "1'"
            }

            normalizedTime.isNotEmpty() ->
                normalizedTime

            normalized.isNotEmpty() ->
                normalized

            else ->
                "–"
        }
    }

    // ========================================================
    // ÉLŐ PERC
    // ========================================================

    private fun extractLiveMinute(
        status: String
    ): Int? {

        val match =
            Regex(
                "^(\\d{1,3})'?"
            ).find(status)
                ?: return null

        return match
            .groupValues[1]
            .toIntOrNull()
            ?.coerceIn(
                1,
                130
            )
    }

    private fun calculateLiveMinute(
        localTime: String?
    ): String? {

        val timeMatch =
            Regex(
                "^(\\d{1,2}):(\\d{2})"
            ).find(
                localTime
                    ?.trim()
                    .orEmpty()
            )
                ?: return null

        val hour =
            timeMatch
                .groupValues[1]
                .toIntOrNull()
                ?: return null

        val minute =
            timeMatch
                .groupValues[2]
                .toIntOrNull()
                ?: return null

        val calendar =
            Calendar.getInstance().apply {

                set(
                    Calendar.HOUR_OF_DAY,
                    hour
                )

                set(
                    Calendar.MINUTE,
                    minute
                )

                set(
                    Calendar.SECOND,
                    0
                )

                set(
                    Calendar.MILLISECOND,
                    0
                )
            }

        val elapsedMinutes =
            (
                (
                    System.currentTimeMillis() -
                        calendar.timeInMillis
                    ) / 60000L
                )
                .toInt()

        if (
            elapsedMinutes < 1 ||
            elapsedMinutes > 130
        ) {
            return null
        }

        return elapsedMinutes
            .coerceIn(
                1,
                130
            )
            .toString() + "'"
    }

    // ========================================================
    // ÉLŐ STÁTUSZ
    // ========================================================

    private fun isLiveStatus(
        status: String?
    ): Boolean {

        val normalized =
            status
                ?.trim()
                ?.uppercase(
                    Locale.getDefault()
                )
                .orEmpty()

        if (normalized.isEmpty()) {
            return false
        }

        return normalized.contains("LIVE") ||
            normalized == "1H" ||
            normalized == "2H" ||
            normalized == "HT" ||
            normalized == "ET" ||
            normalized.matches(
                Regex("\\d{1,3}'?")
            )
    }

    // ========================================================
    // BEFEJEZETT
    // ========================================================

    private fun isFinishedStatus(
        status: String?
    ): Boolean {

        val normalized =
            status
                ?.trim()
                ?.uppercase(
                    Locale.getDefault()
                )
                .orEmpty()

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

    private fun timeSortKey(
        status: String,
        isLive: Boolean
    ): Int {

        if (isLive) {
            return 0
        }

        if (
            status == "Vége" ||
            status == "Hosszabbítás" ||
            status == "Tizenegyesek"
        ) {
            return 3000
        }

        val match =
            Regex(
                "(\\d{1,2}):(\\d{2})"
            ).find(status)

        return if (match != null) {

            match.groupValues[1]
                .toInt() * 60 +
                match.groupValues[2]
                    .toInt()

        } else {
            2000
        }
    }

    // ========================================================
    // FALLBACK ID
    // ========================================================

    private fun buildFallbackId(
        match: StatpalMatchItem,
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
        ).joinToString("|")
            .hashCode()
            .toString()
    }
}
