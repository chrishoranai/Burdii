package com.app.burdii

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class SetupActivity : AppCompatActivity() {
    private lateinit var numHolesEditText: EditText
    private lateinit var numPlayersEditText: EditText
    private lateinit var setPlayerNamesButton: Button
    private lateinit var setParsButton: Button
    private lateinit var startGameButton: Button
    private var playerNames: Array<String>? = null
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
            val numHolesStr = numHolesEditText.text.toString()
            val numPlayersStr = numPlayersEditText.text.toString()
            if (numHolesStr.isEmpty() || numPlayersStr.isEmpty()) {
                Toast.makeText(this, "Please enter number of holes and players", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val numHoles = numHolesStr.toInt()
            val numPlayers = numPlayersStr.toInt()
            if (playerNames == null || playerNames!!.size != numPlayers) {
                Toast.makeText(this, "Please set player names", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, ScorecardActivity::class.java)
            intent.putExtra("numHoles", numHoles)
            intent.putExtra("numPlayers", numPlayers)
            intent.putExtra("playerNames", playerNames)
            if (pars != null) {
                intent.putExtra("pars", pars)
            }
            startActivity(intent)
        }
    }

    // Dialog to input player names
    private fun showPlayerNamesDialog(numPlayers: Int) {
        val dialog = AlertDialog.Builder(this)
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val editTexts = Array(numPlayers) { EditText(this) }
        for (i in 0 until numPlayers) {
            editTexts[i].hint = "Player ${i + 1} name"
            layout.addView(editTexts[i])
        }
        dialog.setView(layout)
        dialog.setPositiveButton("OK") { _, _ ->
            playerNames = editTexts.map { it.text.toString() }.toTypedArray()
        }
        dialog.setNegativeButton("Cancel") { _, _ -> }
        dialog.show()
    }

    // Dialog to input par values
    private fun showParsDialog(numHoles: Int) {
        val dialog = AlertDialog.Builder(this)
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val editTexts = Array(numHoles) { EditText(this) }
        for (i in 0 until numHoles) {
            editTexts[i].hint = "Par for hole ${i + 1}"
            editTexts[i].inputType = android.text.InputType.TYPE_CLASS_NUMBER
            layout.addView(editTexts[i])
        }
        dialog.setView(layout)
        dialog.setPositiveButton("OK") { _, _ ->
            pars = IntArray(numHoles)
            for (i in 0 until numHoles) {
                val parStr = editTexts[i].text.toString()
                pars!![i] = if (parStr.isNotEmpty()) parStr.toInt() else 0
            }
        }
        dialog.setNegativeButton("Cancel") { _, _ -> }
        dialog.show()
    }
}
