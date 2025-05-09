package com.app.burdii

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.burdii.Round

/**
 * Adapter for the recent rounds list.
 * @param rounds list of rounds to display
 * @param onRoundClicked callback invoked when a round is selected
 */
class RoundAdapter(
    private val rounds: List<Round>,
    private val onRoundClicked: (Round) -> Unit = {}
) : RecyclerView.Adapter<RoundAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.nameTextView)
        val dateTextView: TextView = view.findViewById(R.id.dateTextView)
        val scoreTextView: TextView = view.findViewById(R.id.scoreTextView)
        val holesTextView: TextView = view.findViewById(R.id.holesTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_round, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val round = rounds[position]
        holder.nameTextView.text = round.name
        holder.dateTextView.text = round.date
        holder.scoreTextView.text = round.scoreChange
        holder.holesTextView.text = round.holesPlayed

        holder.itemView.setOnClickListener { onRoundClicked(round) }
    }

    override fun getItemCount(): Int = rounds.size
}
