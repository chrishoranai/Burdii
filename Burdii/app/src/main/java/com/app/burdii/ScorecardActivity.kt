package com.app.burdii

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
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
    private lateinit var currentHoleTextView: TextView
    private lateinit var scorecardTable: TableLayout
 private lateinit var finishRoundButton: Button
    private lateinit var nextHoleButton: ImageButton
    private lateinit var previousHoleButton: ImageButton
    private lateinit var previousHoleText: TextView
    private lateinit var nextHoleText: TextView
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private var currentHole = 1
    private var currentPlayerIndex = 0
    private lateinit var playerNames: Array<String>
    private var numHoles: Int = 0
    private lateinit var parValues: IntArray
    private lateinit var scores: Array<IntArray>
    private lateinit var scoreEditTexts: Array<Array<EditText>>
    private lateinit var totalTextViews: Array<TextView>
    private var isListeningForWakeWord = true
    private var isAskingForScore = false
    private lateinit var scoringMethod: String
    private lateinit var voiceRecognitionService: VoiceRecognitionService

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
        micFeedbackCard.visibility = View.GONE
        
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
        numHoles = intent.getIntExtra("NUM_HOLES", 18) // Default to 18 if not found
        val roundName = intent.getStringExtra("ROUND_NAME") ?: "New Round" // Default if not found
        playerNames = intent.getStringArrayExtra("PLAYER_NAMES") ?: arrayOf("Player 1") // Default if not found
        numPlayers = playerNames.size // Get numPlayers from the array size
 parValues = intent.getIntArrayExtra("PAR_VALUES") ?: IntArray(numHoles) { 3 } // Default par 3 if not found
        scoringMethod = intent.getStringExtra("SCORING_METHOD") ?: "MANUAL" // Default to MANUAL
        Log.d(TAG, "onCreate: intent extra 'SCORING_METHOD': $scoringMethod")
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
        // Stop voice recognition if active
        if (::voiceRecognitionService.isInitialized) {
            voiceRecognitionService.stopListeningForWakeWord()
        }
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
     * Initialize the voice recognition service
     */
    private fun startVoiceRecognitionService() {
        // Initialize voice recognition service with callbacks
        voiceRecognitionService = VoiceRecognitionService(
            context = this,
            onScoresConfirmed = { playerScores, holeNum ->
                // Handle score confirmation from voice
                Log.d(TAG, "Voice scores confirmed for hole $holeNum: $playerScores")
                
                // Update the UI with scores
                runOnUiThread {
                    playerScores.forEach { (playerName, score) ->
                        // Find the player index from the name
                        val playerIndex = playerNames.indexOfFirst { it.equals(playerName, ignoreCase = true) }
                        
                        // Update the score if player found
                        if (playerIndex >= 0 && playerIndex < numPlayers) {
                            val holeIndex = currentHole - 1
                            scores[playerIndex][holeIndex] = score
                            scoreEditTexts[playerIndex][holeIndex].setText(score.toString())
                            updateTotalScore(playerIndex)
                        }
                    }
                }
            },
            onStateChanged = { state, message ->
                // Update UI based on voice recognition state
                runOnUiThread {
                    when (state) {
                        VoiceRecognitionService.VoiceState.ACTIVATION_STATE -> {
                            voiceStatusTextView.text = "Listening for 'Hey Birdie'"
                            micFeedbackCard.visibility = View.GONE
                        }
                        VoiceRecognitionService.VoiceState.SCORE_INQUIRY_STATE, 
                        VoiceRecognitionService.VoiceState.SCORE_INPUT_PROCESSING -> {
                            voiceStatusTextView.text = message
                            micFeedbackCard.visibility = View.VISIBLE
                        }
                        VoiceRecognitionService.VoiceState.CONFIRMATION_STATE,
                        VoiceRecognitionService.VoiceState.CONFIRMATION_HANDLING -> {
                            voiceStatusTextView.text = message
                            micFeedbackCard.visibility = View.VISIBLE
                        }
                        VoiceRecognitionService.VoiceState.ERROR_STATE -> {
                            voiceStatusTextView.text = "Error: $message"
                            micFeedbackCard.visibility = View.VISIBLE
                            // Reset after showing error briefly
                            handler.postDelayed({
                                voiceStatusTextView.text = "Listening for 'Hey Birdie'"
                                micFeedbackCard.visibility = View.GONE
                            }, 3000)
                        }
                    }
                }
            }
        )
        
        // Set the initial hole number
        voiceRecognitionService.setCurrentHoleNumber(currentHole)
        
        // Start listening for wake word
        voiceRecognitionService.startListeningForWakeWord()
        
        // Show the voice status card
        voiceStatusCard.visibility = View.VISIBLE
        voiceStatusTextView.text = "Listening for 'Hey Birdie'"
    }
    
    /**
     * Finish round and save data to be implemented
     */
    private fun finishRound() {
        // Basic placeholder implementation
        Toast.makeText(this, "Round finished!", Toast.LENGTH_SHORT).show()
        finish()
    }
}
