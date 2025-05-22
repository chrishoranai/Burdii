package com.app.burdii

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [Round::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class) // Assuming you have or will create a Converters class
abstract class AppDatabase : RoomDatabase() {

    abstract fun roundDao(): RoundDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "burdii_database"
                )
                    .fallbackToDestructiveMigration() // Or handle migrations properly
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}