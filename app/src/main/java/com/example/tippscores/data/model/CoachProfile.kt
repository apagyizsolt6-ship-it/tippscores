package com.example.tippscores.data.model

/** StatPal "Get Coach" (v2/soccer/coaches/{coach_id}) adatok. */
data class CoachProfile(
    val id: String,
    val name: String,
    val fullName: String? = null,
    val team: String? = null,
    val teamId: String? = null,
    val nationality: String? = null,
    val birthdate: String? = null,
    val age: String? = null,
    val birthplace: String? = null,
    val birthcountry: String? = null,
    val height: String? = null,
    val weight: String? = null,
    val trophies: List<TeamTrophy> = emptyList(),
    val career: List<CoachCareerStop> = emptyList()
)

data class CoachCareerStop(
    val teamName: String,
    val teamId: String? = null,
    val from: String? = null,
    val to: String? = null
)
