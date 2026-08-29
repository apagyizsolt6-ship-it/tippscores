package com.example.tippscores

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tippscores.data.local.ApiPreferences
import com.example.tippscores.data.local.AppDatabase
import com.example.tippscores.data.repository.MatchRepository
import com.example.tippscores.ui.screens.MatchListScreen
import com.example.tippscores.ui.viewmodel.MatchViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val apiPreferences by lazy { ApiPreferences(this) }
    private val database by lazy { AppDatabase.getDatabase(this) }
    private val repository by lazy { MatchRepository(database.matchDao(), apiPreferences) }

    private val viewModel: MatchViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MatchViewModel(repository, apiPreferences) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val matches by viewModel.matches.collectAsState()

            MatchListScreen(
                matches = matches,
                statpalKey = apiPreferences.statpalApiKey,
                highlightlyKey = apiPreferences.highlightlyApiKey,
                onSaveKeys = { statpalKey, highlightlyKey ->
                    viewModel.saveKeysAndRefresh(statpalKey, highlightlyKey)
                },
                onMatchClick = { matchId ->
                    // Meccs megnyitása
                }
            )
        }
    }
}
