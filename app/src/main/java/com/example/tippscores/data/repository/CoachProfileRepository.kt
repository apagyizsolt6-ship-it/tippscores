package com.example.tippscores.data.repository

import com.example.tippscores.data.model.CoachCareerStop
import com.example.tippscores.data.model.CoachProfile
import com.example.tippscores.data.model.TeamTrophy
import com.example.tippscores.data.remote.NetworkModule
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException

/**
 * Edzőprofil a StatPal "Get Coach" végpontjából
 * (v2/soccer/coaches/{coach_id}). Az eredményt 30 percig memóriában
 * tartjuk, mert az edzők adatai ritkán változnak.
 */
class CoachProfileRepository(
    private val statpalKeyProvider: () -> String
) {

    private data class CacheEntry(
        val savedAt: Long,
        val profile: CoachProfile
    )

    private val cache = mutableMapOf<String, CacheEntry>()

    suspend fun fetch(coachId: String?, coachName: String): CoachProfile? {
        val id = coachId?.trim().orEmpty()
        val key = statpalKeyProvider().trim()
        if (id.isEmpty() || key.isEmpty()) return null

        val cached = cache[id]
        if (cached != null &&
            System.currentTimeMillis() - cached.savedAt < CACHE_MS
        ) {
            return cached.profile
        }

        return try {
            val root = NetworkModule.statpalMatchDetailsApi.getDetails(
                url = "https://statpal.io/api/v2/soccer/coaches/$id",
                accessKey = key
            )
            val profile = parse(root, id, coachName)
            if (profile != null) {
                cache[id] = CacheEntry(System.currentTimeMillis(), profile)
            }
            profile
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parse(root: JsonObject, id: String, fallbackName: String): CoachProfile? {
        val coach = root.obj("coach") ?: return null

        val trophies = coach.obj("trophies")
            ?.get("trophy")
            .items()
            .mapNotNull { t ->
                val league = t.text("league") ?: return@mapNotNull null
                TeamTrophy(
                    league = league,
                    country = t.text("country"),
                    status = t.text("status"),
                    count = t.text("count"),
                    seasons = t.text("seasons")?.trim()?.trimEnd(',')?.trim()
                )
            }

        val career = coach.obj("career_stats")
            ?.get("team")
            .items()
            .mapNotNull { s ->
                val name = s.text("name") ?: return@mapNotNull null
                CoachCareerStop(
                    teamName = name,
                    teamId = s.text("id"),
                    from = s.text("from"),
                    to = s.text("to")
                )
            }

        return CoachProfile(
            id = coach.text("id") ?: id,
            name = coach.text("name") ?: fallbackName,
            fullName = coach.text("firstname")?.let { first ->
                listOfNotNull(first, coach.text("lastname")).joinToString(" ")
            },
            team = coach.text("team"),
            teamId = coach.text("team_id"),
            nationality = coach.text("nationality"),
            birthdate = coach.text("birthdate"),
            age = coach.text("age"),
            birthplace = coach.text("birthplace"),
            birthcountry = coach.text("birthcountry"),
            height = coach.text("height"),
            weight = coach.text("weight"),
            trophies = trophies,
            career = career
        )
    }

    // --- Biztonságos JSON segédek (a StatPal néha objektumot ad tömb helyett) ---

    private fun JsonElement?.asObj(): JsonObject? =
        if (this != null && this.isJsonObject) this.asJsonObject else null

    private fun JsonObject?.obj(name: String): JsonObject? =
        this?.get(name).asObj()

    private fun JsonObject?.text(name: String): String? {
        val element = this?.get(name) ?: return null
        if (!element.isJsonPrimitive) return null
        return element.asString.trim().takeIf { it.isNotEmpty() }
    }

    private fun JsonElement?.items(): List<JsonObject> {
        val element = this
        if (element == null || element.isJsonNull) return emptyList()
        return when {
            element.isJsonArray -> element.asJsonArray.mapNotNull { it.asObj() }
            element.isJsonObject -> listOf(element.asJsonObject)
            else -> emptyList()
        }
    }

    private companion object {
        const val CACHE_MS = 30 * 60 * 1000L
    }
}
