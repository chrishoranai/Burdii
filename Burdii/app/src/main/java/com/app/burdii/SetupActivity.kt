package com.app.burdii

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * SetupActivity - Configures a new disc golf round
 * - Sets number of holes
 * - Configures player names dynamically
 * - Configures optional par values
 * - Selects scoring method (voice or manual)
 */
class SetupActivity : AppCompatActivity() {
    private lateinit var roundNameEditText: TextInputEditText
    private lateinit var holesNumberPicker: NumberPicker
    private lateinit var player1EditText: TextInputEditText
    private lateinit var playerNamesContainer: LinearLayout
    private lateinit var addPlayerButton: MaterialButton
    private lateinit var setParsButton: MaterialButton
    private lateinit var startGameButton: MaterialButton
    // Voice input is now toggled on the scorecard page
    
    private var playerCount = 1 // Start with one player
    private var pars: IntArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        // Initialize UI components
        roundNameEditText = findViewById(R.id.roundNameEditText)
        holesNumberPicker = findViewById(R.id.holesNumberPicker)
        player1EditText = findViewById(R.id.player1EditText)
        playerNamesContainer = findViewById(R.id.playerNamesContainer)
        addPlayerButton = findViewById(R.id.addPlayerButton)
        setParsButton = findViewById(R.id.setParsButton)
        startGameButton = findViewById(R.id.startGameButton)
        
        // Setup number picker for holes
        holesNumberPicker.apply {
            minValue = 1
            maxValue = 99
            value = 18 // Default to 18 holes
            wrapSelectorWheel = false
        }
        
        // Voice input is now toggled on the scorecard page, not here

        // Add player button click handler
        addPlayerButton.setOnClickListener {
            playerCount++
            addPlayerNameField(playerCount)
        }

        // Set pars button click handler
        setParsButton.setOnClickListener {
            val numHoles = holesNumberPicker.value
            showParsDialog(numHoles)
        }

        // Start game button click handler
        startGameButton.setOnClickListener {
            if (validateInputs()) {
                startGame()
            }
        }
    }
    
    /**
     * Validates all required inputs before starting the game
     */
    private fun validateInputs(): Boolean {
        // Check if round name is entered
        if (roundNameEditText.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "Please enter a name for this round", Toast.LENGTH_SHORT).show()
            return false
        }
        
        // Check if at least one player name is entered
        if (player1EditText.text.toString().trim().isEmpty()) {
            Toast.makeText(this, "Please enter at least one player name", Toast.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }
    
    /**
     * Starts the scorecard activity with all configured settings
     */
    private fun startGame() {
        val numHoles = holesNumberPicker.value
        val roundName = roundNameEditText.text.toString().trim()
        
        // Collect player names from all text fields
        val playerNames = collectPlayerNames()
        
        // Create intent and add data
        val intent = Intent(this, ScorecardActivity::class.java)
        intent.putExtra("NUM_HOLES", numHoles)
        intent.putExtra("ROUND_NAME", roundName)
        intent.putExtra("PLAYER_NAMES", playerNames.toTypedArray())
        
        // Ensure parValues is initialized before passing
        val parsToPass = if (pars != null) pars else IntArray(numHoles) { 3 } // Default par 3 if not set
        intent.putExtra("PAR_VALUES", parsToPass)
        
        // Always default to manual input, voice can be toggled on the scorecard page
        intent.putExtra("SCORING_METHOD", "MANUAL")
        
        startActivity(intent)
    }
    
    /**
     * Collects player names from all the dynamically added text fields
     */
    private fun collectPlayerNames(): List<String> {
        val names = mutableListOf<String>()
        
        // Add the first player (always present)
        val player1Name = player1EditText.text.toString().trim()
        if (player1Name.isNotEmpty()) {
            names.add(player1Name)
        }
        
        // Add names from dynamically added fields
        for (i in 2..playerCount) {
            val playerEditText = playerNamesContainer.findViewWithTag<TextInputEditText>("player_$i")
            playerEditText?.let {
                val playerName = it.text.toString().trim()
                if (playerName.isNotEmpty()) {
                    names.add(playerName)
                }
            }
        }
        
        return names
    }
    
    /**
     * Adds a new player name field to the container
     */
    private fun addPlayerNameField(playerNumber: Int) {
        // Create a new TextInputLayout
        val inputLayout = TextInputLayout(this, null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(8)
                // Add horizontal padding for subtle UI improvement
                marginStart = dpToPx(2)
                marginEnd = dpToPx(2)
            }
            hint = "Player $playerNumber"
        }
        
        // Create a new TextInputEditText
        val editText = TextInputEditText(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_PERSON_NAME
            setText("Player $playerNumber")
            tag = "player_$playerNumber" // Tag for later retrieval
            id = View.generateViewId() // Generate a unique ID
            // Add subtle padding for improved text layout
            setPadding(dpToPx(16), paddingTop, dpToPx(16), paddingBottom)
        }
        
        // Add EditText to the TextInputLayout
        inputLayout.addView(editText)
        
        // Add the TextInputLayout to the container
        playerNamesContainer.addView(inputLayout)
    }
    
    /**
     * Dialog to input par values with improved UI
     */
    private fun showParsDialog(numHoles: Int) {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
        val scrollView = ScrollView(this)
        val containerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(24), dpToPx(16), dpToPx(24), dpToPx(16)) // Using 24dp horizontal padding per user preference
        }
        
        scrollView.addView(containerLayout)
        
        val title = TextView(this).apply {
            text = "Set Par Values"
            textSize = 18f
            setTextColor(ContextCompat.getColor(context, R.color.textPrimary))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dpToPx(16))
        }
        
        containerLayout.addView(title)
        
        val editTexts = Array(numHoles) { index ->
            val holeCard = CardView(this).apply {
                radius = dpToPx(8).toFloat()
                cardElevation = dpToPx(2).toFloat()
                setContentPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, dpToPx(8))
                }
                // Use subtle off-white background per user preference
                setCardBackgroundColor(resources.getColor(android.R.color.white))
            }
            
            val holeLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            
            val holeLabel = TextView(this).apply {
                text = "Hole ${index + 1}"
                layoutParams = LinearLayout.LayoutParams(
                    0, 
                    ViewGroup.LayoutParams.WRAP_CONTENT, 
                    1f
                )
                setTextColor(ContextCompat.getColor(context, R.color.textPrimary))
            }
            
            val layout = TextInputLayout(this, null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox).apply {
                hint = "Par"
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, 
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            
            val editText = TextInputEditText(this).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                setText(if (pars != null && index < pars!!.size) pars!![index].toString() else "3")
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(60), 
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            
            layout.addView(editText)
            holeLayout.addView(holeLabel)
            holeLayout.addView(layout)
            holeCard.addView(holeLayout)
            containerLayout.addView(holeCard)
            
            editText
        }
        
        dialog.setView(scrollView)
        dialog.setPositiveButton("OK") { _, _ ->
            pars = IntArray(numHoles)
            for (i in 0 until numHoles) {
                val parStr = editTexts[i].text.toString()
                pars!![i] = if (parStr.isNotEmpty()) parStr.toInt() else 3
            }
        }
        dialog.setNegativeButton("Cancel", null)
        dialog.show()
    }
    
    /**
     * Convert dp to pixels
     */
    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
