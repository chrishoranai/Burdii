package com.app.burdii

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import android.widget.Toast
import androidx.navigation.fragment.findNavController

class LeagueOptionsFragment : Fragment() {

    private lateinit var gameFormatRadioGroup: RadioGroup
    private lateinit var scorekeepingMethodRadioGroup: RadioGroup
    private lateinit var nextButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_league_options, container, false)

        gameFormatRadioGroup = view.findViewById(R.id.gameFormatRadioGroup)
        scorekeepingMethodRadioGroup = view.findViewById(R.id.scorekeepingMethodRadioGroup)
        nextButton = view.findViewById(R.id.nextButton)

        nextButton.setOnClickListener {
            val selectedGameFormatId = gameFormatRadioGroup.checkedRadioButtonId
            val selectedScorekeepingMethodId = scorekeepingMethodRadioGroup.checkedRadioButtonId

            if (selectedGameFormatId == -1 || selectedScorekeepingMethodId == -1) {
 Toast.makeText(requireContext(), "Please select both a game format and a scorekeeping method.", Toast.LENGTH_SHORT).show()
 return@setOnClickListener
            }
            
            val gameFormat = view.findViewById<RadioButton>(selectedGameFormatId).text.toString()
            val scorekeepingMethod = view.findViewById<RadioButton>(selectedScorekeepingMethodId).text.toString()

            if (gameFormat == "No game format selected" || scorekeepingMethod == "No scorekeeping method selected") {
                // This case should ideally not be reached due to the check above, but as a safeguard
            }
            
            // Log.d("LeagueOptions", "Game Format: $gameFormat, Scorekeeping Method: $scorekeepingMethod") // Uncomment for debugging
            val action = LeagueOptionsFragmentDirections.actionLeagueOptionsFragmentToLeaguePlayersFragment(gameFormat, scorekeepingMethod)
            findNavController().navigate(action)

        }

        return view
    }
}