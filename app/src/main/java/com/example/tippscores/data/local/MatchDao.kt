package com.example.tippscores.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches")
    fun getAllMatches(): Flow<List<MatchEntity>>

    // Egyszeri (nem Flow) lekérdezés - ez kell ahhoz, hogy egy új
    // frissítés előtt megnézhessük a RÉGI állást (pl. gólesemény
    // felismeréséhez: nőtt-e valamelyik csapat gólszáma az előző
    // frissítés óta).
    @Query("SELECT * FROM matches")
    suspend fun getAllMatchesSnapshot(): List<MatchEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatches(matches: List<MatchEntity>)

    @Query("DELETE FROM matches")
    suspend fun clearMatches()
}
