package com.app.burdii.data.league

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LeagueDao {

    @Insert
    fun insertRound(round: LeagueRoundEntity): Long

    @Insert
    fun insertPlayerScores(scores: List<PlayerScoreEntity>): List<Long>

    @Query("SELECT * FROM leagueRound ORDER BY date DESC")
    fun getAllRounds(): LiveData<List<LeagueRoundEntity>>

    @Query("SELECT * FROM playerScore WHERE roundId = :roundId")
    fun getPlayerScoresForRound(roundId: Int): LiveData<List<PlayerScoreEntity>>

    @Delete
    fun deleteRound(round: LeagueRoundEntity)

    @Query("DELETE FROM leagueRound")
    fun deleteAllRounds()
}