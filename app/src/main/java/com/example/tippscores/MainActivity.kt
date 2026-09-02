package com.example.tippscores

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
import com.example.tippscores.data.repository.MatchRepository
import com.example.tippscores.notifications.GoalCheckWorker
import com.example.tippscores.ui.screens.MatchDetailScreen
import com.example.tippscores.ui.screens.MatchListScreen
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

    private val detailsRepository by lazy {
        com.example.tippscores.data.repository.MatchDetailsRepository(
            statpalKeyProvider = { apiPreferences.statpalApiKey },
            highlightlyKeyProvider = { apiPreferences.highlightlyApiKey }
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

    // A rendszer értesítési engedélykérő ablaka (Android 13+). Az
    // eredményt nem kell külön kezelni: ha megtagadja, a
    // NotificationHelper úgyis csendben elnyeli a próbálkozást.
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

        // Ha korábban már be volt kapcsolva a gólértesítés, induláskor
        // is biztosítsuk, hogy az ütemezett háttérellenőrzés fusson
        // (pl. újraindítás vagy frissítés után is).
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

            val highlightlyKey by
                viewModel.highlightlyKey.collectAsState()

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

            MaterialTheme(
                colorScheme =
                    if (darkMode) darkColorScheme() else lightColorScheme()
            ) {

                TippScoresTheme(
                    darkMode = darkMode
                ) {

                    val currentMatchId = selectedMatchId

                    if (currentMatchId != null) {

                        MatchDetailScreen(

                            match =
                                matches.find { it.id == currentMatchId },

                            details =
                                matchDetails,

                            detailsLoading =
                                detailsLoading,

                            detailsError =
                                detailsError,

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
                            }
                        )

                    } else {

                        MatchListScreen(

                            matches = matches,

                            errorMessage = errorMessage,

                            isLoading = isLoading,

                            statpalKey = statpalKey,

                            highlightlyKey = highlightlyKey,

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

                            onSaveKeys = { sKey, hKey ->

                                viewModel.saveKeysAndRefresh(
                                    sKey,
                                    hKey
                                )
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
