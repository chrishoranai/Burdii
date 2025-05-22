package com.app.burdii.ui.league

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.burdii.data.models.firebase.LeagueScore
import com.app.burdii.databinding.FragmentManageScoresBinding

class ManageScoresFragment : Fragment() {

    private var _binding: FragmentManageScoresBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ManageScoresViewModel by viewModels()

    private lateinit var pendingScoresAdapter: PendingScoreAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageScoresBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        binding.manualAddScoreButton.setOnClickListener { 
            // TODO: Navigate to a screen/dialog for manual score entry
            Toast.makeText(requireContext(), "Manual Add Score clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        pendingScoresAdapter = PendingScoreAdapter(
            onApproveClick = { score -> viewModel.reviewScore(score.scoreId, "approved") },
            onDenyClick = { score -> viewModel.reviewScore(score.scoreId, "denied") }
        )
        binding.pendingScoresRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = pendingScoresAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.pendingScores.observe(viewLifecycleOwner, Observer { scores ->
            pendingScoresAdapter.submitList(scores)
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            // TODO: Show/hide a loading indicator for the list
        })

        viewModel.error.observe(viewLifecycleOwner, Observer { errorMessage ->
            errorMessage?.let {
                Toast.makeText(requireContext(), "Error: $it", Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        })
        
        viewModel.actionStatus.observe(viewLifecycleOwner, Observer { status ->
            when(status) {
                is ActionStatus.Idle -> { /* Do nothing */ }
                is ActionStatus.Loading -> {
                    // TODO: Show a loading indicator for the specific item or a general one
                    // You might need to update the adapter item's state if showing per-item loading
                }
                is ActionStatus.Success -> {
                    Toast.makeText(requireContext(), status.message, Toast.LENGTH_SHORT).show()
                    // The pendingScores LiveData should automatically update via Firestore snapshot listener
                    viewModel.clearActionStatus()
                }
                is ActionStatus.Error -> {
                    Toast.makeText(requireContext(), "Action Error: ${status.message}", Toast.LENGTH_LONG).show()
                    viewModel.clearActionStatus()
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
