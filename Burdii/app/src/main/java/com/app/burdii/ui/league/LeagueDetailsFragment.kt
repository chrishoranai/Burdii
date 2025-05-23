package com.app.burdii.ui.league

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.burdii.R
import com.app.burdii.databinding.FragmentLeagueDetailsBinding
import com.app.burdii.ui.adapters.MemberAdapter
import com.app.burdii.ui.adapters.LeagueScoreAdapter

class LeagueDetailsFragment : Fragment() {

    private var _binding: FragmentLeagueDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LeagueDetailsViewModel by viewModels()
    private val args: LeagueDetailsFragmentArgs by navArgs()

    private lateinit var memberAdapter: MemberAdapter
    private lateinit var leagueScoreAdapter: LeagueScoreAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLeagueDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup RecyclerViews
        memberAdapter = MemberAdapter { member ->
            // Handle member click if needed
        }
        binding.membersRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.membersRecyclerView.adapter = memberAdapter

        leagueScoreAdapter = LeagueScoreAdapter { score ->
            // Handle score click if needed
        }
        binding.scoresRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.scoresRecyclerView.adapter = leagueScoreAdapter
        
        // Setup button listeners
        binding.submitScoreButton.setOnClickListener {
            findNavController().navigate(
                R.id.action_leagueDetailsFragment_to_submitScoreFragment,
                bundleOf("leagueId" to args.leagueId)
            )
        }
        
        binding.manageScoresButton.setOnClickListener {
            findNavController().navigate(
                R.id.action_leagueDetailsFragment_to_manageScoresFragment,
                bundleOf("leagueId" to args.leagueId)
            )
        }
        
        // Load league details
        viewModel.loadLeagueDetails(args.leagueId)

        viewModel.league.observe(viewLifecycleOwner) {
            it?.let {
                binding.leagueNameTextView.text = it.name
                binding.hostNameTextView.text = "Host: ${it.hostName}"
                binding.weeksTextView.text = "Weeks: ${it.numberOfWeeks}"
                binding.accessCodeTextView.text = "Access Code: ${it.accessCode}"

                // Update members list
                val membersList = it.members.map { memberUid -> 
                    it.memberNames[memberUid] ?: "Unknown User"
                }
                memberAdapter.submitList(membersList)
                
                // Show/hide manage button based on if user is host
                viewModel.isCurrentUserHost.observe(viewLifecycleOwner) { isHost ->
                    binding.manageScoresButton.visibility = if (isHost) View.VISIBLE else View.GONE
                }
            }
        }

        viewModel.scores.observe(viewLifecycleOwner) {
            leagueScoreAdapter.submitList(it)
            binding.scoresEmptyView.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
            binding.scoresRecyclerView.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.loadingProgressBar.visibility = if (it) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) {
            it?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearErrorMessage()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}