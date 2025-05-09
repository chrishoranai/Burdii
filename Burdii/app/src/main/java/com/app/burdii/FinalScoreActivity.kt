package com.app.burdii

import android.os.Bundle
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Displays the final scores for a completed round.
 */
class FinalScoreActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_final_score)

        // Placeholder: in future, real data passed via intent
        val playerNames = intent.getStringArrayExtra("PLAYER_NAMES") ?: arrayOf("Player 1")
        val scores = intent.getIntArrayExtra("TOTAL_SCORES") ?: IntArray(playerNames.size) { 0 }
        val par = intent.getIntExtra("PAR", 3)

        val table: TableLayout = findViewById(R.id.finalScoreTable)

        playerNames.forEachIndexed { idx, player ->
            val tr = TableRow(this)
            val nameTv = TextView(this).apply { text = player }
            val scoreTv = TextView(this).apply {
                val diff = scores[idx] - par * 18 // assume 18 holes
                text = scores[idx].toString()
                @ColorRes val c = scoreColor(diff, par)
                setTextColor(ContextCompat.getColor(this@FinalScoreActivity, c))
            }
            tr.addView(nameTv)
            tr.addView(scoreTv)
            table.addView(tr)
        }
    }

    @ColorRes
    private fun scoreColor(diff: Int, par: Int): Int = when {
        diff == -1 -> R.color.light_blue
        diff <= -2 -> R.color.dark_blue
        diff == 1 -> R.color.light_red
        diff >= 2 -> R.color.dark_red
        diff == 0 -> android.R.color.white
        par == 1 -> R.color.gold
        else -> android.R.color.white
    }
}
