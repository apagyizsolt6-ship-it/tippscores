package com.example.tippscores.data.model

/** Egy szezon / verseny statisztikasora (bajnokság, kupa, válogatott, összesített). */
data class PlayerStatRow(
    val title: String,
    val subtitle: String = "",
    val teamId: String? = null,
    val leagueId: String? = null,
    val stats: Map<String, String> = emptyMap()
) {
    fun text(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key -> stats[key]?.takeIf { it.isNotBlank() } }

    fun int(vararg keys: String): Int? =
        keys.firstNotNullOfOrNull { key -> stats[key]?.trim()?.toIntOrNull() }

    fun double(vararg keys: String): Double? =
        keys.firstNotNullOfOrNull { key -> stats[key]?.trim()?.replace(',', '.')?.toDoubleOrNull() }
}

/** A StatPal "Get Player" teljes, rugalmasan feldolgozott válasza. */
data class PlayerFull(
    val id: String,
    val name: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val age: String? = null,
    val birthdate: String? = null,
    val birthplace: String? = null,
    val nationality: String? = null,
    val position: String? = null,
    val height: String? = null,
    val weight: String? = null,
    val preferredFoot: String? = null,
    val team: String? = null,
    val teamId: String? = null,
    val marketValueEur: Long? = null,
    val seasons: List<PlayerStatRow> = emptyList(),
    val cups: List<PlayerStatRow> = emptyList(),
    val national: List<PlayerStatRow> = emptyList(),
    val overall: PlayerStatRow? = null,
    val trophies: List<TeamTrophy> = emptyList(),
    val transfers: List<PlayerTransfer> = emptyList(),
    val sidelined: List<String> = emptyList()
) {
    /** A játékos jelenlegi klubjához tartozó (legfrissebb) bajnoki sor. */
    val currentSeason: PlayerStatRow?
        get() = seasons.firstOrNull { it.teamId != null && it.teamId == teamId }
            ?: seasons.firstOrNull()
}
