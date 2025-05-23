package com.app.burdii.ui.league

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.burdii.data.models.firebase.League
import com.app.burdii.data.models.firebase.LeagueScore
import com.app.burdii.data.repositories.FirebaseLeagueRepository
import kotlinx.coroutines.launch

class SubmitScoreViewModel : ViewModel() {

    private val repository = FirebaseLeagueRepository()

    private val _league = MutableLiveData<League?>()
    val league: LiveData<League?> = _league
    
    private val _submitResult = MutableLiveData<Result<Unit>>()
    val submitResult: LiveData<Result<Unit>> = _submitResult
    
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    
    fun loadLeagueDetails(leagueId: String) {
        viewModelScope.launch {
            _loading.value = true
            val result = repository.getLeague(leagueId)
            _league.value = result.getOrNull()
            _loading.value = false
        }
    }

    fun submitScore(leagueId: String, weekNumber: Int, scoreValue: Int) {
        val currentUserId = repository.getCurrentUserId()
        val currentUserName = repository.getCurrentUserDisplayName()
        
        if (currentUserId == null || currentUserName == null) {
            _submitResult.value = Result.failure(Exception("User not authenticated"))
            return
        }
        
        _loading.value = true

        viewModelScope.launch {
            val leagueScore = LeagueScore(
                leagueId = leagueId,
                playerId = currentUserId,
                playerName = currentUserName,
                weekNumber = weekNumber,
                scoreValue = scoreValue,
                status = "pending"
            )
            
            val result = repository.submitScore(leagueScore)
            _submitResult.value = result
            _loading.value = false
        }
    }
}

