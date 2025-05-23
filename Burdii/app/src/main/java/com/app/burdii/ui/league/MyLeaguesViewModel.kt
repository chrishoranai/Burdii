package com.app.burdii.ui.league

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.burdii.data.repositories.FirebaseLeagueRepository
import com.app.burdii.data.models.firebase.League
import kotlinx.coroutines.launch

class MyLeaguesViewModel : ViewModel() {

    private val repository = FirebaseLeagueRepository()

    private val _hostedLeagues = MutableLiveData<List<League>>()
    val hostedLeagues: LiveData<List<League>> = _hostedLeagues

    private val _joinedLeagues = MutableLiveData<List<League>>()
    val joinedLeagues: LiveData<List<League>> = _joinedLeagues
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    init {
        loadMyLeagues()
    }

    fun loadMyLeagues() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            val userId = repository.getCurrentUserId()
            if (userId == null) {
                _errorMessage.value = "User not authenticated."
                _isLoading.value = false
                return@launch
            }

            val hostedResult = repository.getHostedLeagues(userId)
            if (hostedResult.isSuccess) {
                _hostedLeagues.value = hostedResult.getOrNull() ?: emptyList()
            } else {
                _errorMessage.value = hostedResult.exceptionOrNull()?.message ?: "Error loading hosted leagues."
                _hostedLeagues.value = emptyList()
            }

            val joinedResult = repository.getJoinedLeagues(userId)
            if (joinedResult.isSuccess) {
                _joinedLeagues.value = joinedResult.getOrNull() ?: emptyList()
            } else {
                 _errorMessage.value = (_errorMessage.value ?: "") + (joinedResult.exceptionOrNull()?.message ?: "Error loading joined leagues.")
                 _joinedLeagues.value = emptyList()
            }
            _isLoading.value = false
        }
    }
}
