package com.example.tippscores.data.local

import android.content.Context

/** Követett játékosok helyi tárolása (csillag a profilon és a keretlistákban). */
class FollowStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("tippscores_follow", Context.MODE_PRIVATE)

    private fun ids(): Set<String> = prefs.getStringSet(KEY, emptySet()).orEmpty()

    fun isFollowing(playerId: String?): Boolean =
        !playerId.isNullOrBlank() && ids().contains(playerId)

    /** Visszaadja az új állapotot (true = most már követi). */
    fun toggle(playerId: String): Boolean {
        val current = ids().toMutableSet()
        val nowFollowing = if (current.contains(playerId)) {
            current.remove(playerId)
            false
        } else {
            current.add(playerId)
            true
        }
        prefs.edit().putStringSet(KEY, current).apply()
        return nowFollowing
    }

    private companion object {
        const val KEY = "followed_players"
    }
}
