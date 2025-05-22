package com.app.burdii.data.league

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [ForeignKey(
        entity = LeagueRoundEntity::class,
        parentColumns = ["roundId"],
        childColumns = ["roundId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class PlayerScoreEntity(
    @PrimaryKey(autoGenerate = true) val scoreId: Int = 0,
    val roundId: Int,
    val playerName: String,
    val totalScore: Int
)