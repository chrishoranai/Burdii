package com.app.burdii.ui.league

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.burdii.data.firebase.FirebaseLeagueRepository
import kotlinx.coroutines.launch

class CreateLeagueViewModel : ViewModel() {

    private val repository = FirebaseLeagueRepository.getInstance()

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
            val result = repository.createLeague(leagueName, numWeeksInt)
            if (result.isSuccess) {
                val (leagueId, accessCode) = result.getOrThrow()
                _creationStatus.value = CreationStatus.Success(leagueId, accessCode)
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