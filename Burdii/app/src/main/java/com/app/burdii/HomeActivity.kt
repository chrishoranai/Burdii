package com.app.burdii

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HomeActivity : AppCompatActivity() {
    private lateinit var recentRoundsRecyclerView: RecyclerView
    private lateinit var startNewRoundButton: MaterialButton
    private lateinit var clearButton: Button
    private lateinit var upgradeButton: Button
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
        overridePendingTransition(R.anim.fade_in, 0)

        // Initialize views
        recentRoundsRecyclerView = findViewById(R.id.recentRoundsRecyclerView)
        startNewRoundButton = findViewById(R.id.startNewRoundButton)
        clearButton = findViewById(R.id.clearButton)
        upgradeButton = findViewById(R.id.upgradeButton)
        
        // Load rounds from SharedPreferences instead of sample data
        roundsList = loadRounds().toMutableList()

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
            showConfirmationDialog()
        }
        
        upgradeButton.setOnClickListener {
            Toast.makeText(this, "Burdii Pro features are coming soon!", Toast.LENGTH_LONG).show()
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
            .setPositiveButton("Yes") { dialog, which ->
                clearHistory()
            }
            .setNegativeButton("No") { dialog, which ->
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
}
