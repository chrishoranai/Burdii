package com.app.burdii

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class ScorecardActivity : AppCompatActivity() {
    private lateinit var scorecardTable: TableLayout
    private lateinit var currentHoleTextView: TextView
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var speechRecognizer: SpeechRecognizer
    private val handler = Handler(Looper.getMainLooper())
    private var currentHole = 1
    private var currentPlayerIndex = 0
    private lateinit var playerNames: Array<String>
    private var numHoles = 0
    private var numPlayers = 0
    private var pars: IntArray? = null
    private lateinit var scores: Array<IntArray>
    private lateinit var scoreEditTexts: Array<Array<EditText>>
    private lateinit var totalTextViews: Array<TextView>
    private var isListeningForWakeWord = true
    private var isAskingForScore = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scorecard)

        // Initialize UI components
        scorecardTable = findViewById(R.id.scorecardTable)
        currentHoleTextView = findViewById(R.id.currentHoleTextView)

        // Retrieve data from intent
        numHoles = intent.getIntExtra("numHoles", 0)
        numPlayers = intent.getIntExtra("numPlayers", 0)
        playerNames = intent.getStringArrayExtra("playerNames") ?: arrayOf()
        pars = intent.getIntArrayExtra("pars")

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
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                if (isListeningForWakeWord) {
                    startListeningForWakeWord()
                } else if (isAskingForScore) {
                    textToSpeech.speak("Sorry, I didn't catch that. Please say the score again.", TextToSpeech.QUEUE_FLUSH, null, null)
                    handler.postDelayed({
                        startListeningForScore()
                    }, 1500)
                }
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    val spokenText = matches[0].lowercase()
                    if (isListeningForWakeWord && spokenText.contains("hey birdie")) {
                        isListeningForWakeWord = false
                        startScoreInputForHole(currentHole)
                    } else if (isAskingForScore) {
                        val score = parseScore(spokenText)
                        if (score != null) {
                            scores[currentPlayerIndex][currentHole - 1] = score
                            scoreEditTexts[currentPlayerIndex][currentHole - 1].setText(score.toString())
                            updateTotalScore(currentPlayerIndex)
                            currentPlayerIndex++
                            askForScore()
                        } else {
                            textToSpeech.speak("Sorry, I didn't understand. Please say the score again.", TextToSpeech.QUEUE_FLUSH, null, null)
                            handler.postDelayed({
                                startListeningForScore()
                            }, 1500)
                        }
                    }
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        // Start listening for wake word
        startListeningForWakeWord()

        currentHoleTextView.text = "Current Hole: $currentHole"
    }

    // Sets up the scorecard table with headers and player rows
    private fun setupScorecardTable() {
        val headerRow = TableRow(this)
        val playerHeader = TextView(this)
        playerHeader.text = "Player"
        headerRow.addView(playerHeader)
        for (i in 1..numHoles) {
            val holeHeader = TextView(this)
            val holeText = if (pars != null && pars!![i - 1] > 0) "Hole $i (Par ${pars!![i - 1]})" else "Hole $i"
            holeHeader.text = holeText
            headerRow.addView(holeHeader)
        }
        val totalHeader = TextView(this)
        totalHeader.text = "Total"
        headerRow.addView(totalHeader)
        scorecardTable.addView(headerRow)

        for (p in 0 until numPlayers) {
            val row = TableRow(this)
            val nameTextView = TextView(this)
            nameTextView.text = playerNames[p]
            row.addView(nameTextView)
            for (h in 0 until numHoles) {
                val editText = EditText(this)
                editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                editText.setText("")
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
        isListeningForWakeWord = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        speechRecognizer.startListening(intent)
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
            }, 1500) // Delay to allow TTS to finish
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
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechRecognizer.startListening(intent)
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

    // Cleanup resources
    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.shutdown()
        speechRecognizer.destroy()
    }
}
