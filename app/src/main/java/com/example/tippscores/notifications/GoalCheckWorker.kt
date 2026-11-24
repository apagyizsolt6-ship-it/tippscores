package com.example.tippscores.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.example.tippscores.data.local.ApiPreferences
import com.example.tippscores.data.remote.NetworkModule
import com.example.tippscores.data.remote.StatpalMatchItem
import kotlinx.coroutines.CancellationException

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
            val state = applicationContext.getSharedPreferences(
                "tippscores_worker_state",
                Context.MODE_PRIVATE
            )
            val stateEditor = state.edit()
            val liveMatchIds = mutableSetOf<String>()
            val seenStatusKeys = mutableSetOf<String>()

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

                    if (mainId != null && mainId.isNotBlank()) liveMatchIds.add(mainId)

                    // --- Kezdés / félidő / vége értesítések ---
                    val statusNow = match.status?.trim()?.uppercase().orEmpty()
                    val statusBefore = state.getString("status:$localId", null)
                    val scoreText = "$homeName ${homeScore ?: 0} - ${awayScore ?: 0} $awayName"
                    val finished = statusNow in FINISHED_STATUSES
                    val halfTime = statusNow in HALF_TIME_STATUSES
                    val notificationBase = localId.hashCode() and 0x3FFFFFFF

                    val minuteNow = statusNow.filter { it.isDigit() }.toIntOrNull()
                    val justStarted = minuteNow == null || minuteNow <= 15

                    if (statusBefore == null && !finished && !halfTime && justStarted) {
                        NotificationHelper.showGoalNotification(
                            context = applicationContext,
                            notificationId = notificationBase + 1,
                            title = "▶ Elkezdődött a meccs",
                            text = "$homeName – $awayName"
                        )
                    }

                    if (halfTime && statusBefore != null && statusBefore != statusNow) {
                        NotificationHelper.showGoalNotification(
                            context = applicationContext,
                            notificationId = notificationBase + 2,
                            title = "⏸ Félidő",
                            text = scoreText
                        )
                    }

                    if (finished && statusBefore != null && statusBefore !in FINISHED_STATUSES) {
                        NotificationHelper.showGoalNotification(
                            context = applicationContext,
                            notificationId = notificationBase + 3,
                            title = "🏁 Vége a meccsnek",
                            text = scoreText
                        )
                    }

                    stateEditor.putString("status:$localId", statusNow)
                    seenStatusKeys.add("status:$localId")

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

            // --- Kezdőcsapat-értesítés a kedvenc (még nem kezdődött) meccsekre ---
            checkLineups(
                key = statpalKey,
                favoriteMatchIds = favoriteMatchIds - liveMatchIds,
                state = state,
                stateEditor = stateEditor
            )

            // A régi, már nem látott meccsállapotokat időnként töröljük.
            val oldStatusKeys = state.all.keys.filter { it.startsWith("status:") }
            if (oldStatusKeys.size > 400) {
                oldStatusKeys
                    .filterNot { it in seenStatusKeys }
                    .forEach { stateEditor.remove(it) }
            }

            stateEditor.apply()

            Result.success()

        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    /**
     * A kedvenc mérkőzésekre (legfeljebb 5 darab futásonként) megnézi, hogy
     * megjelent-e az összeállítás; ha igen, egyszer értesít. Egy meccsre
     * legfeljebb 40 próbálkozás (kb. 10 óra) van, hogy ne fogyjon feleslegesen
     * az API-keret.
     */
    private suspend fun checkLineups(
        key: String,
        favoriteMatchIds: Set<String>,
        state: android.content.SharedPreferences,
        stateEditor: android.content.SharedPreferences.Editor
    ) {
        var checked = 0

        for (matchId in favoriteMatchIds) {
            if (checked >= 5) break
            if (state.getBoolean("lineupNotified:$matchId", false)) continue

            val attempts = state.getInt("lineupTries:$matchId", 0)
            if (attempts >= 40) continue

            checked++
            stateEditor.putInt("lineupTries:$matchId", attempts + 1)

            try {
                val json = NetworkModule.statpalMatchDetailsApi.getDetails(
                    url = "https://statpal.io/api/v2/soccer/team-lineups?match_id=$matchId",
                    accessKey = key
                )
                val text = json.toString().lowercase()
                val hasLineup = text.contains("starting_xi") || text.contains("startxi")

                if (hasLineup) {
                    val home = json.getAsJsonObject("home")?.get("team_name")?.asString.orEmpty()
                    val away = json.getAsJsonObject("away")?.get("team_name")?.asString.orEmpty()

                    NotificationHelper.showGoalNotification(
                        context = applicationContext,
                        notificationId = (matchId.hashCode() and 0x3FFFFFFF) + 4,
                        title = "📋 Megvan a kezdőcsapat",
                        text = if (home.isNotBlank() && away.isNotBlank()) "$home – $away" else "Kedvenc meccsed összeállítása elérhető"
                    )
                    stateEditor.putBoolean("lineupNotified:$matchId", true)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    private companion object {
        val FINISHED_STATUSES = setOf("FT", "AET", "PEN", "AP", "FINISHED")
        val HALF_TIME_STATUSES = setOf("HT", "HALF TIME", "HALFTIME", "PAUSE", "PAUSED")
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
