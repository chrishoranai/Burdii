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
        Log.d("ScorecardActivity", "onCreate started")

        // Initialize UI components
        scorecardTable = findViewById(R.id.scorecardTable)
        currentHoleTextView = findViewById(R.id.currentHoleTextView)
        micFeedbackCard = findViewById(R.id.micFeedbackCard)
        voiceStatusCard = findViewById(R.id.voiceStatusCard)
        voiceStatusTextView = findViewById(R.id.voiceStatusTextView)
        voiceToggleSwitch = findViewById(R.id.voiceToggleSwitch)
        finishRoundButton = findViewById(R.id.finishRoundButton)
        micFeedbackCard.visibility = View.GONE
        
        // Initially hide voice status until voice is enabled
        voiceStatusCard.visibility = View.GONE

        // Retrieve data from intent
        numHoles = intent.getIntExtra("NUM_HOLES", 18) // Default to 18 if not found
        val roundName = intent.getStringExtra("ROUND_NAME") ?: "New Round" // Default if not found
        playerNames = intent.getStringArrayExtra("PLAYER_NAMES") ?: arrayOf("Player 1") // Default if not found
        numPlayers = playerNames.size // Get numPlayers from the array size
        parValues = intent.getIntArrayExtra("PAR_VALUES") ?: IntArray(numHoles) { 3 } // Default par 3 if not found
        scoringMethod = intent.getStringExtra("SCORING_METHOD") ?: "MANUAL" // Default to MANUAL
        
        // Set round name as title
        title = roundName
        // Set round name if the TextView exists
        findViewById<TextView>(R.id.roundNameTextView)?.text = roundName

        // All features are available in testing phase
        // Initialize UI based on selected scoring method
        voiceToggleSwitch.isChecked = scoringMethod == "VOICE"
        
        // Set up toggle listener
        voiceToggleSwitch.setOnCheckedChangeListener { _, isChecked ->
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
        scores = Array(numPlayers) { IntArray(numHoles) }
        scoreEditTexts = Array(numPlayers) { Array(numHoles) { EditText(this) } }
        totalTextViews = Array(numPlayers) { TextView(this) }

        // Setup the scorecard table
        setupScorecardTable()

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
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            setupSpeechRecognizerListener()
        }
        
        // Initialize the new VoiceRecognitionService
        if (scoringMethod == "VOICE") {
            // Initialize the VoiceRecognitionService with callbacks
            voiceRecognitionService = VoiceRecognitionService(
                context = this,
                onScoresConfirmed = { playerScores, holeNumber ->
                    // Handle confirmed scores
                    handleConfirmedScores(playerScores, holeNumber)
                },
                onStateChanged = { state, message ->
                    // Update UI based on voice recognition state
                    updateVoiceRecognitionUI(state, message)
                }
            )
            
            checkAndRequestPermissions()
            // Service will be initialized after permissions are granted
            // (handled in onRequestPermissionsResult)
        }
        setupSpeechRecognizerListener()

        // Request permissions
        checkAndRequestPermissions()

        // Setup finish round button listener
        finishRoundButton.setOnClickListener {
            finishRound()
        }
    }

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION)
        } else {
            // Permission already granted, safe to start listening (will be handled in onResume)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("ScorecardActivity", "Audio permission granted")
                if (scoringMethod == "VOICE") {
                    // Start the new voice recognition service with the improved flow
                    startVoiceRecognitionService()
                }
            } else {
                Log.w("ScorecardActivity", "Audio permission denied - voice features unavailable")
                // Permission denied - switch to manual input
                scoringMethod = "MANUAL"
                Toast.makeText(this, "Audio permission denied. Voice input disabled.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSpeechRecognizerListener() {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("SpeechRecognizer", "onReadyForSpeech")
                micFeedbackCard.visibility = View.VISIBLE
            }
            override fun onBeginningOfSpeech() {
                Log.d("SpeechRecognizer", "onBeginningOfSpeech")
            }
            override fun onRmsChanged(rmsdB: Float) { /* Log.v("SpeechRecognizer", "onRmsChanged: $rmsdB") */ }
            override fun onBufferReceived(buffer: ByteArray?) { Log.d("SpeechRecognizer", "onBufferReceived") }
            override fun onEndOfSpeech() {
                Log.d("SpeechRecognizer", "onEndOfSpeech")
                micFeedbackCard.visibility = View.GONE
            }

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No match"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Error from server"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown speech recognizer error"
                }
                Log.e("SpeechRecognizer", "onError: $error - $errorMessage")
                micFeedbackCard.visibility = View.GONE
                voiceStatusTextView.text = "Error: $errorMessage. Toggle voice off to use manual input."
                // Reset state or provide retry mechanism
                if (isListeningForWakeWord || isAskingForScore) {
                    // Potentially delay and restart listening, or switch to manual
                    // For now, just log and update UI
                    handler.postDelayed({ resetVoiceInputUIState() }, 3000) // Reset after a delay
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                Log.d("SpeechRecognizer", "onResults: ${matches?.joinToString()}")
                micFeedbackCard.visibility = View.GONE
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0].lowercase(Locale.getDefault())
                    Log.i("SpeechRecognizer", "Spoken text: $spokenText")
                    if (isListeningForWakeWord && spokenText.contains("hey birdie")) {
                        // Handle quick score queries
                        if (spokenText.contains("tell me the scores") || spokenText.contains("who is winning")) {
                            speakScoresSummary()
                            // After speaking, restart listening for wake word
                            handler.postDelayed({ startListeningForWakeWord() }, 2500)
                            return
                        }
                        Log.i("SpeechRecognizer", "Wake word detected!")
                        isListeningForWakeWord = false
                        isAskingForScore = true // Transition to asking for score
                        startScoreInputForHole(currentHole)
                    } else if (isAskingForScore) {
                        Log.i("SpeechRecognizer", "Score input received: $spokenText")
                        val score = parseScore(spokenText)
                        if (score != null) {
                            val holeIndex = currentHole - 1
                            scores[currentPlayerIndex][holeIndex] = score
                            scoreEditTexts[currentPlayerIndex][holeIndex].setText(score.toString())
                            updateTotalScore(currentPlayerIndex)
                            currentPlayerIndex++
                            if (currentPlayerIndex < numPlayers) {
                                askForScore() // Ask for next player's score
                            } else {
                                // All players have entered scores for the current hole
                                currentPlayerIndex = 0 // Reset for next hole
                                currentHole++
                                if (currentHole <= numHoles) {
                                    currentHoleTextView.text = "Hole $currentHole / $numHoles"
                                    startScoreInputForHole(currentHole) // Start next hole
                                } else {
                                    Toast.makeText(this@ScorecardActivity, "End of round!", Toast.LENGTH_LONG).show()
                                    // Optionally, trigger finishRound() or navigate away
                                    switchToManualInput() // Or some other end-of-round state
                                }
                            }
                        } else {
                            textToSpeech.speak("Sorry, I didn't catch that score. Please try again.", TextToSpeech.QUEUE_FLUSH, null, "RETRY_SCORE")
                            handler.postDelayed({ startListeningForScore() }, 1000) // Retry listening for score
                        }
                    }
                } else {
                    Log.w("SpeechRecognizer", "No matches found in onResults")
                    // If expecting something, might need to re-prompt or retry
                    if (isAskingForScore) {
                        textToSpeech.speak("I didn't hear a score. Please try again.", TextToSpeech.QUEUE_FLUSH, null, "NO_SCORE_HEARD")
                        handler.postDelayed({ startListeningForScore() }, 1000)
                    } else if (isListeningForWakeWord) {
                         handler.postDelayed({ startListeningForWakeWord() }, 500) // Briefly wait and restart wake word
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) { Log.d("SpeechRecognizer", "onPartialResults") }
            override fun onEvent(eventType: Int, params: Bundle?) { Log.d("SpeechRecognizer", "onEvent") }
        })
    }

    // Sets up the scorecard table with headers and player rows
    private fun setupScorecardTable() {
        // Clear any existing rows
        scorecardTable.removeAllViews()
        
        // Add round name as title if needed (optional)
        // Currently using the title bar to display the round name
        val headerRow = TableRow(this)
        
        // Player column header
        headerRow.addView(TextView(this).apply { 
            text = "Player"
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
            setPadding(16, 12, 16, 12)
            gravity = Gravity.CENTER_VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.grid_header_background)
        })
        
        // Hole number headers
        for (h in 1..numHoles) {
            headerRow.addView(TextView(this).apply { 
                text = "H$h"
                setTypeface(null, Typeface.BOLD)
                setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
                gravity = Gravity.CENTER
                minWidth = 70 // Match the width of score cells
                setPadding(8, 12, 8, 12)
                background = ContextCompat.getDrawable(context, R.drawable.grid_header_background)
            })
        }
        
        // Total column header
        headerRow.addView(TextView(this).apply { 
            text = "Total"
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
            gravity = Gravity.CENTER
            minWidth = 80 // Slightly wider for totals
            setPadding(8, 12, 8, 12)
            background = ContextCompat.getDrawable(context, R.drawable.grid_header_background)
        })
        
        scorecardTable.addView(headerRow)

        for (p in 0 until numPlayers) {
            val row = TableRow(this)
            row.addView(TextView(this).apply {
                text = playerNames[p]
                setPadding(16, 8, 16, 8)
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER_VERTICAL
            })
            for (h in 0 until numHoles) {
                val editText = EditText(this).apply {
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    gravity = Gravity.CENTER
                    minWidth = 70 // Set minimum width for score cells
                    setPadding(8, 8, 8, 8)
                    // Add grid-like appearance with borders
                    background = ContextCompat.getDrawable(context, R.drawable.grid_cell_background)
                    // Initialize as enabled for manual input
                    isEnabled = true
                    isFocusable = true
                    isFocusableInTouchMode = true
                }
                editText.addTextChangedListener(object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        val text = s.toString()
                        if (text.isNotEmpty()) {
                            scores[p][h] = text.toInt()
                            updateTotalScore(p)
                        } else {
                            scores[p][h] = 0
                            updateTotalScore(p)
                        }
                    }
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                })
                row.addView(editText)
                scoreEditTexts[p][h] = editText
            }
            // Create styled total cell with grid appearance
            val totalTextView = TextView(this).apply {
                text = "0"
                gravity = Gravity.CENTER
                minWidth = 80 // Slightly wider for total column
                setPadding(8, 8, 8, 8)
                setTypeface(null, Typeface.BOLD)
                // Make the total column stand out with a different background
                background = ContextCompat.getDrawable(context, R.drawable.grid_total_background)
            }
            row.addView(totalTextView)
            totalTextViews[p] = totalTextView
            scorecardTable.addView(row)
        }
    }

    /**
     * Enable manual score input
     */
    private fun enableManualInput() {
        // Update UI for manual input
        micFeedbackCard.visibility = View.GONE
        voiceStatusCard.visibility = View.GONE
        
        // Enable manual entry in EditTexts
        for (player in 0 until numPlayers) {
            for (hole in 0 until numHoles) {
                scoreEditTexts[player][hole].isEnabled = true
                
                // Remove existing text watchers to avoid duplicates
                val oldTextWatchers = scoreEditTexts[player][hole].getTag(R.id.tag_text_watchers)
                if (oldTextWatchers is ArrayList<*>) {
                    for (watcher in oldTextWatchers) {
                        if (watcher is TextWatcher) {
                            scoreEditTexts[player][hole].removeTextChangedListener(watcher)
                        }
                    }
                }
                
                // Add text change listener to handle score updates
                val textWatcher = object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {
                        if (s.isNullOrEmpty()) return
                        try {
                            val score = s.toString().toInt()
                            scores[player][hole] = score
                            updateTotalScore(player)
                            saveCurrentScores() // Save after each update
                        } catch (e: NumberFormatException) {
                            // Invalid input, clear it
                            scoreEditTexts[player][hole].setText("")
                        }
                    }
                    
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                }
                
                scoreEditTexts[player][hole].addTextChangedListener(textWatcher)
                
                // Store the text watcher for potential future removal
                val textWatchers = ArrayList<TextWatcher>()
                textWatchers.add(textWatcher)
                scoreEditTexts[player][hole].setTag(R.id.tag_text_watchers, textWatchers)
            }
        }
    }

    // Resets the voice input UI elements to their default state
    private fun resetVoiceInputUIState() {
        voiceStatusTextView.text = "Listening for 'Hey Birdie'" 
        voiceStatusCard.visibility = if (voiceToggleSwitch.isChecked) View.VISIBLE else View.GONE
        micFeedbackCard.visibility = View.GONE
        
        // Ensure flags are reset so recognizer restarts correctly
        isListeningForWakeWord = true
        isAskingForScore = false
        
        // Enable or disable manual input based on voice toggle state
        if (voiceToggleSwitch.isChecked) {
            // Voice mode - disable manual input
            for (player in 0 until numPlayers) {
                for (hole in 0 until numHoles) {
                    scoreEditTexts[player][hole].isEnabled = false
                }
            }
        } else {
            // Manual mode - enable input fields
            enableManualInput()
        }
    }

    // Starts listening for the wake word "Hey Birdie" - Legacy implementation
    private fun startListeningForWakeWord() {
        Log.i("VoiceInput", "Using legacy wake word detection.")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                Toast.makeText(this, "Speech recognition is not available on this device.", Toast.LENGTH_LONG).show()
                Log.e("SpeechRecognizer", "Speech recognition service not available.")
                resetVoiceInputUIState() // Ensure UI is reset
                return
            }

            isListeningForWakeWord = true
            isAskingForScore = false // Ensure correct state
            // Disable manual input when using voice
            for (player in 0 until numPlayers) {
                for (hole in 0 until numHoles) {
                    scoreEditTexts[player][hole].isEnabled = false
                }
            }
            voiceStatusCard.visibility = View.VISIBLE
            voiceStatusTextView.text = "Listening for 'Hey Birdie'"
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true) // Prefer offline recognition
            speechRecognizer.startListening(intent)
        }
    }

    // Starts the score input process for the current hole
    private fun startScoreInputForHole(hole: Int) {
        currentHole = hole
        currentPlayerIndex = 0
        Log.i("VoiceInput", "Starting score input for Hole $currentHole")
        currentHoleTextView.text = "Hole $currentHole / $numHoles"
        askForScore()
    }

    // Prompts for the next player's score
    private fun askForScore() {
        if (currentPlayerIndex < numPlayers) {
            val playerName = playerNames[currentPlayerIndex]
            val message = "What's the score for $playerName on hole $currentHole?"
            Log.i("TTS_SPEAK", "Attempting to speak: '$message'")
            voiceStatusTextView.text = "Speaking: $message"
            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, "ASK_SCORE_${playerName}_H${currentHole}")
            // Wait for TTS to finish before listening for score
            // A more robust way is to use UtteranceProgressListener
            handler.postDelayed({
                if (scoringMethod == "VOICE") { // Only listen if in voice mode
                    startListeningForScore()
                }
            }, 2000) // Adjust delay as needed for speech length
        } else {
            Log.i("VoiceInput", "All players scored for hole $currentHole. Moving to next or finishing.")
            // This case should be handled in onResults after the last player's score for a hole
        }
    }

    // Starts listening for the score input
    private fun startListeningForScore() {
        Log.i("VoiceInput", "Attempting to start listening for SCORE.")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                Toast.makeText(this, "Speech recognition is not available on this device.", Toast.LENGTH_LONG).show()
                Log.e("SpeechRecognizer", "Speech recognition service not available.")
                resetVoiceInputUIState() // Ensure UI is reset
                return
            }

            isAskingForScore = true
            isListeningForWakeWord = false
            voiceStatusCard.visibility = View.VISIBLE
            voiceStatusTextView.text = "Listening for score..."
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true) // Prefer offline recognition
            speechRecognizer.startListening(intent)
        } else {
            Toast.makeText(this, "Audio permission needed for voice input.", Toast.LENGTH_SHORT).show()
        }
    }

    // Parses spoken score into an integer
    private fun parseScore(text: String): Int? {
        return try {
            text.toInt()
        } catch (e: NumberFormatException) {
            when (text.lowercase()) {
                "zero" -> 0
                "one" -> 1
                "two" -> 2
                "three" -> 3
                "four" -> 4
                "five" -> 5
                "six" -> 6
                "seven" -> 7
                "eight" -> 8
                "nine" -> 9
                "ten" -> 10
                else -> null
            }
        }
    }

    // Updates the total score for a player
    private fun updateTotalScore(playerIndex: Int) {
        val total = scores[playerIndex].sum()
        totalTextViews[playerIndex].text = total.toString()
        saveCurrentScores()
    }

    // Saves current per-player totals so a background component could read them later
    private fun saveCurrentScores() {
        val prefs: SharedPreferences = getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val totals = (0 until numPlayers).map { scores[it].sum() }
        editor.putString("CURRENT_PLAYER_NAMES", gson.toJson(playerNames))
        editor.putString("CURRENT_TOTAL_SCORES", gson.toJson(totals))
        editor.apply()
    }

    // Speaks a summary of the current scores or the leader if requested
    private fun speakScoresSummary() {
        // Compute totals for each player
        val totals = IntArray(numPlayers) { scores[it].sum() }
        // Build spoken message
        val builder = StringBuilder()
        var leaderIndex = 0
        for (i in totals.indices) {
            builder.append("${playerNames[i]} has ${totals[i]}.")
            if (totals[i] < totals[leaderIndex]) leaderIndex = i
            if (i != totals.lastIndex) builder.append(" ")
        }
        builder.append(" ${playerNames[leaderIndex]} is currently leading.")
        val message = builder.toString()
        voiceStatusTextView.text = message
        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, "SCORES_SUMMARY")
    }

    // Switch to manual input mode
    private fun switchToManualInput() {
        // Stop any ongoing speech recognition or TTS
        speechRecognizer.stopListening()
        textToSpeech.stop()
        handler.removeCallbacksAndMessages(null) // Stop pending actions

        // Update state flags
        isListeningForWakeWord = false
        isAskingForScore = false

        // Update UI
        micFeedbackCard.visibility = View.GONE
        voiceStatusCard.visibility = View.GONE // Hide voice status
        
        // Set toggle switch to off position
        voiceToggleSwitch.isChecked = false

        // Enable manual input for all holes
        enableManualInput()

        // Optionally focus the first player's EditText for the current hole
        if (numPlayers > 0 && currentHole > 0 && currentHole <= numHoles) {
            scoreEditTexts[0][currentHole - 1].requestFocus()
        }
        Toast.makeText(this, "Manual input enabled", Toast.LENGTH_SHORT).show()
    }

    // Enable editing for a specific hole or all holes
    private fun enableEditingForHole(holeIndex: Int? = null) {
        for (p in 0 until numPlayers) {
            for (h in 0 until numHoles) {
                val editText = scoreEditTexts[p][h]
                // Enable only for the specified hole, or all if holeIndex is null
                if (holeIndex == null || h == holeIndex) {
                    editText.isEnabled = true
                } else {
                    // Keep other holes disabled if focusing on one hole
                    editText.isEnabled = false
                }
            }
        }
    }

    // --- Finish Round Logic --- 
    private fun finishRound() {
        // 1. Calculate total score and par
        var totalPlayerScore = 0
        // For simplicity, we'll just use the first player's score for the summary.
        // A more complex app might show all scores or the best score.
        if (numPlayers > 0) { 
            totalPlayerScore = scores[0].sum()
        }
        val totalPar = parValues.sum()
        val scoreDifference = totalPlayerScore - totalPar

        // 2. Format score change string
        val scoreChangeStr = when {
            scoreDifference > 0 -> "+${scoreDifference}"
            scoreDifference == 0 -> "E"
            else -> scoreDifference.toString() // Negative sign is included
        }

        // 3. Get current date
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val currentDate = sdf.format(Date())

        // 4. Create Round object
        val completedRound = Round(
            date = currentDate,
            scoreChange = scoreChangeStr,
            holesPlayed = "${numHoles} Holes"
        )

        // 5. Load, add, and save rounds
        val currentRounds = loadRounds().toMutableList()
        currentRounds.add(0, completedRound) // Add to the beginning of the list
        saveRounds(currentRounds)

        // 6. Show confirmation and finish
        Toast.makeText(this, "Round Finished and Saved!", Toast.LENGTH_LONG).show()
        finish() // Closes ScorecardActivity and returns to HomeActivity
    }
    // ------------------------

    // --- SharedPreferences Helper Functions --- 
    private fun saveRounds(rounds: List<Round>) {
        val prefs: SharedPreferences = getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val json = gson.toJson(rounds)
        editor.putString(ROUNDS_KEY, json)
        editor.apply()
    }
    
    private fun loadRounds(): List<Round> {
        val prefs: SharedPreferences = getSharedPreferences(PREFS_FILENAME, Context.MODE_PRIVATE)
        val json = prefs.getString(ROUNDS_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<List<Round>>() {}.type
            try {
                gson.fromJson(json, type)
            } catch (e: Exception) {
                e.printStackTrace()
                listOf()
            }
        } else {
            listOf()
        }
    }
    // -----------------------------------------------------------------

    // Cleanup resources
    override fun onDestroy() {
        super.onDestroy()
        // Clean up legacy resources
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.stopListening()
            speechRecognizer.destroy()
        }
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        
        // Clean up new voice recognition service
        if (::voiceRecognitionService.isInitialized) {
            voiceRecognitionService.release()
        }
        
        handler.removeCallbacksAndMessages(null)
    }

    override fun onPause() {
        super.onPause()
        // Pause voice recognition unless we're in explicit VOICE mode
        if (scoringMethod != "VOICE") {
            // Clean up legacy components
            if (::speechRecognizer.isInitialized) {
                speechRecognizer.stopListening()
            }
            if (::textToSpeech.isInitialized) {
                textToSpeech.stop() // Stop any ongoing TTS
            }
            
            // Update UI
            micFeedbackCard.visibility = View.GONE // Hide feedback
            voiceStatusCard.visibility = View.GONE // Hide status card
            
            // Reset state flags
            isListeningForWakeWord = false
            isAskingForScore = false
        } else {
            // We're in voice mode, but we're pausing the activity
            // Don't stop voice recognition service as it should continue in background
        }
        
        handler.removeCallbacksAndMessages(null)
    }

    override fun onResume() {
        super.onResume()
        // Restart listening for the wake word if permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            if (scoringMethod == "VOICE" && ::voiceRecognitionService.isInitialized) {
                // Resume using the new service
                voiceRecognitionService.startListeningForWakeWord()
            } else {
                // Fallback to old implementation
                startListeningForWakeWord()
            }
        } else {
            // Optional: Inform user or request permission again if it was denied previously
            // checkAndRequestPermissions()
        }
    }
    
    /**
     * Handle confirmed scores from the VoiceRecognitionService
     */
    private fun handleConfirmedScores(playerScores: Map<String, Int>, holeNumber: Int) {
        Log.d("VoiceInput", "Confirmed scores for hole $holeNumber: $playerScores")
        
        val holeIndex = holeNumber - 1
        if (holeIndex < 0 || holeIndex >= numHoles) {
            Log.e("VoiceInput", "Invalid hole number: $holeNumber")
            return
        }
        
        // Match player names from voice input to our app's player names
        // This handles cases where voice recognition might not match exact player names
        for ((voicePlayerName, score) in playerScores) {
            // Find matching player in our list (case-insensitive partial match)
            val playerIndex = playerNames.indexOfFirst { playerName ->
                playerName.contains(voicePlayerName, ignoreCase = true) || 
                voicePlayerName.contains(playerName, ignoreCase = true)
            }
            
            if (playerIndex != -1) {
                // Valid player found, update score
                scores[playerIndex][holeIndex] = score
                scoreEditTexts[playerIndex][holeIndex].setText(score.toString())
                updateTotalScore(playerIndex)
            } else {
                Log.w("VoiceInput", "Couldn't match voice player name: $voicePlayerName to any known player")
            }
        }
        
        // Update UI to reflect current hole
        currentHole = holeNumber + 1 // Move to next hole
        if (currentHole <= numHoles) {
            currentHoleTextView.text = "Hole $currentHole / $numHoles"
        } else {
            // Manual mode
            Toast.makeText(this, "Switching to manual input mode.", Toast.LENGTH_SHORT).show()
            voiceToggleSwitch.isChecked = false
            currentHole = numHoles
            currentHoleTextView.text = "Hole $currentHole / $numHoles"
        }
        
        // Save the current scores for persistence
        saveCurrentScores()
    }
    
    /**
     * Start the Voice Recognition Service
     * This initializes the service and begins listening for the wake word
     */
    private fun startVoiceRecognitionService() {
        if (!::voiceRecognitionService.isInitialized) {
            // Initialize the VoiceRecognitionService with callbacks
            voiceRecognitionService = VoiceRecognitionService(
                context = this,
                onScoresConfirmed = { playerScores, holeNumber ->
                    // Handle confirmed scores
                    handleConfirmedScores(playerScores, holeNumber)
                },
                onStateChanged = { state, message ->
                    // Update UI based on voice recognition state
                    updateVoiceRecognitionUI(state, message)
                }
            )
        }
        
        // Initialize and start the service
        voiceRecognitionService.initialize()
        voiceRecognitionService.setCurrentHoleNumber(currentHole)
        voiceRecognitionService.startListeningForWakeWord()
        
        // Update UI state
        voiceStatusCard.visibility = View.VISIBLE
        voiceStatusTextView.text = "Listening for 'Hey Birdie'"
        micFeedbackCard.visibility = View.GONE
        
        // Disable manual input fields when in voice mode
        for (player in 0 until numPlayers) {
            for (hole in 0 until numHoles) {
                scoreEditTexts[player][hole].isEnabled = false
            }
        }
    }

    /**
     * Update UI based on voice recognition state changes
     */
    private fun updateVoiceRecognitionUI(state: VoiceRecognitionService.VoiceState, message: String) {
        runOnUiThread {
            voiceStatusCard.visibility = View.VISIBLE
            voiceStatusTextView.text = message
            
            when (state) {
                VoiceRecognitionService.VoiceState.ACTIVATION_STATE -> {
                    // Listening for wake word
                    micFeedbackCard.visibility = View.GONE
                    // Disable manual input when in voice mode
                    for (player in 0 until numPlayers) {
                        for (hole in 0 until numHoles) {
                            scoreEditTexts[player][hole].isEnabled = false
                        }
                    }
                }
                VoiceRecognitionService.VoiceState.SCORE_INQUIRY_STATE,
                VoiceRecognitionService.VoiceState.CONFIRMATION_STATE -> {
                    // System is speaking
                    micFeedbackCard.visibility = View.GONE
                }
                VoiceRecognitionService.VoiceState.SCORE_INPUT_PROCESSING,
                VoiceRecognitionService.VoiceState.CONFIRMATION_HANDLING -> {
                    // Listening for user input
                    micFeedbackCard.visibility = View.VISIBLE
                }
                VoiceRecognitionService.VoiceState.ERROR_STATE -> {
                    // Error occurred
                    micFeedbackCard.visibility = View.GONE
                    Toast.makeText(this, "Voice recognition error: $message", Toast.LENGTH_SHORT).show()
                    // Show error to user but keep voice mode if still toggled on
                    if (voiceToggleSwitch.isChecked) {
                        Toast.makeText(this, "Voice recognition had an issue. Try again or switch to manual input.", Toast.LENGTH_SHORT).show()
                    } else {
                        enableManualInput()
                    }
                }
            }
        }
    }
}
