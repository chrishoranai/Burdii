package com.app.burdii.ui.league

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.burdii.data.models.firebase.LeagueScore
import com.app.burdii.databinding.ItemScoreBinding

class LeagueScoreAdapter(private val onScoreClick: (LeagueScore) -> Unit) : ListAdapter<LeagueScore, LeagueScoreAdapter.ScoreViewHolder>(LeagueScoreDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScoreViewHolder {
        val binding = ItemScoreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScoreViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScoreViewHolder, position: Int) {
        val score = getItem(position)
        holder.bind(score, onScoreClick)
    }

    class ScoreViewHolder(private val binding: ItemScoreBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(score: LeagueScore, onScoreClick: (LeagueScore) -> Unit) {
            binding.scorePlayerNameTextView.text = score.playerName
            binding.scoreWeekTextView.text = "Week: ${score.weekNumber}"
            binding.scoreValueTextView.text = score.scoreValue.toString()
            // Set background color or text style based on status (e.g., pending)
            // if (score.status == "pending") {
            //     binding.root.setBackgroundColor(...) // Example
            // }
            itemView.setOnClickListener { onScoreClick(score) }
        }
    }

    private class LeagueScoreDiffCallback : DiffUtil.ItemCallback<LeagueScore>() {
        override fun areItemsTheSame(oldItem: LeagueScore, newItem: LeagueScore): Boolean {
            return oldItem.scoreId == newItem.scoreId
        }

        override fun areContentsTheSame(oldItem: LeagueScore, newItem: LeagueScore): Boolean {
            // Simple equality check for data classes
            return oldItem == newItem
        }
    }
}
