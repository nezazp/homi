package si.uni.lj.fe.tnuv.homi

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import si.uni.lj.fe.tnuv.homi.databinding.ActivityDashboardBinding
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var events: List<Event>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewBinding
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve events from intent
        @Suppress("UNCHECKED_CAST")
        events = intent.getSerializableExtra("events") as? List<Event> ?: emptyList()

        // Set welcome message (placeholder user, replace with actual user if available)
        val currentUser = "User" // Replace with actual user data if authentication is implemented
        binding.welcomeText.text = "Welcome, $currentUser!"

        // Display event summary
        val sdf = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
        val currentDate = sdf.format(Date())
        val todayEvents = events.filter { it.date == currentDate }
        binding.eventSummary.text = "Today's Events: ${todayEvents.size}"

        // Display upcoming events (within next 7 days)
        displayUpcomingEvents()

        // Navigate to MainActivity for event management
        binding.manageEventsButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("events", ArrayList(events)) // Pass events back
            startActivity(intent)
        }

        // Navigate to MainActivity to add a new event
        binding.addEventButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("events", ArrayList(events))
            intent.putExtra("openAddEvent", true) // Flag to open add event dialog
            startActivity(intent)
        }
    }

    private fun displayUpcomingEvents() {
        val container = binding.upcomingEventsContainer
        container.removeAllViews()

        val sdf = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val today = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        val sevenDaysFromNow = calendar.time

        val upcomingEvents = events.filter {
            try {
                val eventDate = sdf.parse(it.date)
                eventDate != null && eventDate.after(today) && eventDate.before(sevenDaysFromNow)
            } catch (e: Exception) {
                false
            }
        }.sortedBy {
            sdf.parse(it.date)?.time ?: 0
        }

        if (upcomingEvents.isEmpty()) {
            val noEventsView = TextView(this)
            noEventsView.text = "No upcoming events in the next 7 days"
            noEventsView.setPadding(8, 8, 8, 8)
            noEventsView.textSize = 16f
            container.addView(noEventsView)
            return
        }

        for (event in upcomingEvents) {
            val eventView = TextView(this)
            eventView.text = "📅 ${event.title}\n👥 ${event.participants.joinToString { it.username }}\n📆 ${event.date}\n⭐ Task Worth: ${event.taskWorth}"
            eventView.setPadding(8, 8, 8, 8)
            eventView.textSize = 16f
            container.addView(eventView)
        }
    }
}