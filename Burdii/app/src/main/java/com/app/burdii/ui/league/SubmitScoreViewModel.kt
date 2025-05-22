package com.app.burdii.ui.league

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.burdii.data.firebase.FirebaseLeagueRepository
import kotlinx.coroutines.launch

class SubmitScoreViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    private val repository = FirebaseLeagueRepository.getInstance()
    private val leagueId: String = savedStateHandle["leagueId"] ?: throw IllegalArgumentException("leagueId is required")

    private val _submitStatus = MutableLiveData<SubmitStatus>()
    val submitStatus: LiveData<SubmitStatus> = _submitStatus

    fun submitScore(weekNumber: String, scoreValue: String) {
        _submitStatus.value = SubmitStatus.Loading

        val weekInt = weekNumber.toIntOrNull()
        val scoreInt = scoreValue.toIntOrNull()

        if (weekInt == null || scoreInt == null || weekInt <= 0 || scoreInt < 0) {
            _submitStatus.value = SubmitStatus.Error("Please enter valid week number and score.")
            return
        }

        viewModelScope.launch {
            // TODO: Optionally pass roundIdForApp if you want to link this Firebase score to a local round
            val result = repository.submitScore(leagueId, weekInt, scoreInt)
            if (result.isSuccess) {
                _submitStatus.value = SubmitStatus.Success("Score submitted successfully!")
            } else {
                _submitStatus.value = SubmitStatus.Error(result.exceptionOrNull()?.message ?: "Unknown error submitting score.")
            }
        }
    }
    
    fun clearStatus() {
        _submitStatus.value = SubmitStatus.Idle
    }
}

sealed class SubmitStatus {
    object Idle : SubmitStatus()
    object Loading : SubmitStatus()
    data class Success(val message: String) : SubmitStatus()
    data class Error(val message: String) : SubmitStatus()
}