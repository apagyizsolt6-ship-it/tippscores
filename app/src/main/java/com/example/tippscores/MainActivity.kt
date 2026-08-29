package com.example.tippscores

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.tippscores.data.local.AppDatabase
import com.example.tippscores.data.repository.MatchRepository
import com.example.tippscores.ui.screens.MatchListScreen
import com.example.tippscores.ui.viewmodel.MatchViewModel
import com.example.tippscores.ui.viewmodel.MatchViewModelFactory

class MainActivity : ComponentActivity() {

    private val database by lazy { AppDatabase.getDatabase(this) }
    private val repository by lazy { MatchRepository(database.matchDao()) }
    private val viewModel: MatchViewModel by viewModels { MatchViewModelFactory(repository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val matches by viewModel.matches.collectAsState()

            MatchListScreen(
                matches = matches,
                onMatchClick = { matchId ->
                    // Megnyitja a meccs részleteit
                }
            )
        }
    }
}
