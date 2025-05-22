package com.app.burdii.ui.league

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.burdii.data.models.firebase.League
import com.app.burdii.databinding.ItemLeagueBinding

class LeagueAdapter(private val onLeagueClick: (League) -> Unit) : ListAdapter<League, LeagueAdapter.LeagueViewHolder>(LeagueDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeagueViewHolder {
        val binding = ItemLeagueBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LeagueViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LeagueViewHolder, position: Int) {
        val league = getItem(position)
        holder.bind(league, onLeagueClick)
    }

    class LeagueViewHolder(private val binding: ItemLeagueBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(league: League, onLeagueClick: (League) -> Unit) {
            binding.leagueNameTextView.text = league.name
            binding.leagueHostTextView.text = "Hosted by: ${league.hostName}"
            binding.leagueWeeksTextView.text = "Weeks: ${league.numberOfWeeks}"
            itemView.setOnClickListener { onLeagueClick(league) }
        }
    }

    private class LeagueDiffCallback : DiffUtil.ItemCallback<League>() {
        override fun areItemsTheSame(oldItem: League, newItem: League): Boolean {
            return oldItem.leagueId == newItem.leagueId
        }

        override fun areContentsTheSame(oldItem: League, newItem: League): Boolean {
            return oldItem == newItem
        }
    }
}
