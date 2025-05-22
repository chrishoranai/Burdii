package com.app.burdii

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete

@Dao
interface RoundDao {
    @Insert
    suspend fun insertRound(round: Round): Long

    @Query("SELECT * FROM rounds ORDER BY id DESC")
    suspend fun getAllRounds(): List<Round>

    @Query("SELECT * FROM rounds WHERE id = :roundId")
    suspend fun getRoundById(roundId: Long): Round?

    @Delete
    suspend fun deleteRound(round: Round)
}