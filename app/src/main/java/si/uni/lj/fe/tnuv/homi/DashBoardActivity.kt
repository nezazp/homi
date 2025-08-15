package si.uni.lj.fe.tnuv.homi

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import si.uni.lj.fe.tnuv.homi.databinding.ActivityDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private var groupId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        database = Firebase.database.reference

        // Set welcome message with user's display name
        val currentUser: FirebaseUser? = auth.currentUser
        val userName = currentUser?.displayName ?: "User"
        binding.welcomeText.text = "Welcome, $userName!"

        // Fetch user's groupId
        if (currentUser != null) {
            database.child("users").child(currentUser.uid).child("groupId").get()
                .addOnSuccessListener { snapshot ->
                    groupId = snapshot.value as? String
                    if (groupId == null || groupId!!.isEmpty()) {
                        binding.noTodaysEventsText.text = "Join a group to view events"
                        binding.noTodaysEventsText.visibility = View.VISIBLE
                        binding.TodaysEventsContainer.visibility = View.GONE
                        binding.noEventsText.text = "Join a group to view events"
                        binding.noEventsText.visibility = View.VISIBLE
                        binding.upcomingEventsContainer.visibility = View.GONE
                        Toast.makeText(this, "Please join a group to view events", Toast.LENGTH_LONG).show()
                    } else {
                        // Display events only after groupId is fetched
                        displayTodaysEvents()
                        displayUpcomingEvents()
                    }
                }
                .addOnFailureListener { error ->
                    Log.e("DashboardActivity", "Failed to fetch groupId", error)
                    binding.noTodaysEventsText.text = "Error loading group"
                    binding.noTodaysEventsText.visibility = View.VISIBLE
                    binding.TodaysEventsContainer.visibility = View.GONE
                    binding.noEventsText.text = "Error loading group"
                    binding.noEventsText.visibility = View.VISIBLE
                    binding.upcomingEventsContainer.visibility = View.GONE
                    Toast.makeText(this, "Error fetching group: ${error.message}", Toast.LENGTH_LONG).show()
                }
        } else {
            // Redirect to LoginActivity if not authenticated
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

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
        if (groupId == null || groupId!!.isEmpty()) {
            binding.noTodaysEventsText.text = "Join a group to view events"
            binding.noTodaysEventsText.visibility = View.VISIBLE
            binding.TodaysEventsContainer.visibility = View.GONE
            return
        }

        val sdf = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
        val currentDate = sdf.format(Date())

        database.child("groups").child(groupId!!).child("events")
            .orderByChild("date").equalTo(currentDate)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val todaysEvents = mutableListOf<Event>()
                    for (eventSnapshot in snapshot.children) {
                        val eventMap = eventSnapshot.value as? Map<String, Any> ?: continue
                        val title = eventMap["title"] as? String ?: ""
                        val repeating = eventMap["repeating"] as? Boolean ?: false
                        val taskWorth = (eventMap["taskWorth"] as? Long)?.toInt() ?: 5
                        val eventDate = eventMap["date"] as? String ?: ""
                        val participantsList = eventMap["participants"] as? List<Map<String, String>> ?: emptyList()
                        val participants = participantsList.map { User(it["username"] ?: "", it["email"] ?: "") }

                        val event = Event(title, participants, repeating, taskWorth, eventDate)
                        todaysEvents.add(event)
                    }

                    if (todaysEvents.isEmpty()) {
                        binding.noTodaysEventsText.text = "No events for today"
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

                override fun onCancelled(error: DatabaseError) {
                    Log.e("DashboardActivity", "Error loading today's events", error.toException())
                    binding.noTodaysEventsText.text = "Error loading events"
                    binding.noTodaysEventsText.visibility = View.VISIBLE
                    binding.TodaysEventsContainer.visibility = View.GONE
                    Toast.makeText(this@DashboardActivity, "Error loading events: ${error.message}", Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun displayUpcomingEvents() {
        if (groupId == null || groupId!!.isEmpty()) {
            binding.noEventsText.text = "Join a group to view events"
            binding.noEventsText.visibility = View.VISIBLE
            binding.upcomingEventsContainer.visibility = View.GONE
            return
        }

        val sdf = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val today = calendar.time
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        val sevenDaysFromNow = calendar.time

        database.child("groups").child(groupId!!).child("events")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val upcomingEvents = mutableListOf<Event>()
                    for (eventSnapshot in snapshot.children) {
                        val eventMap = eventSnapshot.value as? Map<String, Any> ?: continue
                        val title = eventMap["title"] as? String ?: ""
                        val repeating = eventMap["repeating"] as? Boolean ?: false
                        val taskWorth = (eventMap["taskWorth"] as? Long)?.toInt() ?: 5
                        val eventDate = eventMap["date"] as? String ?: ""
                        val participantsList = eventMap["participants"] as? List<Map<String, String>> ?: emptyList()
                        val participants = participantsList.map { User(it["username"] ?: "", it["email"] ?: "") }

                        try {
                            val parsedDate = sdf.parse(eventDate)
                            if (parsedDate != null && parsedDate.after(today) && parsedDate.before(sevenDaysFromNow)) {
                                val event = Event(title, participants, repeating, taskWorth, eventDate)
                                upcomingEvents.add(event)
                            }
                        } catch (e: Exception) {
                            Log.w("DashboardActivity", "Invalid date format for event: $title, date: $eventDate")
                        }
                    }

                    // Sort events by date
                    val sortedEvents = upcomingEvents.sortedBy { sdf.parse(it.date)?.time ?: 0 }

                    if (sortedEvents.isEmpty()) {
                        binding.noEventsText.text = "No upcoming events"
                        binding.noEventsText.visibility = View.VISIBLE
                        binding.upcomingEventsContainer.visibility = View.GONE
                        binding.upcomingEventsContainer.adapter = EventAdapter(emptyList()) { /* No-op */ }
                    } else {
                        binding.noEventsText.visibility = View.GONE
                        binding.upcomingEventsContainer.visibility = View.VISIBLE
                        binding.upcomingEventsContainer.apply {
                            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                            adapter = EventAdapter(sortedEvents) { event ->
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

                override fun onCancelled(error: DatabaseError) {
                    Log.e("DashboardActivity", "Error loading upcoming events", error.toException())
                    binding.noEventsText.text = "Error loading events"
                    binding.noEventsText.visibility = View.VISIBLE
                    binding.upcomingEventsContainer.visibility = View.GONE
                    Toast.makeText(this@DashboardActivity, "Error loading events: ${error.message}", Toast.LENGTH_LONG).show()
                }
            })
    }
}