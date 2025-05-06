import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class HomeActivity : AppCompatActivity() {
    private lateinit var recentRoundsRecyclerView: RecyclerView
    private lateinit var startNewRoundButton: MaterialButton
    private lateinit var clearButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        recentRoundsRecyclerView = findViewById(R.id.recentRoundsRecyclerView)
        startNewRoundButton = findViewById(R.id.startNewRoundButton)
        clearButton = findViewById(R.id.clearButton)

        // Sample data for recent rounds
        val sampleRounds = listOf(
            Round("Riverside Park", "Today, 2:30 PM", "+2", "18 holes"),
            Round("Mountain View", "Yesterday, 10:15 AM", "-1", "18 holes"),
            Round("Cedar Hills", "Jul 13, 4:45 PM", "+4", "18 holes"),
            Round("Lake Front", "Jul 13, 12:00 PM", "+1", "18 holes"),
            Round("Sunnydale", "Jul 10, 9:00 AM", "-3", "18 holes")
        )

        // Set up RecyclerView
        val adapter = RoundAdapter(sampleRounds)
        recentRoundsRecyclerView.adapter = adapter
        recentRoundsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Button click listeners
        startNewRoundButton.setOnClickListener {
            Toast.makeText(this, "Start New Round", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to new round activity
        }

        clearButton.setOnClickListener {
            Toast.makeText(this, "Clear Recent Rounds", Toast.LENGTH_SHORT).show()
            // TODO: Implement clear functionality
        }
    }
}
