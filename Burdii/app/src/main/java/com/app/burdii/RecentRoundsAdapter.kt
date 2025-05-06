package com.app.burdii

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Define the Round data class
data class Round(val id: Int, val date: String, val players: List<String>)

// RecentRoundsAdapter implementation
class RecentRoundsAdapter(private val rounds: List<Round>) : RecyclerView.Adapter<RecentRoundsAdapter.ViewHolder>() {

    // ViewHolder implementation
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateTextView: TextView = view.findViewById(R.id.dateTextView)
        val playersTextView: TextView = view.findViewById(R.id.playersTextView)
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recent_round, parent, false)
        return ViewHolder(view)
    }

    // Bind the data to the views (invoked by the layout manager)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val round = rounds[position]
        holder.dateTextView.text = round.date
        holder.playersTextView.text = "Players: ${round.players.size}"
    }

    // Return the size of the dataset (invoked by the layout manager)
    override fun getItemCount(): Int = rounds.size
}
