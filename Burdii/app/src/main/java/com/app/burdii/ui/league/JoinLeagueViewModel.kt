package com.app.burdii.ui.league

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.burdii.data.firebase.FirebaseLeagueRepository
import kotlinx.coroutines.launch

class JoinLeagueViewModel : ViewModel() {

    private val repository = FirebaseLeagueRepository.getInstance()

    private val _joinStatus = MutableLiveData<JoinStatus>()
    val joinStatus: LiveData<JoinStatus> = _joinStatus

    fun joinLeague(accessCode: String) {
        _joinStatus.value = JoinStatus.Loading

        if (accessCode.isBlank()) {
            _joinStatus.value = JoinStatus.Error("Please enter an access code.")
            return
        }

        viewModelScope.launch {
            val result = repository.joinLeague(accessCode.trim())
            if (result.isSuccess) {
                val leagueId = result.getOrNull()
                _joinStatus.value = JoinStatus.Success(leagueId)
            } else {
                _joinStatus.value = JoinStatus.Error(result.exceptionOrNull()?.message ?: "Unknown error joining league.")
            }
        }
    }
}

sealed class JoinStatus {
    object Idle : JoinStatus()
    object Loading : JoinStatus()
    data class Success(val leagueId: String?) : JoinStatus()
    data class Error(val message: String) : JoinStatus()
}