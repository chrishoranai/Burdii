package com.app.burdii

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.TableRow
import android.view.Gravity
import android.graphics.Typeface
import android.util.TypedValue
import android.view.ViewGroup.LayoutParams


class SeasonLeagueTrackerFragment : Fragment() {

    private lateinit var seasonTitleTextView: TextView
    private lateinit var weekSpinner: Spinner
    private lateinit var scorecardContainer: LinearLayout // Using LinearLayout as a placeholder container
    private lateinit var startNewRoundButton: Button
    private lateinit var finishSeasonButton: Button

    private lateinit var roundDao: RoundDao
    private var savedRounds: List<Round> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_season_league_tracker, container, false)

        seasonTitleTextView = view.findViewById(R.id.seasonTitleTextView)
        weekSpinner = view.findViewById(R.id.weekSpinner)
        scorecardContainer = view.findViewById(R.id.scorecardContainer) // Make sure you have this ID in your layout
        startNewRoundButton = view.findViewById(R.id.startNewLeagueRoundButton)
        finishSeasonButton = view.findViewById(R.id.finishSeasonButton) // Make sure you have this ID in your layout

        // Get database instance and DAO
        roundDao = AppDatabase.getDatabase(requireContext()).roundDao()

        val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        weekSpinner.adapter = adapter

        weekSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedWeek = parent.getItemAtPosition(position).toString()
                if (selectedWeek != "Select a Week") {
                    displayRoundScorecard(selectedWeek)
                } else {
                    scorecardContainer.removeAllViews() // Clear previous scorecard
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

        startNewRoundButton.setOnClickListener {
            // Navigate back to the League Options Fragment
            findNavController().navigate(R.id.action_seasonLeagueTrackerFragment_to_leagueOptionsFragment)
        }

        finishSeasonButton.setOnClickListener {
            // Show a Toast message for now
            Toast.makeText(requireContext(), "Finish Season clicked", Toast.LENGTH_SHORT).show()
            // TODO: Implement finish season logic (calculate standings, display results, show dialog)
        }

        return view
    }
}