package com.app.burdii

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * SetupActivity - Configures a new disc golf round
 * - Sets number of holes and players
 * - Configures player names and optional par values
 * - Selects scoring method (voice or manual)
 */
class SetupActivity : AppCompatActivity() {
    private lateinit var numHolesEditText: TextInputEditText
    private lateinit var numPlayersEditText: TextInputEditText
    private lateinit var setPlayerNamesButton: MaterialButton
    private lateinit var setParsButton: MaterialButton
    private lateinit var startGameButton: MaterialButton
    private lateinit var voiceInputRadioButton: RadioButton
    private lateinit var manualInputRadioButton: RadioButton
    
    private lateinit var playerNames: Array<String>
    private var pars: IntArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        // Initialize UI components
        numHolesEditText = findViewById(R.id.numHolesEditText)
        numPlayersEditText = findViewById(R.id.numPlayersEditText)
        setPlayerNamesButton = findViewById(R.id.setPlayerNamesButton)
        setParsButton = findViewById(R.id.setParsButton)
        startGameButton = findViewById(R.id.startGameButton)
        voiceInputRadioButton = findViewById(R.id.voiceInputRadioButton)
        manualInputRadioButton = findViewById(R.id.manualInputRadioButton)

        // Set player names button click handler
        setPlayerNamesButton.setOnClickListener {
            val numPlayersStr = numPlayersEditText.text.toString()
            if (numPlayersStr.isEmpty()) {
                Toast.makeText(this, "Please enter number of players", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val numPlayers = numPlayersStr.toInt()
            showPlayerNamesDialog(numPlayers)
        }

        // Set pars button click handler
        setParsButton.setOnClickListener {
            val numHolesStr = numHolesEditText.text.toString()
            if (numHolesStr.isEmpty()) {
                Toast.makeText(this, "Please enter number of holes", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val numHoles = numHolesStr.toInt()
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
        val numHolesStr = numHolesEditText.text.toString()
        val numPlayersStr = numPlayersEditText.text.toString()
        
        if (numHolesStr.isEmpty() || numPlayersStr.isEmpty()) {
            Toast.makeText(this, "Please enter number of holes and players", Toast.LENGTH_SHORT).show()
            return false
        }
        
        val numHoles = numHolesStr.toInt()
        val numPlayers = numPlayersStr.toInt()
        
        if (numHoles <= 0) {
            Toast.makeText(this, "Number of holes must be greater than 0", Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (numPlayers <= 0) {
            Toast.makeText(this, "Number of players must be greater than 0", Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (!::playerNames.isInitialized || playerNames.size != numPlayers) {
            Toast.makeText(this, "Please set player names", Toast.LENGTH_SHORT).show()
            return false
        }
        
        return true
    }
    
    /**
     * Starts the scorecard activity with all configured settings
     */
    private fun startGame() {
        val numHoles = numHolesEditText.text.toString().toInt()
        val numPlayers = numPlayersEditText.text.toString().toInt()
        val useVoiceInput = voiceInputRadioButton.isChecked
        
        val intent = Intent(this, ScorecardActivity::class.java).apply {
            putExtra("NUM_HOLES", numHoles)
            putExtra("PLAYER_NAMES", playerNames)
            
            // Ensure parValues is initialized before passing (it might not be if 'Set Pars' wasn't clicked)
            // Pass a default array or handle null in ScorecardActivity if pars weren't set.
            val parsToPass = if (pars != null) pars else IntArray(numHoles) { 3 } // Default par 3 if not set
            putExtra("PAR_VALUES", parsToPass)
            
            val selectedScoringMethod = when {
                voiceInputRadioButton.isChecked -> "VOICE"
                manualInputRadioButton.isChecked -> "MANUAL"
                else -> "MANUAL" // Default to manual if somehow none is checked
            }
            putExtra("SCORING_METHOD", selectedScoringMethod)
        }
        
        startActivity(intent)
    }

    /**
     * Dialog to input player names with improved UI
     */
    private fun showPlayerNamesDialog(numPlayers: Int) {
        val scrollView = ScrollView(this)
        val containerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
        }
        
        scrollView.addView(containerLayout)
        
        val title = TextView(this).apply {
            text = "Set Player Names"
            textSize = 18f
            setTextColor(ContextCompat.getColor(context, R.color.textPrimary))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dpToPx(16))
        }
        
        containerLayout.addView(title)
        
        // Change from val to var to allow adding elements later
        var inputLayouts = Array(numPlayers) { index ->
            val layout = TextInputLayout(this, null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox).apply {
                hint = "Player ${index + 1}"
            }
            
            val editText = TextInputEditText(this).apply {
                setSingleLine(true)
                setText(if (::playerNames.isInitialized && index < playerNames.size) playerNames[index] else "")
            }
            
            layout.addView(editText)
            containerLayout.addView(layout)
            
            // Add space between items
            val space = Space(this)
            space.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(8))
            containerLayout.addView(space)
            
            layout to editText
        }
        
        val addPlayerButton = MaterialButton(this, null, com.google.android.material.R.style.Widget_MaterialComponents_Button_TextButton).apply {
            text = getString(R.string.add_player)
            icon = ContextCompat.getDrawable(context, R.drawable.ic_add)
            iconGravity = MaterialButton.ICON_GRAVITY_START
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
        }
        
        containerLayout.addView(addPlayerButton)
        
        // Button to add another player
        addPlayerButton.setOnClickListener {
            val newIndex = inputLayouts.size
            
            val layout = TextInputLayout(this, null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox).apply {
                hint = "Player ${newIndex + 1}"
            }
            
            val editText = TextInputEditText(this).apply {
                setSingleLine(true)
            }
            
            layout.addView(editText)
            
            // Insert the new layout before the button
            containerLayout.removeView(addPlayerButton)
            containerLayout.addView(layout)
            
            // Add space
            val space = Space(this)
            space.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(8))
            containerLayout.addView(space)
            
            // Add the button back
            containerLayout.addView(addPlayerButton)
            
            inputLayouts += layout to editText
        }
        
        val dialog = AlertDialog.Builder(this)
            .setView(scrollView)
            .setPositiveButton("OK") { _, _ ->
                // Explicitly use 'this' to refer to the class member
                this.playerNames = inputLayouts.map { pair -> 
                    val (layout, editText) = pair
                    editText.text.toString().ifEmpty { "Player ${inputLayouts.indexOf(pair) + 1}" } 
                }.toTypedArray()
                
                // Update the player count to match the number of names
                numPlayersEditText.setText(playerNames.size.toString())
            }
            .setNegativeButton("Cancel", null)
            .create()
            
        dialog.show()
    }

    /**
     * Dialog to input par values with improved UI
     */
    private fun showParsDialog(numHoles: Int) {
        val dialog = AlertDialog.Builder(this)
        val scrollView = ScrollView(this)
        val containerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
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
                setContentPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, dpToPx(8))
                }
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
