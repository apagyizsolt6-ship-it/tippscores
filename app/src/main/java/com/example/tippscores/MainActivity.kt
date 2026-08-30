package com.example.tippscores

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.tippscores.data.local.ApiPreferences
import com.example.tippscores.data.local.AppDatabase
import com.example.tippscores.data.repository.MatchRepository
import com.example.tippscores.ui.screens.MatchListScreen
import com.example.tippscores.ui.viewmodel.MatchViewModel

class MainActivity : ComponentActivity() {

    private val apiPreferences by lazy {
        ApiPreferences(this)
    }

    private val database by lazy {
        AppDatabase.getDatabase(this)
    }

    private val repository by lazy {
        MatchRepository(
            database.matchDao(),
            apiPreferences
        )
    }

    private val viewModel: MatchViewModel by viewModels {

        object : ViewModelProvider.Factory {

            override fun <T : ViewModel> create(
                modelClass: Class<T>
            ): T {

                @Suppress("UNCHECKED_CAST")

                return MatchViewModel(
                    repository = repository,
                    apiPreferences = apiPreferences
                ) as T
            }
        }
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContent {

            val matches by
                viewModel.matches.collectAsState()

            val isLoading by
                viewModel.isLoading.collectAsState()

            val errorMessage by
                viewModel.errorMessage.collectAsState()

            val statpalKey by
                viewModel.statpalKey.collectAsState()

            val highlightlyKey by
                viewModel.highlightlyKey.collectAsState()

            val selectedOffset by
                viewModel.selectedOffset.collectAsState()

            MatchListScreen(

                matches = matches,

                errorMessage = errorMessage,

                isLoading = isLoading,

                statpalKey = statpalKey,

                highlightlyKey = highlightlyKey,

                selectedOffset = selectedOffset,

                onRefresh = {
                    viewModel.refreshData()
                },

                onDateSelected = { offset ->
                    viewModel.selectOffset(offset)
                },

                onSaveKeys = { sKey, hKey ->

                    viewModel.saveKeysAndRefresh(
                        sKey,
                        hKey
                    )
                },

                onToggleFavorite = { matchId ->
                    viewModel.toggleFavorite(matchId)
                },

                onMatchClick = { matchId ->

                    // Meccs részletei
                    // későbbi képernyőre kötve.
                }
            )
        }
    }
}
