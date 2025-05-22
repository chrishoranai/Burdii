package com.app.burdii.data.league

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "league_rounds")
data class LeagueRoundEntity(
    @PrimaryKey(autoGenerate = true)
    val roundId: Int = 0,
    val date: Long,
    val gameFormat: String,
    val scorekeepingMethod: String
)