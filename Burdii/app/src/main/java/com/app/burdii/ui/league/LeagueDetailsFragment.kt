package com.app.burdii.ui.league

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.burdii.databinding.FragmentLeagueDetailsBinding

// You'll likely need adapters for members and scores:
// import com.app.burdii.ui.adapters.MemberAdapter
// import com.app.burdii.ui.adapters.LeagueScoreAdapter

class LeagueDetailsFragment : Fragment() {

    private var _binding: FragmentLeagueDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LeagueDetailsViewModel by viewModels()
    private val args: LeagueDetailsFragmentArgs by navArgs()

    // private lateinit var memberAdapter: MemberAdapter // Uncomment and use your adapter
    // private lateinit var leagueScoreAdapter: LeagueScoreAdapter // Uncomment and use your adapter

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

        // Setup RecyclerViews (Uncomment and replace with your actual adapters)
//        memberAdapter = MemberAdapter { member ->
//            // Handle member click if needed
//        }
//        binding.membersRecyclerView.layoutManager = LinearLayoutManager(context)
//        binding.membersRecyclerView.adapter = memberAdapter
//
//        leagueScoreAdapter = LeagueScoreAdapter { score ->
//            // Handle score click if needed (e.g., for editing if user is host)
//        }
//        binding.scoresRecyclerView.layoutManager = LinearLayoutManager(context)
//        binding.scoresRecyclerView.adapter = leagueScoreAdapter

        viewModel.league.observe(viewLifecycleOwner) {
            it?.let {
                binding.leagueNameTextView.text = it.name
                binding.hostNameTextView.text = "Host: ${it.hostName}"
                binding.weeksTextView.text = "Weeks: ${it.numberOfWeeks}"
                binding.accessCodeTextView.text = "Access Code: ${it.accessCode}"

                // Update members list (Uncomment when adapter is ready)
                // memberAdapter.submitList(it.members.map { memberUid -> it.memberNames[memberUid] ?: memberUid }) // Using memberNames map
            }
        }

        viewModel.scores.observe(viewLifecycleOwner) {
            // Update scores list (Uncomment when adapter is ready)
            // leagueScoreAdapter.submitList(it)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.loadingProgressBar.visibility = if (it) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) {
            it?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel._errorMessage.value = null // Consume the error message
            }
        }

        // You'll need to implement logic here to show/hide UI elements based on
        // whether the current user is the host of the league (e.g., manage scores button).
        // You can potentially get the current user ID from the repository and compare it to league.hostId
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}