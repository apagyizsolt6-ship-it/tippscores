package com.example.tippscores.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tippscores.data.local.ApiPreferences
import com.example.tippscores.data.model.Match
import com.example.tippscores.data.repository.MatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MatchViewModel(
    private val repository: MatchRepository,
    private val apiPreferences: ApiPreferences
) : ViewModel() {

    // ========================================================
    // MATCHES
    // ========================================================

    val matches: StateFlow<List<Match>> =
        repository.allMatches.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // ========================================================
    // LOADING
    // ========================================================

    private val _isLoading =
        MutableStateFlow(false)

    val isLoading: StateFlow<Boolean> =
        _isLoading

    // ========================================================
    // ERROR
    // ========================================================

    private val _errorMessage =
        MutableStateFlow<String?>(null)

    val errorMessage: StateFlow<String?> =
        _errorMessage

    // ========================================================
    // STATPAL KEY
    // ========================================================

    private val _statpalKey =
        MutableStateFlow(
            apiPreferences.statpalApiKey
        )

    val statpalKey: StateFlow<String> =
        _statpalKey

    // ========================================================
    // HIGHLIGHTLY KEY
    // ========================================================

    private val _highlightlyKey =
        MutableStateFlow(
            apiPreferences.highlightlyApiKey
        )

    val highlightlyKey: StateFlow<String> =
        _highlightlyKey

    // ========================================================
    // INIT
    // ========================================================

    init {
        refreshData()
    }

    // ========================================================
    // REFRESH
    // ========================================================

    fun refreshData() {

        viewModelScope.launch {

            _isLoading.value = true
            _errorMessage.value = null

            try {

                // MAI ÖSSZES MÉRKŐZÉS
                repository.fetchMatchesForOffset(0)

            } catch (e: Exception) {

                e.printStackTrace()

                _errorMessage.value =
                    e.localizedMessage
                        ?: "Hálózati hiba történt az API lekérése közben."

            } finally {

                _isLoading.value = false
            }
        }
    }

    // ========================================================
    // LIVE REFRESH
    // ========================================================

    fun refreshLiveData() {

        viewModelScope.launch {

            _isLoading.value = true
            _errorMessage.value = null

            try {

                repository.fetchLiveMatches()

            } catch (e: Exception) {

                e.printStackTrace()

                _errorMessage.value =
                    e.localizedMessage
                        ?: "Nem sikerült betölteni az élő mérkőzéseket."

            } finally {

                _isLoading.value = false
            }
        }
    }

    // ========================================================
    // SAVE API KEYS
    // ========================================================

    fun saveKeysAndRefresh(
        statpalKey: String,
        highlightlyKey: String
    ) {

        val cleanStatpalKey =
            statpalKey.trim()

        val cleanHighlightlyKey =
            highlightlyKey.trim()

        apiPreferences.statpalApiKey =
            cleanStatpalKey

        apiPreferences.highlightlyApiKey =
            cleanHighlightlyKey

        _statpalKey.value =
            cleanStatpalKey

        _highlightlyKey.value =
            cleanHighlightlyKey

        refreshData()
    }
}
