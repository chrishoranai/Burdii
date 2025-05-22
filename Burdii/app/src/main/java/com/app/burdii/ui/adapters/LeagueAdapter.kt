package com.app.burdii.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.app.burdii.data.models.firebase.League
import com.app.burdii.databinding.ItemLeagueResultBinding // Assuming this layout is used for league items

class LeagueAdapter(private val onItemClick: (League) -> Unit) :
    ListAdapter<League, LeagueAdapter.LeagueViewHolder>(LeagueDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeagueViewHolder {
        val binding = ItemLeagueResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent, false
        )
        return LeagueViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: LeagueViewHolder, position: Int) {
        val league = getItem(position)
        holder.bind(league)
    }

    class LeagueViewHolder(private val binding: ItemLeagueResultBinding, private val onItemClick: (League) -> Unit) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(league: League) {
            // TODO: Bind League data to views in item_league_result.xml
            // Example (assuming item_league_result has TextViews with these IDs):
            // binding.leagueNameTextView.text = league.name
            // binding.hostNameTextView.text = "Host: ${league.hostName}"
            // binding.weeksTextView.text = "Weeks: ${league.numberOfWeeks}"

            itemView.setOnClickListener { onItemClick(league) }
        }
    }
}

class LeagueDiffCallback : DiffUtil.ItemCallback<League>() {
    override fun areItemsTheSame(oldItem: League, newItem: League): Boolean {
        return oldItem.leagueId == newItem.leagueId
    }

    override fun areContentsTheSame(oldItem: League, newItem: League): Boolean {
        // TODO: Compare relevant fields to determine if content has changed
        return oldItem == newItem
    }
}