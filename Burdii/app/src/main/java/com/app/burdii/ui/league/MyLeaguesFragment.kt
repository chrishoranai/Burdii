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
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.burdii.R
import com.app.burdii.databinding.FragmentMyLeaguesBinding
import com.app.burdii.ui.adapters.LeagueAdapter

class MyLeaguesFragment : Fragment() {

    private var _binding: FragmentMyLeaguesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MyLeaguesViewModel by viewModels()

    private lateinit var hostedLeaguesAdapter: LeagueAdapter
    private lateinit var joinedLeaguesAdapter: LeagueAdapter

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

        // Setup RecyclerViews
        hostedLeaguesAdapter = LeagueAdapter { league ->
            // Handle click on a hosted league item
            findNavController().navigate(
                R.id.action_myLeaguesFragment_to_leagueDetailsFragment, 
                bundleOf("leagueId" to league.leagueId)
            )
        }
        binding.hostedLeaguesRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.hostedLeaguesRecyclerView.adapter = hostedLeaguesAdapter

        joinedLeaguesAdapter = LeagueAdapter { league ->
            // Handle click on a joined league item
            findNavController().navigate(
                R.id.action_myLeaguesFragment_to_leagueDetailsFragment, 
                bundleOf("leagueId" to league.leagueId)
            )
        }
        binding.joinedLeaguesRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.joinedLeaguesRecyclerView.adapter = joinedLeaguesAdapter

        viewModel.hostedLeagues.observe(viewLifecycleOwner) {
            hostedLeaguesAdapter.submitList(it)
            binding.hostedLeaguesEmptyView.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
            binding.hostedLeaguesRecyclerView.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.joinedLeagues.observe(viewLifecycleOwner) {
            joinedLeaguesAdapter.submitList(it)
            binding.joinedLeaguesEmptyView.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
            binding.joinedLeaguesRecyclerView.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) {
            binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) {
            it?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearErrorMessage()
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