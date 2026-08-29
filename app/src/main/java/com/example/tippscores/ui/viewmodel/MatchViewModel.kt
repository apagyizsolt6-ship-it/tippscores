package com.example.tippscores.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tippscores.data.local.ApiPreferences
import com.example.tippscores.data.model.Match
import com.example.tippscores.data.repository.MatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
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

    // REAKTÍV ÁLLAPOTOK AZ API KULCSOKNAK
    private val _statpalKey = MutableStateFlow(apiPreferences.statpalApiKey)
    val statpalKey: StateFlow<String> = _statpalKey

    private val _highlightlyKey = MutableStateFlow(apiPreferences.highlightlyApiKey)
    val highlightlyKey: StateFlow<String> = _highlightlyKey

    init {
        viewModelScope.launch {
            repository.fetchRealMatchesFromNetwork()
        }
    }

    fun saveKeysAndRefresh(statpalKey: String, highlightlyKey: String) {
        // 1. Mentés a Preferences-be
        apiPreferences.statpalApiKey = statpalKey
        apiPreferences.highlightlyApiKey = highlightlyKey

        // 2. State-ek frissítése a UI felé
        _statpalKey.value = statpalKey
        _highlightlyKey.value = highlightlyKey

        // 3. Hálózati frissítés indítása
        viewModelScope.launch {
            repository.fetchRealMatchesFromNetwork()
        }
    }
}
