package com.app.burdii

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.burdii.data.league.LeagueDatabase
import com.app.burdii.data.league.LeagueRoundEntity
import com.app.burdii.data.league.PlayerScoreEntity
import kotlinx.coroutines.launch
import java.util.*
import android.widget.Button
import android.widget.LinearLayout
import com.google.android.material.button.MaterialButton

class LeagueResultsFragment : Fragment() {

    private lateinit var saveButton: Button

    private var playerNames: Array<String>? = null
    private var scoresByHole: Array<IntArray>? = null
    private var finalScores: IntArray? = null

    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var leagueResultAdapter: LeagueResultAdapter

    private lateinit var leagueDatabase: LeagueDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_league_results, container, false)

        resultsRecyclerView = view.findViewById(R.id.resultsRecyclerView)
        saveButton = view.findViewById(R.id.saveButton)

        // Receive arguments
        arguments?.let {
            playerNames = it.getStringArray("playerNames")
            scoresByHole = it.getSerializable("scoresByHole") as? Array<IntArray>
            finalScores = it.getIntArray("finalScores")
        }

        leagueDatabase = LeagueDatabase.getDatabase(requireContext())

        displayResults()



        saveButton.setOnClickListener {
            saveRound()
        }

        return view
    }


    private fun calculateTotalScores(): List<Pair<String, Int>> {
        val calculatedScores = mutableListOf<Pair<String, Int>>()

        playerNames?.let { names ->
            if (scoresByHole != null) {
                scoresByHole?.forEachIndexed { playerIndex, holeScores ->
                    if (playerIndex >= names.size) {
                        Log.e("LeagueResults", "Player index out of bounds: $playerIndex")
 return@forEachIndexed // Skip this player if index is out of bounds
                    }
                    val totalScore = holeScores.sum()
                    calculatedScores.add(Pair(names[playerIndex], totalScore))
                }
            } else if (finalScores != null) {
 calculatedScores.addAll(names.zip(finalScores!!.toList()))
            }

            // Sort results before saving (optional but good for display consistency later)
 // calculatedScores.sortBy { it.second } // Sorting is usually for display
        }
 return calculatedScores
    }

    private fun saveRound() {
        val calculatedScores = calculateTotalScores()

        if (calculatedScores.isEmpty()) {
 Log.e("LeagueResultsFragment", "No scores to save.")
 return
        }

        lifecycleScope.launch {
            val gameFormat = arguments?.getString("gameFormat") ?: "Unknown"
            val scorekeepingMethod = arguments?.getString("scorekeepingMethod") ?: "Unknown"
            
            val leagueRound = LeagueRoundEntity(
                date = System.currentTimeMillis(),
                gameFormat = gameFormat,
                scorekeepingMethod = scorekeepingMethod
            )
            val roundId = leagueDatabase.leagueDao().insertRound(leagueRound).toInt()

            val playerScores = calculatedScores.map { (playerName, totalScore) ->
                PlayerScoreEntity(
                    roundId = roundId,
                    playerName = playerName,
                    totalScore = totalScore
                )
            }
            leagueDatabase.leagueDao().insertPlayerScores(playerScores)

            // Navigate to SeasonLeagueTrackerFragment after saving
            val action = LeagueResultsFragmentDirections.actionLeagueResultsFragmentToSeasonLeagueTrackerFragment()
 findNavController().navigate(action)
        }
    }

    private fun displayResults() {
        val calculatedScores = calculateTotalScores().sortedBy { it.second } // Sort for display

            // Sort results
            calculatedScores.sortBy { it.second }

            // TODO: Replace TextView with RecyclerView for a more dynamic display
        }
    }
}
        leagueResultAdapter = LeagueResultAdapter(calculatedScores)
        resultsRecyclerView.layoutManager = LinearLayoutManager(context)
        resultsRecyclerView.adapter = leagueResultAdapter
    }

    class LeagueResultAdapter(private val results: List<Pair<String, Int>>) :
        RecyclerView.Adapter<LeagueResultAdapter.ResultViewHolder>() {

        class ResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val playerNameTextView: TextView = itemView.findViewById(R.id.playerNameTextView)
            val scoreTextView: TextView = itemView.findViewById(R.id.scoreTextView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_league_result, parent, false)
            return ResultViewHolder(view)
        }

        override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
            val (playerName, totalScore) = results[position]
            holder.playerNameTextView.text = playerName
            holder.scoreTextView.text = totalScore.toString()
        }

        override fun getItemCount(): Int = results.size
    }
