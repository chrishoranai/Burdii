package com.app.burdii.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.burdii.data.models.firebase.LeagueScore
import com.app.burdii.databinding.ItemLeagueScoreBinding
import java.text.SimpleDateFormat
import java.util.Locale

class LeagueScoreAdapter(private val onItemClick: (LeagueScore) -> Unit) :
    ListAdapter<LeagueScore, LeagueScoreAdapter.LeagueScoreViewHolder>(LeagueScoreDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeagueScoreViewHolder {
        val binding = ItemLeagueScoreBinding.inflate(
            LayoutInflater.from(parent.context),
            parent, false
        )
        return LeagueScoreViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: LeagueScoreViewHolder, position: Int) {
        val score = getItem(position)
        holder.bind(score)
    }

    class LeagueScoreViewHolder(private val binding: ItemLeagueScoreBinding, private val onItemClick: (LeagueScore) -> Unit) :
        RecyclerView.ViewHolder(binding.root) {

        private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        fun bind(score: LeagueScore) {
            binding.scorePlayerNameTextView.text = score.playerNameForManualEntry ?: score.playerName // Prefer manual name if available
            
            val dateString = score.submittedAt?.let { dateFormatter.format(it) } ?: "Unknown Date"
            
            binding.scoreDetailsTextView.text = "Week ${score.weekNumber}: ${score.scoreValue} (${score.status.capitalize(Locale.getDefault())}) on $dateString"

            itemView.setOnClickListener { onItemClick(score) }
        }
    }
}

class LeagueScoreDiffCallback : DiffUtil.ItemCallback<LeagueScore>() {
    override fun areItemsTheSame(oldItem: LeagueScore, newItem: LeagueScore): Boolean {
        return oldItem.scoreId == newItem.scoreId
    }

    override fun areContentsTheSame(oldItem: LeagueScore, newItem: LeagueScore): Boolean {
        // Compare relevant fields to determine if content has changed
        return oldItem.leagueId == newItem.leagueId &&
               oldItem.playerId == newItem.playerId &&
               oldItem.playerName == newItem.playerName &&
               oldItem.playerNameForManualEntry == newItem.playerNameForManualEntry &&
               oldItem.weekNumber == newItem.weekNumber &&
               oldItem.scoreValue == newItem.scoreValue &&
               oldItem.status == newItem.status &&
               oldItem.submittedAt == newItem.submittedAt &&
               oldItem.reviewedBy == newItem.reviewedBy &&
               oldItem.reviewedAt == newItem.reviewedAt
    }
}