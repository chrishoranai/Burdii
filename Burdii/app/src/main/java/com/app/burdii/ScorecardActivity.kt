package com.app.burdii

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class ScorecardActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "ScorecardActivity"
    }
    
    private lateinit var currentHoleTextView: TextView
    private lateinit var scorecardTable: TableLayout
    private lateinit var finishRoundButton: Button
    private lateinit var nextHoleButton: ImageButton
    private lateinit var previousHoleButton: ImageButton
    private lateinit var previousHoleText: TextView
    private lateinit var nextHoleText: TextView
    private var currentHole = 1
    private var currentPlayerIndex = 0
    private lateinit var playerNames: Array<String>
    private var numPlayers: Int = 0
    private var numHoles: Int = 0
    private lateinit var parValues: IntArray
    private lateinit var scores: Array<IntArray>
    private lateinit var scoreEditTexts: Array<Array<EditText>>
    private lateinit var totalTextViews: Array<TextView>
    private lateinit var scoringMethod: String
    private var roundName: String = "New Round"

    // --- SharedPreferences Constants and Gson --- 
    private val PREFS_FILENAME = "com.app.burdii.prefs"
    private val ROUNDS_KEY = "rounds_history"
    private val gson = Gson()
    // ------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scorecard)

        // Initialize UI components
        scorecardTable = findViewById(R.id.scorecardTable)
        currentHoleTextView = findViewById(R.id.currentHoleTextView)
        finishRoundButton = findViewById(R.id.finishRoundButton)
        // Text views for hole numbers were already initialized
        previousHoleButton = findViewById(R.id.previousHoleButton)
        nextHoleButton = findViewById(R.id.nextHoleButton)
        previousHoleText = findViewById(R.id.previousHoleText)
        nextHoleText = findViewById(R.id.nextHoleText)
        
        // Setup button click listeners
        nextHoleButton.setOnClickListener {
            advanceToNextHole()
        }
        
        previousHoleButton.setOnClickListener {
            goToPreviousHole()
        }
        
        finishRoundButton.setOnClickListener {
            finishRound()
        }

        // Retrieve data from intent
        numHoles = intent.getIntExtra("NUM_HOLES", 18)
        roundName = intent.getStringExtra("ROUND_NAME") ?: "New Round"
        playerNames = intent.getStringArrayExtra("PLAYER_NAMES") ?: arrayOf("Player 1")
        numPlayers = playerNames.size
        parValues = intent.getIntArrayExtra("PAR_VALUES") ?: IntArray(numHoles) { 3 }
        scoringMethod = intent.getStringExtra("SCORING_METHOD") ?: "MANUAL"
        Log.d(TAG, "onCreate: Effective scoringMethod after default: $scoringMethod")

        // Initialize scores and UI arrays
        scores = Array(numPlayers) { IntArray(numHoles) } // Initialize scores array
        scoreEditTexts = Array(numPlayers) { Array(numHoles) { EditText(this) } } // Initialize EditText array
        totalTextViews = Array(numPlayers) { TextView(this) } // Initialize total score TextViews
        
        // Set initial hole display which will trigger scorecard setup
        updateHoleDisplay()
        
        // Make sure the scorecard is visible and fully initialized
        findViewById<View>(R.id.scorecardCard).visibility = View.VISIBLE
        
        // Log that we're setting up the scorecard

        // Ensure manual input is enabled as voice recognition is removed
        enableManualInput()

        // Setup finish round button listener
        finishRoundButton.setOnClickListener {
            finishRound()
        }
    }

    override fun onPause() {
        super.onPause()
        // Save current scores when pausing
        saveCurrentScores()
    }
    
    /**
     * Advance to the next hole in the round
     */
    private fun advanceToNextHole() {
        // Save current scores
        saveCurrentScores()
        
        // Make sure we haven't reached the last hole
        if (currentHole < numHoles) {
            currentHole++
            updateHoleDisplay()
            
            Toast.makeText(this, "Moved to Hole $currentHole", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Already at the last hole", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Update the current hole display and button text
     */
    private fun updateHoleDisplay() {
        currentHoleTextView.text = "Current Hole: $currentHole"
        
        // Update text views below buttons to show actual hole numbers
        val prevHole = currentHole - 1
        val nextHole = currentHole + 1
        previousHoleText.text = if (prevHole >= 1) "Hole $prevHole" else "Start"
        nextHoleText.text = if (nextHole <= numHoles) "Hole $nextHole" else "End"
        
        // Update button enabled states
        previousHoleButton.isEnabled = prevHole >= 1
        nextHoleButton.isEnabled = nextHole <= numHoles
        previousHoleButton.alpha = if (prevHole >= 1) 1.0f else 0.5f
        nextHoleButton.alpha = if (nextHole <= numHoles) 1.0f else 0.5f
        
        setupScorecardTable() // Refresh the scorecard to highlight the current hole
        
        // Make sure scorecard is visible
        findViewById<View>(R.id.scorecardCard).visibility = View.VISIBLE
    }
    
    /**
     * Navigate to the previous hole
     */
    private fun goToPreviousHole() {
        if (currentHole > 1) {
            // Save current scores first
            saveCurrentScores()
            
            // Go to previous hole
            currentHole--
            updateHoleDisplay()
            
            Toast.makeText(this, "Returned to Hole $currentHole", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Already at the first hole", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Save current scores
     */
    private fun saveCurrentScores() {
        // Loop through all players and save their current scores
        for (playerIndex in 0 until numPlayers) {
            val holeIndex = currentHole - 1 // Convert 1-indexed hole to 0-indexed array
            if (holeIndex >= 0 && holeIndex < numHoles) {
                // Get the score from the EditText and update the scores array
                val scoreText = scoreEditTexts[playerIndex][holeIndex].text.toString()
                scores[playerIndex][holeIndex] = scoreText.toIntOrNull() ?: 0
            }
        }
    }
    
    /**
     * Enable manual input for score entry
     */
    private fun enableManualInput() {
        // Make sure score EditTexts are enabled
        for (playerIndex in 0 until numPlayers) {
            for (holeIndex in 0 until numHoles) {
                scoreEditTexts[playerIndex][holeIndex].isEnabled = true
            }
        }
    }
    
    /**
     * Set up a simple scorecard grid
     */
    private fun setupScorecardTable() {
        // Clear any existing views
        scorecardTable.removeAllViews()
        
        // Create header row with hole numbers
        val headerRow = TableRow(this)
        
        // First column (empty corner)
        val cornerCell = TextView(this)
        cornerCell.text = "Player/Hole"
        cornerCell.setTypeface(null, Typeface.BOLD)
        cornerCell.setPadding(24, 16, 24, 16)
        cornerCell.gravity = Gravity.CENTER
        headerRow.addView(cornerCell)
        
        // Hole number cells
        for (hole in 1..numHoles) {
            val holeCell = TextView(this)
            holeCell.text = hole.toString()
            holeCell.setTypeface(null, Typeface.BOLD)
            holeCell.setPadding(24, 16, 24, 16)
            holeCell.gravity = Gravity.CENTER
            
            // Highlight current hole
            if (hole == currentHole) {
                holeCell.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50")) // Green
                holeCell.setTextColor(android.graphics.Color.WHITE)
            }
            
            headerRow.addView(holeCell)
        }
        
        // Total column
        val totalHeaderCell = TextView(this)
        totalHeaderCell.text = "Total"
        totalHeaderCell.setTypeface(null, Typeface.BOLD)
        totalHeaderCell.setPadding(24, 16, 24, 16)
        totalHeaderCell.gravity = Gravity.CENTER
        headerRow.addView(totalHeaderCell)
        
        scorecardTable.addView(headerRow)
        
        // Player rows with score inputs
        for (playerIndex in 0 until numPlayers) {
            val playerRow = TableRow(this)
            
            // Player name cell
            val nameCell = TextView(this)
            nameCell.text = playerNames[playerIndex]
            nameCell.setTypeface(null, Typeface.BOLD)
            nameCell.setPadding(24, 16, 24, 16)
            nameCell.gravity = Gravity.START
            playerRow.addView(nameCell)
            
            // Score cells for each hole
            for (holeIndex in 0 until numHoles) {
                val scoreCell = EditText(this)
                scoreCell.layoutParams = TableRow.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                scoreCell.minWidth = 80
                scoreCell.setPadding(24, 8, 24, 8)
                scoreCell.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                scoreCell.gravity = Gravity.CENTER
                
                // Highlight current hole
                if (holeIndex + 1 == currentHole) {
                    scoreCell.setBackgroundColor(android.graphics.Color.parseColor("#E8F5E9")) // Light green
                } else {
                    scoreCell.setBackgroundColor(android.graphics.Color.parseColor("#FAFAFA")) // Off-white
                }
                
                // Set existing score
                val score = scores[playerIndex][holeIndex]
                if (score > 0) {
                    scoreCell.setText(score.toString())
                }
                
                // Save reference and add listener
                scoreEditTexts[playerIndex][holeIndex] = scoreCell
                
                val currentPlayerIndex = playerIndex
                val currentHoleIndex = holeIndex
                scoreCell.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        try {
                            val newScore = s.toString().toIntOrNull() ?: 0
                            scores[currentPlayerIndex][currentHoleIndex] = newScore
                            updateTotalScore(currentPlayerIndex)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating score: ${e.message}")
                        }
                    }
                    
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                })
                
                playerRow.addView(scoreCell)
            }
            
            // Total score cell
            val totalCell = TextView(this)
            totalCell.setPadding(24, 16, 24, 16)
            totalCell.gravity = Gravity.CENTER
            totalCell.setTypeface(null, Typeface.BOLD)
            totalTextViews[playerIndex] = totalCell
            playerRow.addView(totalCell)
            
            scorecardTable.addView(playerRow)
        }
        
        // Update all totals
        for (playerIndex in 0 until numPlayers) {
            updateTotalScore(playerIndex)
        }
    }
    
    /**
     * Update total score for a player
     */
    private fun updateTotalScore(playerIndex: Int) {
        var total = 0
        for (score in scores[playerIndex]) {
            total += score
        }
        totalTextViews[playerIndex].text = total.toString()
    }
    
    
    /**
     * Finish round and save data
     */
    private fun finishRound() {
        // Save current scores first
        saveCurrentScores()
        
        // Create round object to save
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        
        // Calculate total scores and determine winner
        val totalScores = mutableListOf<Pair<String, Int>>()
        for (playerIndex in 0 until numPlayers) {
            var total = 0
            var holesCompleted = 0
            for (holeIndex in 0 until numHoles) {
                if (scores[playerIndex][holeIndex] > 0) {
                    total += scores[playerIndex][holeIndex]
                    holesCompleted++
                }
            }
            totalScores.add(Pair(playerNames[playerIndex], total))
        }
        
        // Find the best score (lowest in golf)
        val bestScore = totalScores.minByOrNull { it.second }
        val scoreChangeText = if (bestScore != null) "${bestScore.first}: ${bestScore.second}" else "No scores"
        val holesPlayedText = "$numHoles holes"
        
        // Create round object
        val round = Round(
            name = roundName,
            date = currentDate,
            scoreChange = scoreChangeText,
            holesPlayed = holesPlayedText,
            isComplete = true,
            currentHole = numHoles
        )
        
        // Save to SharedPreferences
        saveRoundToHistory(round)
        
        // Navigate to final score activity
        val intent = Intent(this, FinalScoreActivity::class.java)
        intent.putExtra("ROUND_NAME", roundName)
        intent.putExtra("PLAYER_NAMES", playerNames)
        intent.putExtra("SCORES", scores.map { it.toList().toIntArray() }.toTypedArray())
        intent.putExtra("PAR_VALUES", parValues)
        intent.putExtra("NUM_HOLES", numHoles)
        startActivity(intent)
        finish()
    }
    
    /**
     * Save round to history in SharedPreferences
     */
    private fun saveRoundToHistory(round: Round) {
        val prefs: SharedPreferences = getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
        val json = prefs.getString(ROUNDS_KEY, null)
        val type = object : TypeToken<MutableList<Round>>() {}.type
        val rounds: MutableList<Round> = if (json != null) {
            try {
                gson.fromJson(json, type)
            } catch (e: Exception) {
                mutableListOf()
            }
        } else {
            mutableListOf()
        }
        
        // Add new round at the beginning
        rounds.add(0, round)
        
        // Save updated list
        val editor = prefs.edit()
        val updatedJson = gson.toJson(rounds)
        editor.putString(ROUNDS_KEY, updatedJson)
        editor.apply()
    }
}
