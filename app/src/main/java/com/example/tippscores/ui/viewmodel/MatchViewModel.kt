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

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _statpalKey = MutableStateFlow(apiPreferences.statpalApiKey)
    val statpalKey: StateFlow<String> = _statpalKey

    private val _highlightlyKey = MutableStateFlow(apiPreferences.highlightlyApiKey)
    val highlightlyKey: StateFlow<String> = _highlightlyKey

    init {
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                repository.fetchMatchesForOffset(0)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Hálózati hiba történt az API lekérése közben."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveKeysAndRefresh(statpalKey: String, highlightlyKey: String) {
        apiPreferences.statpalApiKey = statpalKey
        apiPreferences.highlightlyApiKey = highlightlyKey
        _statpalKey.value = statpalKey
        _highlightlyKey.value = highlightlyKey

        refreshData()
    }
}
