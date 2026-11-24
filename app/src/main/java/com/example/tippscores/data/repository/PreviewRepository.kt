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
import com.google.gson.JsonArray
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

    suspend fun fetch(matchId: String, isLive: Boolean): MatchPreview? {
        val key = statpalKeyProvider().trim()
        if (key.isEmpty() || matchId.isBlank()) return null

        val cacheKey = "$matchId:$isLive"
        cache.get(cacheKey, if (isLive) LIVE_TTL_MS else PRE_TTL_MS)?.let { return it }

        val preview = coroutineScope {
            val stories = async {
                firstJson(
                    key,
                    listOf(
                        "$BASE/live-storylines?match_id=$matchId",
                        "$BASE/matches/$matchId/live-storylines"
                    )
                )
            }
            val prediction = async {
                firstJson(key, listOf("$BASE/predictions?match_id=$matchId"))
            }
            val weather = async {
                firstJson(key, listOf("$BASE/weather-forecast?match_id=$matchId"))
            }
            val odds = async {
                val urls = if (isLive) {
                    listOf(
                        "$BASE/odds/live?match_id=$matchId",
                        "$BASE/matches/$matchId/live-odds"
                    )
                } else {
                    listOf(
                        "$BASE/odds/pre-match?match_id=$matchId",
                        "$BASE/matches/$matchId/odds",
                        "$BASE/odds?match_id=$matchId"
                    )
                }
                firstJson(key, urls)
            }

            MatchPreview(
                storylines = parseStorylines(stories.await()),
                prediction = parsePrediction(prediction.await()),
                weather = parseWeather(weather.await()),
                odds = parseOdds(odds.await(), isLive)
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
    // SZTORIK
    // ------------------------------------------------------------

    private fun parseStorylines(root: JsonObject?): List<Storyline> {
        if (root == null) return emptyList()
        val result = mutableListOf<Storyline>()
        JsonX.walkObjects(root) { o ->
            val text = JsonX.text(
                o, "text", "content", "body", "insight", "story",
                "narrative", "quote", "description"
            )
            if (text != null && text.length >= 25) {
                val title = JsonX.text(o, "title", "headline", "summary")
                    ?.takeIf { it != text }
                result += Storyline(
                    date = JsonX.text(o, "date", "published", "created_at"),
                    tag = JsonX.text(o, "type", "category", "kind", "label"),
                    title = title,
                    text = text
                )
            }
        }
        return result.distinctBy { it.text }.take(12)
    }

    // ------------------------------------------------------------
    // MODELL-TIPP
    // ------------------------------------------------------------

    private fun parsePrediction(root: JsonObject?): PredictionInfo? {
        if (root == null) return null
        val pick = JsonX.deepText(
            root, 3, "outcome", "pick", "model_pick", "prediction",
            "recommendation", "winner"
        ) ?: return null

        val reasoning = listOfNotNull(
            JsonX.deepText(root, 3, "reasoning", "analysis", "explanation", "rationale", "text", "summary")
        ).firstOrNull()

        return PredictionInfo(
            market = JsonX.deepText(root, 3, "market", "bet_type", "type"),
            pick = pick,
            odds = JsonX.toDouble(JsonX.deepText(root, 3, "odds", "price", "odd")),
            reasoning = reasoning
        )
    }

    // ------------------------------------------------------------
    // IDŐJÁRÁS
    // ------------------------------------------------------------

    private fun parseWeather(root: JsonObject?): WeatherInfo? {
        if (root == null) return null
        val temperature = JsonX.deepText(root, 3, "temperature", "temp", "temperature_c")
        val condition = JsonX.deepText(root, 3, "condition", "description", "summary", "weather")
        if (temperature == null && condition == null) return null

        return WeatherInfo(
            temperature = temperature,
            condition = condition,
            feelsLike = JsonX.deepText(root, 3, "feels_like", "feelslike", "apparent_temperature"),
            wind = JsonX.deepText(root, 3, "wind_speed", "windspeed", "wind"),
            rainChance = JsonX.deepText(
                root, 3, "rain_chance", "precipitation_probability", "precip_prob",
                "chance_of_rain", "rain"
            ),
            humidity = JsonX.deepText(root, 3, "humidity")
        )
    }

    // ------------------------------------------------------------
    // SZORZÓK
    // ------------------------------------------------------------

    private class RawOdd(
        val market: String,
        val label: String,
        val value: Double,
        val bookmaker: String?
    )

    /**
     * A szorzók pontos szerkezetét nem ismerjük, ezért két gyakori mintát
     * keresünk: (1) {"home": 1.9, "draw": 3.4, "away": 4.1} jellegű
     * objektumot, és (2) {"name": "Home", "odd": 1.9} elemekből álló
     * listát. Az eredményt piaconként összesítjük.
     */
    private fun parseOdds(root: JsonObject?, isLive: Boolean): OddsInfo? {
        if (root == null) return null

        val raw = mutableListOf<RawOdd>()
        collectOdds(root, null, null, raw)
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

    private fun collectOdds(
        element: JsonElement?,
        bookmaker: String?,
        marketName: String?,
        out: MutableList<RawOdd>
    ) {
        if (element == null || element.isJsonNull) return

        if (element.isJsonArray) {
            val array: JsonArray = element.asJsonArray
            if (collectLabelledArray(array, bookmaker, marketName, out)) return
            array.forEach { collectOdds(it, bookmaker, marketName, out) }
            return
        }

        if (!element.isJsonObject) return
        val o = element.asJsonObject

        val ownBookmaker = JsonX.text(o, "bookmaker", "bookie", "bookmaker_name", "provider")
            ?: bookmaker

        // Minta (1): {"home": 1.9, "draw": 3.4, "away": 4.1}
        val home = oddFrom(o, "home", "1")
        val draw = oddFrom(o, "draw", "x")
        val away = oddFrom(o, "away", "2")
        if (home != null && draw != null && away != null) {
            out += RawOdd(MARKET_1X2, "1", home, ownBookmaker)
            out += RawOdd(MARKET_1X2, "X", draw, ownBookmaker)
            out += RawOdd(MARKET_1X2, "2", away, ownBookmaker)
        }

        val ownMarket = JsonX.text(o, "market", "market_name", "bet", "bet_name") ?: marketName

        for ((key, value) in o.entrySet()) {
            var childBookmaker = ownBookmaker
            val normalizedKey = JsonX.norm(key)
            if ((normalizedKey.contains("bookmaker") || normalizedKey.contains("bookie")) &&
                value.isJsonObject
            ) {
                childBookmaker = JsonX.text(value.asJsonObject, "name", "title", "bookmaker") ?: ownBookmaker
            }
            val childMarket = if (value.isJsonArray || value.isJsonObject) {
                ownMarket ?: key
            } else {
                ownMarket
            }
            collectOdds(value, childBookmaker, childMarket, out)
        }
    }

    /** Minta (2): [{"name": "Home", "odd": "1.9"}, ...]. Igaz, ha felismerte. */
    private fun collectLabelledArray(
        array: JsonArray,
        bookmaker: String?,
        marketName: String?,
        out: MutableList<RawOdd>
    ): Boolean {
        val items = array.mapNotNull { JsonX.obj(it) }
        if (items.isEmpty() || items.size != array.size()) return false

        val pairs = items.mapNotNull { item ->
            val label = JsonX.text(item, "label", "outcome", "selection", "name", "value")
            val odd = JsonX.toDouble(JsonX.text(item, "odd", "odds", "price", "decimal"))
                ?: JsonX.toDouble(JsonX.text(item, "value"))?.takeIf { label != null && JsonX.text(item, "value") != label }
            if (label != null && odd != null && odd > 1.0) label.lowercase() to odd else null
        }
        if (pairs.size != items.size) return false

        val labels = pairs.map { it.first.replace(" ", "").replace(",", ".") }
        val market = marketName.orEmpty().lowercase()

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
            return true
        }

        val over = labels.indexOfFirst { it.startsWith("over") && it.contains("2.5") }
        val under = labels.indexOfFirst { it.startsWith("under") && it.contains("2.5") }
        if (over >= 0 && under >= 0) {
            out += RawOdd(MARKET_GOALS, "OVER", pairs[over].second, bookmaker)
            out += RawOdd(MARKET_GOALS, "UNDER", pairs[under].second, bookmaker)
            return true
        }

        val yes = labels.indexOf("yes")
        val no = labels.indexOf("no")
        if (yes >= 0 && no >= 0 &&
            (market.contains("both") || market.contains("btts") || market.contains("score"))
        ) {
            out += RawOdd(MARKET_BTTS, "YES", pairs[yes].second, bookmaker)
            out += RawOdd(MARKET_BTTS, "NO", pairs[no].second, bookmaker)
            return true
        }

        return false
    }

    private fun oddFrom(o: JsonObject, vararg keys: String): Double? {
        val value = JsonX.toDouble(JsonX.text(o, *keys))
        return value?.takeIf { it > 1.0 }
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

        const val MARKET_1X2 = "1X2"
        const val MARKET_GOALS = "GOALS"
        const val MARKET_BTTS = "BTTS"
    }
}
