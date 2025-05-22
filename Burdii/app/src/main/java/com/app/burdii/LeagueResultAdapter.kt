package com.app.burdii

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.burdii.R // Assuming your R file is in com.app.burdii

class LeagueResultAdapter(private val results: List<Pair<String, Int>>) :
    RecyclerView.Adapter<LeagueResultAdapter.LeagueResultViewHolder>() {

    class LeagueResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val playerNameTextView: TextView = itemView.findViewById(R.id.playerNameTextView)
        val scoreTextView: TextView = itemView.findViewById(R.id.scoreTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeagueResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_league_result, parent, false)
        return LeagueResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: LeagueResultViewHolder, position: Int) {
        val (playerName, totalScore) = results[position]
        holder.playerNameTextView.text = playerName
        holder.scoreTextView.text = totalScore.toString()
    }

    override fun getItemCount(): Int {
        return results.size
    }
}