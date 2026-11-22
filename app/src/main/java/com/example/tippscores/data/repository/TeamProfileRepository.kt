package com.example.tippscores.data.repository

import com.example.tippscores.data.model.TeamProfile

class TeamProfileRepository {

    suspend fun fetch(teamName: String): TeamProfile? {
        // Csapatprofil jelenleg nem elérhető – csak StatPal van használatban
        return null
    }
}
