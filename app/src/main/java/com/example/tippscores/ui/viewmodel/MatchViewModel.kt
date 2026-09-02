package com.example.tippscores.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tippscores.data.local.ApiPreferences
import com.example.tippscores.data.model.Match
import com.example.tippscores.data.model.MatchDetails
import com.example.tippscores.data.repository.MatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MatchViewModel(
    private val repository: MatchRepository,
    private val apiPreferences: ApiPreferences,
    private val detailsRepository: com.example.tippscores.data.repository.MatchDetailsRepository
) : ViewModel() {

    // ========================================================
    // KEDVENC MECCSEK (a csillag a mérkőzéssoron)
    // ========================================================

    private val _favoriteIds =
        MutableStateFlow(apiPreferences.favoriteMatchIds)

    val favoriteIds: StateFlow<Set<String>> =
        _favoriteIds

    fun toggleFavorite(matchId: String) {
        _favoriteIds.value = apiPreferences.toggleFavoriteMatch(matchId)
    }

    // ========================================================
    // KEDVENC CSAPATOK (csapat követése minden napra)
    // ========================================================

    private val _favoriteTeamNames =
        MutableStateFlow(apiPreferences.favoriteTeamNames)

    val favoriteTeamNames: StateFlow<Set<String>> =
        _favoriteTeamNames

    fun toggleFavoriteTeam(teamName: String) {
        _favoriteTeamNames.value = apiPreferences.toggleFavoriteTeam(teamName)
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
    // SÖTÉT MÓD
    // ========================================================

    private val _darkMode =
        MutableStateFlow(apiPreferences.darkModeEnabled)

    val darkMode: StateFlow<Boolean> =
        _darkMode

    fun setDarkMode(enabled: Boolean) {
        apiPreferences.darkModeEnabled = enabled
        _darkMode.value = enabled
    }

    // ========================================================
    // PUSH ÉRTESÍTÉS GÓLNÁL
    // ========================================================

    private val _goalNotificationsEnabled =
        MutableStateFlow(apiPreferences.goalNotificationsEnabled)

    val goalNotificationsEnabled: StateFlow<Boolean> =
        _goalNotificationsEnabled

    fun setGoalNotificationsEnabled(enabled: Boolean) {
        apiPreferences.goalNotificationsEnabled = enabled
        _goalNotificationsEnabled.value = enabled
    }

    // ========================================================
    // MÉRKŐZÉSEK (a nyers lista + kedvenc-jelölés + csapatkövetés)
    // ========================================================

    val matches: StateFlow<List<Match>> =
        combine(
            repository.allMatches,
            _favoriteIds,
            _favoriteTeamNames
        ) { list, favIds, favTeams ->
            list.map {
                it.copy(
                    isFavorite = favIds.contains(it.id),
                    isHomeTeamFollowed = favTeams.contains(it.homeTeam),
                    isAwayTeamFollowed = favTeams.contains(it.awayTeam)
                )
            }
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
    // MÉRKŐZÉS RÉSZLETEK
    // ========================================================

    private val _matchDetails =
        MutableStateFlow<MatchDetails?>(null)

    val matchDetails: StateFlow<MatchDetails?> =
        _matchDetails

    private val _detailsLoading =
        MutableStateFlow(false)

    val detailsLoading: StateFlow<Boolean> =
        _detailsLoading

    private val _detailsError =
        MutableStateFlow<String?>(null)

    val detailsError: StateFlow<String?> =
        _detailsError

    private var openedMatchId: String? = null

    fun openMatchDetails(match: Match) {
        if (openedMatchId == match.id && _matchDetails.value != null) return

        openedMatchId = match.id
        _matchDetails.value = null
        _detailsError.value = null

        viewModelScope.launch {
            _detailsLoading.value = true
            try {
                _matchDetails.value = detailsRepository.fetch(
                    matchId = match.id,
                    homeTeam = match.homeTeam,
                    awayTeam = match.awayTeam
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _detailsError.value =
                    e.localizedMessage ?: "A mérkőzés részletes adatai nem tölthetők be."
            } finally {
                _detailsLoading.value = false
            }
        }
    }

    fun retryMatchDetails() {
        val id = openedMatchId ?: return
        val match = matches.value.find { it.id == id } ?: return
        openedMatchId = null
        openMatchDetails(match)
    }

    fun clearMatchDetails() {
        openedMatchId = null
        _matchDetails.value = null
        _detailsError.value = null
        _detailsLoading.value = false
    }
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
