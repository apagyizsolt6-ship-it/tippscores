package com.example.tippscores.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tippscores.data.local.ApiPreferences
import com.example.tippscores.data.model.Match
import com.example.tippscores.data.repository.MatchRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MatchViewModel(
    private val repository: MatchRepository,
    private val apiPreferences: ApiPreferences
) : ViewModel() {

    val matches: StateFlow<List<Match>> = repository.allMatches.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Indításkor megnézzük, van-e elmentett kulcs, és frissítünk
        viewModelScope.launch {
            repository.fetchRealMatchesFromNetwork()
        }
    }

    fun saveKeysAndRefresh(statpalKey: String, highlightlyKey: String) {
        apiPreferences.statpalApiKey = statpalKey
        apiPreferences.highlightlyApiKey = highlightlyKey
        
        viewModelScope.launch {
            repository.fetchRealMatchesFromNetwork()
        }
    }
}
