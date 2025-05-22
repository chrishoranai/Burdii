package com.app.burdii.ui.league

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.app.burdii.databinding.FragmentCreateLeagueBinding

class CreateLeagueFragment : Fragment() {

    private var _binding: FragmentCreateLeagueBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CreateLeagueViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateLeagueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.createLeagueButton.setOnClickListener {
            val leagueName = binding.leagueNameEditText.text.toString()
            val numberOfWeeks = binding.numberOfWeeksEditText.text.toString()
            viewModel.createLeague(leagueName, numberOfWeeks)
        }

        viewModel.creationStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                is CreationStatus.Idle -> {
                    // Initial state, do nothing or reset UI
                }
                is CreationStatus.Loading -> {
                    // Show a loading indicator
                    binding.createLeagueButton.isEnabled = false
                    binding.createLeagueButton.text = "Creating..."
                }
                is CreationStatus.Success -> {
                    // Hide loading, show success message and access code
                    binding.createLeagueButton.isEnabled = true
                    binding.createLeagueButton.text = "Create League"
                    val message = "League created! Access Code: ${status.accessCode}"
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    // Optionally navigate away or clear fields
                    binding.leagueNameEditText.text?.clear()
                    binding.numberOfWeeksEditText.text?.clear()
                }
                is CreationStatus.Error -> {
                    // Hide loading, show error message
                     binding.createLeagueButton.isEnabled = true
                    binding.createLeagueButton.text = "Create League"
                    Toast.makeText(requireContext(), "Error: ${status.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}