package com.app.burdii.ui.league

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.app.burdii.databinding.FragmentJoinLeagueBinding

class JoinLeagueFragment : Fragment() {

    private var _binding: FragmentJoinLeagueBinding? = null
    private val binding get() = _binding!!

    private val viewModel: JoinLeagueViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJoinLeagueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.joinLeagueButton.setOnClickListener {
            val accessCode = binding.accessCodeEditText.text.toString()
            viewModel.joinLeague(accessCode)
        }

        viewModel.joinStatus.observe(viewLifecycleOwner) {
            when (it) {
                is JoinStatus.Idle -> {
                    // Initial state
                }
                is JoinStatus.Loading -> {
                    binding.joinLeagueButton.isEnabled = false
                    binding.joinLeagueButton.text = "Joining..."
                }
                is JoinStatus.Success -> {
                    binding.joinLeagueButton.isEnabled = true
                    binding.joinLeagueButton.text = "Join League"
                    val leagueId = it.leagueId
                    if (leagueId != null) {
                        Toast.makeText(requireContext(), "Successfully joined league!", Toast.LENGTH_SHORT).show()
                        // TODO: Navigate to League Details Fragment, passing leagueId
                        // findNavController().navigate(R.id.action_joinLeagueFragment_to_leagueDetailsFragment, bundleOf("leagueId" to leagueId))
                    } else {
                         Toast.makeText(requireContext(), "Successfully joined league, but could not retrieve league ID.", Toast.LENGTH_SHORT).show()
                    }
                     binding.accessCodeEditText.text?.clear()
                }
                is JoinStatus.Error -> {
                    binding.joinLeagueButton.isEnabled = true
                    binding.joinLeagueButton.text = "Join League"
                    Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}