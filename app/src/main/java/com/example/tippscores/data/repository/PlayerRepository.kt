package com.example.tippscores.data.repository

import com.example.tippscores.data.model.PlayerFull
import com.example.tippscores.data.model.PlayerStatRow
import com.example.tippscores.data.model.PlayerTransfer
import com.example.tippscores.data.model.TeamTrophy
import com.example.tippscores.data.remote.NetworkModule
import com.example.tippscores.data.util.JsonX
import com.example.tippscores.data.util.TtlCache
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException

/**
 * Teljes játékosprofil a StatPal "Get Player" végpontjából
 * (v2/soccer/players/{player_id}). 30 percig gyorsítótárazzuk.
 */
class PlayerRepository(
    private val statpalKeyProvider: () -> String
) {

    private val cache = TtlCache<PlayerFull>(maxEntries = 40)

    suspend fun fetch(playerId: String?): PlayerFull? {
        val id = playerId?.trim().orEmpty()
        val key = statpalKeyProvider().trim()
        if (id.isEmpty() || key.isEmpty()) return null

        cache.get(id, CACHE_MS)?.let { return it }

        return try {
            val root = NetworkModule.statpalMatchDetailsApi.getDetails(
                url = "https://statpal.io/api/v2/soccer/players/$id",
                accessKey = key
            )
            val player = parse(root, id)
            if (player != null) cache.put(id, player)
            player
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parse(root: JsonObject, id: String): PlayerFull? {
        val p = JsonX.obj(root.get("player")) ?: return null

        val seasons = JsonX.objects(JsonX.obj(p.get("club_league_statistics"))?.get("club"))
            .map { toRow(it) }

        val cups = (
            JsonX.objects(JsonX.obj(p.get("club_domestic_cup_statistics"))?.get("club")) +
                JsonX.objects(JsonX.obj(p.get("club_intl_cup_statistics"))?.get("club"))
            ).map { toRow(it) }

        val nationalObject = JsonX.obj(p.get("national_team_statistics"))
        val national = (
            JsonX.objects(nationalObject?.get("leagues")) +
                JsonX.objects(nationalObject?.get("league")) +
                JsonX.objects(nationalObject?.get("team"))
            ).map { toRow(it) }

        val overall = JsonX.obj(p.get("overall_club_statistics"))?.let {
            PlayerStatRow(title = "Klubkarrier összesen", stats = primitives(it))
        }

        val trophies = JsonX.objects(JsonX.obj(p.get("trophies"))?.get("trophy"))
            .mapNotNull { t ->
                val league = JsonX.text(t, "league") ?: return@mapNotNull null
                TeamTrophy(
                    league = league,
                    country = JsonX.text(t, "country"),
                    status = JsonX.text(t, "status"),
                    count = JsonX.text(t, "count"),
                    seasons = JsonX.text(t, "seasons")?.trim()?.trimEnd(',')?.trim()
                )
            }

        val transfers = mutableListOf<PlayerTransfer>()
        collectTransfers(p.get("transfers"), transfers)

        val sidelined = JsonX.objects(p.get("sidelined_history")).mapNotNull { describeSidelined(it) }

        return PlayerFull(
            id = JsonX.text(p, "id") ?: id,
            name = JsonX.text(p, "name") ?: "Ismeretlen játékos",
            firstName = JsonX.text(p, "firstname"),
            lastName = JsonX.text(p, "lastname"),
            age = JsonX.text(p, "age"),
            birthdate = JsonX.text(p, "birthdate"),
            birthplace = listOfNotNull(
                JsonX.text(p, "birthplace"),
                JsonX.text(p, "birthcountry")
            ).distinct().joinToString(", ").takeIf { it.isNotBlank() },
            nationality = JsonX.text(p, "nationality"),
            position = JsonX.text(p, "position"),
            height = JsonX.text(p, "height"),
            weight = JsonX.text(p, "weight"),
            preferredFoot = JsonX.text(p, "preferred_foot"),
            team = JsonX.text(p, "team"),
            teamId = JsonX.text(p, "team_id"),
            marketValueEur = JsonX.text(p, "market_value_eur")?.toLongOrNull(),
            seasons = seasons,
            cups = cups,
            national = national,
            overall = overall,
            trophies = trophies,
            transfers = transfers,
            sidelined = sidelined
        )
    }

    private fun primitives(o: JsonObject): Map<String, String> =
        o.entrySet()
            .filter { it.value.isJsonPrimitive }
            .associate { it.key.lowercase() to it.value.asString.trim() }

    private fun toRow(o: JsonObject): PlayerStatRow {
        val stats = primitives(o)
        val title = JsonX.text(o, "league", "competition", "league_name", "name") ?: "Mérkőzések"
        val subtitle = listOfNotNull(
            JsonX.text(o, "season"),
            JsonX.text(o, "team_name", "team")
        ).joinToString(" • ")

        return PlayerStatRow(
            title = title,
            subtitle = subtitle,
            teamId = JsonX.text(o, "team_id"),
            leagueId = JsonX.text(o, "league_id"),
            stats = stats
        )
    }

    /** Az átigazolások szerkezetét rugalmasan keressük: dátum + honnan / hova. */
    private fun collectTransfers(element: JsonElement?, out: MutableList<PlayerTransfer>) {
        if (element == null || element.isJsonNull) return
        when {
            element.isJsonArray -> element.asJsonArray.forEach { collectTransfers(it, out) }
            element.isJsonObject -> {
                val o = element.asJsonObject
                val from = JsonX.text(o, "from", "from_team", "team_from", "from_team_name")
                val to = JsonX.text(o, "to", "to_team", "team_to", "to_team_name")
                val date = JsonX.text(o, "date", "transfer_date")
                if (date != null && (from != null || to != null)) {
                    out += PlayerTransfer(
                        date = date,
                        from = from,
                        to = to,
                        type = JsonX.text(o, "type"),
                        price = JsonX.text(o, "price", "fee")
                    )
                } else {
                    o.entrySet().forEach { collectTransfers(it.value, out) }
                }
            }
        }
    }

    private fun describeSidelined(o: JsonObject): String? {
        val what = JsonX.text(o, "type", "injury", "reason", "description", "name")
        val start = JsonX.text(o, "start_date", "start", "from", "date")
        val end = JsonX.text(o, "end_date", "end", "to", "until")

        val period = when {
            start != null && end != null -> "$start – $end"
            start != null -> start
            end != null -> end
            else -> null
        }

        val text = listOfNotNull(what, period).joinToString(" • ")
        if (text.isNotBlank()) return text

        val all = primitives(o).values.filter { it.isNotBlank() }.joinToString(" • ")
        return all.takeIf { it.isNotBlank() }
    }

    private companion object {
        const val CACHE_MS = 30 * 60 * 1000L
    }
}
