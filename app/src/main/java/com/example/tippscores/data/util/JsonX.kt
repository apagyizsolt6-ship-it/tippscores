package com.example.tippscores.data.util

import com.google.gson.JsonElement
import com.google.gson.JsonObject

/**
 * Rugalmas JSON-segédek a StatPal válaszokhoz: a mezőnevek kis- és
 * nagybetű, valamint aláhúzás nélkül hasonlítódnak össze, és az
 * "objektum vagy tömb" eltéréseket is kezelik.
 */
object JsonX {

    fun norm(value: String): String =
        value.lowercase().filter { it.isLetterOrDigit() }

    fun obj(element: JsonElement?): JsonObject? =
        if (element != null && element.isJsonObject) element.asJsonObject else null

    /** Tömb elemei objektumként; egyetlen objektum esetén egyelemű lista. */
    fun objects(element: JsonElement?): List<JsonObject> {
        if (element == null || element.isJsonNull) return emptyList()
        return when {
            element.isJsonArray -> element.asJsonArray.mapNotNull { obj(it) }
            element.isJsonObject -> listOf(element.asJsonObject)
            else -> emptyList()
        }
    }

    /** Az első nem üres, primitív érték a megadott kulcsok közül (kulcssorrend szerint). */
    fun text(o: JsonObject?, vararg keys: String): String? {
        if (o == null) return null
        for (wanted in keys.map { norm(it) }) {
            for ((key, value) in o.entrySet()) {
                if (norm(key) == wanted && value.isJsonPrimitive) {
                    val s = value.asString.trim()
                    if (s.isNotEmpty()) return s
                }
            }
        }
        return null
    }

    /** Mint a [text], de a gyerekobjektumokban is keres (a megadott mélységig). */
    fun deepText(o: JsonObject?, depth: Int, vararg keys: String): String? {
        if (o == null) return null
        text(o, *keys)?.let { return it }
        if (depth <= 0) return null
        for ((_, value) in o.entrySet()) {
            if (value.isJsonObject) {
                deepText(value.asJsonObject, depth - 1, *keys)?.let { return it }
            }
        }
        return null
    }

    /** Minden objektumot bejár (mélységi sorrendben). */
    fun walkObjects(element: JsonElement?, visit: (JsonObject) -> Unit) {
        if (element == null || element.isJsonNull) return
        when {
            element.isJsonObject -> {
                val o = element.asJsonObject
                visit(o)
                o.entrySet().forEach { walkObjects(it.value, visit) }
            }
            element.isJsonArray -> element.asJsonArray.forEach { walkObjects(it, visit) }
        }
    }

    fun toDouble(value: String?): Double? =
        value?.replace(',', '.')?.trim()?.toDoubleOrNull()

    fun toInt(value: String?): Int? =
        value?.trim()?.toIntOrNull() ?: toDouble(value)?.toInt()

    /** dd.MM.yyyy vagy yyyy-MM-dd dátum összehasonlítható számmá (yyyyMMdd). */
    fun dateKey(raw: String?): Int {
        val s = raw?.trim().orEmpty()
        Regex("""(\d{1,2})[./](\d{1,2})[./](\d{4})""").find(s)?.let {
            val (d, m, y) = it.destructured
            return y.toInt() * 10000 + m.toInt() * 100 + d.toInt()
        }
        Regex("""(\d{4})-(\d{2})-(\d{2})""").find(s)?.let {
            val (y, m, d) = it.destructured
            return y.toInt() * 10000 + m.toInt() * 100 + d.toInt()
        }
        return 0
    }
}
