package com.app.burdii

import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Displays the final scores for a completed round.
 */
class FinalScoreActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_final_score)

        val roundName = intent.getStringExtra("ROUND_NAME") ?: "Round"
        val playerNames = intent.getStringArrayExtra("PLAYER_NAMES") ?: arrayOf("Player 1")
        val scoresArray = intent.getSerializableExtra("SCORES") as? Array<IntArray> ?: Array(playerNames.size) { IntArray(18) }
        val parValues = intent.getIntArrayExtra("PAR_VALUES") ?: IntArray(18) { 3 }
        val numHoles = intent.getIntExtra("NUM_HOLES", 18)

        // Set round name
        findViewById<TextView>(R.id.roundNameTextView).text = roundName

        val table: TableLayout = findViewById(R.id.finalScoreTable)
        table.removeAllViews()

        // Add header row
        val headerRow = TableRow(this).apply {
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
        }
        
        val playerHeader = createTextView("Player", true)
        headerRow.addView(playerHeader)
        
        for (hole in 1..numHoles) {
            val holeHeader = createTextView(hole.toString(), true)
            headerRow.addView(holeHeader)
        }
        
        val totalHeader = createTextView("Total", true)
        headerRow.addView(totalHeader)
        
        table.addView(headerRow)

        // Add par row
        val parRow = TableRow(this).apply {
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            setBackgroundColor(ContextCompat.getColor(this@FinalScoreActivity, R.color.burdii_background_off_white))
        }
        
        val parLabel = createTextView("Par", true)
        parRow.addView(parLabel)
        
        var totalPar = 0
        for (i in 0 until numHoles) {
            val parValue = parValues[i]
            totalPar += parValue
            val parCell = createTextView(parValue.toString(), false)
            parRow.addView(parCell)
        }
        
        val totalParCell = createTextView(totalPar.toString(), true)
        parRow.addView(totalParCell)
        
        table.addView(parRow)

        // Add player rows
        playerNames.forEachIndexed { playerIndex, playerName ->
            val playerRow = TableRow(this).apply {
                setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            }
            
            val nameCell = createTextView(playerName, true)
            playerRow.addView(nameCell)
            
            var totalScore = 0
            for (holeIndex in 0 until numHoles) {
                val score = scoresArray[playerIndex][holeIndex]
                totalScore += score
                val scoreCell = createTextView(if (score > 0) score.toString() else "-", false)
                
                // Color code based on par
                if (score > 0) {
                    val diff = score - parValues[holeIndex]
                    when {
                        diff <= -2 -> scoreCell.setTextColor(ContextCompat.getColor(this, R.color.dark_blue))
                        diff == -1 -> scoreCell.setTextColor(ContextCompat.getColor(this, R.color.light_blue))
                        diff == 0 -> scoreCell.setTextColor(ContextCompat.getColor(this, R.color.textPrimary))
                        diff == 1 -> scoreCell.setTextColor(ContextCompat.getColor(this, R.color.light_red))
                        diff >= 2 -> scoreCell.setTextColor(ContextCompat.getColor(this, R.color.dark_red))
                    }
                }
                
                playerRow.addView(scoreCell)
            }
            
            val totalCell = createTextView(totalScore.toString(), true)
            val totalDiff = totalScore - totalPar
            when {
                totalDiff <= -2 -> totalCell.setTextColor(ContextCompat.getColor(this, R.color.dark_blue))
                totalDiff == -1 -> totalCell.setTextColor(ContextCompat.getColor(this, R.color.light_blue))
                totalDiff == 0 -> totalCell.setTextColor(ContextCompat.getColor(this, R.color.textPrimary))
                totalDiff == 1 -> totalCell.setTextColor(ContextCompat.getColor(this, R.color.light_red))
                totalDiff >= 2 -> totalCell.setTextColor(ContextCompat.getColor(this, R.color.dark_red))
            }
            playerRow.addView(totalCell)
            
            table.addView(playerRow)
        }

        // Setup back button
        findViewById<Button>(R.id.backButton).setOnClickListener {
            finish()
        }
    }
    
    private fun createTextView(text: String, isBold: Boolean): TextView {
        return TextView(this).apply {
            this.text = text
            setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))
            gravity = Gravity.CENTER
            if (isBold) {
                setTypeface(null, Typeface.BOLD)
            }
        }
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
