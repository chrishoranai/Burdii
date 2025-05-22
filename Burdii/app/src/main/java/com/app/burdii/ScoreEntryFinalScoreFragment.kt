package com.app.burdii

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import android.widget.Toast

class ScoreEntryFinalScoreFragment : Fragment() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var nextButton: Button
    private lateinit var playerNames: List<String>
    private val finalScores = mutableMapOf<String, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            playerNames = it.getStringArrayList("playerNames") ?: emptyList()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_score_entry_final_score, container, false)

        recyclerView = view.findViewById(R.id.finalScoresRecyclerView)
        nextButton = view.findViewById(R.id.nextButton)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = FinalScoreAdapter(playerNames, finalScores, layoutInflater)
        
        nextButton.setOnClickListener {
            var allScoresEntered = true
            val scoresArray = IntArray(playerNames.size)
            
            for ((index, playerName) in playerNames.withIndex()) {
 val score = finalScores[playerName] // Using the map updated by TextChangedListener
                if (score == null) {
                    allScoresEntered = false
                    // You might want to highlight the missing score field in the UI
                    break
                }
                scoresArray[index] = score
            }
            
            if (allScoresEntered) {
                val action = ScoreEntryFinalScoreFragmentDirections.actionScoreEntryFinalScoreFragmentToLeagueResultsFragment(
                    playerNames.toTypedArray(), null, scoresArray)
                findNavController().navigate(action)
            } else {
 Toast.makeText(requireContext(), "Please enter scores for all players.", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    class FinalScoreAdapter(private val players: List<String>, private val scores: MutableMap<String, Int>, private val inflater: LayoutInflater) :
 RecyclerView.Adapter<FinalScoreAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val playerNameTextView: TextView = view.findViewById(R.id.playerNameTextView)
            val finalScoreEditText: EditText = view.findViewById(R.id.scoreEditText)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_final_score_entry, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val playerName = players[position]
            holder.playerNameTextView.text = playerName
 // Set text without triggering the listener initially
            val initialScore = scores[playerName]
            holder.finalScoreEditText.setText(if (initialScore != null) initialScore.toString() else "")

            // Remove any existing listeners to prevent multiple calls
            holder.finalScoreEditText.addTextChangedListener(null)

            holder.finalScoreEditText.addTextChangedListener { text ->
                val scoreText = text.toString()
                val score = scoreText.toIntOrNull()
 if (score != null) { // Update the map only if the score is a valid integer
 scores[playerName] = score
 holder.finalScoreEditText.error = null // Clear any previous error
                } else if (scoreText.isNotEmpty()) {
                    // Optionally set an error if the text is not empty but not a number
                }
            }
        }

        override fun getItemCount(): Int {
            return players.size
        }
    }
}