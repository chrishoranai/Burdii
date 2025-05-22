package com.app.burdii

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import android.widget.Toast
import androidx.navigation.fragment.findNavController

class LeaguePlayersFragment : Fragment() {

    private lateinit var playerNameEditText: EditText
    private lateinit var addPlayerButton: Button
    private lateinit var playersRecyclerView: RecyclerView
 private lateinit var nextButton: Button

    private val playerList = mutableListOf<String>()

    private var gameFormat: String? = null
 private var scorekeepingMethod: String = ""
    private var numHoles: Int = 0 // Assuming number of holes is passed from options
 private lateinit var adapter: PlayerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_league_players, container, false)

        playerNameEditText = view.findViewById(R.id.playerNameEditText)
        addPlayerButton = view.findViewById(R.id.addPlayerButton)
        playersRecyclerView = view.findViewById(R.id.playersRecyclerView)
 nextButton = view.findViewById(R.id.nextButton)

        arguments?.let {
            gameFormat = it.getString("gameFormat")
 scorekeepingMethod = it.getString("scorekeepingMethod") ?: "" // Provide a default or handle null
            numHoles = it.getInt("numHoles") // Assuming numHoles is passed as an Int
        }

        adapter = PlayerAdapter(playerList)
        playersRecyclerView.layoutManager = LinearLayoutManager(context)
        playersRecyclerView.adapter = adapter

        addPlayerButton.setOnClickListener {
            val playerName = playerNameEditText.text.toString().trim()
            if (playerName.isNotEmpty()) {
                playerList.add(playerName)
                adapter.notifyItemInserted(playerList.size - 1)
                playerNameEditText.text.clear()
            }
        }

 nextButton.setOnClickListener {
 if (playerList.isEmpty()) {
 Toast.makeText(requireContext(), "Please add at least one player or team.", Toast.LENGTH_SHORT).show()
 return@setOnClickListener
 val bundle = Bundle().apply {
 putStringArrayList("playerNames", ArrayList(playerList))
 }

 when (scorekeepingMethod) {
                "By Hole" -> {
                    bundle.putInt("numHoles", numHoles)
                    findNavController().navigate(R.id.action_leaguePlayersFragment_to_scoreEntryByHoleFragment, bundle)
                }
                "Final Score Only" -> {
                    findNavController().navigate(R.id.action_leaguePlayersFragment_to_scoreEntryFinalScoreFragment, bundle)
                }
 else -> {
                    // Handle other scorekeeping methods or show an error
                }
            }
        }

        
        // Setup ItemTouchHelper for reorder and remove
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, // Enable drag up and down
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT // Enable swipe left and right
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                // Swap items in the list
                val movedPlayer = playerList.removeAt(fromPosition)
                playerList.add(toPosition, movedPlayer)
                // Notify the adapter of the move
                adapter.notifyItemMoved(fromPosition, toPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                // Remove item from the list
                playerList.removeAt(position)
                // Notify the adapter of the removal
                adapter.notifyItemRemoved(position)
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    viewHolder?.itemView?.alpha = 0.5f // Optional: Add visual feedback for dragging
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                viewHolder.itemView.alpha = 1.0f // Reset alpha
            }
        })
        itemTouchHelper.attachToRecyclerView(playersRecyclerView)


        return view
    }

    // Basic Adapter for displaying player names
    class PlayerAdapter(private val players: MutableList<String>) :
        RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder>() {

        class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val playerNameTextView: TextView = itemView.findViewById(R.id.playerNameTextView)
            val removeButton: ImageButton = itemView.findViewById(R.id.removePlayerButton) // Assuming an ImageButton for removal
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_player, parent, false) // Assuming item_player.xml layout
            return PlayerViewHolder(view)
        }

        override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
            holder.playerNameTextView.text = players[position]
            holder.removeButton.setOnClickListener {
                players.removeAt(position)
                notifyItemRemoved(position)
            }
        }

        override fun getItemCount(): Int = players.size
    }
}