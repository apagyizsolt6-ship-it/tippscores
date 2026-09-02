package com.example.tippscores.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.example.tippscores.data.local.ApiPreferences
import com.example.tippscores.data.remote.NetworkModule
import com.example.tippscores.data.remote.StatpalMatchItem

/**
 * Kb. 15 percenként lefutó háttérellenőrzés (a WorkManager minimum
 * periodikus intervalluma ennyi - ennél gyakoribb, valódi real-time
 * push csak külön szerver-oldali komponenssel lenne megoldható).
 *
 * Megnézi az ÉLŐ meccseket, összeveti az előző futáskor mentett
 * állással, és ha egy KÖVETETT (kedvenc) csapatnál vagy KEDVENC
 * meccsnél nőtt a gólszám, helyi értesítést küld.
 */
class GoalCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {

        val prefs = ApiPreferences(applicationContext)

        if (!prefs.goalNotificationsEnabled) {
            return Result.success()
        }

        val statpalKey = prefs.statpalApiKey.trim()

        if (statpalKey.isEmpty()) {
            return Result.success()
        }

        return try {

            val response = NetworkModule.statpalApi.getLiveMatches(statpalKey)
            val leagues = response.data?.leagues.orEmpty()

            val favoriteMatchIds = prefs.favoriteMatchIds
            val favoriteTeamNames = prefs.favoriteTeamNames
            val previousScores = prefs.lastKnownScores

            val newScores = mutableMapOf<String, String>()

            leagues.forEach { league ->

                league.matches.orEmpty().forEach { match ->

                    val localId = localMatchId(match)

                    val homeScore = match.home?.goals?.toIntOrNull()
                    val awayScore = match.away?.goals?.toIntOrNull()

                    newScores[localId] = "${homeScore ?: -1}-${awayScore ?: -1}"

                    val mainId = match.mainId
                    val homeName = match.home?.name.orEmpty()
                    val awayName = match.away?.name.orEmpty()

                    val isRelevant =
                        (mainId != null && mainId.isNotBlank() && favoriteMatchIds.contains(mainId)) ||
                            favoriteTeamNames.contains(homeName) ||
                            favoriteTeamNames.contains(awayName)

                    if (!isRelevant) {
                        return@forEach
                    }

                    val previous = previousScores[localId]?.split("-")
                    val prevHome = previous?.getOrNull(0)?.toIntOrNull()
                    val prevAway = previous?.getOrNull(1)?.toIntOrNull()

                    val homeGoal =
                        prevHome != null && homeScore != null && homeScore > prevHome

                    val awayGoal =
                        prevAway != null && awayScore != null && awayScore > prevAway

                    if (homeGoal || awayGoal) {

                        val scorer = if (homeGoal) homeName else awayName

                        NotificationHelper.showGoalNotification(
                            context = applicationContext,
                            notificationId = (localId.hashCode() and 0x7FFFFFFF),
                            title = "⚽ Gól! $scorer",
                            text = "$homeName ${homeScore ?: 0} - ${awayScore ?: 0} $awayName"
                        )
                    }
                }
            }

            prefs.lastKnownScores = newScores

            Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun localMatchId(match: StatpalMatchItem): String {
        return match.mainId?.takeIf { it.isNotBlank() }
            ?: listOf(
                match.date.orEmpty(),
                match.time.orEmpty(),
                match.home?.name.orEmpty(),
                match.away?.name.orEmpty()
            ).joinToString("|")
    }
}
