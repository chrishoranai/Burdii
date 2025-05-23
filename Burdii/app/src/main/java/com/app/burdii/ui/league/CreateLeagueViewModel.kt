package com.app.burdii.ui.league

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.burdii.data.models.firebase.League
import com.app.burdii.data.repositories.FirebaseLeagueRepository
import kotlinx.coroutines.launch

class CreateLeagueViewModel : ViewModel() {

    private val repository = FirebaseLeagueRepository()

    private val _creationStatus = MutableLiveData<CreationStatus>()
    val creationStatus: LiveData<CreationStatus> = _creationStatus

    fun createLeague(leagueName: String, numberOfWeeks: String) {
        _creationStatus.value = CreationStatus.Loading

        val numWeeksInt = numberOfWeeks.toIntOrNull()

        if (leagueName.isBlank() || numWeeksInt == null || numWeeksInt <= 0) {
            _creationStatus.value = CreationStatus.Error("Please enter a valid league name and number of weeks.")
            return
        }

        viewModelScope.launch {
            val currentUserId = repository.getCurrentUserId()
            val currentUserName = repository.getCurrentUserDisplayName()
            
            if (currentUserId == null || currentUserName == null) {
                _creationStatus.value = CreationStatus.Error("User not authenticated.")
                return@launch
            }
            
            val league = League(
                name = leagueName,
                hostId = currentUserId,
                hostName = currentUserName,
                numberOfWeeks = numWeeksInt,
                members = listOf(currentUserId),
                memberNames = mapOf(currentUserId to currentUserName)
            )
            
            val result = repository.createLeagueWithCode(league)
            if (result.isSuccess) {
                val leagueId = result.getOrNull()
                // Get the access code from the created league
                val createdLeague = repository.getLeague(leagueId ?: "").getOrNull()
                _creationStatus.value = CreationStatus.Success(leagueId, createdLeague?.accessCode)
            } else {
                _creationStatus.value = CreationStatus.Error(result.exceptionOrNull()?.message ?: "Unknown error creating league.")
            }
        }
    }
}

sealed class CreationStatus {
    object Idle : CreationStatus()
    object Loading : CreationStatus()
    data class Success(val leagueId: String?, val accessCode: String?) : CreationStatus()
    data class Error(val message: String) : CreationStatus()
}