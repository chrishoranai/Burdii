package com.app.burdii

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.app.burdii.Round

class RoundAdapter(private val rounds: List<Round>) : RecyclerView.Adapter<RoundAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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
        holder.dateTextView.text = round.date
        holder.scoreTextView.text = round.scoreChange
        holder.holesTextView.text = round.holesPlayed
    }

    override fun getItemCount(): Int = rounds.size
}
