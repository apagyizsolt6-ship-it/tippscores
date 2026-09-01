package com.example.tippscores.data.localization

import java.util.Locale

/**
 * Az 5 alapértelmezetten kiemelt ("top") bajnokság.
 *
 * Az országot ÉS a bajnokság nevét EGYÜTT nézzük, nem csak a liga
 * nevét - mert több országnak is van pl. "Bundesliga" (Ausztria) vagy
 * "Serie A" (Brazília: "Serie A Betano") nevű bajnoksága. Az egyeztetés
 * a StatPal EREDETI (fordítás előtti) angol szövegein történik, mert
 * azok konzisztensek - a magyarra fordított névre hagyatkozni törékeny
 * lenne (elírás vagy formázási eltérés esetén némán nem találna rá).
 */
object FeaturedLeagues {

    private data class Preset(
        val countryKey: String,
        val leagueKey: String
    )

    // A sorrend számít: ez lesz a kiemelt bajnokságok alapértelmezett
    // megjelenési sorrendje (amíg a felhasználó át nem rendezi/törli).
    private val presets = listOf(
        Preset("ENGLAND", "premierleague"),
        Preset("GERMANY", "bundesliga"),
        Preset("FRANCE", "ligue1"),
        Preset("ITALY", "seriea"),
        Preset("SPAIN", "laliga")
    )

    // Minden nem betű/szám karaktert (szóköz, kötőjel, stb.) eldobunk,
    // így "La Liga", "Laliga" és "LaLiga" is ugyanarra normalizálódik,
    // de pl. "Bundesliga 2" ("bundesliga2") nem keveredik a
    // "Bundesliga" ("bundesliga") alapbajnoksággal.
    private fun normalize(raw: String): String =
        raw.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }

    /**
     * A preset sorindexét adja vissza (0-4), vagy -1-et, ha a megadott
     * ország/bajnokság nem az 5 alapértelmezett kiemelt bajnokság egyike.
     */
    fun presetOrder(rawCountry: String, rawLeagueName: String): Int {

        val countryKey = rawCountry.trim().uppercase(Locale.ROOT)
        val leagueKey = normalize(rawLeagueName)

        return presets.indexOfFirst {
            it.countryKey == countryKey && it.leagueKey == leagueKey
        }
    }

    /**
     * Spanyolország élvonalánál a StatPal "Laliga" (szóköz nélkül) nevet
     * ad - ez itt szebb, szóközös "La Liga" formára cserélődik. Minden
     * más bajnokságnál null-t ad vissza (nincs felülírás).
     */
    fun displayNameOverride(rawCountry: String, rawLeagueName: String): String? {

        val countryKey = rawCountry.trim().uppercase(Locale.ROOT)
        val leagueKey = normalize(rawLeagueName)

        return if (countryKey == "SPAIN" && leagueKey == "laliga") {
            "La Liga"
        } else {
            null
        }
    }
}
