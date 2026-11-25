package com.example.tippscores.data.util

import java.util.Locale

/**
 * Gyakori angol kifejezések szótára (poszt, időjárás, átigazolás típusa,
 * trófea-státusz...). Ezeket nem kell fordítani, azonnal, pontosan megvannak.
 * Ami nincs benne, azt a [HuTranslator] fordítja le a háttérben.
 */
object HuText {

    private val exact: Map<String, String> = mapOf(
        // posztok
        "goalkeeper" to "Kapus", "defender" to "Védő", "midfielder" to "Középpályás",
        "attacker" to "Támadó", "forward" to "Csatár", "striker" to "Csatár",
        "winger" to "Szélső", "centre-back" to "Középhátvéd",
        // időjárás
        "clear" to "Derült", "sunny" to "Napos", "mostly sunny" to "Többnyire napos",
        "partly cloudy" to "Részben felhős", "cloudy" to "Felhős", "overcast" to "Borult",
        "rain" to "Eső", "light rain" to "Gyenge eső", "heavy rain" to "Erős eső",
        "drizzle" to "Szitálás", "light drizzle" to "Gyenge szitálás", "snow" to "Havazás",
        "light snow" to "Gyenge havazás", "heavy snow" to "Erős havazás", "fog" to "Köd",
        "mist" to "Pára", "thunderstorm" to "Zivatar", "showers" to "Zápor",
        "light showers" to "Gyenge zápor", "windy" to "Szeles",
        // trófea-státusz
        "winner" to "Győztes", "runner-up" to "Második", "runner up" to "Második",
        "finalist" to "Döntős",
        // átigazolás típusa
        "loan" to "Kölcsönadás", "return from loan" to "Visszatért a kölcsönből",
        "end of loan" to "A kölcsön lejárt", "free" to "Szabadon", "free transfer" to "Szabad átigazolás",
        "transfer" to "Átigazolás", "n/a" to "–",
        // pálya
        "grass" to "Füves", "artificial" to "Műfüves", "artificial turf" to "Műfüves",
        // események
        "normal goal" to "Gól", "goal" to "Gól", "penalty" to "Tizenegyes",
        "own goal" to "Öngól", "yellow card" to "Sárga lap", "red card" to "Piros lap",
        "second yellow card" to "Második sárga lap", "substitution" to "Csere",
        "missed penalty" to "Kihagyott tizenegyes", "var" to "VAR",
        // modell-tipp
        "draw" to "Döntetlen", "home win" to "Hazai győzelem", "away win" to "Vendég győzelem"
    )

    private val months = mapOf(
        "january" to "január", "february" to "február", "march" to "március",
        "april" to "április", "may" to "május", "june" to "június", "july" to "július",
        "august" to "augusztus", "september" to "szeptember", "october" to "október",
        "november" to "november", "december" to "december"
    )

    private val phraseReplacements = listOf(
        "Relegation play-offs" to "Kiesési rájátszás",
        "Promotion play-offs" to "Feljutási rájátszás",
        "Promotion" to "Feljutás",
        "Relegation" to "Kiesés",
        "Champions League" to "Bajnokok Ligája",
        "Europa League" to "Európa-liga",
        "Conference League" to "Konferencia-liga",
        "Play-offs" to "Rájátszás",
        "Playoffs" to "Rájátszás"
    )

    private val toWin = Regex("^(.+?) to win$", RegexOption.IGNORE_CASE)
    private val monthYear = Regex("^([A-Za-z]+)\\s+(\\d{4})$")

    /** Azonnali, szótár alapú fordítás; null, ha nincs találat. */
    fun lookup(text: String): String? {
        val t = text.trim()
        if (t.isEmpty()) return null

        exact[t.lowercase(Locale.ROOT)]?.let { return it }

        toWin.find(t)?.let { return "${it.groupValues[1]} győz" }

        monthYear.find(t)?.let { match ->
            val month = months[match.groupValues[1].lowercase(Locale.ROOT)]
            if (month != null) return "${match.groupValues[2]}. $month"
        }

        if (t.length <= 80) {
            var replaced = t
            var changed = false
            phraseReplacements.forEach { (from, to) ->
                if (replaced.contains(from, ignoreCase = true)) {
                    replaced = replaced.replace(from, to, ignoreCase = true)
                    changed = true
                }
            }
            if (changed) return replaced
        }

        return null
    }

    /** Igaz, ha a szöveg már valószínűleg magyar (ékezetes betűk aránya). */
    fun looksHungarian(text: String): Boolean {
        val letters = text.count { it.isLetter() }
        if (letters == 0) return true
        val hungarian = text.count { it in "áéíóöőúüűÁÉÍÓÖŐÚÜŰ" }
        return hungarian * 100 / letters >= 4
    }
}
