package com.app.burdii.ui.league

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.app.burdii.data.firebase.FirebaseLeagueRepository
import com.app.burdii.data.models.firebase.League
import com.app.burdii.data.models.firebase.LeagueScore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class LeagueDetailsViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    private val repository = FirebaseLeagueRepository.getInstance()
    private val leagueId: String = savedStateHandle["leagueId"] ?: throw IllegalArgumentException("leagueId is required")

    val league: LiveData<League?> = repository.getLeagueDetailsFlow(leagueId).asLiveData()

    val scores: LiveData<List<LeagueScore>> = repository.getScoresForLeagueFlow(leagueId).asLiveData()

    // You might want to expose members separately if you need a different adapter
    val members: LiveData<List<String>> = league.asLiveData().map {
        it?.members ?: emptyList()
    }
    
    // Example of combining data if needed
//    val leagueDetailsWithScores: LiveData<Pair<League?, List<LeagueScore>>> = combine(league, scores) { league, scores ->
//        Pair(league, scores)
//    }.asLiveData()

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // You'll likely need more ViewModel functions here for actions like:
    // - Navigating to Manage Scores (if host)
    // - Submitting a score (if member)
    // - Inviting players (if host)
}
