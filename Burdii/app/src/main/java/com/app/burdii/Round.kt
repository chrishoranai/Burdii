package com.app.burdii

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rounds")
data class Round(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val date: String,
    // Consider if scoreChange and holesPlayed are needed in the database entity
    // They seem like display properties for the home screen.
    // For now, keeping them but might need adjustment based on actual data storage.
    val scoreChange: String,
    val holesPlayed: String,
    val isComplete: Boolean,
    val currentHole: Int = 1 // Default to first hole if not specified
)