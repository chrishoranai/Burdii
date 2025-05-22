package com.app.burdii.ui.league

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.burdii.data.models.firebase.LeagueScore
import com.app.burdii.databinding.ItemPendingScoreBinding

class PendingScoreAdapter(
    private val onApproveClick: (LeagueScore) -> Unit,
    private val onDenyClick: (LeagueScore) -> Unit
) : ListAdapter<LeagueScore, PendingScoreAdapter.PendingScoreViewHolder>(PendingScoreDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PendingScoreViewHolder {
        val binding = ItemPendingScoreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PendingScoreViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PendingScoreViewHolder, position: Int) {
        val score = getItem(position)
        holder.bind(score, onApproveClick, onDenyClick)
    }

    class PendingScoreViewHolder(private val binding: ItemPendingScoreBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(score: LeagueScore, onApproveClick: (LeagueScore) -> Unit, onDenyClick: (LeagueScore) -> Unit) {
            binding.pendingScorePlayerNameTextView.text = score.playerNameForManualEntry ?: score.playerName // Use manual name if available
            binding.pendingScoreWeekTextView.text = "Week: ${score.weekNumber}"
            binding.pendingScoreValueTextView.text = score.scoreValue.toString()

            binding.approveScoreButton.setOnClickListener { onApproveClick(score) }
            binding.denyScoreButton.setOnClickListener { onDenyClick(score) }
        }
    }

    private class PendingScoreDiffCallback : DiffUtil.ItemCallback<LeagueScore>() {
        override fun areItemsTheSame(oldItem: LeagueScore, newItem: LeagueScore): Boolean {
            return oldItem.scoreId == newItem.scoreId
        }

        override fun areContentsTheSame(oldItem: LeagueScore, newItem: LeagueScore): Boolean {
            // Simple equality check for data classes
            return oldItem == newItem
        }
    }
}
