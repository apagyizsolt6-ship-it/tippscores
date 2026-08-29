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

    private val _selectedOffset = MutableStateFlow(0) // 0 = MA
    val selectedOffset: StateFlow<Int> = _selectedOffset

    private val _statpalKey = MutableStateFlow(apiPreferences.statpalApiKey)
    val statpalKey: StateFlow<String> = _statpalKey

    private val _highlightlyKey = MutableStateFlow(apiPreferences.highlightlyApiKey)
    val highlightlyKey: StateFlow<String> = _highlightlyKey

    init {
        viewModelScope.launch {
            repository.fetchMatchesForOffset(0)
        }
    }

    fun selectOffset(offset: Int) {
        _selectedOffset.value = offset
        viewModelScope.launch {
            repository.fetchMatchesForOffset(offset)
        }
    }

    fun saveKeysAndRefresh(statpalKey: String, highlightlyKey: String) {
        apiPreferences.statpalApiKey = statpalKey
        apiPreferences.highlightlyApiKey = highlightlyKey
        _statpalKey.value = statpalKey
        _highlightlyKey.value = highlightlyKey

        viewModelScope.launch {
            repository.fetchMatchesForOffset(_selectedOffset.value)
        }
    }
}
