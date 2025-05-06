package com.app.burdii

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.app.burdii.RoundAdapter
import com.app.burdii.Round

class HomeActivity : AppCompatActivity() {
    private lateinit var recentRoundsRecyclerView: RecyclerView
    private lateinit var startNewRoundButton: MaterialButton
    private lateinit var clearButton: Button
    private lateinit var adapter: RoundAdapter
    private val roundsList = mutableListOf<Round>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Initialize views
        recentRoundsRecyclerView = findViewById(R.id.recentRoundsRecyclerView)
        startNewRoundButton = findViewById(R.id.startNewRoundButton)
        clearButton = findViewById(R.id.clearButton)

        loadSampleData()

        // Set up RecyclerView with adapter
        adapter = RoundAdapter(roundsList)
        recentRoundsRecyclerView.adapter = adapter
        recentRoundsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Button click listeners
        startNewRoundButton.setOnClickListener {
            // Navigate to setup activity
            val intent = Intent(this, SetupActivity::class.java)
            startActivity(intent)
        }

        clearButton.setOnClickListener {
            // Show confirmation dialog before clearing
            AlertDialog.Builder(this)
                .setTitle("Clear Recent Rounds")
                .setMessage("Are you sure you want to clear all recent rounds?")
                .setPositiveButton("Clear") { _, _ ->
                    // TODO: Implement actual clearing from storage
                    clearHistory()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun loadSampleData() {
        // Clear existing data
        roundsList.clear()
        // Add sample rounds (replace with actual data loading logic later)
        roundsList.add(Round(date = "05/06/2024", scoreChange = "+5", holesPlayed = "18 Holes"))
        roundsList.add(Round(date = "05/04/2024", scoreChange = "E", holesPlayed = "9 Holes"))
        roundsList.add(Round(date = "05/01/2024", scoreChange = "-2", holesPlayed = "18 Holes"))
        roundsList.add(Round(date = "04/28/2024", scoreChange = "+1", holesPlayed = "18 Holes"))
        roundsList.add(Round(date = "04/25/2024", scoreChange = "-1", holesPlayed = "9 Holes"))

        // Notify adapter
        adapter.notifyDataSetChanged()
    }

    private fun clearHistory() {
        roundsList.clear()
        adapter.notifyDataSetChanged()
        Toast.makeText(this, "History Cleared", Toast.LENGTH_SHORT).show()
    }
}
