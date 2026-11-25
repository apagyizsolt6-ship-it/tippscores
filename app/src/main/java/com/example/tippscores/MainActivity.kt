package com.example.tippscores

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.tippscores.data.local.ApiPreferences
import com.example.tippscores.data.local.AppDatabase
import com.example.tippscores.data.local.TipStore
import com.example.tippscores.data.remote.StatpalImages
import com.example.tippscores.data.repository.PreviewRepository
import com.example.tippscores.data.repository.StandingsRepository
import com.example.tippscores.data.util.JsonX
import com.example.tippscores.ui.screens.LeagueScreen
import com.example.tippscores.ui.screens.TipsScreen
import com.example.tippscores.data.repository.CoachProfileRepository
import com.example.tippscores.data.repository.MatchRepository
import com.example.tippscores.data.repository.TeamProfileRepository
import com.example.tippscores.notifications.GoalCheckWorker
import com.example.tippscores.ui.screens.CoachProfileScreen
import com.example.tippscores.ui.screens.MatchDetailScreen
import com.example.tippscores.ui.screens.MatchListScreen
import com.example.tippscores.ui.screens.TeamProfileScreen
import com.example.tippscores.ui.theme.TippScoresTheme
import com.example.tippscores.ui.viewmodel.MatchViewModel
import java.util.concurrent.TimeUnit

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

    private val teamProfileRepository by lazy {
        TeamProfileRepository(
            statpalKeyProvider = { apiPreferences.statpalApiKey }
        )
    }

    private val coachProfileRepository by lazy {
        CoachProfileRepository(
            statpalKeyProvider = { apiPreferences.statpalApiKey }
        )
    }

    private val previewRepository by lazy {
        PreviewRepository(
            statpalKeyProvider = { apiPreferences.statpalApiKey }
        )
    }

    private val standingsRepository by lazy {
        StandingsRepository(
            statpalKeyProvider = { apiPreferences.statpalApiKey }
        )
    }

    private val tipStore by lazy {
        TipStore(this)
    }

    private val detailsRepository by lazy {
        com.example.tippscores.data.repository.MatchDetailsRepository(
            statpalKeyProvider = { apiPreferences.statpalApiKey },
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
                    apiPreferences = apiPreferences,
                    detailsRepository = detailsRepository
                ) as T
            }
        }
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

    private fun requestNotificationPermissionIfNeeded() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            val granted =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
    }

    private fun updateGoalNotificationScheduling(enabled: Boolean) {

        val workManager =
            WorkManager.getInstance(applicationContext)

        if (enabled) {

            val request =
                PeriodicWorkRequestBuilder<GoalCheckWorker>(
                    15,
                    TimeUnit.MINUTES
                ).build()

            workManager.enqueueUniquePeriodicWork(
                "goal_check",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )

        } else {

            workManager.cancelUniqueWork("goal_check")
        }
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        StatpalImages.keyProvider = { apiPreferences.statpalApiKey }

        if (apiPreferences.goalNotificationsEnabled) {
            updateGoalNotificationScheduling(true)
        }

        setContent {

            val matches by
                viewModel.matches.collectAsState()

            val isLoading by
                viewModel.isLoading.collectAsState()

            val errorMessage by
                viewModel.errorMessage.collectAsState()

            val statpalKey by
                viewModel.statpalKey.collectAsState()

            val lastUpdated by
                viewModel.lastUpdated.collectAsState()

            val selectedOffset by
                viewModel.selectedOffset.collectAsState()

            val matchDetails by
                viewModel.matchDetails.collectAsState()

            val detailsLoading by
                viewModel.detailsLoading.collectAsState()

            val detailsError by
                viewModel.detailsError.collectAsState()

            val featuredAdditions by
                viewModel.featuredAdditions.collectAsState()

            val featuredRemovals by
                viewModel.featuredRemovals.collectAsState()

            val favoriteTeamNames by
                viewModel.favoriteTeamNames.collectAsState()

            val darkMode by
                viewModel.darkMode.collectAsState()

            val goalNotificationsEnabled by
                viewModel.goalNotificationsEnabled.collectAsState()

            var selectedMatchId by
                remember {
                    mutableStateOf<String?>(null)
                }

            var selectedTeamName by
                remember {
                    mutableStateOf<String?>(null)
                }

            var selectedTeamLogo by
                remember {
                    mutableStateOf("")
                }

            var selectedTeamId by
                remember {
                    mutableStateOf<String?>(null)
                }

            var selectedTeamLeagueId by
                remember {
                    mutableStateOf<String?>(null)
                }

            var selectedCoachId by
                remember {
                    mutableStateOf<String?>(null)
                }

            var selectedCoachName by
                remember {
                    mutableStateOf("")
                }

            var selectedLeagueId by
                remember {
                    mutableStateOf<String?>(null)
                }

            var selectedLeagueName by
                remember {
                    mutableStateOf("")
                }

            var selectedLeagueCountry by
                remember {
                    mutableStateOf("")
                }

            var selectedLeagueFlag by
                remember {
                    mutableStateOf("")
                }

            var showTips by
                remember {
                    mutableStateOf(false)
                }

            LaunchedEffect(matches) {
                tipStore.settleFrom(matches)
            }

            MaterialTheme(
                colorScheme =
                    if (darkMode) darkColorScheme() else lightColorScheme()
            ) {

                TippScoresTheme(
                    darkMode = darkMode
                ) {

                    val currentMatchId = selectedMatchId
                    val currentTeamName = selectedTeamName
                    val currentCoachId = selectedCoachId
                    val currentLeagueId = selectedLeagueId

                    val screenKey = when {
                        currentCoachId != null -> "coach"
                        currentTeamName != null -> "team"
                        currentMatchId != null -> "match"
                        currentLeagueId != null -> "league"
                        showTips -> "tips"
                        else -> "list"
                    }

                    AnimatedContent(
                        targetState = screenKey,
                        transitionSpec = {
                            fadeIn(tween(220)) togetherWith fadeOut(tween(160))
                        },
                        label = "screenTransition"
                    ) { key ->

                    if (key == "coach" && currentCoachId != null) {

                        CoachProfileScreen(
                            coachId = currentCoachId,
                            coachName = selectedCoachName,
                            repository = coachProfileRepository,
                            onBack = {
                                selectedCoachId = null
                                selectedCoachName = ""
                            }
                        )

                    } else if (key == "team" && currentTeamName != null) {

                        TeamProfileScreen(
                            teamId = selectedTeamId,
                            teamName = currentTeamName,
                            fallbackLogoUrl = selectedTeamLogo,
                            repository = teamProfileRepository,
                            standingsRepository = standingsRepository,
                            leagueIdHint = selectedTeamLeagueId,
                            onCoachClick = { coachId, coachName ->
                                selectedCoachId = coachId
                                selectedCoachName = coachName
                            },
                            onBack = {
                                selectedTeamName = null
                                selectedTeamLogo = ""
                                selectedTeamId = null
                                selectedTeamLeagueId = null
                            }
                        )

                    } else if (key == "match" && currentMatchId != null) {

                        MatchDetailScreen(

                            match =
                                matches.find { it.id == currentMatchId },

                            details =
                                matchDetails,

                            detailsLoading =
                                detailsLoading,

                            detailsError =
                                detailsError,

                            detailsRepository =
                                detailsRepository,

                            onRetryDetails = {
                                viewModel.retryMatchDetails()
                            },

                            favoriteTeamNames =
                                favoriteTeamNames,

                            onBack = {
                                viewModel.clearMatchDetails()
                                selectedMatchId = null
                            },

                            onToggleFavorite = { matchId ->
                                viewModel.toggleFavorite(matchId)
                            },

                            onToggleFollowTeam = { teamName ->
                                viewModel.toggleFavoriteTeam(teamName)
                            },

                            onCoachClick = { coachId, coachName ->
                                selectedCoachId = coachId
                                selectedCoachName = coachName
                            },

                            previewRepository = previewRepository,

                            tipStore = tipStore,

                            onTeamClick = { teamName, logoUrl ->
                                val openMatch = matches.find { it.id == currentMatchId }
                                val normTarget = JsonX.norm(teamName)

                                val foundId = when {
                                    openMatch?.homeTeam == teamName && !openMatch.homeTeamId.isNullOrBlank() ->
                                        openMatch.homeTeamId
                                    openMatch?.awayTeam == teamName && !openMatch.awayTeamId.isNullOrBlank() ->
                                        openMatch.awayTeamId
                                    else -> matches.firstNotNullOfOrNull { m ->
                                        when {
                                            JsonX.norm(m.homeTeam) == normTarget && !m.homeTeamId.isNullOrBlank() ->
                                                m.homeTeamId
                                            JsonX.norm(m.awayTeam) == normTarget && !m.awayTeamId.isNullOrBlank() ->
                                                m.awayTeamId
                                            else -> null
                                        }
                                    }
                                }

                                selectedTeamId = foundId
                                selectedTeamLeagueId = openMatch?.leagueId
                                selectedTeamName = teamName
                                selectedTeamLogo = logoUrl
                            }
                        )

                    } else if (key == "league" && currentLeagueId != null) {

                        LeagueScreen(
                            leagueId = currentLeagueId,
                            leagueName = selectedLeagueName,
                            country = selectedLeagueCountry,
                            flag = selectedLeagueFlag,
                            standingsRepository = standingsRepository,
                            teamRepository = teamProfileRepository,
                            leagueMatches = matches.filter { it.leagueId == currentLeagueId },
                            onMatchClick = { matchId ->
                                selectedMatchId = matchId
                                matches.find { it.id == matchId }?.let {
                                    viewModel.openMatchDetails(it)
                                }
                            },
                            onBack = {
                                selectedLeagueId = null
                            }
                        )

                    } else if (key == "tips") {

                        TipsScreen(
                            tipStore = tipStore,
                            onBack = {
                                showTips = false
                            }
                        )

                    } else {

                        MatchListScreen(

                            matches = matches,

                            errorMessage = errorMessage,

                            isLoading = isLoading,

                            statpalKey = statpalKey,

                            selectedOffset = selectedOffset,

                            featuredAdditions = featuredAdditions,

                            featuredRemovals = featuredRemovals,

                            darkMode = darkMode,

                            goalNotificationsEnabled = goalNotificationsEnabled,

                            onRefresh = {
                                viewModel.refreshData()
                            },

                            onDateSelected = { offset ->
                                viewModel.selectOffset(offset)
                            },

                            onSaveKeys = { sKey ->
                                viewModel.saveKeysAndRefresh(sKey)
                            },

                            onToggleFavorite = { matchId ->
                                viewModel.toggleFavorite(matchId)
                            },

                            onToggleFollowTeam = { teamName ->
                                viewModel.toggleFavoriteTeam(teamName)
                            },

                            onToggleFeaturedLeague = { leagueKey, isPreset, currentlyFeatured ->
                                viewModel.toggleFeaturedLeague(leagueKey, isPreset, currentlyFeatured)
                            },

                            onDarkModeChange = { enabled ->
                                viewModel.setDarkMode(enabled)
                            },

                            onGoalNotificationsChange = { enabled ->

                                viewModel.setGoalNotificationsEnabled(enabled)

                                updateGoalNotificationScheduling(enabled)

                                if (enabled) {
                                    requestNotificationPermissionIfNeeded()
                                }
                            },

                            lastUpdated = lastUpdated,

                            onLeagueClick = { leagueId, leagueName, country, flag ->
                                selectedLeagueId = leagueId
                                selectedLeagueName = leagueName
                                selectedLeagueCountry = country
                                selectedLeagueFlag = flag
                            },

                            onOpenTips = {
                                showTips = true
                            },

                            onMatchClick = { matchId ->
                                selectedMatchId = matchId
                                matches.find { it.id == matchId }?.let {
                                    viewModel.openMatchDetails(it)
                                }
                            }
                        )
                    }
                    }
                }
            }
        }
    }
}
