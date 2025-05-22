package com.app.burdii

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import androidx.navigation.findNavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

// FinalScoreActivity is in the same package; explicit import not required.

class HomeActivity : AppCompatActivity() {
    private lateinit var recentRoundsRecyclerView: RecyclerView
    private lateinit var roundScorecardButton: MaterialButton
    private lateinit var leagueScorecardButton: MaterialButton
    private lateinit var seasonLeagueTrackerButton: MaterialButton
    private lateinit var clearButton: Button
    private lateinit var adapter: RoundAdapter
    private var roundsList = mutableListOf<Round>()
    
    // --- SharedPreferences Constants and Gson --- 
    private val PREFS_FILENAME = "com.app.burdii.prefs"
    private val ROUNDS_KEY = "rounds_history"
    private val gson = Gson()
    // ------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide() // Hide the action bar
        setContentView(R.layout.activity_home)
        
        // Apply fade in animation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, R.anim.fade_in, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.fade_in, 0)
        }

        // Initialize views
        // Removed startNewRoundButton and upgradeButton references
        recentRoundsRecyclerView = findViewById(R.id.recentRoundsRecyclerView)

        clearButton = findViewById(R.id.clearButton)
        
        // Load rounds from SharedPreferences instead of sample data
        roundsList = loadRounds().toMutableList()

        // Set up RecyclerView with adapter and click handling
        adapter = RoundAdapter(roundsList) { round -> onRoundSelected(round) }
        recentRoundsRecyclerView.adapter = adapter
        recentRoundsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Get references to the new buttons
        roundScorecardButton = findViewById(R.id.roundScorecardButton)
        leagueScorecardButton = findViewById(R.id.leagueScorecardButton)
        seasonLeagueTrackerButton = findViewById(R.id.seasonLeagueTrackerButton)

        // Button click listeners
        roundScorecardButton.setOnClickListener {
            findNavController(R.id.nav_host_fragment).navigate(R.id.scorecardActivity)
        }

        clearButton.setOnClickListener {
            // Show confirmation dialog before clearing
            showConfirmationDialog()
        }
    }

    // --- SharedPreferences Helper Functions --- 
    private fun saveRounds(rounds: List<Round>) {
        val prefs: SharedPreferences = getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val json = gson.toJson(rounds) // Serialize List<Round> to JSON
        editor.putString(ROUNDS_KEY, json)
        editor.apply() // Use apply for asynchronous saving
    }
    
    private fun loadRounds(): List<Round> {
        val prefs: SharedPreferences = getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
        val json = prefs.getString(ROUNDS_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<List<Round>>() {}.type // Get type token for List<Round>
            try {
                gson.fromJson(json, type) // Deserialize JSON to List<Round>
            } catch (e: Exception) {
                // Handle potential deserialization errors (e.g., corrupted data)
                e.printStackTrace()
                listOf() // Return empty list on error
            }
        } else {
            listOf() // Return empty list if no data saved
        }
    }
    // ------------------------------------------

    private fun showConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear History")
            .setMessage("Are you sure you want to clear all round history?")
            .setPositiveButton("Yes") { _, _ ->
                clearHistory()
            }
            .setNegativeButton("No") { dialog, _ ->
                // Optionally, handle the "No" case, or just dismiss
                dialog.dismiss()
            }
            .show()
    }

    private fun clearHistory() {
        // Clear persistent storage
        saveRounds(listOf()) // Save an empty list
        
        // Clear in-memory list and update UI
        roundsList.clear()
        adapter.notifyDataSetChanged()
        Toast.makeText(this, "History Cleared", Toast.LENGTH_SHORT).show()
    }
    
    override fun onResume() {
        super.onResume()
        // Load rounds and update adapter in case of changes from other activities
        roundsList.clear()
        roundsList.addAll(loadRounds())
        adapter.notifyDataSetChanged()
    }

    /**
     * Handle click on a round item.
     * If the round is complete open final score page; otherwise resume.
     */
    private fun onRoundSelected(round: Round) {
        if (round.isComplete) {
            val intent = Intent(this, FinalScoreActivity::class.java)
            intent.putExtra("ROUND_NAME", round.name)
            // Additional data can be passed later (scores etc.)
            startActivity(intent)
        } else {
            // Resume incomplete round
            val intent = Intent(this, ScorecardActivity::class.java)
            intent.putExtra("RESUME_ROUND_NAME", round.name)
            startActivity(intent)
        }
    }
}
