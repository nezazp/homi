package si.uni.lj.fe.tnuv.homi

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import si.uni.lj.fe.tnuv.homi.databinding.ActivityDashboardBinding
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Set welcome message with user's display name
        val currentUser: FirebaseUser? = auth.currentUser
        val userName = currentUser?.displayName ?: "User" // Fallback to "User" if no name
        binding.welcomeText.text = "Welcome, $userName!"

        // Display today's and upcoming events
        displayTodaysEvents()
        displayUpcomingEvents()

        // Set up BottomNavigationView
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> true
                R.id.nav_calendar -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_messages -> {
                    startActivity(Intent(this, MessageBoardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_shopping -> {
                    startActivity(Intent(this, ShoppingListActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }

        binding.bottomNavigation.selectedItemId = R.id.nav_dashboard
    }

    private fun displayTodaysEvents() {
        val sdf = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
        val currentDate = sdf.format(Date())

        val todaysEvents = EventStore.getEvents().filter { it.date == currentDate }

        // Handle empty state
        if (todaysEvents.isEmpty()) {
            binding.noTodaysEventsText.visibility = View.VISIBLE
            binding.TodaysEventsContainer.visibility = View.GONE
            binding.TodaysEventsContainer.adapter = EventAdapter(emptyList()) { /* No-op */ }
        } else {
            binding.noTodaysEventsText.visibility = View.GONE
            binding.TodaysEventsContainer.visibility = View.VISIBLE
            binding.TodaysEventsContainer.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                adapter = EventAdapter(todaysEvents) { event ->
                    Log.d("DashboardActivity", "Starting EventActionActivity with event: ${event.title}")
                    try {
                        val intent = Intent(this@DashboardActivity, EventActionActivity::class.java).apply {
                            putExtra("event", event)
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("DashboardActivity", "Failed to start EventActionActivity: ${e.message}")
                    }
                }
            }
        }
    }

    private fun displayUpcomingEvents() {
        val sdf = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val today = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        val sevenDaysFromNow = calendar.time

        val upcomingEvents = EventStore.getEvents().filter {
            try {
                val eventDate = sdf.parse(it.date)
                eventDate != null && eventDate.after(today) && eventDate.before(sevenDaysFromNow)
            } catch (e: Exception) {
                false
            }
        }.sortedBy {
            sdf.parse(it.date)?.time ?: 0
        }

        // Handle empty state
        if (upcomingEvents.isEmpty()) {
            binding.noEventsText.visibility = View.VISIBLE
            binding.upcomingEventsContainer.visibility = View.GONE
            binding.upcomingEventsContainer.adapter = EventAdapter(emptyList()) { /* No-op */ }
        } else {
            binding.noEventsText.visibility = View.GONE
            binding.upcomingEventsContainer.visibility = View.VISIBLE
            binding.upcomingEventsContainer.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                adapter = EventAdapter(upcomingEvents) { event ->
                    Log.d("DashboardActivity", "Starting EventActionActivity with event: ${event.title}")
                    try {
                        val intent = Intent(this@DashboardActivity, EventActionActivity::class.java).apply {
                            putExtra("event", event)
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("DashboardActivity", "Failed to start EventActionActivity: ${e.message}")
                    }
                }
            }
        }
    }
}