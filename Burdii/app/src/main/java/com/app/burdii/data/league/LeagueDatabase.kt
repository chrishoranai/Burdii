package com.app.burdii.data.league

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LeagueRoundEntity::class, PlayerScoreEntity::class], version = 1, exportSchema = false)
abstract class LeagueDatabase : RoomDatabase() {

    abstract fun leagueDao(): LeagueDao

    companion object {
        @Volatile
        private var INSTANCE: LeagueDatabase? = null

        fun getDatabase(context: Context): LeagueDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LeagueDatabase::class.java,
                    "league_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}