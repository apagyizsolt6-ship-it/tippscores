package com.example.tippscores.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tippscores.data.local.ApiPreferences
import com.example.tippscores.data.model.Match
import com.example.tippscores.data.repository.MatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MatchViewModel(
    private val repository: MatchRepository,
    private val apiPreferences: ApiPreferences
) : ViewModel() {

    // ========================================================
    // KEDVENCEK (a csillag a mérkőzéssoron - SharedPreferences-ben
    // tárolva, mert a "matches" tábla minden frissítéskor törlődik)
    // ========================================================

    private val _favoriteIds =
        MutableStateFlow(apiPreferences.favoriteMatchIds)

    val favoriteIds: StateFlow<Set<String>> =
        _favoriteIds

    fun toggleFavorite(matchId: String) {
        _favoriteIds.value = apiPreferences.toggleFavoriteMatch(matchId)
    }

    // ========================================================
    // KIEMELT BAJNOKSÁGOK (az 5 alapértelmezett + a felhasználó
    // saját kiemelései/visszavonásai)
    // ========================================================

    private val _featuredAdditions =
        MutableStateFlow(apiPreferences.featuredLeagueAdditions)

    val featuredAdditions: StateFlow<Set<String>> =
        _featuredAdditions

    private val _featuredRemovals =
        MutableStateFlow(apiPreferences.featuredLeagueRemovals)

    val featuredRemovals: StateFlow<Set<String>> =
        _featuredRemovals

    fun toggleFeaturedLeague(leagueKey: String, isPreset: Boolean, currentlyFeatured: Boolean) {
        apiPreferences.toggleFeaturedLeague(leagueKey, isPreset, currentlyFeatured)
        _featuredAdditions.value = apiPreferences.featuredLeagueAdditions
        _featuredRemovals.value = apiPreferences.featuredLeagueRemovals
    }

    // ========================================================
    // MÉRKŐZÉSEK (a nyers lista + a kedvenc-jelölés összefésülve)
    // ========================================================

    val matches: StateFlow<List<Match>> =
        combine(repository.allMatches, _favoriteIds) { list, favIds ->
            list.map { it.copy(isFavorite = favIds.contains(it.id)) }
        }.stateIn(
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
    // HIBA
    // ========================================================

    private val _errorMessage =
        MutableStateFlow<String?>(null)

    val errorMessage: StateFlow<String?> =
        _errorMessage

    // ========================================================
    // API KULCSOK
    // ========================================================

    private val _statpalKey =
        MutableStateFlow(
            apiPreferences.statpalApiKey
        )

    val statpalKey: StateFlow<String> =
        _statpalKey

    private val _highlightlyKey =
        MutableStateFlow(
            apiPreferences.highlightlyApiKey
        )

    val highlightlyKey: StateFlow<String> =
        _highlightlyKey

    // ========================================================
    // KIVÁLASZTOTT NAP
    // ========================================================

    private val _selectedOffset =
        MutableStateFlow(0)

    val selectedOffset: StateFlow<Int> =
        _selectedOffset

    // ========================================================
    // INIT
    // ========================================================

    init {
        refreshData()
    }

    // ========================================================
    // AKTUÁLIS NAP FRISSÍTÉSE
    // ========================================================

    fun refreshData() {

        loadMatches(
            offset = _selectedOffset.value
        )
    }

    // ========================================================
    // NAP KIVÁLASZTÁSA
    // ========================================================

    fun selectOffset(
        offset: Int
    ) {

        val safeOffset =
            offset.coerceIn(-7, 7)

        if (_selectedOffset.value == safeOffset) {
            refreshData()
            return
        }

        _selectedOffset.value =
            safeOffset

        loadMatches(
            offset = safeOffset
        )
    }

    // ========================================================
    // MÉRKŐZÉSEK BETÖLTÉSE
    // ========================================================

    private fun loadMatches(
        offset: Int
    ) {

        viewModelScope.launch {

            _isLoading.value = true
            _errorMessage.value = null

            try {

                repository.fetchMatchesForOffset(
                    offset = offset
                )

            } catch (e: Exception) {

                e.printStackTrace()

                _errorMessage.value =
                    e.localizedMessage
                        ?: "Nem sikerült betölteni a mérkőzéseket."

            } finally {

                _isLoading.value = false
            }
        }
    }

    // ========================================================
    // LIVE ADATOK
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
    // API KULCSOK MENTÉSE
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
