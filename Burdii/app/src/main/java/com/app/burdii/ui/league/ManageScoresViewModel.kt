package com.app.burdii.ui.league

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.app.burdii.data.firebase.FirebaseLeagueRepository
import com.app.burdii.data.models.firebase.LeagueScore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

class ManageScoresViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    private val repository = FirebaseLeagueRepository.getInstance()
    private val leagueId: String = savedStateHandle["leagueId"] ?: throw IllegalArgumentException("leagueId is required")

    private val _isLoading = MutableLiveData<Boolean>(true)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val _actionStatus = MutableLiveData<ActionStatus>()
    val actionStatus: LiveData<ActionStatus> = _actionStatus

    // Flow to hold the pending scores for the current league
    val pendingScores: LiveData<List<LeagueScore>> = flow { // Use flow builder to fetch data once
        _isLoading.postValue(true) // Post value because this is in a coroutine
        _error.postValue(null)
        val userId = repository.getCurrentUserId()
        if (userId == null) {
            _error.postValue("User not authenticated.")
            _isLoading.postValue(false)
            emit(emptyList<LeagueScore>()) // Emit empty list on error
            return@flow
        }
        val result = repository.getPendingScoresForLeague(leagueId, userId)
        _isLoading.postValue(false)

        if (result.isSuccess) {
            emit(result.getOrNull() ?: emptyList<LeagueScore>()) // Emit fetched data
        } else {
            _error.postValue(result.exceptionOrNull()?.message ?: "Error fetching pending scores.")
            emit(emptyList<LeagueScore>()) // Emit empty list on error
        }
    }
    .catch { e -> // Catch any exceptions in the flow
        _error.postValue(e.message ?: "An unexpected error occurred.")
        _isLoading.postValue(false)
        emit(emptyList<LeagueScore>()) // Emit empty list on error
    }
    .asLiveData(viewModelScope.coroutineContext)

    fun reviewScore(scoreId: String, status: String) {
        _actionStatus.value = ActionStatus.Loading(scoreId) // Indicate which score is being acted upon
        viewModelScope.launch {
            val result = repository.reviewScore(scoreId, leagueId, status)
            if (result.isSuccess) {
                _actionStatus.value = ActionStatus.Success("Score reviewed successfully.")
                // Optionally refresh the pending scores list after successful action
                // fetchPendingScores() // If using a function to fetch, or rely on UI updating from list change
            } else {
                _actionStatus.value = ActionStatus.Error(result.exceptionOrNull()?.message ?: "Failed to review score.")
            }
        }
    }
    
    // You might want functions for manual add score as well
    // fun manualAddScore(...)

    fun clearError() {
        _error.value = null
    }
    
    fun clearActionStatus() {
        _actionStatus.value = ActionStatus.Idle
    }
}

sealed class ActionStatus {
    object Idle : ActionStatus()
    data class Loading(val scoreId: String) : ActionStatus() // Indicate which score is loading
    data class Success(val message: String) : ActionStatus()
    data class Error(val message: String) : ActionStatus()
}