package com.example.tippscores

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.tippscores.data.model.Match
import com.example.tippscores.ui.screens.MatchListScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Minta adatok a teszteléshez
        val dummyMatches = listOf(
            Match(
                id = "1",
                leagueName = "Premier League",
                leagueCountry = "ANGLIA",
                homeTeam = "Liverpool",
                homeTeamLogo = "",
                awayTeam = "Nottingham",
                awayTeamLogo = "",
                homeScore = 2,
                awayScore = 2,
                status = "FT",
                isLive = false,
                tipPrediction = "TÚL 2.5",
                tipConfidence = 88,
                hasVideoHighlight = true
            ),
            Match(
                id = "2",
                leagueName = "Premier League",
                leagueCountry = "ANGLIA",
                homeTeam = "Bournemouth",
                homeTeamLogo = "",
                awayTeam = "Everton",
                awayTeamLogo = "",
                homeScore = 1,
                awayScore = 0,
                status = "72'",
                isLive = true,
                tipPrediction = "HAZAI",
                tipConfidence = 75,
                hasVideoHighlight = true
            )
        )

        setContent {
            MatchListScreen(
                matches = dummyMatches,
                onMatchClick = { matchId ->
                    // Megnyitja a meccs részleteit
                }
            )
        }
    }
}
