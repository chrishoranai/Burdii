package com.app.burdii.ui.league

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.burdii.databinding.FragmentMyLeaguesBinding
// import com.app.burdii.ui.adapters.LeagueAdapter // You'll need to create this adapter

class MyLeaguesFragment : Fragment() {

    private var _binding: FragmentMyLeaguesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MyLeaguesViewModel by viewModels()

    // private lateinit var hostedLeaguesAdapter: LeagueAdapter // Uncomment and use your adapter
    // private lateinit var joinedLeaguesAdapter: LeagueAdapter // Uncomment and use your adapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyLeaguesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup RecyclerViews (Uncomment and replace with your actual adapter)
//        hostedLeaguesAdapter = LeagueAdapter { league ->
//            // Handle click on a hosted league item (e.g., navigate to LeagueDetailsFragment)
//            // findNavController().navigate(R.id.action_myLeaguesFragment_to_leagueDetailsFragment, bundleOf("leagueId" to league.leagueId))
//        }
//        binding.hostedLeaguesRecyclerView.layoutManager = LinearLayoutManager(context)
//        binding.hostedLeaguesRecyclerView.adapter = hostedLeaguesAdapter
//
//        joinedLeaguesAdapter = LeagueAdapter { league ->
//            // Handle click on a joined league item (e.g., navigate to LeagueDetailsFragment)
//            // findNavController().navigate(R.id.action_myLeaguesFragment_to_leagueDetailsFragment, bundleOf("leagueId" to league.leagueId))
//        }
//        binding.joinedLeaguesRecyclerView.layoutManager = LinearLayoutManager(context)
//        binding.joinedLeaguesRecyclerView.adapter = joinedLeaguesAdapter

        viewModel.hostedLeagues.observe(viewLifecycleOwner) {
            // Update hosted leagues list in the adapter (Uncomment when adapter is ready)
            // hostedLeaguesAdapter.submitList(it)
        }

        viewModel.joinedLeagues.observe(viewLifecycleOwner) {
            // Update joined leagues list in the adapter (Uncomment when adapter is ready)
            // joinedLeaguesAdapter.submitList(it)
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) {
            // Show/hide loading indicator based on this value
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) {
            it?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel._errorMessage.value = null // Consume the error message
            }
        }

        // Initial data load is triggered in the ViewModel's init block
        // If you need to refresh, you can call viewModel.loadMyLeagues() here or on a pull-to-refresh action.
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}