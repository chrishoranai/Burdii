package com.app.burdii.data.repositories

import com.app.burdii.data.models.firebase.League
import com.app.burdii.data.models.firebase.LeagueScore
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirebaseLeagueRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val leaguesCollection = firestore.collection("leagues")
    private val scoresCollection = firestore.collection("leagueScores")

    suspend fun createLeague(league: League): Result<Unit> = try {
        leaguesCollection.add(league).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getLeague(leagueId: String): Result<League?> = try {
        val document = leaguesCollection.document(leagueId).get().await()
        Result.success(document.toObject(League::class.java))
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getAllLeagues(): Result<List<League>> = try {
        val querySnapshot = leaguesCollection.get().await()
        val leagues = querySnapshot.documents.mapNotNull { it.toObject(League::class.java) }
        Result.success(leagues)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateLeague(leagueId: String, updatedLeague: League): Result<Unit> = try {
        leaguesCollection.document(leagueId).set(updatedLeague).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteLeague(leagueId: String): Result<Unit> = try {
        leaguesCollection.document(leagueId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun addLeagueScore(leagueScore: LeagueScore): Result<Unit> = try {
        scoresCollection.add(leagueScore).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getLeagueScores(leagueId: String): Result<List<LeagueScore>> = try {
        val querySnapshot = scoresCollection.whereEqualTo("leagueId", leagueId).get().await()
        val scores = querySnapshot.documents.mapNotNull { it.toObject(LeagueScore::class.java) }
        Result.success(scores)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getPlayerLeagueScores(leagueId: String, playerId: String): Result<List<LeagueScore>> = try {
        val querySnapshot = scoresCollection
            .whereEqualTo("leagueId", leagueId)
            .whereEqualTo("playerId", playerId)
            .get()
            .await()
        val scores = querySnapshot.documents.mapNotNull { it.toObject(LeagueScore::class.java) }
        Result.success(scores)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
