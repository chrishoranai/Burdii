package com.app.burdii

import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
class ScoreEntryByHoleFragment : Fragment() {

    private lateinit var scorecardTable: TableLayout
    private lateinit var nextButton: MaterialButton
    private lateinit var playerNames: List<String>
    private var numHoles: Int = 0
    private lateinit var scoreEditTexts: List<List<EditText>>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_score_entry_by_hole, container, false)

        scorecardTable = view.findViewById(R.id.scorecardTable)
        nextButton = view.findViewById(R.id.nextButton)

        arguments?.let {
            playerNames = it.getStringArrayList("playerNames") ?: emptyList()
            numHoles = it.getInt("numHoles", 0)
        }

        scoreEditTexts = List(playerNames.size) { List(numHoles) { EditText(requireContext()) } }
        
        setupScorecardTable()

        nextButton.setOnClickListener {
            collectScoresAndNavigate()
        }

        return view
    }

    private fun setupScorecardTable() {
        scorecardTable.removeAllViews()

        // Create header row with hole numbers
        val headerRow = TableRow(requireContext())
        headerRow.addView(createTextView("Player/Hole", true)) // Corner cell

        for (hole in 1..numHoles) {
            headerRow.addView(createTextView(hole.toString(), true))
        }

        headerRow.addView(createTextView("Total", true)) // Total column header
        scorecardTable.addView(headerRow)

        // Player rows with score inputs
        for (playerIndex in playerNames.indices) {
            val playerRow = TableRow(requireContext())
            playerRow.addView(createTextView(playerNames[playerIndex])) // Player name cell

            for (holeIndex in 0 until numHoles) {
                val scoreCell = createEditText()
                scoreEditTexts[playerIndex][holeIndex] = scoreCell
                scoreCell.setOnFocusChangeListener { _, hasFocus ->
 if (!hasFocus) { // Validate when focus is lost
 validateScoreInput(scoreCell)
 }
                }
                playerRow.addView(scoreCell)
            }

            // Total score cell (will update dynamically later)
            val totalCell = createTextView("0")
            playerRow.addView(totalCell)
            
            scorecardTable.addView(playerRow)
        }        
    }

    private fun createTextView(text: String, isHeader: Boolean = false): TextView {
        val textView = TextView(requireContext())
        textView.text = text
        textView.setPadding(16, 16, 16, 16)
        textView.gravity = Gravity.CENTER
        if (isHeader) {
            textView.setTypeface(null, android.graphics.Typeface.BOLD)
        }
        return textView
    }

    private fun createEditText(): EditText { // Use EditText for input
        val editText = EditText(requireContext())
        editText.setPadding(16, 8, 16, 8)
        editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        editText.gravity = Gravity.CENTER
        editText.minWidth = 100 // Adjust as needed
        return editText
    }

    private fun validateScoreInput(editText: EditText) {
        val input = editText.text.toString().trim()
        if (input.isNotEmpty() && input.toIntOrNull() == null) {
            editText.error = "Enter a number"
            Toast.makeText(requireContext(), "Please enter a valid number for the score.", Toast.LENGTH_SHORT).show()
        } else {
            editText.error = null // Clear error if input is valid
        }
    }

    private fun collectScoresAndNavigate() {
        val scores = Array(playerNames.size) { IntArray(numHoles) { 0 } }
        var allScoresValid = true

        for (playerIndex in playerNames.indices) {
            for (holeIndex in 0 until numHoles) {
                val scoreText = scoreEditTexts[playerIndex][holeIndex].text.toString().trim()
                val score = scoreText.toIntOrNull()

                if (score != null) {
                    scores[playerIndex][holeIndex] = score
                } else {
                    allScoresValid = false
                    scoreEditTexts[playerIndex][holeIndex].error = "Invalid score"
                }
            }
        }

        if (allScoresValid) {
            val action = ScoreEntryByHoleFragmentDirections.actionScoreEntryByHoleFragmentToLeagueResultsFragment(
                playerNames.toTypedArray(), scores.map { it.toIntArray() }.toTypedArray(), null // Pass scores as a 2D array of Int
            )
 findNavController().navigate(action)
        }
    }
}