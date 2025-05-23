package com.app.burdii.ui.league

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.app.burdii.R
import kotlinx.coroutines.launch

class SubmitScoreFragment : Fragment() {
    
    private val viewModel: SubmitScoreViewModel by viewModels()
    private val args: SubmitScoreFragmentArgs by navArgs()
    
    private lateinit var leagueNameTextView: TextView
    private lateinit var weekSpinner: Spinner
    private lateinit var scoreEditText: EditText
    private lateinit var submitButton: Button
    private lateinit var cancelButton: Button
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_submit_score, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupObservers()
        setupClickListeners()
        
        // Load league details
        viewModel.loadLeagueDetails(args.leagueId)
    }
    
    private fun initViews(view: View) {
        leagueNameTextView = view.findViewById(R.id.leagueNameTextView)
        weekSpinner = view.findViewById(R.id.weekSpinner)
        scoreEditText = view.findViewById(R.id.scoreEditText)
        submitButton = view.findViewById(R.id.submitButton)
        cancelButton = view.findViewById(R.id.cancelButton)
    }
    
    private fun setupObservers() {
        viewModel.league.observe(viewLifecycleOwner) { league ->
            if (league != null) {
                leagueNameTextView.text = league.name
                setupWeekSpinner(league.numberOfWeeks)
            }
        }
        
        viewModel.submitResult.observe(viewLifecycleOwner) { result ->
            if (result.isSuccess) {
                Toast.makeText(requireContext(), "Score submitted for review!", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } else {
                Toast.makeText(
                    requireContext(), 
                    "Failed to submit score: ${result.exceptionOrNull()?.message}", 
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            submitButton.isEnabled = !loading
            submitButton.text = if (loading) "Submitting..." else "Submit Score"
        }
    }
    
    private fun setupClickListeners() {
        submitButton.setOnClickListener {
            val scoreText = scoreEditText.text.toString().trim()
            val selectedWeek = weekSpinner.selectedItemPosition + 1
            
            if (scoreText.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a score", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            try {
                val score = scoreText.toInt()
                viewModel.submitScore(args.leagueId, selectedWeek, score)
            } catch (e: NumberFormatException) {
                Toast.makeText(requireContext(), "Please enter a valid number", Toast.LENGTH_SHORT).show()
            }
        }
        
        cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun setupWeekSpinner(numberOfWeeks: Int) {
        val weeks = (1..numberOfWeeks).map { "Week $it" }.toTypedArray()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, weeks)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        weekSpinner.adapter = adapter
    }
}