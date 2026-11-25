package com.example.tippscores.data.repository

import com.example.tippscores.data.model.MatchPreview
import com.example.tippscores.data.model.OddsInfo
import com.example.tippscores.data.model.OddsMarket
import com.example.tippscores.data.model.OddsOutcome
import com.example.tippscores.data.model.PredictionInfo
import com.example.tippscores.data.model.Storyline
import com.example.tippscores.data.model.WeatherInfo
import com.example.tippscores.data.remote.NetworkModule
import com.example.tippscores.data.util.JsonX
import com.example.tippscores.data.util.TtlCache
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * A mérkőzés "Előzetes" füle: StatPal storylines, modell-tipp, időjárás
 * és fogadóirodai szorzók. Minden részt külön, hibatűrően kérünk le: ha
 * egy végpont nem elérhető (vagy az adott meccsre nincs adat), a többi
 * rész ettől még megjelenik.
 */
class PreviewRepository(
    private val statpalKeyProvider: () -> String
) {

    private val cache = TtlCache<MatchPreview>(maxEntries = 60)

    suspend fun fetch(matchId: String, leagueId: String?, isLive: Boolean): MatchPreview? {
        val key = statpalKeyProvider().trim()
        if (key.isEmpty() || matchId.isBlank()) return null

        val cacheKey = "$matchId:$isLive"
        cache.get(cacheKey, if (isLive) LIVE_TTL_MS else PRE_TTL_MS)?.let { return it }

        val preview = coroutineScope {
            val stories = async {
                firstJson(key, listOf("$BASE/live-storylines?match_id=$matchId"))
            }
            val prediction = async {
                firstJson(key, listOf("$BASE/predictions?match_id=$matchId"))
            }
            val weather = async {
                firstJson(key, listOf("$BASE/weather-forecast?match_id=$matchId"))
            }
            val odds = async { fetchLeagueOdds(key, leagueId) }

            val storyResult = parseStorylines(stories.await())

            MatchPreview(
                storylines = storyResult.storylines,
                rivalry = storyResult.rivalry,
                prediction = parsePrediction(prediction.await()),
                weather = parseWeather(weather.await()),
                odds = parseLeagueOdds(odds.await(), matchId, isLive)
            )
        }

        if (!preview.isEmpty) cache.put(cacheKey, preview)
        return preview
    }

    private suspend fun firstJson(key: String, urls: List<String>): JsonObject? {
        for (url in urls) {
            try {
                val json = NetworkModule.statpalMatchDetailsApi.getDetails(
                    url = url,
                    accessKey = key
                )
                if (json.entrySet().isNotEmpty()) return json
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
        return null
    }

    // ------------------------------------------------------------
    // SZTORIK  (live_storylines: match_context / home / away / rivalry)
    // ------------------------------------------------------------

    private class StoryResult(
        val storylines: List<Storyline>,
        val rivalry: String?
    )

    private fun parseStorylines(root: JsonObject?): StoryResult {
        val live = JsonX.obj(root?.get("live_storylines"))
            ?: return StoryResult(emptyList(), null)

        val meta = JsonX.obj(root?.get("meta"))
        val homeName = JsonX.text(JsonX.obj(meta?.get("home_team")), "name") ?: "Hazai csapat"
        val awayName = JsonX.text(JsonX.obj(meta?.get("away_team")), "name") ?: "Vendégcsapat"

        val result = mutableListOf<Storyline>()

        fun addSection(element: JsonElement?, label: String) {
            JsonX.objects(element).forEach { o ->
                val type = JsonX.text(o, "type")?.lowercase()
                if (type == "quote") {
                    val quote = JsonX.text(o, "quote", "text") ?: return@forEach
                    result += Storyline(
                        section = label,
                        isQuote = true,
                        text = quote,
                        speaker = JsonX.text(o, "speaker"),
                        context = JsonX.text(o, "context")
                    )
                } else {
                    val summary = JsonX.text(o, "summary", "text", "insight", "content")
                        ?: return@forEach
                    result += Storyline(section = label, text = summary)
                }
            }
        }

        addSection(live.get("match_context"), "Mérkőzés")
        addSection(live.get("home"), homeName)
        addSection(live.get("away"), awayName)

        val rivalryObject = JsonX.obj(live.get("rivalry"))
        val derbyName = JsonX.text(rivalryObject, "derby_name_localized", "derby_name")
        val rivalry = when {
            derbyName != null -> "Derbi: $derbyName"
            JsonX.text(rivalryObject, "is_named_derby") == "true" -> "Híres derbi"
            JsonX.text(rivalryObject, "same_city") == "true" -> "Városi rangadó"
            else -> null
        }

        return StoryResult(result.distinctBy { it.text }.take(16), rivalry)
    }

    // ------------------------------------------------------------
    // MODELL-TIPP  (prediction: choice / reasoning / pre_match_odds)
    // ------------------------------------------------------------

    private fun parsePrediction(root: JsonObject?): PredictionInfo? {
        val prediction = JsonX.obj(root?.get("prediction")) ?: return null
        val choice = JsonX.text(prediction, "choice", "outcome", "pick") ?: return null

        val odds = JsonX.obj(prediction.get("pre_match_odds"))
        val market = JsonX.text(odds, "market")
        val modifier = JsonX.text(odds, "modifier")

        val marketLabel = market?.let {
            val base = when (it.lowercase()) {
                "asian_handicap" -> "Ázsiai hendikep"
                "over_under", "goals_over_under" -> "Gólok száma"
                "1x2", "match_winner" -> "Mérkőzés kimenetele"
                "both_teams_to_score", "btts" -> "Mindkét csapat szerez gólt"
                else -> it.replace('_', ' ').replaceFirstChar { c -> c.uppercase() }
            }
            if (modifier != null) "$base ($modifier)" else base
        }

        val selection = JsonX.text(odds, "selection")?.let {
            when (it.lowercase()) {
                "home" -> "Hazai"
                "away" -> "Vendég"
                "draw" -> "Döntetlen"
                "over" -> "Több"
                "under" -> "Kevesebb"
                else -> it
            }
        }

        return PredictionInfo(
            market = marketLabel,
            selection = selection,
            pick = choice,
            odds = JsonX.toDouble(JsonX.text(odds, "odd", "odds")),
            reasoning = JsonX.text(prediction, "reasoning", "analysis", "explanation")
        )
    }

    // ------------------------------------------------------------
    // IDŐJÁRÁS  (location + forecast)
    // ------------------------------------------------------------

    private fun parseWeather(root: JsonObject?): WeatherInfo? {
        val forecast = JsonX.obj(root?.get("forecast")) ?: return null
        val location = JsonX.obj(root?.get("location"))

        val temperature = JsonX.text(forecast, "temp_celsius")?.let { "$it °C" }
        val condition = JsonX.text(forecast, "condition")
        if (temperature == null && condition == null) return null

        val windDirection = JsonX.text(forecast, "wind_direction")

        return WeatherInfo(
            place = listOfNotNull(
                JsonX.text(location, "city"),
                JsonX.text(location, "country")
            ).joinToString(", ").takeIf { it.isNotBlank() },
            localTime = JsonX.text(root, "match_time_local"),
            temperature = temperature,
            condition = condition,
            feelsLike = JsonX.text(forecast, "feels_like_celsius")?.let { "$it °C" },
            wind = JsonX.text(forecast, "wind_kmh")?.let {
                "$it km/h" + (windDirection?.let { d -> " $d" } ?: "")
            },
            rainChance = JsonX.text(forecast, "rain_chance_percent")?.let { "$it%" },
            humidity = JsonX.text(forecast, "humidity_percent")?.let { "$it%" },
            cloudCover = JsonX.text(forecast, "cloud_cover_percent")?.let { "$it%" },
            uvIndex = JsonX.text(forecast, "uv_index")
        )
    }

    // ------------------------------------------------------------
    // SZORZÓK  (leagues/{league_id}/odds/prematch)
    // ------------------------------------------------------------

    private class RawOdd(
        val market: String,
        val label: String,
        val value: Double,
        val bookmaker: String?
    )

    private val leagueOddsCache = TtlCache<JsonObject>(maxEntries = 4)

    /**
     * A StatPal egy bajnokság összes közelgő mérkőzésének szorzóit egyben adja
     * (30 percenként frissül), ezért a választ bajnokságonként gyorsítótárazzuk.
     */
    private suspend fun fetchLeagueOdds(key: String, leagueId: String?): JsonObject? {
        val id = leagueId?.trim().orEmpty()
        if (id.isEmpty()) return null

        leagueOddsCache.get(id, LEAGUE_ODDS_TTL_MS)?.let { return it }

        return try {
            val json = NetworkModule.statpalMatchDetailsApi.getDetails(
                url = "$BASE/leagues/$id/odds/prematch",
                accessKey = key
            )
            if (json.entrySet().isEmpty()) {
                null
            } else {
                leagueOddsCache.put(id, json)
                json
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private fun parseLeagueOdds(root: JsonObject?, matchId: String, isLive: Boolean): OddsInfo? {
        val league = JsonX.objects(JsonX.obj(root?.get("pre_match_odds"))?.get("league"))
            .firstOrNull() ?: return null

        val target = JsonX.objects(league.get("match")).firstOrNull { match ->
            listOf("main_id", "fallback_id_1", "fallback_id_2", "fallback_id_3")
                .any { JsonX.text(match, it) == matchId }
        } ?: return null

        val raw = mutableListOf<RawOdd>()

        JsonX.objects(target.get("odds")).forEach { market ->
            if (JsonX.text(market, "stop")?.lowercase() == "true") return@forEach

            val marketName = JsonX.text(market, "name").orEmpty()

            JsonX.objects(market.get("bookmaker")).forEach { bookmaker ->
                val bookmakerName = JsonX.text(bookmaker, "name")

                val pairs = JsonX.objects(bookmaker.get("odd")).mapNotNull { item ->
                    val label = JsonX.text(item, "name") ?: return@mapNotNull null
                    val line = JsonX.text(item, "total", "handicap", "line", "hcp")
                    val value = JsonX.toDouble(JsonX.text(item, "value", "odd", "odds"))
                        ?: return@mapNotNull null
                    if (value <= 1.0) return@mapNotNull null

                    val fullLabel = if (line != null && !label.contains(line)) "$label $line" else label
                    fullLabel.lowercase() to value
                }

                addPairs(pairs, marketName, bookmakerName, raw)
            }
        }

        return buildOddsInfo(raw, isLive)
    }

    /** Egy fogadóiroda egy piacának (név, szorzó) párjait a megfelelő piachoz rendeli. */
    private fun addPairs(
        pairs: List<Pair<String, Double>>,
        marketName: String,
        bookmaker: String?,
        out: MutableList<RawOdd>
    ) {
        if (pairs.isEmpty()) return

        val labels = pairs.map { it.first.replace(" ", "").replace(",", ".") }
        val market = marketName.lowercase()

        val has1 = labels.any { it == "home" || it == "1" }
        val hasX = labels.any { it == "draw" || it == "x" }
        val has2 = labels.any { it == "away" || it == "2" }
        if (has1 && hasX && has2) {
            pairs.forEachIndexed { index, pair ->
                val canonical = when (labels[index]) {
                    "home", "1" -> "1"
                    "draw", "x" -> "X"
                    "away", "2" -> "2"
                    else -> null
                }
                if (canonical != null) out += RawOdd(MARKET_1X2, canonical, pair.second, bookmaker)
            }
            return
        }

        val over = labels.indexOfFirst { it.startsWith("over") && it.contains("2.5") }
        val under = labels.indexOfFirst { it.startsWith("under") && it.contains("2.5") }
        if (over >= 0 && under >= 0) {
            out += RawOdd(MARKET_GOALS, "OVER", pairs[over].second, bookmaker)
            out += RawOdd(MARKET_GOALS, "UNDER", pairs[under].second, bookmaker)
            return
        }

        val yes = labels.indexOf("yes")
        val no = labels.indexOf("no")
        if (yes >= 0 && no >= 0 &&
            (market.contains("both") || market.contains("btts") || market.contains("score"))
        ) {
            out += RawOdd(MARKET_BTTS, "YES", pairs[yes].second, bookmaker)
            out += RawOdd(MARKET_BTTS, "NO", pairs[no].second, bookmaker)
        }
    }

    private fun buildOddsInfo(raw: List<RawOdd>, isLive: Boolean): OddsInfo? {
        if (raw.isEmpty()) return null

        val bookmakers = raw.mapNotNull { it.bookmaker }.distinct().size

        val markets = listOf(MARKET_1X2, MARKET_GOALS, MARKET_BTTS).mapNotNull { marketKey ->
            val rows = raw.filter { it.market == marketKey }
            if (rows.isEmpty()) return@mapNotNull null

            val grouped = rows.groupBy { it.label }
            val labels = when (marketKey) {
                MARKET_1X2 -> listOf("1", "X", "2")
                MARKET_GOALS -> listOf("OVER", "UNDER")
                else -> listOf("YES", "NO")
            }
            if (!labels.all { grouped.containsKey(it) }) return@mapNotNull null

            val averages = labels.map { label ->
                grouped.getValue(label).map { it.value }.average()
            }
            val inverse = averages.map { 1.0 / it }
            val inverseSum = inverse.sum()

            val outcomes = labels.mapIndexed { index, label ->
                val items = grouped.getValue(label)
                val best = items.maxByOrNull { it.value }
                OddsOutcome(
                    label = displayLabel(marketKey, label),
                    average = averages[index],
                    best = best?.value ?: averages[index],
                    bestBookmaker = best?.bookmaker,
                    probability = if (inverseSum > 0) inverse[index] / inverseSum else 0.0
                )
            }

            OddsMarket(
                title = when (marketKey) {
                    MARKET_1X2 -> "Mérkőzés kimenetele (1X2)"
                    MARKET_GOALS -> "Gólok száma (2,5)"
                    else -> "Mindkét csapat szerez gólt"
                },
                outcomes = outcomes
            )
        }

        if (markets.isEmpty()) return null
        return OddsInfo(isLive = isLive, bookmakerCount = bookmakers, markets = markets)
    }

    private fun displayLabel(market: String, label: String): String =
        when (market) {
            MARKET_1X2 -> when (label) {
                "1" -> "Hazai"
                "X" -> "Döntetlen"
                else -> "Vendég"
            }
            MARKET_GOALS -> if (label == "OVER") "Több mint 2,5" else "Kevesebb mint 2,5"
            else -> if (label == "YES") "Igen" else "Nem"
        }

    private companion object {
        const val BASE = "https://statpal.io/api/v2/soccer"
        const val PRE_TTL_MS = 10 * 60 * 1000L
        const val LIVE_TTL_MS = 30 * 1000L
        const val LEAGUE_ODDS_TTL_MS = 15 * 60 * 1000L

        const val MARKET_1X2 = "1X2"
        const val MARKET_GOALS = "GOALS"
        const val MARKET_BTTS = "BTTS"
    }
}
