package com.app.burdii.ui.league

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.app.burdii.data.models.firebase.League
import com.app.burdii.data.models.firebase.LeagueScore
import com.app.burdii.data.repositories.FirebaseLeagueRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class LeagueDetailsViewModel : ViewModel() {

    private val repository = FirebaseLeagueRepository()
    
    private val _league = MutableLiveData<League?>()
    val league: LiveData<League?> = _league
    
    private val _scores = MutableLiveData<List<LeagueScore>>()
    val scores: LiveData<List<LeagueScore>> = _scores
    
    private val _isCurrentUserHost = MutableLiveData<Boolean>()
    val isCurrentUserHost: LiveData<Boolean> = _isCurrentUserHost

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
    
    fun loadLeagueDetails(leagueId: String) {
        _isLoading.value = true
        
        viewModelScope.launch {
            // Load league details
            val leagueResult = repository.getLeague(leagueId)
            if (leagueResult.isSuccess) {
                val league = leagueResult.getOrNull()
                _league.value = league
                
                // Check if current user is host
                val currentUserId = repository.getCurrentUserId()
                _isCurrentUserHost.value = currentUserId != null && currentUserId == league?.hostId
            } else {
                _errorMessage.value = "Failed to load league details"
            }
            
            // Load scores
            viewModelScope.launch {
                repository.getScoresForLeagueFlow(leagueId).asLiveData().observeForever { scoresList ->
                    _scores.value = scoresList
                }
            }
            
            _isLoading.value = false
        }
    }
}
