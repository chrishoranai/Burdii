package com.app.burdii

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
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
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.cardview.widget.CardView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class ScorecardActivity : AppCompatActivity() {
    private lateinit var scorecardTable: TableLayout
    private lateinit var currentHoleTextView: TextView
    private lateinit var micFeedbackCard: CardView
    private lateinit var voiceStatusCard: CardView
    private lateinit var voiceStatusTextView: TextView
    private lateinit var voiceInputButton: Button
    private lateinit var finishRoundButton: Button
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
        voiceInputButton = findViewById(R.id.voiceInputButton)
        finishRoundButton = findViewById(R.id.finishRoundButton)
        micFeedbackCard.visibility = View.GONE
        voiceStatusCard.visibility = View.VISIBLE
        voiceStatusTextView.text = "Listening for 'Hey Birdie'"

        // Retrieve data from intent
        numHoles = intent.getIntExtra("NUM_HOLES", 18) // Default to 18 if not found
        playerNames = intent.getStringArrayExtra("PLAYER_NAMES") ?: arrayOf("Player 1") // Default if not found
        numPlayers = playerNames.size // Get numPlayers from the array size
        parValues = intent.getIntArrayExtra("PAR_VALUES") ?: IntArray(numHoles) { 3 } // Default par 3 if not found
        scoringMethod = intent.getStringExtra("SCORING_METHOD") ?: "MANUAL" // Default to MANUAL

        // Check if voice feature is actually unlocked
        if (scoringMethod == "VOICE") {
            val prefs = getSharedPreferences("com.app.burdii.prefs", MODE_PRIVATE)
            val unlocked = prefs.getBoolean("VOICE_UNLOCKED", false)
            if (!unlocked) {
                Toast.makeText(this, "Voice feature not unlocked. Switching to manual input.", Toast.LENGTH_LONG).show()
                scoringMethod = "MANUAL"
            }
        }

        // Initialize scores and UI arrays
        scores = Array(numPlayers) { IntArray(numHoles) }
        scoreEditTexts = Array(numPlayers) { Array(numHoles) { EditText(this) } }
        totalTextViews = Array(numPlayers) { TextView(this) }

        // Setup the scorecard table
        setupScorecardTable()

        // Initialize TextToSpeech
        textToSpeech = TextToSpeech(this, TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS_INIT", "The Language specified is not supported!")
                    Toast.makeText(this, "TTS Language not supported.", Toast.LENGTH_SHORT).show()
                } else {
                    Log.i("TTS_INIT", "TTS Engine Initialized Successfully.")
                }
            } else {
                Log.e("TTS_INIT", "TTS Initialization Failed! Status: $status")
                Toast.makeText(this, "TTS Initialization Failed.", Toast.LENGTH_SHORT).show()
            }
        })

        // Initialize SpeechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        setupSpeechRecognizerListener()

        // Request permissions
        checkAndRequestPermissions()

        // Setup button listeners
        voiceInputButton.setOnClickListener {
            switchToManualInput()
        }
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
                // Permission granted, start listening in onResume
                // No immediate action needed here, onResume will handle it
                Toast.makeText(this, "Audio permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Audio permission denied. Voice input disabled.", Toast.LENGTH_LONG).show()
                // Handle permission denial (e.g., disable voice features)
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
                voiceStatusTextView.text = "Error: $errorMessage. Tap to retry or use manual input."
                voiceInputButton.text = "Retry Voice"
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
        val headerRow = TableRow(this)
        headerRow.addView(TextView(this).apply { text = "Player" })
        for (h in 1..numHoles) {
            headerRow.addView(TextView(this).apply { text = "H$h"; gravity = Gravity.CENTER })
        }
        headerRow.addView(TextView(this).apply { text = "Total"; gravity = Gravity.CENTER })
        scorecardTable.addView(headerRow)

        for (p in 0 until numPlayers) {
            val row = TableRow(this)
            row.addView(TextView(this).apply { text = playerNames[p] })
            for (h in 0 until numHoles) {
                val editText = EditText(this)
                editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                editText.gravity = Gravity.CENTER
                // Initially not focusable for voice input mode
                editText.isFocusable = false
                editText.isFocusableInTouchMode = false
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
            val totalTextView = TextView(this)
            totalTextView.text = "0"
            row.addView(totalTextView)
            totalTextViews[p] = totalTextView
            scorecardTable.addView(row)
        }
    }

    // Resets the voice input UI elements to their default state
    private fun resetVoiceInputUIState() {
        voiceStatusTextView.text = "Listening for 'Hey Birdie'" // Or some default
        voiceStatusCard.visibility = View.VISIBLE // Or GONE, depending on default
        micFeedbackCard.visibility = View.GONE
        voiceInputButton.text = "Voice Input" // Or your default text
        // Ensure flags are reset so recognizer restarts correctly
        isListeningForWakeWord = true
        isAskingForScore = false
        // Make EditTexts focusable again if needed for manual input fallback
        setManualInputEnabled(true)
    }

    // Starts listening for the wake word "Hey Birdie"
    private fun startListeningForWakeWord() {
        Log.i("VoiceInput", "Attempting to start listening for WAKE WORD.")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                Toast.makeText(this, "Speech recognition is not available on this device.", Toast.LENGTH_LONG).show()
                Log.e("SpeechRecognizer", "Speech recognition service not available.")
                resetVoiceInputUIState() // Ensure UI is reset
                return
            }

            isListeningForWakeWord = true
            isAskingForScore = false // Ensure correct state
            // Make EditTexts non-focusable when listening for wake word
            setManualInputEnabled(false)
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

        // Enable manual input for the current hole
        setManualInputEnabled(true, currentHole -1) // Enable for current hole index

        // Optionally focus the first player's EditText for the current hole
        if (numPlayers > 0 && currentHole > 0 && currentHole <= numHoles) {
            scoreEditTexts[0][currentHole - 1].requestFocus()
            // Consider showing the keyboard - requires InputMethodManager
        }
        Toast.makeText(this, "Manual input enabled for Hole $currentHole", Toast.LENGTH_SHORT).show()
    }

    // Enable or disable manual input for the specified hole(s)
    private fun setManualInputEnabled(enabled: Boolean, holeIndex: Int? = null) {
        for (p in 0 until numPlayers) {
            for (h in 0 until numHoles) {
                val editText = scoreEditTexts[p][h]
                // Enable/disable only for the specified hole, or all if holeIndex is null
                if (holeIndex == null || h == holeIndex) {
                    editText.isFocusable = enabled
                    editText.isFocusableInTouchMode = enabled
                } else {
                    // Ensure other holes remain non-focusable if enabling only one hole
                    if (enabled) {
                        editText.isFocusable = false
                        editText.isFocusableInTouchMode = false
                    }
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
        speechRecognizer.stopListening()
        speechRecognizer.destroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onPause() {
        super.onPause()
        if (scoringMethod != "VOICE") {
            speechRecognizer.stopListening()
            textToSpeech.stop() // Stop any ongoing TTS
            micFeedbackCard.visibility = View.GONE // Hide feedback
            voiceStatusCard.visibility = View.GONE // Hide status card
            isListeningForWakeWord = false // Reset state flags if needed
            isAskingForScore = false
        }
        handler.removeCallbacksAndMessages(null)
    }

    override fun onResume() {
        super.onResume()
        // Restart listening for the wake word if permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            // Check if we should be listening or if manual mode was active
            // For simplicity, always restart wake word listening on resume for now.
            // A more robust solution might save/restore the input mode state.
            startListeningForWakeWord() // Start listening when activity resumes
        } else {
            // Optional: Inform user or request permission again if it was denied previously
            // checkAndRequestPermissions()
        }
    }
}
