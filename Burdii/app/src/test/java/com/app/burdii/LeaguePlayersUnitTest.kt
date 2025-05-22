package com.app.burdii

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.app.burdii.LeaguePlayersFragment.PlayerAdapter

@RunWith(MockitoJUnitRunner::class)
class LeaguePlayersUnitTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockNavController: NavController

    private lateinit var fragment: LeaguePlayersFragment
    private lateinit var playerList: MutableList<String>
    private lateinit var adapter: PlayerAdapter

    @Before
    fun setup() {
        // Reset the playerList and adapter before each test
        playerList = mutableListOf()
        adapter = PlayerAdapter(playerList)

        // Create a new fragment instance for each test
        fragment = LeaguePlayersFragment()

        // Mock the view and attach NavController
        val mockView = mock<View>()
        Navigation.setViewNavController(mockView, mockNavController)
        fragment.viewLifecycleOwnerLiveData.observeForever { lifecycleOwner ->
            if (lifecycleOwner != null) {
                // Simulate fragment view creation and binding
                val mockRecyclerView = mock<RecyclerView>()
                whenever(mockView.findViewById<RecyclerView>(R.id.playersRecyclerView)).thenReturn(mockRecyclerView)
                whenever(mockRecyclerView.layoutManager).thenReturn(mock()) // Mocking LayoutManager
                whenever(mockRecyclerView.adapter).thenReturn(adapter)

                val mockEditText = mock<android.widget.EditText>()
                whenever(mockView.findViewById<android.widget.EditText>(R.id.playerNameEditText)).thenReturn(mockEditText)
                whenever(mockEditText.text).thenReturn(mock()) // Mocking Editable

                val mockAddButton = mock<android.widget.Button>()
                whenever(mockView.findViewById<android.widget.Button>(R.id.addPlayerButton)).thenReturn(mockAddButton)

                val mockNextButton = mock<android.widget.Button>()
                whenever(mockView.findViewById<android.widget.Button>(R.id.nextStepButton)).thenReturn(mockNextButton)

                fragment.onViewCreated(mockView, null)
            }
        }

        // Simulate fragment creation
        fragment.onCreateView(mock(), mock(), null)
    }

    @Test
    fun testAddPlayer() {
        val playerName = "Test Player"
        val playerNameEditText = fragment.view?.findViewById<android.widget.EditText>(R.id.playerNameEditText)
        val addPlayerButton = fragment.view?.findViewById<android.widget.Button>(R.id.addPlayerButton)

        // Simulate typing and clicking add
        whenever(playerNameEditText?.text?.toString()).thenReturn(playerName)
        addPlayerButton?.performClick()

        assert(playerList.contains(playerName))
        assert(adapter.itemCount == 1)
        verify(adapter).notifyItemInserted(0)
        verify(playerNameEditText?.text)?.clear()
    }

    @Test
    fun testRemovePlayer() {
        val player1 = "Player 1"
        val player2 = "Player 2"
        playerList.add(player1)
        playerList.add(player2)
        adapter.notifyDataSetChanged() // Simulate initial data

        // Simulate removing the first player
        adapter.removeItem(0)

        assert(!playerList.contains(player1))
        assert(playerList.contains(player2))
        assert(adapter.itemCount == 1)
        verify(adapter).notifyItemRemoved(0)
    }

    @Test
    fun testReorderPlayers() {
        val player1 = "Player 1"
        val player2 = "Player 2"
        val player3 = "Player 3"
        playerList.add(player1)
        playerList.add(player2)
        playerList.add(player3)
        adapter.notifyDataSetChanged() // Simulate initial data

        // Simulate moving player1 from position 0 to position 2
        adapter.moveItem(0, 2)

        assert(playerList[0] == player2)
        assert(playerList[1] == player3)
        assert(playerList[2] == player1)
        verify(adapter).notifyItemMoved(0, 2)
    }

    @Test
    fun testNavigateToNextFragmentWithPlayers() {
        val player1 = "Player A"
        val player2 = "Player B"
        playerList.add(player1)
        playerList.add(player2)

        val nextButton = fragment.view?.findViewById<android.widget.Button>(R.id.nextStepButton)
        nextButton?.performClick()

        // Verify that navigation occurred with the correct arguments
        verify(mockNavController).navigate(
            eq(R.id.action_leaguePlayersFragment_to_scoreEntryByHoleFragment), // Assuming default navigation for now
            argThat { bundle ->
                bundle.getStringArrayList("playerNames")?.containsAll(listOf(player1, player2)) == true
            }
        )
    }
}

// Dummy PlayerAdapter for testing purposes
// This should mirror the adapter in LeaguePlayersFragment but can be simplified for tests
class PlayerAdapter(private val players: MutableList<String>) :
    RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder>() {

    class PlayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val playerNameTextView: TextView = itemView.findViewById(R.id.playerNameTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        // In a real test, you might mock the layout inflation
        val view = mock<View>() // Mock the item view
        whenever(view.findViewById<TextView>(R.id.playerNameTextView)).thenReturn(mock()) // Mock the TextView
        return PlayerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        holder.playerNameTextView.text = players[position]
    }

    override fun getItemCount(): Int = players.size

    // Test helper methods for removing and moving
    fun removeItem(position: Int) {
        if (position < players.size) {
            players.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition < players.size && toPosition < players.size) {
            val player = players.removeAt(fromPosition)
            players.add(toPosition, player)
            notifyItemMoved(fromPosition, toPosition)
        }
    }
}