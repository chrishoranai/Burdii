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
import kotlin.text.RegexOption

class VoiceRecognitionService(
    private val context: Context,
    private val onScoresConfirmed: (Map<String, Int>, Int) -> Unit,
    private val onStateChanged: (VoiceState, String) -> Unit
) {
    enum class VoiceState {
        ACTIVATION_STATE, SCORE_INQUIRY_STATE, SCORE_INPUT_PROCESSING,
        CONFIRMATION_STATE, CONFIRMATION_HANDLING, ERROR_STATE
    }

    private var currentState = VoiceState.ACTIVATION_STATE
    private var currentHoleNumber = 1
    private var tempScores = mutableMapOf<String, Int>()
    private var retryAttempts = 0
    private val MAX_RETRY_ATTEMPTS = 2
    
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private val handler = Handler(Looper.getMainLooper())
    
    private val affirmations = listOf("Got it!", "Great!", "Good work!", "Scores recorded.", "Perfect.")
    private var isTtsReady = false
    private var isListening = false

    // Wake word patterns for better detection
    private val wakeWordPatterns = listOf(
        "hey birdie",
        "hay birdie", 
        "hey burdy",
        "hey bertie"
    )
    
    // Enhanced regex patterns for score parsing
    private val scorePatterns = listOf(
        Regex("(\\w+)(?:\\s+(?:got|had|made|shot|scored|took))?\\s+(?:a|an)?\\s*(\\d+|zero|one|two|three|four|five|six|seven|eight|nine|ten)", RegexOption.IGNORE_CASE),
        Regex("(\\w+)\\s+(?:is|was)\\s+(?:a|an)?\\s*(\\d+|zero|one|two|three|four|five|six|seven|eight|nine|ten)", RegexOption.IGNORE_CASE),
        Regex("(\\w+)\\s+(\\d+|zero|one|two|three|four|five|six|seven|eight|nine|ten)", RegexOption.IGNORE_CASE)
    )
    
    private val wordNumberMap = mapOf(
        "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4,
        "five" to 5, "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10
    )

    private val ttsListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {}
        override fun onDone(utteranceId: String?) {
            when {
                utteranceId?.startsWith("SCORE_INQUIRY") == true -> scheduleAction(1200) { startListeningForScoreInput() }
                utteranceId?.startsWith("CONFIRM_SCORES") == true -> scheduleAction(1000) { startListeningForConfirmation() }
                utteranceId?.startsWith("AFFIRMATION") == true -> scheduleAction(500) { resetToActivationState() }
                utteranceId?.startsWith("RETRY") == true -> scheduleAction(500) { startListeningForScoreInput() }
            }
        }
        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            Log.e(TAG, "TTS error: $utteranceId")
        }
    }

    fun initialize() {
        initializeSpeechRecognizer()
        initializeTextToSpeech()
    }

    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer.setRecognitionListener(createRecognitionListener())
        } else {
            handleError("Speech recognition not available")
        }
    }

    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.apply {
                    val langStatus = setLanguage(Locale.US)
                    if (langStatus == TextToSpeech.LANG_NOT_SUPPORTED) {
                        handleError("TTS language not supported")
                        return@apply
                    }
                    setSpeechRate(0.8f)
                    setPitch(1.0f)
                    setOnUtteranceProgressListener(ttsListener)
                    isTtsReady = true
                }
            } else {
                handleError("TTS initialization failed")
            }
        }
    }

    fun startListeningForWakeWord() {
        if (currentState != VoiceState.ACTIVATION_STATE) return
        
        onStateChanged(currentState, "Listening for 'Hey Birdie'")
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        
        safeStartListening(intent)
    }
    
    // Set the current hole number (called from ScorecardActivity when hole changes)
    fun setCurrentHoleNumber(holeNumber: Int) {
        if (holeNumber in 1..18) {  // Reasonable range check
            currentHoleNumber = holeNumber
        }
    }

    fun stopListeningForWakeWord() {
        if (::speechRecognizer.isInitialized && isListening) {
            speechRecognizer.stopListening()
            isListening = false
        }
    }

    fun release() {
        currentState = VoiceState.ERROR_STATE
        handler.removeCallbacksAndMessages(null)
        stopListeningForWakeWord()
        
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }
    }

    // Improved safeStartListening with auto-retry
    private fun safeStartListening(intent: Intent, maxRetries: Int = 2) {
        if (!::speechRecognizer.isInitialized) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer.setRecognitionListener(createRecognitionListener())
            } catch (e: Exception) {
                handleError("Failed to initialize speech recognizer: ${e.message}")
                return
            }
        }
        
        var retryCount = 0
        fun attemptListen() {
            try {
                speechRecognizer.startListening(intent)
                isListening = true
            } catch (e: Exception) {
                if (retryCount < maxRetries) {
                    retryCount++
                    Log.w(TAG, "Speech recognition failed, retrying (${retryCount}/${maxRetries}): ${e.message}")
                    handler.postDelayed({ attemptListen() }, 1000)
                } else {
                    handleError("Error starting speech recognition after $maxRetries retries: ${e.message}")
                }
            }
        }
        
        attemptListen()
    }
    
    // Enhanced wake word detection with fuzzy matching
    private fun containsWakeWord(text: String): Boolean {
        val normalizedText = text.lowercase().trim()
        return wakeWordPatterns.any { pattern -> 
            normalizedText.contains(pattern) || 
            calculateLevenshteinDistance(normalizedText, pattern) <= 2
        }
    }

    // Calculate edit distance for fuzzy matching
    private fun calculateLevenshteinDistance(s1: String, s2: String): Int {
        val costs = IntArray(s2.length + 1)
        for (i in 0..s2.length) costs[i] = i
        
        for (i in 1..s1.length) {
            var lastValue = i
            for (j in 1..s2.length) {
                val oldValue = costs[j]
                costs[j] = minOf(
                    costs[j] + 1,
                    costs[j - 1] + 1,
                    lastValue + if (s1[i - 1] == s2[j - 1]) 0 else 1
                )
                lastValue = oldValue
            }
        }
        return costs[s2.length]
    }

    private fun startScoreInquiry() {
        if (!isTtsReady) return
        currentState = VoiceState.SCORE_INQUIRY_STATE
        onStateChanged(currentState, "Asking for scores on hole $currentHoleNumber")
        speak("Please tell me the scores for hole $currentHoleNumber.", "SCORE_INQUIRY")
    }

    private fun startConfirmationProcess(playerScores: Map<String, Int>) {
        if (playerScores.isEmpty()) {
            retryAttempts++
            if (retryAttempts < MAX_RETRY_ATTEMPTS) {
                speak("I didn't catch any scores. Please try again.", "RETRY")
            } else {
                speak("Let's try again later. Say Hey Birdie when ready.", "RESET")
                resetToActivationState()
            }
            return
        }
        retryAttempts = 0
        tempScores = playerScores.toMutableMap()
        val message = "For hole $currentHoleNumber, I heard " + playerScores.entries.joinToString(" and ") { "${it.key} scored ${it.value}" } + ". Is that correct?"
        currentState = VoiceState.CONFIRMATION_STATE
        onStateChanged(currentState, message)
        speak(message, "CONFIRM_SCORES")
    }

    private fun handleConfirmationResponse(spokenText: String) {
        when {
            isAffirmativeResponse(spokenText) -> handlePositiveConfirmation()
            isNegativeResponse(spokenText) -> handleNegativeConfirmation()
            retryAttempts < MAX_RETRY_ATTEMPTS -> {
                retryAttempts++
                speak("Please say yes or no.", "RETRY")
            }
            else -> {
                speak("Let's try again later. Say Hey Birdie when ready.", "RESET")
                resetToActivationState()
            }
        }
    }

    private fun parsePlayerScores(text: String): Map<String, Int> {
        val scores = mutableMapOf<String, Int>()
        val normalizedText = text.lowercase().trim()
        
        // Try to extract scores using our patterns
        scorePatterns.forEach { pattern ->
            pattern.findAll(normalizedText).forEach { match ->
                val playerName = match.groupValues[1].trim()
                val scoreText = match.groupValues[2].trim()
                
                // Skip empty matches
                if (playerName.isEmpty() || scoreText.isEmpty()) return@forEach
                
                // Process player name (handle special cases like "me", "I", etc.)
                val finalPlayerName = when (playerName.lowercase()) {
                    "me", "i", "myself" -> "Player One"
                    else -> playerName.replaceFirstChar { it.uppercase() }
                }
                
                // Process score (convert word numbers to digits)
                val finalScore = scoreText.toIntOrNull() ?: wordNumberMap[scoreText.lowercase()] ?: return@forEach
                
                // Add to scores map
                scores[finalPlayerName] = finalScore
            }
        }
        
        // If we didn't find any scores, try the fallback method
        return scores.ifEmpty { parseFallbackScore(normalizedText) }
    }

    private fun parseFallbackScore(text: String): Map<String, Int> {
        val numberPattern = "\\b(\\d+|zero|one|two|three|four|five|six|seven|eight|nine|ten)\\b".toRegex(RegexOption.IGNORE_CASE)
        val match = numberPattern.find(text)?.groupValues?.getOrNull(1)?.lowercase()
        val number = match?.toIntOrNull() ?: wordNumberMap[match]
        return if (number != null) mapOf("Player One" to number) else emptyMap()
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() { isListening = false }

        override fun onError(error: Int) {
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH -> {
                    // No matching speech - common and not critical
                    Log.w(TAG, "No speech detected (ERROR_NO_MATCH)")
                    if (currentState == VoiceState.ACTIVATION_STATE) {
                        handler.postDelayed(::startListeningForWakeWord, 1000)
                    } else {
                        handleSpeechError("I didn't catch that. Please try again.")
                    }
                }
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    // Speech timeout - common and not critical
                    Log.w(TAG, "Speech timeout (ERROR_SPEECH_TIMEOUT)")
                    if (currentState == VoiceState.ACTIVATION_STATE) {
                        handler.postDelayed(::startListeningForWakeWord, 1000)
                    } else {
                        handleSpeechError("I didn't hear anything. Please try again.")
                    }
                }
                SpeechRecognizer.ERROR_NETWORK, 
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                    // Network-related errors
                    Log.e(TAG, "Network error in speech recognition: $error")
                    handleSpeechError("I'm having trouble with the network. Please try again.")
                }
                else -> {
                    // Other errors
                    Log.e(TAG, "Speech recognition error: ${getErrorText(error)} ($error)")
                    handleError("Recognition error: ${getErrorText(error)}")
                }
            }
        }

        override fun onPartialResults(results: Bundle?) {
            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { partial ->
                if (currentState == VoiceState.ACTIVATION_STATE && containsWakeWord(partial)) {
                    Log.d(TAG, "Wake word detected in partial results: '$partial'")
                    stopListeningForWakeWord()
                    startScoreInquiry()
                }
            }
        }

        override fun onResults(results: Bundle?) {
            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { spokenText ->
                Log.d(TAG, "Speech recognized in state $currentState: '$spokenText'")
                when (currentState) {
                    VoiceState.ACTIVATION_STATE -> if (containsWakeWord(spokenText)) startScoreInquiry()
                    VoiceState.SCORE_INPUT_PROCESSING -> {
                        val scores = parsePlayerScores(spokenText)
                        Log.d(TAG, "Parsed scores: $scores from '$spokenText'")
                        startConfirmationProcess(scores)
                    }
                    VoiceState.CONFIRMATION_HANDLING -> handleConfirmationResponse(spokenText)
                    else -> Unit
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun handleError(message: String) {
        Log.e(TAG, message)
        currentState = VoiceState.ERROR_STATE
        onStateChanged(currentState, message)
    }

    private fun scheduleAction(delayMs: Long, action: () -> Unit) {
        handler.postDelayed(action, delayMs)
    }

    private fun startListeningForScoreInput() {
        currentState = VoiceState.SCORE_INPUT_PROCESSING
        onStateChanged(currentState, "Listening for scores...")
        safeStartListening(createScoreInputIntent())
    }

    private fun startListeningForConfirmation() {
        currentState = VoiceState.CONFIRMATION_HANDLING
        onStateChanged(currentState, "Listening for confirmation")
        safeStartListening(createConfirmationIntent())
    }

    private fun resetToActivationState() {
        tempScores.clear()
        retryAttempts = 0
        currentState = VoiceState.ACTIVATION_STATE
        // Explicitly notify of state change to ensure UI updates
        onStateChanged(currentState, "Listening for 'Hey Birdie'")
        startListeningForWakeWord()
    }

    private fun speak(message: String, utterancePrefix: String) {
        if (!isTtsReady) return
        val utteranceId = utterancePrefix + "_" + System.currentTimeMillis().toString()
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    private fun isAffirmativeResponse(text: String) = listOf("yes", "yeah", "yep", "correct", "right").any { text.contains(it, ignoreCase = true) }

    private fun isNegativeResponse(text: String) = listOf("no", "nope", "nah", "incorrect", "wrong").any { text.contains(it, ignoreCase = true) }

    private fun handlePositiveConfirmation() {
        onScoresConfirmed(tempScores, currentHoleNumber)
        currentHoleNumber++
        // Let the caller know we're going back to activation state
        onStateChanged(VoiceState.ACTIVATION_STATE, "Listening for 'Hey Birdie'")
        speak(affirmations[Random.nextInt(affirmations.size)], "AFFIRMATION")
    }

    private fun handleNegativeConfirmation() {
        tempScores.clear()
        retryAttempts++
        if (retryAttempts < MAX_RETRY_ATTEMPTS) {
            speak("Sorry, let's try again. Please tell me the scores.", "RETRY")
        } else {
            // Update state before speaking to ensure UI reflects the change
            onStateChanged(VoiceState.ACTIVATION_STATE, "Listening for 'Hey Birdie'")
            speak("Let's try again later. Say Hey Birdie when ready.", "RESET")
            resetToActivationState()
        }
    }

    private fun createScoreInputIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
    }

    private fun createConfirmationIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
    }

    // Handle speech errors with appropriate retry mechanism
    private fun handleSpeechError(message: String) {
        if (retryAttempts < MAX_RETRY_ATTEMPTS) {
            retryAttempts++
            speak(message, "RETRY")
        } else {
            speak("Let's try again later. Say Hey Birdie when ready.", "RESET")
            resetToActivationState()
        }
    }

    companion object {
        private const val TAG = "VoiceRecognitionService"
        private fun getErrorText(errorCode: Int): String = when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No recognition match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
            else -> "Unknown error"
        }
    }
}