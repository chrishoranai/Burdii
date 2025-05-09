package com.app.burdii

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
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
import com.google.android.material.button.MaterialButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class ScorecardActivity : AppCompatActivity() {
    private lateinit var currentHoleTextView: TextView
    private lateinit var scorecardTable: TableLayout
    private lateinit var micFeedbackCard: CardView
    private lateinit var voiceStatusCard: CardView
    private lateinit var voiceStatusTextView: TextView
    private lateinit var voiceToggleSwitch: Switch
    private lateinit var finishRoundButton: MaterialButton
    private lateinit var nextHoleButton: ImageButton
    private lateinit var previousHoleButton: ImageButton
    private lateinit var previousHoleText: TextView
    private lateinit var nextHoleText: TextView
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private val handler = Handler(Looper.getMainLooper())
    private var currentHole = 1
    private var currentPlayerIndex = 0
    private lateinit var playerNames: Array<String>
    private var numHoles: Int = 0
    private var numPlayers: Int = 0
    private lateinit var parValues: IntArray
    private lateinit var scores: Array<IntArray>
    private lateinit var scoreEditTexts: Array<Array<EditText>>
    private lateinit var totalTextViews: Array<TextView>
    private var isListeningForWakeWord = true
    private var isAskingForScore = false
    private lateinit var scoringMethod: String
    private val TAG = "ScorecardActivity"

    // New voice recognition service
    private lateinit var voiceRecognitionService: VoiceRecognitionService

    // --- SharedPreferences Constants and Gson --- 
    private val PREFS_FILENAME = "com.app.burdii.prefs"
    private val ROUNDS_KEY = "rounds_history"
    private val gson = Gson()
    // ------------------------------------------

    private val REQUEST_RECORD_AUDIO_PERMISSION = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scorecard)
        Log.d(TAG, "onCreate started")

        // Initialize UI components
        scorecardTable = findViewById(R.id.scorecardTable)
        currentHoleTextView = findViewById(R.id.currentHoleTextView)
        micFeedbackCard = findViewById(R.id.micFeedbackCard)
        voiceStatusCard = findViewById(R.id.voiceStatusCard)
        voiceStatusTextView = findViewById(R.id.voiceStatusTextView)
        voiceToggleSwitch = findViewById(R.id.voiceToggleSwitch)
        finishRoundButton = findViewById(R.id.finishRoundButton)
        // Text views for hole numbers were already initialized
        previousHoleButton = findViewById(R.id.previousHoleButton)
        nextHoleButton = findViewById(R.id.nextHoleButton)
        previousHoleText = findViewById(R.id.previousHoleText)
        nextHoleText = findViewById(R.id.nextHoleText)
        micFeedbackCard.visibility = View.GONE
        
        // Initially hide voice status until voice is enabled
        voiceStatusCard.visibility = View.GONE
        
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

        // Set round name as title
        title = roundName
        // Set round name if the TextView exists
        findViewById<TextView>(R.id.roundNameTextView)?.text = roundName

        // All features are available in testing phase
        // Initialize UI based on selected scoring method
        voiceToggleSwitch.isChecked = scoringMethod == "VOICE"
        Log.d(TAG, "onCreate: voiceToggleSwitch.isChecked set to: ${voiceToggleSwitch.isChecked} based on scoringMethod: $scoringMethod")

        // Set up toggle listener
        voiceToggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            Log.d(TAG, "voiceToggleSwitch.setOnCheckedChangeListener: isChecked: $isChecked")
            if (isChecked) {
                // Enable voice input
                voiceStatusCard.visibility = View.VISIBLE
                voiceStatusTextView.text = "Listening for 'Hey Birdie'"
                startVoiceRecognitionService()
            } else {
                // Disable voice input, enable manual input
                voiceStatusCard.visibility = View.GONE
                if (::voiceRecognitionService.isInitialized) {
                    // Stop the voice recognition service if it's running
                    try {
                        // For now just initialize a new instance which should stop any ongoing recognition
                        voiceRecognitionService = VoiceRecognitionService(
                            context = this,
                            onScoresConfirmed = { _, _ -> },
                            onStateChanged = { _, _ -> }
                        )
                    } catch (e: Exception) {
                        // Ignore any errors while stopping
                    }
                }
                enableManualInput()
            }
        }

        // Initialize scores and UI arrays
        scores = Array(numPlayers) { IntArray(numHoles) } // Initialize scores array
        scoreEditTexts = Array(numPlayers) { Array(numHoles) { EditText(this) } } // Initialize EditText array
        totalTextViews = Array(numPlayers) { TextView(this) } // Initialize total score TextViews
        
        // Set initial hole display which will trigger scorecard setup
        updateHoleDisplay()
        
        // Make sure the scorecard is visible and fully initialized
        findViewById<View>(R.id.scorecardCard).visibility = View.VISIBLE
        
        // Log that we're setting up the scorecard
        Log.d(TAG, "Initial scorecard setup complete with ${playerNames.size} players and $numHoles holes")

        // Initialize TextToSpeech (maintaining for backward compatibility)
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "Language not supported")
                }
            } else {
                Log.e("TTS", "Initialization failed")
            }
        }

        // Initialize SpeechRecognizer (maintaining for backward compatibility)
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.d(TAG, "setupSpeechRecognizerListener() would be called here - but using voice recognition service");
            // Using new voice recognition service instead
        }
        
        // Initialize the new VoiceRecognitionService
        Log.d(TAG, "onCreate: Evaluating scoringMethod before if/else. Current value: $scoringMethod, voiceToggleSwitch.isChecked: ${voiceToggleSwitch.isChecked}")
        if (scoringMethod == "VOICE") {
            Log.d(TAG, "onCreate: scoringMethod IS 'VOICE'. Initializing voice services.")
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecognitionService()
            }
        } else {
            Log.d(TAG, "onCreate: scoringMethod IS NOT 'VOICE' (it's '$scoringMethod'). Setting up for manual input.")
            micFeedbackCard.visibility = View.GONE
            Log.d(TAG, "onCreate (manual mode): micFeedbackCard visibility explicitly set to GONE. Actual visibility: ${micFeedbackCard.visibility}")
            voiceStatusCard.visibility = View.GONE
            Log.d(TAG, "onCreate (manual mode): voiceStatusCard visibility explicitly set to GONE. Actual visibility: ${voiceStatusCard.visibility}")
            enableManualInput()
        }

        // Request permissions
        Log.d(TAG, "onCreate: Calling checkAndRequestPermissions(). Current voiceToggleSwitch.isChecked: ${voiceToggleSwitch.isChecked}")
        checkAndRequestPermissions()

        // Setup finish round button listener
        finishRoundButton.setOnClickListener {
            finishRound()
        }
    }

    private fun checkAndRequestPermissions() {
        Log.d(TAG, "checkAndRequestPermissions called. Current voiceToggleSwitch.isChecked: ${voiceToggleSwitch.isChecked}, scoringMethod: $scoringMethod")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "checkAndRequestPermissions: Permissions already granted.")
            if (voiceToggleSwitch.isChecked) {
                Log.d(TAG, "checkAndRequestPermissions: Permissions granted and voiceToggleSwitch IS CHECKED. Attempting to startVoiceRecognitionService.")
                startVoiceRecognitionService()
            } else {
                Log.d(TAG, "checkAndRequestPermissions: Permissions granted BUT voiceToggleSwitch IS NOT CHECKED. Not starting voice service.")
            }
        } else {
            Log.d(TAG, "checkAndRequestPermissions: Permissions not granted. Requesting permissions.")
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "onRequestPermissionsResult called. RequestCode: $requestCode")
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onRequestPermissionsResult: Record audio permission GRANTED. Current voiceToggleSwitch.isChecked: ${voiceToggleSwitch.isChecked}, scoringMethod: $scoringMethod")
                // Start voice recognition if the toggle is on (or scoring method implies it should be)
                if (voiceToggleSwitch.isChecked) { // Check the toggle's current state
                    Log.d(TAG, "onRequestPermissionsResult: Permission granted and voiceToggleSwitch IS CHECKED. Starting voice service.")
                    startVoiceRecognitionService()
                } else {
                    Log.d(TAG, "onRequestPermissionsResult: Permission granted BUT voiceToggleSwitch IS NOT CHECKED. Not starting voice service.")
                }
            } else {
                Log.d(TAG, "onRequestPermissionsResult: Record audio permission DENIED.")
                Toast.makeText(this, "Record audio permission is required for voice input.", Toast.LENGTH_LONG).show()
                // Ensure UI reflects that voice mode cannot be enabled
                voiceToggleSwitch.isChecked = false // Turn off the toggle if permission is denied
                micFeedbackCard.visibility = View.GONE
                voiceStatusCard.visibility = View.GONE
                enableManualInput()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called. voiceToggleSwitch.isChecked: ${voiceToggleSwitch.isChecked}, scoringMethod: $scoringMethod") // Added log
        // Restart listening only if the toggle is actually ON
        if (voiceToggleSwitch.isChecked) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onResume: Permissions granted and toggle is ON.")
                if (scoringMethod == "VOICE" && ::voiceRecognitionService.isInitialized) {
                    Log.d(TAG, "onResume: Resuming new VoiceRecognitionService.")
                    voiceRecognitionService.startListeningForWakeWord()
                } else if (scoringMethod == "VOICE") { // Should ideally be covered by isInitialized or started in onCreate/toggle
                    Log.d(TAG, "onResume: scoringMethod is VOICE but new service not initialized. Starting new service.")
                    startVoiceRecognitionService() // Or re-initialize if necessary
                } else {
                    // This case implies toggle is ON but scoringMethod is not VOICE - a bit contradictory.
                    // For safety, if toggle is on, let's try to start the new service.
                    // Or, consider if legacy should ever run if toggle is ON but scoringMethod isn't VOICE.
                    // For now, let's assume if toggle is on, we want voice.
                    Log.d(TAG, "onResume: Toggle is ON, but scoringMethod is NOT 'VOICE'. Attempting to start new service as a fallback.")
                    startVoiceRecognitionService()
                }
            } else {
                Log.d(TAG, "onResume: Permissions NOT granted. Cannot start voice service.")
                // Optionally, re-request permissions or inform user
                // checkAndRequestPermissions() // Be careful of loops if called here
            }
        } else {
            Log.d(TAG, "onResume: voiceToggleSwitch is OFF. Not starting any voice service.")
            // Ensure UI reflects that voice is off if onResume is called and toggle is off
            micFeedbackCard.visibility = View.GONE
            voiceStatusCard.visibility = View.GONE
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
            
            // If voice recognition is active, update the hole number
            if (voiceToggleSwitch.isChecked && ::voiceRecognitionService.isInitialized) {
                voiceRecognitionService.setCurrentHoleNumber(currentHole)
                // Reset to listen for "Hey Birdie" for the new hole
                voiceStatusTextView.text = "Listening for 'Hey Birdie'"
                micFeedbackCard.visibility = View.GONE
            }
            
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
            
            // If voice recognition is active, update the hole number
            if (voiceToggleSwitch.isChecked && ::voiceRecognitionService.isInitialized) {
                voiceRecognitionService.setCurrentHoleNumber(currentHole)
                // Reset to listen for "Hey Birdie" for the new hole
                voiceStatusTextView.text = "Listening for 'Hey Birdie'"
                micFeedbackCard.visibility = View.GONE
            }
            
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
