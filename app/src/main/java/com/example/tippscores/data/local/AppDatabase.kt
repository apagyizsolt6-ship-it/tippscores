package com.example.tippscores.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// v2: új leagueCountryFlag oszlop a "matches" táblában (zászló emoji).
// A "matches" tábla amúgy is csak egy ideiglenes gyorsítótár (minden
// frissítéskor törlődik és újratöltődik a StatPal-ból), ezért egyszerű
// destruktív migrációval oldjuk meg a séma bővítését - nincs olyan adat,
// amit meg kellene őrizni verzióváltáskor.
@Database(entities = [MatchEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun matchDao(): MatchDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tippscores_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
