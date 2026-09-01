package com.example.tippscores.data.localization

import java.util.Locale

/**
 * Egy ország adatai a magyarosításhoz: magyar név + ISO 3166-1 alpha-2 kód
 * (ez utóbbiból generáljuk a zászló emojit).
 */
private data class CountryInfo(val hu: String, val iso2: String)

/**
 * Országnevek magyarra fordítása + zászló emoji hozzárendelése.
 *
 * A StatPal országneveket angolul, nagybetűvel adja vissza (pl. "AUSTRALIA").
 * Ha egy ország nincs benne a szótárban (elvileg 1000+ bajnokság van a
 * StatPal-nál, nem lehet mindet lefedni), a névre egy szép Title Case
 * formázást alkalmazunk eredeti (angol) nyelven, a zászló helyén pedig
 * a 🏆 jelenik meg tartalék ikonként - így semmi nem törik el, csak
 * nem lesz lefordítva/zászlózva az adott ritka ország.
 */
object CountryLocalizer {

    private val countries: Map<String, CountryInfo> = buildMap {
        fun put(hu: String, iso2: String, vararg keys: String) {
            keys.forEach { key -> this[key.uppercase(Locale.ROOT)] = CountryInfo(hu, iso2) }
        }

        // --- Európa ---
        put("Albánia", "AL", "ALBANIA")
        put("Andorra", "AD", "ANDORRA")
        put("Ausztria", "AT", "AUSTRIA")
        put("Fehéroroszország", "BY", "BELARUS")
        put("Belgium", "BE", "BELGIUM")
        put("Bosznia-Hercegovina", "BA", "BOSNIA AND HERZEGOVINA", "BOSNIA & HERZEGOVINA", "BOSNIA")
        put("Bulgária", "BG", "BULGARIA")
        put("Horvátország", "HR", "CROATIA")
        put("Ciprus", "CY", "CYPRUS")
        put("Csehország", "CZ", "CZECH REPUBLIC", "CZECHIA")
        put("Dánia", "DK", "DENMARK")
        put("Észtország", "EE", "ESTONIA")
        put("Feröer-szigetek", "FO", "FAROE ISLANDS")
        put("Finnország", "FI", "FINLAND")
        put("Franciaország", "FR", "FRANCE")
        put("Grúzia", "GE", "GEORGIA")
        put("Németország", "DE", "GERMANY")
        put("Gibraltár", "GI", "GIBRALTAR")
        put("Görögország", "GR", "GREECE")
        put("Magyarország", "HU", "HUNGARY")
        put("Izland", "IS", "ICELAND")
        put("Írország", "IE", "IRELAND", "REPUBLIC OF IRELAND", "EIRE")
        put("Olaszország", "IT", "ITALY")
        put("Koszovó", "XK", "KOSOVO")
        put("Lettország", "LV", "LATVIA")
        put("Liechtenstein", "LI", "LIECHTENSTEIN")
        put("Litvánia", "LT", "LITHUANIA")
        put("Luxemburg", "LU", "LUXEMBOURG")
        put("Málta", "MT", "MALTA")
        put("Moldova", "MD", "MOLDOVA")
        put("Monaco", "MC", "MONACO")
        put("Montenegró", "ME", "MONTENEGRO")
        put("Hollandia", "NL", "NETHERLANDS")
        put("Észak-Macedónia", "MK", "NORTH MACEDONIA", "MACEDONIA", "FYROM")
        put("Norvégia", "NO", "NORWAY")
        put("Lengyelország", "PL", "POLAND")
        put("Portugália", "PT", "PORTUGAL")
        put("Románia", "RO", "ROMANIA")
        put("Oroszország", "RU", "RUSSIA")
        put("San Marino", "SM", "SAN MARINO")
        put("Szerbia", "RS", "SERBIA")
        put("Szlovákia", "SK", "SLOVAKIA")
        put("Szlovénia", "SI", "SLOVENIA")
        put("Spanyolország", "ES", "SPAIN")
        put("Svédország", "SE", "SWEDEN")
        put("Svájc", "CH", "SWITZERLAND")
        put("Törökország", "TR", "TURKEY", "TURKIYE", "TÜRKIYE")
        put("Ukrajna", "UA", "UKRAINE")
        put("Örményország", "AM", "ARMENIA")
        put("Azerbajdzsán", "AZ", "AZERBAIJAN")
        put("Kazahsztán", "KZ", "KAZAKHSTAN")
        // Brit "nemzetek" (külön válogatottak / bajnokságok, de a Unicode nem
        // támogat egyedi Anglia/Skócia/Wales zászlót biztonságosan minden
        // eszközön - ezért itt az Egyesült Királyság zászlóját kapják).
        put("Anglia", "GB", "ENGLAND")
        put("Skócia", "GB", "SCOTLAND")
        put("Wales", "GB", "WALES")
        put("Észak-Írország", "GB", "NORTHERN IRELAND")
        put("Egyesült Királyság", "GB", "UNITED KINGDOM")

        // --- Amerika ---
        put("Argentína", "AR", "ARGENTINA")
        put("Bolívia", "BO", "BOLIVIA")
        put("Brazília", "BR", "BRAZIL")
        put("Kanada", "CA", "CANADA")
        put("Chile", "CL", "CHILE")
        put("Kolumbia", "CO", "COLOMBIA")
        put("Costa Rica", "CR", "COSTA RICA")
        put("Kuba", "CU", "CUBA")
        put("Dominikai Köztársaság", "DO", "DOMINICAN REPUBLIC")
        put("Ecuador", "EC", "ECUADOR")
        put("Salvador", "SV", "EL SALVADOR")
        put("Guatemala", "GT", "GUATEMALA")
        put("Honduras", "HN", "HONDURAS")
        put("Jamaica", "JM", "JAMAICA")
        put("Mexikó", "MX", "MEXICO")
        put("Nicaragua", "NI", "NICARAGUA")
        put("Panama", "PA", "PANAMA")
        put("Paraguay", "PY", "PARAGUAY")
        put("Peru", "PE", "PERU")
        put("Uruguay", "UY", "URUGUAY")
        put("USA", "US", "USA", "UNITED STATES", "UNITED STATES OF AMERICA")
        put("Venezuela", "VE", "VENEZUELA")
        put("Trinidad és Tobago", "TT", "TRINIDAD AND TOBAGO")
        put("Haiti", "HT", "HAITI")
        put("Guyana", "GY", "GUYANA")
        put("Suriname", "SR", "SURINAME")
        put("Belize", "BZ", "BELIZE")
        put("Bahama-szigetek", "BS", "BAHAMAS")
        put("Barbados", "BB", "BARBADOS")
        put("Grenada", "GD", "GRENADA")
        put("Dominika", "DM", "DOMINICA")
        put("Antigua és Barbuda", "AG", "ANTIGUA AND BARBUDA")
        put("Curaçao", "CW", "CURACAO", "CURAÇAO")
        put("Aruba", "AW", "ARUBA")
        put("Puerto Rico", "PR", "PUERTO RICO")
        put("Bermuda", "BM", "BERMUDA")

        // --- Ázsia / Óceánia (labdarúgásban az AFC-hez tartozik Ausztrália is) ---
        put("Afganisztán", "AF", "AFGHANISTAN")
        put("Ausztrália", "AU", "AUSTRALIA")
        put("Bahrein", "BH", "BAHRAIN")
        put("Banglades", "BD", "BANGLADESH")
        put("Kína", "CN", "CHINA")
        put("Kínai Tajpej", "TW", "CHINESE TAIPEI", "TAIWAN")
        put("Hongkong", "HK", "HONG KONG")
        put("India", "IN", "INDIA")
        put("Indonézia", "ID", "INDONESIA")
        put("Irán", "IR", "IRAN")
        put("Irak", "IQ", "IRAQ")
        put("Izrael", "IL", "ISRAEL")
        put("Japán", "JP", "JAPAN")
        put("Jordánia", "JO", "JORDAN")
        put("Kuvait", "KW", "KUWAIT")
        put("Kirgizisztán", "KG", "KYRGYZSTAN")
        put("Laosz", "LA", "LAOS")
        put("Libanon", "LB", "LEBANON")
        put("Malajzia", "MY", "MALAYSIA")
        put("Maldív-szigetek", "MV", "MALDIVES")
        put("Mongólia", "MN", "MONGOLIA")
        put("Mianmar", "MM", "MYANMAR", "BURMA")
        put("Nepál", "NP", "NEPAL")
        put("Észak-Korea", "KP", "NORTH KOREA", "KOREA DPR")
        put("Omán", "OM", "OMAN")
        put("Pakisztán", "PK", "PAKISTAN")
        put("Palesztina", "PS", "PALESTINE")
        put("Fülöp-szigetek", "PH", "PHILIPPINES")
        put("Katar", "QA", "QATAR")
        put("Szaúd-Arábia", "SA", "SAUDI ARABIA")
        put("Szingapúr", "SG", "SINGAPORE")
        put("Dél-Korea", "KR", "SOUTH KOREA", "KOREA REPUBLIC", "REPUBLIC OF KOREA")
        put("Srí Lanka", "LK", "SRI LANKA")
        put("Szíria", "SY", "SYRIA")
        put("Tádzsikisztán", "TJ", "TAJIKISTAN")
        put("Thaiföld", "TH", "THAILAND")
        put("Türkmenisztán", "TM", "TURKMENISTAN")
        put("Egyesült Arab Emírségek", "AE", "UAE", "UNITED ARAB EMIRATES")
        put("Üzbegisztán", "UZ", "UZBEKISTAN")
        put("Vietnám", "VN", "VIETNAM")
        put("Jemen", "YE", "YEMEN")
        put("Bhután", "BT", "BHUTAN")
        put("Brunei", "BN", "BRUNEI")
        put("Kambodzsa", "KH", "CAMBODIA")
        put("Makaó", "MO", "MACAU")
        put("Guam", "GU", "GUAM")
        put("Új-Zéland", "NZ", "NEW ZEALAND")
        put("Fidzsi", "FJ", "FIJI")
        put("Pápua Új-Guinea", "PG", "PAPUA NEW GUINEA")
        put("Salamon-szigetek", "SB", "SOLOMON ISLANDS")
        put("Vanuatu", "VU", "VANUATU")
        put("Új-Kaledónia", "NC", "NEW CALEDONIA")
        put("Tahiti", "PF", "TAHITI")
        put("Szamoa", "WS", "SAMOA")
        put("Tonga", "TO", "TONGA")

        // --- Afrika ---
        put("Algéria", "DZ", "ALGERIA")
        put("Angola", "AO", "ANGOLA")
        put("Benin", "BJ", "BENIN")
        put("Botswana", "BW", "BOTSWANA")
        put("Burkina Faso", "BF", "BURKINA FASO")
        put("Burundi", "BI", "BURUNDI")
        put("Kamerun", "CM", "CAMEROON")
        put("Zöld-foki Köztársaság", "CV", "CAPE VERDE", "CABO VERDE")
        put("Közép-afrikai Köztársaság", "CF", "CENTRAL AFRICAN REPUBLIC")
        put("Csád", "TD", "CHAD")
        put("Comore-szigetek", "KM", "COMOROS")
        put("Kongó", "CG", "CONGO", "CONGO REPUBLIC", "REPUBLIC OF THE CONGO")
        put("Kongói Demokratikus Köztársaság", "CD", "DR CONGO", "DEMOCRATIC REPUBLIC OF THE CONGO", "CONGO DR")
        put("Dzsibuti", "DJ", "DJIBOUTI")
        put("Egyiptom", "EG", "EGYPT")
        put("Egyenlítői-Guinea", "GQ", "EQUATORIAL GUINEA")
        put("Eritrea", "ER", "ERITREA")
        put("Eswatini", "SZ", "ESWATINI", "SWAZILAND")
        put("Etiópia", "ET", "ETHIOPIA")
        put("Gabon", "GA", "GABON")
        put("Gambia", "GM", "GAMBIA")
        put("Ghána", "GH", "GHANA")
        put("Guinea", "GN", "GUINEA")
        put("Bissau-Guinea", "GW", "GUINEA-BISSAU")
        put("Elefántcsontpart", "CI", "IVORY COAST", "COTE D'IVOIRE", "CÔTE D'IVOIRE")
        put("Kenya", "KE", "KENYA")
        put("Lesotho", "LS", "LESOTHO")
        put("Libéria", "LR", "LIBERIA")
        put("Líbia", "LY", "LIBYA")
        put("Madagaszkár", "MG", "MADAGASCAR")
        put("Malawi", "MW", "MALAWI")
        put("Mali", "ML", "MALI")
        put("Mauritánia", "MR", "MAURITANIA")
        put("Mauritius", "MU", "MAURITIUS")
        put("Marokkó", "MA", "MOROCCO")
        put("Mozambik", "MZ", "MOZAMBIQUE")
        put("Namíbia", "NA", "NAMIBIA")
        put("Niger", "NE", "NIGER")
        put("Nigéria", "NG", "NIGERIA")
        put("Ruanda", "RW", "RWANDA")
        put("Szenegál", "SN", "SENEGAL")
        put("Sierra Leone", "SL", "SIERRA LEONE")
        put("Szomália", "SO", "SOMALIA")
        put("Dél-afrikai Köztársaság", "ZA", "SOUTH AFRICA")
        put("Dél-Szudán", "SS", "SOUTH SUDAN")
        put("Szudán", "SD", "SUDAN")
        put("Tanzánia", "TZ", "TANZANIA")
        put("Togo", "TG", "TOGO")
        put("Tunézia", "TN", "TUNISIA")
        put("Uganda", "UG", "UGANDA")
        put("Zambia", "ZM", "ZAMBIA")
        put("Zimbabwe", "ZW", "ZIMBABWE")
    }

    /** Angol országnév -> magyar országnév. Ismeretlen országnál Title Case-elt eredeti szöveg. */
    fun hungarianName(raw: String): String {
        val key = raw.trim().uppercase(Locale.ROOT)
        if (key.isEmpty()) return raw
        return countries[key]?.hu ?: toTitleCase(raw)
    }

    /** Angol országnév -> zászló emoji. Ismeretlen országnál 🏆 (tartalék). */
    fun flagEmoji(raw: String): String {
        val key = raw.trim().uppercase(Locale.ROOT)
        val iso2 = countries[key]?.iso2 ?: return "🏆"
        return flagFromIso2(iso2)
    }

    private fun flagFromIso2(iso2: String): String {
        if (iso2.length != 2) return "🏆"
        val base = 0x1F1E6
        val sb = StringBuilder()
        for (c in iso2.uppercase(Locale.ROOT)) {
            if (c < 'A' || c > 'Z') return "🏆"
            sb.appendCodePoint(base + (c - 'A'))
        }
        return sb.toString()
    }

    private fun toTitleCase(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return trimmed
        return trimmed.lowercase(Locale.ROOT)
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.titlecase(Locale.ROOT) }
            }
    }
}

/**
 * A bajnokság/forduló elnevezésekben előforduló, gyakran ismétlődő ANGOL
 * szakkifejezések magyarra fordítása (rájátszás, kiesés, csoport, stb.).
 *
 * FONTOS KORLÁT: a StatPal-nál 1000+ egyedi bajnokság van, ezeknek nincs
 * és nem is lehet statikus szótárral 100%-osan helyes, nyelvtanilag szép
 * magyar fordítása (pl. "Malaysia Cup" -> tükörfordítással "Malaysia Kupa"
 * lenne, ami nyelvtanilag nem helyes "Malajziai kupa" helyett - egy ilyen
 * fordítás elkészítése valódi gépi fordítást igényelne, nem szótárt).
 *
 * Ezért csak a leggyakoribb, jól azonosítható SZERKEZETI kifejezéseket
 * cseréljük (rájátszás, kiesés, feljutás, selejtező, csoport, forduló-
 * elnevezések), a bajnokság saját (márkanévszerű) részét pedig
 * változatlanul hagyjuk - ahogy a magyar sportmédia is teszi a legtöbb
 * külföldi bajnoksággal (pl. "Bundesliga", "Serie A" is így marad).
 */
object LeagueLocalizer {

    private val phraseReplacements: List<Pair<Regex, String>> = listOf(
        Regex("\\bRegular Season\\b", RegexOption.IGNORE_CASE) to "Alapszakasz",
        Regex("\\bPlay[- ]?Offs?\\b", RegexOption.IGNORE_CASE) to "Rájátszás",
        Regex("\\bRelegation Round\\b", RegexOption.IGNORE_CASE) to "Kiesési rájátszás",
        Regex("\\bRelegation Group\\b", RegexOption.IGNORE_CASE) to "Kiesési csoport",
        Regex("\\bRelegation\\b", RegexOption.IGNORE_CASE) to "Kiesés",
        Regex("\\bPromotion Round\\b", RegexOption.IGNORE_CASE) to "Feljutási rájátszás",
        Regex("\\bPromotion Group\\b", RegexOption.IGNORE_CASE) to "Feljutási csoport",
        Regex("\\bPromotion\\b", RegexOption.IGNORE_CASE) to "Feljutás",
        Regex("\\bQualification\\b", RegexOption.IGNORE_CASE) to "Selejtező",
        Regex("\\bQualifying\\b", RegexOption.IGNORE_CASE) to "Selejtező",
        Regex("\\bQualifiers?\\b", RegexOption.IGNORE_CASE) to "Selejtező",
        Regex("\\bFriendlies\\b", RegexOption.IGNORE_CASE) to "Barátságos mérkőzések",
        Regex("\\bFriendly\\b", RegexOption.IGNORE_CASE) to "Barátságos",
        Regex("\\bWomen's?\\b", RegexOption.IGNORE_CASE) to "Női",
        Regex("\\bReserves?\\b", RegexOption.IGNORE_CASE) to "Tartalék",
        Regex("\\bYouth\\b", RegexOption.IGNORE_CASE) to "Ifjúsági",
        Regex("\\bFirst Division\\b", RegexOption.IGNORE_CASE) to "1. osztály",
        Regex("\\bSecond Division\\b", RegexOption.IGNORE_CASE) to "2. osztály",
        Regex("\\bThird Division\\b", RegexOption.IGNORE_CASE) to "3. osztály",
        Regex("\\bFourth Division\\b", RegexOption.IGNORE_CASE) to "4. osztály",
        Regex("\\b1st Division\\b", RegexOption.IGNORE_CASE) to "1. osztály",
        Regex("\\b2nd Division\\b", RegexOption.IGNORE_CASE) to "2. osztály",
        Regex("\\b3rd Division\\b", RegexOption.IGNORE_CASE) to "3. osztály",
        Regex("\\b4th Division\\b", RegexOption.IGNORE_CASE) to "4. osztály",
        Regex("\\bGroup Stage\\b", RegexOption.IGNORE_CASE) to "Csoportkör",
        Regex("\\bFinal Stage\\b", RegexOption.IGNORE_CASE) to "Döntő szakasz",
        Regex("\\bRound of 16\\b", RegexOption.IGNORE_CASE) to "Nyolcaddöntő",
        Regex("\\bQuarter[- ]?Finals?\\b", RegexOption.IGNORE_CASE) to "Negyeddöntő",
        Regex("\\bSemi[- ]?Finals?\\b", RegexOption.IGNORE_CASE) to "Elődöntő",
        Regex("\\bFinal\\b", RegexOption.IGNORE_CASE) to "Döntő",
        Regex("\\bSuper Cup\\b", RegexOption.IGNORE_CASE) to "Szuperkupa"
    )

    private val groupLetterRegex = Regex("\\bGroup\\s+([A-Za-z0-9]+)\\b", RegexOption.IGNORE_CASE)
    private val bareGroupRegex = Regex("\\bGroup\\b", RegexOption.IGNORE_CASE)

    fun hungarianLeagueName(raw: String): String {
        var text = raw

        for ((regex, replacement) in phraseReplacements) {
            text = regex.replace(text, replacement)
        }

        text = groupLetterRegex.replace(text) { m -> "${m.groupValues[1]} csoport" }
        text = bareGroupRegex.replace(text, "Csoport")

        return text
    }
}
