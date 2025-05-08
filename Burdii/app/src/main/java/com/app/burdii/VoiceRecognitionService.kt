package com.app.burdii

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.*
import kotlin.random.Random

/**
 * A service that manages voice interaction for golf score input following a strict conversational flow.
 * Flow states:
 * 1. ACTIVATION_STATE - Listening for "Hey Birdie"
 * 2. SCORE_INQUIRY_STATE - Asking for scores on current hole
 * 3. SCORE_INPUT_PROCESSING - Processing spoken scores for multiple players
 * 4. CONFIRMATION_STATE - Confirming understanding of scores
 * 5. CONFIRMATION_HANDLING - Processing user's yes/no response
 */
class VoiceRecognitionService(
    private val context: Context,
    private val onScoresConfirmed: (Map<String, Int>, Int) -> Unit, // Callback with (playerScores, holeNumber)
    private val onStateChanged: (VoiceState, String) -> Unit // Callback to update UI based on current state
) {
    // Voice recognition states
    enum class VoiceState {
        ACTIVATION_STATE,              // Listening for wake word
        SCORE_INQUIRY_STATE,           // Asking for scores
        SCORE_INPUT_PROCESSING,        // Processing score input
        CONFIRMATION_STATE,            // Confirming scores
        CONFIRMATION_HANDLING,         // Processing yes/no response
        ERROR_STATE                    // Error occurred
    }

    private var currentState = VoiceState.ACTIVATION_STATE
    private var currentHoleNumber = 1
    private var tempScores: MutableMap<String, Int> = mutableMapOf()
    
    // Speech recognition components
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private val handler = Handler(Looper.getMainLooper())
    
    // Random affirmations for positive confirmation
    private val affirmations = listOf(
        "Got it!",
        "Great!",
        "Good work!",
        "Scores recorded.",
        "Perfect."
    )
    
    // Flag to track TTS initialization
    private var isTtsReady = false
    
    /**
     * Initialize voice recognition components
     */
    fun initialize() {
        initializeSpeechRecognizer()
        initializeTextToSpeech()
    }
    
    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer.setRecognitionListener(createRecognitionListener())
        } else {
            Log.e(TAG, "Speech recognition is not available on this device")
            currentState = VoiceState.ERROR_STATE
            onStateChanged(currentState, "Speech recognition not available on this device")
        }
    }
    
    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Language not supported")
                    currentState = VoiceState.ERROR_STATE
                    onStateChanged(currentState, "Text-to-speech language not supported")
                } else {
                    isTtsReady = true
                    textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            Log.d(TAG, "TTS started: $utteranceId")
                        }
                        
                        override fun onDone(utteranceId: String?) {
                            Log.d(TAG, "TTS done: $utteranceId")
                            when {
                                utteranceId?.startsWith("SCORE_INQUIRY") == true -> {
                                    // After asking for scores, start listening for score input
                                    handler.postDelayed({
                                        startListeningForScoreInput()
                                    }, 500)
                                }
                                utteranceId?.startsWith("CONFIRM_SCORES") == true -> {
                                    // After asking for confirmation, start listening for yes/no
                                    handler.postDelayed({
                                        startListeningForConfirmation()
                                    }, 500)
                                }
                                utteranceId?.startsWith("AFFIRMATION") == true -> {
                                    // After confirmation affirmation, go back to activation state
                                    handler.postDelayed({
                                        resetToActivationState()
                                    }, 500)
                                }
                                utteranceId?.startsWith("RETRY") == true -> {
                                    // After asking to retry, listen for scores again
                                    handler.postDelayed({
                                        startListeningForScoreInput()
                                    }, 500)
                                }
                            }
                        }
                        
                        override fun onError(utteranceId: String?) {
                            Log.e(TAG, "TTS error: $utteranceId")
                        }
                    })
                }
            } else {
                Log.e(TAG, "TTS initialization failed")
                currentState = VoiceState.ERROR_STATE
                onStateChanged(currentState, "Text-to-speech initialization failed")
            }
        }
    }
    
    /**
     * Start listening for the wake word "Hey Birdie"
     */
    fun startListeningForWakeWord() {
        if (currentState != VoiceState.ACTIVATION_STATE) {
            currentState = VoiceState.ACTIVATION_STATE
        }
        
        onStateChanged(currentState, "Listening for 'Hey Birdie'")
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        
        try {
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition: ${e.message}")
            currentState = VoiceState.ERROR_STATE
            onStateChanged(currentState, "Error starting speech recognition")
        }
    }
    
    /**
     * Start the score inquiry process for the current hole
     */
    private fun startScoreInquiry() {
        if (!isTtsReady) {
            Log.e(TAG, "TTS not ready")
            return
        }
        
        currentState = VoiceState.SCORE_INQUIRY_STATE
        onStateChanged(currentState, "Asking for scores on hole $currentHoleNumber")
        
        val message = "What were the scores on hole $currentHoleNumber?"
        textToSpeech.speak(
            message,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "SCORE_INQUIRY_$currentHoleNumber"
        )
    }
    
    /**
     * Start listening for score input
     */
    private fun startListeningForScoreInput() {
        currentState = VoiceState.SCORE_INPUT_PROCESSING
        onStateChanged(currentState, "Listening for player scores")
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        
        try {
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition: ${e.message}")
            currentState = VoiceState.ERROR_STATE
            onStateChanged(currentState, "Error listening for scores")
        }
    }
    
    /**
     * Parse player scores from the recognized text
     * Example inputs:
     * - "Jon got 3, Adam got 5, Nancy got 2"
     * - "Jon 3, Adam 5, Nancy 2"
     * - "Jon made a 3, Adam got a 5"
     */
    private fun parsePlayerScores(text: String): Map<String, Int> {
        val scores = mutableMapOf<String, Int>()
        
        // Common patterns for score statements
        val patterns = listOf(
            "(\\w+)\\s+(?:got|had|made|shot|scored|took)\\s+(?:a|an)?\\s*(\\d+)",  // "Jon got a 3"
            "(\\w+)\\s+(?:got|had|made|shot|scored|took)\\s*(\\d+)",               // "Jon got 3"
            "(\\w+)\\s+(\\d+)"                                                    // "Jon 3"
        )
        
        // Mapping of spoken number words to their numeric values for simple 0-10 range
        val wordNumberMap = mapOf(
            "zero" to 0,
            "one" to 1,
            "two" to 2,
            "three" to 3,
            "four" to 4,
            "five" to 5,
            "six" to 6,
            "seven" to 7,
            "eight" to 8,
            "nine" to 9,
            "ten" to 10
        )
        
        for (pattern in patterns) {
            val regex = pattern.toRegex(RegexOption.IGNORE_CASE)
            val matches = regex.findAll(text)
            
            for (match in matches) {
                if (match.groupValues.size >= 3) {
                    val playerName = match.groupValues[1].trim().capitalize()
                    val scoreStr = match.groupValues[2]
                    val score = try {
                        scoreStr.toInt()
                    } catch (e: NumberFormatException) {
                        // Attempt to map word number to integer (e.g., "four" -> 4)
                        wordNumberMap[scoreStr.lowercase()]
                    }
                    
                    if (score != null) {
                        scores[playerName] = score
                    } else {
                        Log.e(TAG, "Could not parse score: $scoreStr")
                    }
                }
            }
        }
        
        // Fallback: if we still have no player scores but a standalone number was spoken
        if (scores.isEmpty()) {
            val standaloneNumberRegex = "(\\d+)".toRegex()
            val numMatch = standaloneNumberRegex.find(text)
            val numberWordMatch = wordNumberMap.keys.firstOrNull { text.contains(it, ignoreCase = true) }
            
            val detectedScore = when {
                numMatch != null -> numMatch.groupValues[1].toIntOrNull()
                numberWordMatch != null -> wordNumberMap[numberWordMatch.lowercase()]
                else -> null
            }
            
            if (detectedScore != null) {
                // Use an empty key. Caller can map this to the only player if needed.
                scores["__UNKNOWN__"] = detectedScore
            }
        }
        
        return scores
    }
    
    /**
     * Start the confirmation process for scores
     */
    private fun startConfirmationProcess(playerScores: Map<String, Int>) {
        if (playerScores.isEmpty()) {
            // If no scores were parsed, retry
            speak("I didn't catch any scores. Please tell me the scores again for hole $currentHoleNumber.", "RETRY")
            return
        }
        
        // Save scores temporarily
        tempScores = playerScores.toMutableMap()
        
        // Build confirmation message
        val confirmationBuilder = StringBuilder("Okay, so that was ")
        val scoreEntries = playerScores.entries.toList()
        
        scoreEntries.forEachIndexed { index, entry ->
            if (index > 0) {
                if (index == scoreEntries.size - 1) {
                    confirmationBuilder.append(" and ")
                } else {
                    confirmationBuilder.append(", ")
                }
            }
            confirmationBuilder.append("${entry.key} with a ${entry.value}")
        }
        confirmationBuilder.append(". Is that correct?")
        
        currentState = VoiceState.CONFIRMATION_STATE
        onStateChanged(currentState, "Confirming scores")
        
        speak(confirmationBuilder.toString(), "CONFIRM_SCORES")
    }
    
    /**
     * Start listening for confirmation (yes/no)
     */
    private fun startListeningForConfirmation() {
        currentState = VoiceState.CONFIRMATION_HANDLING
        onStateChanged(currentState, "Listening for confirmation")
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        
        try {
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition: ${e.message}")
            currentState = VoiceState.ERROR_STATE
            onStateChanged(currentState, "Error listening for confirmation")
        }
    }
    
    /**
     * Handle positive confirmation
     */
    private fun handlePositiveConfirmation() {
        // Save confirmed scores
        onScoresConfirmed(tempScores, currentHoleNumber)
        
        // Increment hole number for next interaction
        currentHoleNumber++
        
        // Speak random affirmation
        val affirmation = affirmations[Random.nextInt(affirmations.size)]
        speak(affirmation, "AFFIRMATION")
    }
    
    /**
     * Handle negative confirmation
     */
    private fun handleNegativeConfirmation() {
        // Clear temporary scores
        tempScores.clear()
        
        // Ask for scores again for the same hole
        speak("My apologies. Please tell me the scores again for hole $currentHoleNumber.", "RETRY")
    }
    
    /**
     * Reset to activation state (listening for wake word)
     */
    private fun resetToActivationState() {
        tempScores.clear()
        startListeningForWakeWord()
    }
    
    /**
     * Utility function to speak text with utterance ID
     */
    private fun speak(text: String, utteranceId: String) {
        if (isTtsReady) {
            textToSpeech.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                utteranceId
            )
        } else {
            Log.e(TAG, "TTS not ready")
        }
    }
    
    /**
     * Create recognition listener for speech recognition
     */
    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
            }
            
            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Speech started")
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // Can be used for UI feedback on speech volume
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {
                // Not needed for this implementation
            }
            
            override fun onEndOfSpeech() {
                Log.d(TAG, "Speech ended")
            }
            
            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                    else -> "Unknown error"
                }
                
                Log.e(TAG, "Speech recognition error: $errorMessage ($error)")
                
                // Handle error based on current state
                when (currentState) {
                    VoiceState.ACTIVATION_STATE -> {
                        // If we're in activation state, just restart listening for wake word
                        handler.postDelayed({
                            startListeningForWakeWord()
                        }, 1000)
                    }
                    VoiceState.SCORE_INPUT_PROCESSING -> {
                        // If error during score input, ask to try again
                        speak("I'm sorry, I didn't catch that. Please tell me the scores again for hole $currentHoleNumber.", "RETRY")
                    }
                    VoiceState.CONFIRMATION_HANDLING -> {
                        // If error during confirmation, ask again
                        speak("I didn't hear your confirmation. Is that correct?", "CONFIRM_SCORES")
                    }
                    else -> {
                        // For other states, reset to activation state
                        handler.postDelayed({
                            resetToActivationState()
                        }, 1000)
                    }
                }
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches.isNullOrEmpty()) {
                    Log.e(TAG, "No speech recognition results")
                    return
                }
                
                val spokenText = matches[0].lowercase(Locale.getDefault())
                Log.d(TAG, "Speech recognized: $spokenText")
                
                when (currentState) {
                    VoiceState.ACTIVATION_STATE -> {
                        if (spokenText.contains("hey birdie")) {
                            Log.d(TAG, "Wake word detected")
                            startScoreInquiry()
                        } else {
                            // Not wake word, continue listening
                            startListeningForWakeWord()
                        }
                    }
                    VoiceState.SCORE_INPUT_PROCESSING -> {
                        val playerScores = parsePlayerScores(spokenText)
                        startConfirmationProcess(playerScores)
                    }
                    VoiceState.CONFIRMATION_HANDLING -> {
                        // Check if response is affirmative
                        if (isAffirmativeResponse(spokenText)) {
                            handlePositiveConfirmation()
                        } else if (isNegativeResponse(spokenText)) {
                            handleNegativeConfirmation()
                        } else {
                            // Unclear response, ask again
                            speak("I didn't understand. Please say yes or no.", "CONFIRM_SCORES")
                        }
                    }
                    else -> {
                        Log.w(TAG, "Received speech results in unexpected state: $currentState")
                    }
                }
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                // Only used for wake word detection in activation state
                if (currentState == VoiceState.ACTIVATION_STATE) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val partialText = matches[0].lowercase(Locale.getDefault())
                        if (partialText.contains("hey birdie")) {
                            // Wake word detected in partial results
                            Log.d(TAG, "Wake word detected in partial results")
                            speechRecognizer.stopListening() // Stop and process full results
                        }
                    }
                }
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {
                // Not used in this implementation
            }
        }
    }
    
    /**
     * Check if the response is affirmative (yes)
     */
    private fun isAffirmativeResponse(text: String): Boolean {
        val affirmativePatterns = listOf(
            "yes", "yeah", "yep", "yup", "correct", "right", "that's right", "that is right",
            "sounds good", "good", "perfect", "exactly", "affirmative", "yes that's correct",
            "yes that is correct"
        )
        
        return affirmativePatterns.any { pattern -> 
            text.contains(pattern) || text == pattern
        }
    }
    
    /**
     * Check if the response is negative (no)
     */
    private fun isNegativeResponse(text: String): Boolean {
        val negativePatterns = listOf(
            "no", "nope", "nah", "incorrect", "wrong", "that's wrong", "that is wrong",
            "not right", "negative", "no that's incorrect", "no that is incorrect",
            "no that's wrong", "no that is wrong"
        )
        
        return negativePatterns.any { pattern -> 
            text.contains(pattern) || text == pattern
        }
    }
    
    /**
     * Set current hole number (e.g., when resuming a saved round)
     */
    fun setCurrentHoleNumber(holeNumber: Int) {
        currentHoleNumber = holeNumber
    }
    
    /**
     * Get current hole number
     */
    fun getCurrentHoleNumber(): Int {
        return currentHoleNumber
    }
    
    /**
     * Release resources
     */
    fun release() {
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.stopListening()
            speechRecognizer.cancel()
            speechRecognizer.destroy()
        }
        
        if (::textToSpeech.isInitialized && isTtsReady) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        
        handler.removeCallbacksAndMessages(null)
    }
    
    companion object {
        private const val TAG = "VoiceRecognitionService"
    }
}

// String extension function to capitalize first letter
private fun String.capitalize(): String {
    return if (isNotEmpty()) this[0].uppercaseChar() + substring(1).lowercase() else this
}
