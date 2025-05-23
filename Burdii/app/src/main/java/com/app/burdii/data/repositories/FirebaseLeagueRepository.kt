package com.app.burdii.data.repositories

import com.app.burdii.data.models.firebase.League
import com.app.burdii.data.models.firebase.LeagueScore
import com.app.burdii.data.models.firebase.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class FirebaseLeagueRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val leaguesCollection = firestore.collection("leagues")
    private val scoresCollection = firestore.collection("leagueScores")
    private val usersCollection = firestore.collection("users")

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
    
    // New methods for Phase 2
    
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    
    fun getCurrentUserDisplayName(): String? {
        return auth.currentUser?.displayName
    }
    
    suspend fun createUserProfile(userProfile: UserProfile): Result<Unit> = try {
        usersCollection.document(userProfile.uid).set(userProfile).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    suspend fun getUserProfile(uid: String): Result<UserProfile?> = try {
        val document = usersCollection.document(uid).get().await()
        Result.success(document.toObject(UserProfile::class.java))
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    suspend fun updateUserProfile(userProfile: UserProfile): Result<Unit> = try {
        usersCollection.document(userProfile.uid).set(userProfile).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    suspend fun getHostedLeagues(hostId: String): Result<List<League>> = try {
        val querySnapshot = leaguesCollection
            .whereEqualTo("hostId", hostId)
            .get()
            .await()
        val leagues = querySnapshot.documents.mapNotNull { 
            it.toObject(League::class.java)?.apply { leagueId = it.id }
        }
        Result.success(leagues)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    suspend fun getJoinedLeagues(userId: String): Result<List<League>> = try {
        val querySnapshot = leaguesCollection
            .whereArrayContains("members", userId)
            .get()
            .await()
        val leagues = querySnapshot.documents.mapNotNull { 
            it.toObject(League::class.java)?.apply { leagueId = it.id }
        }
        Result.success(leagues)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    fun getLeagueDetailsFlow(leagueId: String): Flow<League?> = flow {
        try {
            val document = leaguesCollection.document(leagueId).get().await()
            emit(document.toObject(League::class.java)?.apply { this.leagueId = document.id })
        } catch (e: Exception) {
            emit(null)
        }
    }
    
    fun getScoresForLeagueFlow(leagueId: String): Flow<List<LeagueScore>> = flow {
        try {
            val querySnapshot = scoresCollection
                .whereEqualTo("leagueId", leagueId)
                .whereEqualTo("status", "approved")
                .orderBy("weekNumber", Query.Direction.DESCENDING)
                .get()
                .await()
            val scores = querySnapshot.documents.mapNotNull { 
                it.toObject(LeagueScore::class.java)?.apply { scoreId = it.id }
            }
            emit(scores)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    suspend fun getPendingScoresForLeague(leagueId: String): Result<List<LeagueScore>> = try {
        val querySnapshot = scoresCollection
            .whereEqualTo("leagueId", leagueId)
            .whereEqualTo("status", "pending")
            .orderBy("submittedAt", Query.Direction.DESCENDING)
            .get()
            .await()
        val scores = querySnapshot.documents.mapNotNull { 
            it.toObject(LeagueScore::class.java)?.apply { scoreId = it.id }
        }
        Result.success(scores)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    suspend fun reviewScore(scoreId: String, approved: Boolean, reviewerId: String): Result<Unit> = try {
        val updateData = mapOf(
            "status" to if (approved) "approved" else "denied",
            "reviewedBy" to reviewerId,
            "reviewedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        scoresCollection.document(scoreId).update(updateData).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    suspend fun submitScore(leagueScore: LeagueScore): Result<Unit> = try {
        scoresCollection.add(leagueScore).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    suspend fun joinLeagueByAccessCode(accessCode: String, userId: String, userName: String): Result<League?> = try {
        val querySnapshot = leaguesCollection
            .whereEqualTo("accessCode", accessCode)
            .limit(1)
            .get()
            .await()
            
        if (querySnapshot.documents.isEmpty()) {
            Result.failure(Exception("League not found"))
        } else {
            val document = querySnapshot.documents.first()
            val league = document.toObject(League::class.java)?.apply { leagueId = document.id }
            
            if (league != null && !league.members.contains(userId)) {
                val updatedMembers = league.members + userId
                val updatedMemberNames = league.memberNames + (userId to userName)
                
                val updateData = mapOf(
                    "members" to updatedMembers,
                    "memberNames" to updatedMemberNames
                )
                
                leaguesCollection.document(document.id).update(updateData).await()
                
                // Update user profile
                val userProfile = getUserProfile(userId).getOrNull()
                if (userProfile != null) {
                    val updatedJoinedLeagues = userProfile.joinedLeagues + document.id
                    updateUserProfile(userProfile.copy(joinedLeagues = updatedJoinedLeagues))
                }
                
                Result.success(league.copy(
                    members = updatedMembers,
                    memberNames = updatedMemberNames
                ))
            } else {
                Result.success(league)
            }
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    suspend fun createLeagueWithCode(league: League): Result<String> = try {
        val accessCode = generateAccessCode()
        val leagueWithCode = league.copy(accessCode = accessCode)
        val documentRef = leaguesCollection.add(leagueWithCode).await()
        
        // Update the document with its ID
        leaguesCollection.document(documentRef.id).update("leagueId", documentRef.id).await()
        
        // Update user profile
        val userId = getCurrentUserId()
        if (userId != null) {
            val userProfile = getUserProfile(userId).getOrNull()
            if (userProfile != null) {
                val updatedHostedLeagues = userProfile.hostedLeagues + documentRef.id
                updateUserProfile(userProfile.copy(hostedLeagues = updatedHostedLeagues))
            }
        }
        
        Result.success(documentRef.id)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    private fun generateAccessCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6).map { chars.random() }.joinToString("")
    }
}
