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

    private val _selectedOffset = MutableStateFlow(0)
    val selectedOffset: StateFlow<Int> = _selectedOffset

    init {
        refreshData()
    }

    fun refreshData() {
        refreshForOffset(_selectedOffset.value)
    }

    fun selectDate(offset: Int) {
        val safeOffset = offset.coerceIn(-7, 7)
        if (_selectedOffset.value == safeOffset && matches.value.isNotEmpty()) {
            return
        }
        _selectedOffset.value = safeOffset
        refreshForOffset(safeOffset)
    }

    fun refreshForOffset(offset: Int) {
        val safeOffset = offset.coerceIn(-7, 7)
        _selectedOffset.value = safeOffset

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                repository.fetchMatchesForOffset(safeOffset)
            } catch (e: Exception) {
                _errorMessage.value = readableError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshLiveData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                repository.fetchLiveMatches()
            } catch (e: Exception) {
                _errorMessage.value = readableError(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveKeysAndRefresh(statpalKey: String, highlightlyKey: String) {
        val cleanStatpalKey = statpalKey.trim()
        val cleanHighlightlyKey = highlightlyKey.trim()

        apiPreferences.statpalApiKey = cleanStatpalKey
        apiPreferences.highlightlyApiKey = cleanHighlightlyKey

        _statpalKey.value = cleanStatpalKey
        _highlightlyKey.value = cleanHighlightlyKey

        refreshData()
    }

    private fun readableError(exception: Exception): String {
        val message = exception.localizedMessage.orEmpty()

        return when {
            message.contains("401") -> "A StatPal API-kulcs érvénytelen vagy nem jogosult erre a lekérésre."
            message.contains("403") -> "A StatPal API-kulcs hozzáférése megtagadva."
            message.contains("404") -> "A StatPal végpont nem található."
            message.contains("Unable to resolve host", ignoreCase = true) ->
                "Nincs internetkapcsolat vagy a szerver nem érhető el."
            message.contains("Expected BEGIN_ARRAY", ignoreCase = true) ->
                "A StatPal válasz formátuma nem várt módon érkezett."
            message.isNotBlank() -> message
            else -> "Hálózati hiba történt az API lekérése közben."
        }
    }
}
