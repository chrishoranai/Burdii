package com.app.burdii

import android.Manifest
import android.content.Intent
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
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.cardview.widget.CardView
import java.util.*

class ScorecardActivity : AppCompatActivity() {
    private lateinit var scorecardTable: TableLayout
    private lateinit var currentHoleTextView: TextView
    private lateinit var micFeedbackCard: CardView
    private lateinit var voiceStatusCard: CardView
    private lateinit var voiceStatusTextView: TextView
    private lateinit var voiceInputButton: Button
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

    private val REQUEST_RECORD_AUDIO_PERMISSION = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scorecard)

        // Initialize UI components
        scorecardTable = findViewById(R.id.scorecardTable)
        currentHoleTextView = findViewById(R.id.currentHoleTextView)
        micFeedbackCard = findViewById(R.id.micFeedbackCard)
        voiceStatusCard = findViewById(R.id.voiceStatusCard)
        voiceStatusTextView = findViewById(R.id.voiceStatusTextView)
        voiceInputButton = findViewById(R.id.voiceInputButton)
        micFeedbackCard.visibility = View.GONE
        voiceStatusCard.visibility = View.VISIBLE
        voiceStatusTextView.text = "Listening for 'Hey Birdie'"

        // Retrieve data from intent
        numHoles = intent.getIntExtra("NUM_HOLES", 18) // Default to 18 if not found
        playerNames = intent.getStringArrayExtra("PLAYER_NAMES") ?: arrayOf("Player 1") // Default if not found
        numPlayers = playerNames.size // Get numPlayers from the array size
        parValues = intent.getIntArrayExtra("PAR_VALUES") ?: IntArray(numHoles) { 3 } // Default par 3 if not found
        scoringMethod = intent.getStringExtra("SCORING_METHOD") ?: "MANUAL" // Default to MANUAL

        // Initialize scores and UI arrays
        scores = Array(numPlayers) { IntArray(numHoles) }
        scoreEditTexts = Array(numPlayers) { Array(numHoles) { EditText(this) } }
        totalTextViews = Array(numPlayers) { TextView(this) }

        // Setup the scorecard table
        setupScorecardTable()

        // Initialize TextToSpeech
        textToSpeech = TextToSpeech(this, TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.US
            } else {
                Toast.makeText(this, "TextToSpeech initialization failed", Toast.LENGTH_SHORT).show()
            }
        })

        // Initialize SpeechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        setupSpeechRecognizerListener()

        // Request permissions
        checkAndRequestPermissions()

        // Setup button listener
        voiceInputButton.setOnClickListener {
            switchToManualInput()
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
                micFeedbackCard.visibility = View.VISIBLE
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                micFeedbackCard.visibility = View.GONE
            }
            override fun onError(error: Int) {
                micFeedbackCard.visibility = View.GONE
                // Avoid infinite loops on continuous errors
                if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    if (isListeningForWakeWord) {
                        handler.postDelayed({ startListeningForWakeWord() }, 500)
                    } else if (isAskingForScore) {
                        textToSpeech.speak("Sorry, I didn't catch that. Please say the score again.", TextToSpeech.QUEUE_FLUSH, null, null)
                        handler.postDelayed({ startListeningForScore() }, 1500)
                    }
                } else {
                    // Handle other errors (log, show message, etc.)
                    Toast.makeText(this@ScorecardActivity, "Speech Recognizer Error: $error", Toast.LENGTH_SHORT).show()
                    // Maybe try restarting wake word listening after a longer delay or stop trying?
                    if (isListeningForWakeWord) {
                        handler.postDelayed({ startListeningForWakeWord() }, 2000)
                    }
                }
            }
            override fun onResults(results: Bundle?) {
                micFeedbackCard.visibility = View.GONE
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    val spokenText = matches[0].lowercase()
                    if (isListeningForWakeWord && spokenText.contains("hey birdie")) {
                        isListeningForWakeWord = false
                        isAskingForScore = false
                        micFeedbackCard.visibility = View.GONE
                        startScoreInputForHole(currentHole)
                    } else if (isAskingForScore) {
                        val score = parseScore(spokenText)
                        if (score != null) {
                            scores[currentPlayerIndex][currentHole - 1] = score
                            scoreEditTexts[currentPlayerIndex][currentHole - 1].setText(score.toString())
                            updateTotalScore(currentPlayerIndex)
                            currentPlayerIndex++
                            isAskingForScore = false
                            handler.postDelayed({ askForScore() }, 500)
                        } else {
                            textToSpeech.speak("Sorry, that wasn't a valid score. Please try again.", TextToSpeech.QUEUE_FLUSH, null, null)
                            handler.postDelayed({ startListeningForScore() }, 1500)
                        }
                    }
                } else {
                    // No matches, restart listening if appropriate
                    if (isListeningForWakeWord) {
                        startListeningForWakeWord()
                    } else if (isAskingForScore) {
                        textToSpeech.speak("Didn't get that. Please say the score.", TextToSpeech.QUEUE_FLUSH, null, null)
                        handler.postDelayed({ startListeningForScore() }, 1500)
                    }
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
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

    // Starts listening for the wake word "Hey Birdie"
    private fun startListeningForWakeWord() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            isListeningForWakeWord = true
            isAskingForScore = false // Ensure correct state
            // Make EditTexts non-focusable when listening for wake word
            setManualInputEnabled(false)
            voiceStatusCard.visibility = View.VISIBLE
            voiceStatusTextView.text = "Listening for 'Hey Birdie'"
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            speechRecognizer.startListening(intent)
        }
    }

    // Starts the score input process for the current hole
    private fun startScoreInputForHole(hole: Int) {
        currentHole = hole
        currentPlayerIndex = 0
        askForScore()
    }

    // Prompts for the next player's score
    private fun askForScore() {
        if (currentPlayerIndex < numPlayers) {
            val playerName = playerNames[currentPlayerIndex]
            val prompt = "Tell me the score for $playerName on Hole $currentHole"
            textToSpeech.speak(prompt, TextToSpeech.QUEUE_FLUSH, null, null)
            handler.postDelayed({
                isAskingForScore = true
                startListeningForScore()
            }, 1500)
        } else {
            currentHole++
            if (currentHole <= numHoles) {
                currentHoleTextView.text = "Current Hole: $currentHole"
                isListeningForWakeWord = true
                startListeningForWakeWord()
            } else {
                textToSpeech.speak("Game Finished", TextToSpeech.QUEUE_FLUSH, null, null)
                Toast.makeText(this, "Game Finished", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Starts listening for the score input
    private fun startListeningForScore() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            isAskingForScore = true
            isListeningForWakeWord = false
            voiceStatusCard.visibility = View.VISIBLE
            voiceStatusTextView.text = "Listening for score..."
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
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
        speechRecognizer.stopListening()
        textToSpeech.stop() // Stop any ongoing TTS
        micFeedbackCard.visibility = View.GONE // Hide feedback
        voiceStatusCard.visibility = View.GONE // Hide status card
        isListeningForWakeWord = false // Reset state flags if needed
        isAskingForScore = false
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
