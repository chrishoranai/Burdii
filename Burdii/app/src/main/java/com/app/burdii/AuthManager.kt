package com.app.burdii

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

object AuthManager {
    private val auth = FirebaseAuth.getInstance()
    
    fun getCurrentUser(): FirebaseUser? = auth.currentUser
    
    fun isUserSignedIn(): Boolean = getCurrentUser() != null
    
    suspend fun signInAnonymously(): Result<FirebaseUser> = try {
        val result = auth.signInAnonymously().await()
        result.user?.let { user ->
            Result.success(user)
        } ?: Result.failure(Exception("Authentication failed"))
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    fun signOut() {
        auth.signOut()
    }
    
    suspend fun updateDisplayName(displayName: String): Result<Unit> = try {
        val user = getCurrentUser() ?: throw Exception("No user signed in")
        val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
            this.displayName = displayName
        }
        user.updateProfile(profileUpdates).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}