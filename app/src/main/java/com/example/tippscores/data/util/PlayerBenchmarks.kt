package com.example.tippscores.data.util

import com.example.tippscores.data.model.LeaguePlayerStat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * A játékos mutatóit a bajnokság azonos posztú, legalább 270 percet játszó
 * játékosaihoz hasonlítja (percentilis: hány százalékuknál jobb).
 */
object PlayerBenchmarks {

    enum class Role { GOALKEEPER, DEFENDER, MIDFIELDER, FORWARD }

    class Metric(
        val label: String,
        val display: String,
        val percentile: Int
    )

    private class Definition(
        val label: String,
        val higherIsBetter: Boolean,
        val formatAsPercent: Boolean,
        val value: (LeaguePlayerStat) -> Double?
    )

    fun roleOf(position: String?): Role {
        val p = position?.trim()?.lowercase(Locale.ROOT).orEmpty()
        return when {
            p == "g" || p == "gk" || p.contains("keeper") || p.contains("kapus") -> Role.GOALKEEPER
            p == "d" || p.contains("defen") || p.contains("back") || p.contains("véd") -> Role.DEFENDER
            p == "m" || p.contains("mid") || p.contains("közép") -> Role.MIDFIELDER
            else -> Role.FORWARD
        }
    }

    private fun per90(count: Int, minutes: Int): Double? =
        if (minutes <= 0) null else count * 90.0 / minutes

    private fun ratio(part: Int, total: Int, minimumTotal: Int): Double? =
        if (total < minimumTotal || part > total) null else part * 100.0 / total

    private fun definitions(role: Role): List<Definition> =
        when (role) {
            Role.FORWARD -> listOf(
                Definition("Gól / 90", true, false) { per90(it.goals, it.minutes) },
                Definition("Gólpassz / 90", true, false) { per90(it.assists, it.minutes) },
                Definition("Lövés / 90", true, false) { per90(it.shotsTotal, it.minutes) },
                Definition("Kapura lövés", true, true) { ratio(it.shotsOnTarget, it.shotsTotal, 8) },
                Definition("Kulcspassz / 90", true, false) { per90(it.keyPasses, it.minutes) },
                Definition("Cselezés", true, true) { ratio(it.dribbleSuccess, it.dribbleAttempts, 8) }
            )
            Role.MIDFIELDER -> listOf(
                Definition("Gól / 90", true, false) { per90(it.goals, it.minutes) },
                Definition("Gólpassz / 90", true, false) { per90(it.assists, it.minutes) },
                Definition("Kulcspassz / 90", true, false) { per90(it.keyPasses, it.minutes) },
                Definition("Szerelés / 90", true, false) { per90(it.tackles, it.minutes) },
                Definition("Párharc", true, true) { ratio(it.duelsWon, it.duelsTotal, 15) },
                Definition("Passzpontosság", true, true) { ratio(it.passSuccess, it.passAttempts, 40) }
            )
            Role.DEFENDER -> listOf(
                Definition("Szerelés / 90", true, false) { per90(it.tackles, it.minutes) },
                Definition("Labdaszerzés / 90", true, false) { per90(it.interceptions, it.minutes) },
                Definition("Felszabadítás / 90", true, false) { per90(it.clearances, it.minutes) },
                Definition("Párharc", true, true) { ratio(it.duelsWon, it.duelsTotal, 15) },
                Definition("Passzpontosság", true, true) { ratio(it.passSuccess, it.passAttempts, 40) },
                Definition("Gól+assziszt / 90", true, false) { per90(it.goals + it.assists, it.minutes) }
            )
            Role.GOALKEEPER -> listOf(
                Definition("Védés / 90", true, false) { per90(it.saves, it.minutes) },
                Definition("Kapott gól / 90", false, false) { per90(it.goalsConceded, it.minutes) },
                Definition("Védési arány", true, true) {
                    ratio(it.saves, it.saves + it.goalsConceded, 10)
                },
                Definition("Passzpontosság", true, true) { ratio(it.passSuccess, it.passAttempts, 40) }
            )
        }

    /**
     * A bajnokság azonos posztú játékosai közül a mutatók alapján legjobban
     * hasonlókat adja vissza (a mutatókat a társak szórásával normalizálva).
     */
    fun similar(
        target: LeaguePlayerStat,
        all: List<LeaguePlayerStat>,
        limit: Int = 5
    ): List<LeaguePlayerStat> {
        if (target.minutes < 270) return emptyList()

        val role = roleOf(target.position)
        val peers = all.filter {
            roleOf(it.position) == role && it.minutes >= 270 && it.playerId != target.playerId
        }
        if (peers.size < 8) return emptyList()

        val defs = definitions(role)

        // Átlag és szórás mutatónként
        val stats = defs.map { def ->
            val values = peers.mapNotNull { def.value(it) }
            if (values.size < 8) {
                null
            } else {
                val mean = values.average()
                val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
                val deviation = kotlin.math.sqrt(variance).takeIf { it > 1e-9 } ?: 1.0
                mean to deviation
            }
        }

        val targetValues = defs.map { it.value(target) }

        return peers
            .mapNotNull { peer ->
                var sum = 0.0
                var used = 0
                defs.forEachIndexed { index, def ->
                    val stat = stats[index] ?: return@forEachIndexed
                    val own = targetValues[index] ?: return@forEachIndexed
                    val other = def.value(peer) ?: return@forEachIndexed
                    val diff = (own - other) / stat.second
                    sum += diff * diff
                    used++
                }
                if (used < 3) null else peer to kotlin.math.sqrt(sum / used)
            }
            .sortedBy { it.second }
            .take(limit)
            .map { it.first }
    }

    /** Üres lista, ha kevés az adat (kevés játszott perc vagy kevés összehasonlítható társ). */
    fun metrics(target: LeaguePlayerStat, all: List<LeaguePlayerStat>): List<Metric> {
        if (target.minutes < 180) return emptyList()

        val role = roleOf(target.position)
        val peers = all.filter { roleOf(it.position) == role && it.minutes >= 270 }
        if (peers.size < 8) return emptyList()

        return definitions(role).mapNotNull { definition ->
            val own = definition.value(target) ?: return@mapNotNull null
            val values = peers.mapNotNull { definition.value(it) }
            if (values.size < 8) return@mapNotNull null

            val lower = values.count { it < own }
            val equal = values.count { it == own }
            var percentile = ((lower + equal * 0.5) * 100.0 / values.size).roundToInt()
            if (!definition.higherIsBetter) percentile = 100 - percentile

            val display = if (definition.formatAsPercent) {
                String.format(Locale.US, "%.0f%%", own)
            } else {
                String.format(Locale.US, "%.2f", own)
            }

            Metric(definition.label, display, percentile.coerceIn(0, 100))
        }
    }
}
