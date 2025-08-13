package si.uni.lj.fe.tnuv.homi

import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import si.uni.lj.fe.tnuv.homi.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*
import android.widget.EditText
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.content.ContextCompat

data class Event
    (val title: String,
     val participants: List<User>,
     val repeating: Boolean,
     val taskWorth: Int,
     val date: String
) : java.io.Serializable

data class User(
    val username: String = "",
    val email: String = "",
) : java.io.Serializable


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val events = mutableListOf<Event>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        // Initialize ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // Retrieve events from intent
        @Suppress("UNCHECKED_CAST")
        intent.getSerializableExtra("events")?.let {
            events.clear()
            events.addAll(it as List<Event>)
        }

        // Set default date to current day
        val sdf = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
        val currentDate = sdf.format(Date())
        binding.selectedDate.text = "Selected Date: $currentDate"
        binding.calendarView.setDate(System.currentTimeMillis(), true, true)
        displayEventsForDate(currentDate)

        // Set listener for CalendarView
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = "$dayOfMonth/${month + 1}/$year"
            binding.selectedDate.text = "Selected Date: $selectedDate"
            displayEventsForDate(selectedDate)
        }

        // Handle "Add Event" button click
        binding.addEventButton.setOnClickListener {
            showAddEventDialog()
        }

        // Open add event dialog if flagged
        if (intent.getBooleanExtra("openAddEvent", false)) {
            showAddEventDialog()
        }

        // Set up BottomNavigationView
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    val intent = Intent(this, DashboardActivity::class.java)
                    intent.putExtra("events", ArrayList(events)) // Keep existing crash-prone code
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_calendar -> true
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

        binding.testFirebaseButton.setOnClickListener {
            listUsersFromFirebase()
        }
        binding.bottomNavigation.selectedItemId = R.id.nav_calendar
    }
    private fun listUsersFromFirebase() {
        if (isFinishing) return
        val container = findViewById<LinearLayout>(R.id.userListLayout)
        container.removeAllViews()

        val database = Firebase.database
        val usersRef = database.getReference("users")

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isFinishing) return
                if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                    val noUsersView = TextView(this@MainActivity)
                    noUsersView.text = "No users found in Firebase"
                    container.addView(noUsersView)
                    Toast.makeText(this@MainActivity, "No users found", Toast.LENGTH_SHORT).show()
                    Log.d("FirebaseUsers", "No users found in the database")
                    return
                }

                val users = mutableListOf<User>()
                for (userSnapshot in snapshot.children) {
                    try {
                        val user = userSnapshot.getValue(User::class.java)
                        if (user != null && user.username.isNotEmpty() && user.email.isNotEmpty()) {
                            users.add(user)
                            Log.d("FirebaseUsers", "User: username=${user.username}, email=${user.email}")
                            val userView = TextView(this@MainActivity)
                            userView.text = "👤 ${user.username} (${user.email})"
                            userView.setPadding(8, 8, 8, 8)
                            userView.textSize = 16f
                            container.addView(userView)
                        } else {
                            Log.w("FirebaseUsers", "Invalid user data for key: ${userSnapshot.key}")
                        }
                    } catch (e: com.google.firebase.database.DatabaseException) {
                        Log.e("FirebaseUsers", "Error parsing user data for key: ${userSnapshot.key}", e)
                    }
                }
                Toast.makeText(this@MainActivity, "Loaded ${users.size} user(s)", Toast.LENGTH_SHORT).show()
                Log.d("FirebaseUsers", "Total users loaded: ${users.size}")
            }

            override fun onCancelled(error: DatabaseError) {
                if (isFinishing) return
                Toast.makeText(this@MainActivity, "Error reading users: ${error.message}", Toast.LENGTH_LONG).show()
                Log.e("FirebaseUsers", "Error reading users: ${error.message}", error.toException())
            }
        })
    }
    override fun onBackPressed() {
        // Navigate back to DashboardActivity with updated events
        val intent = Intent(this, DashboardActivity::class.java)
        intent.putExtra("events", ArrayList(events))
        startActivity(intent)
        finish()
        super.onBackPressed()
    }

    private fun showAddEventDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_event, null)
        val titleEditText = dialogView.findViewById<EditText>(R.id.eventTitleEditText)
        val repeatToggle = dialogView.findViewById<Button>(R.id.repeatToggle)
        val repeatNumber = dialogView.findViewById<EditText>(R.id.repeatNumber)
        val frequencyRadioGroup = dialogView.findViewById<RadioGroup>(R.id.frequencyRadioGroup)
        val taskWorth = dialogView.findViewById<EditText>(R.id.taskWorth)
        val user1Button = dialogView.findViewById<Button>(R.id.user1Button)
        val user2Button = dialogView.findViewById<Button>(R.id.user2Button)
        val user3Button = dialogView.findViewById<Button>(R.id.user3Button)

        // Fetch users from Firebase
        val database = Firebase.database
        val usersRef = database.getReference("users")
        val availableUsers = mutableListOf<User>()
        val selectedUsers = mutableListOf<User>()

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isFinishing) return
                availableUsers.clear()
                for (userSnapshot in snapshot.children) {
                    try {
                        val user = userSnapshot.getValue(User::class.java)
                        if (user != null && user.username.isNotEmpty() && user.email.isNotEmpty()) {
                            availableUsers.add(user)
                        }
                    } catch (e: com.google.firebase.database.DatabaseException) {
                        Log.e("FirebaseUsers", "Error parsing user data for key: ${userSnapshot.key}", e)
                    }
                }

                // Update button texts with usernames (up to 3 users)
                if (availableUsers.isNotEmpty()) {
                    user1Button.text = availableUsers.getOrNull(0)?.username ?: "User 1"
                    user2Button.text = availableUsers.getOrNull(1)?.username ?: "User 2"
                    user3Button.text = availableUsers.getOrNull(2)?.username ?: "User 3"
                } else {
                    user1Button.text = "No User"
                    user2Button.text = "No User"
                    user3Button.text = "No User"
                    Toast.makeText(this@MainActivity, "No users found in Firebase", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (isFinishing) return
                Toast.makeText(this@MainActivity, "Error loading users: ${error.message}", Toast.LENGTH_LONG).show()
                Log.e("FirebaseUsers", "Error reading users: ${error.message}", error.toException())
                user1Button.text = "No User"
                user2Button.text = "No User"
                user3Button.text = "No User"
            }
        })

        // Repeat toggle logic
        repeatToggle.setOnClickListener {
            if (repeatToggle.text.toString() == "repeating") {
                repeatToggle.text = "not repeating"
                repeatNumber.visibility = View.GONE
                frequencyRadioGroup.visibility = View.GONE
            } else {
                repeatToggle.text = "repeating"
                repeatNumber.visibility = View.VISIBLE
                frequencyRadioGroup.visibility = View.VISIBLE
            }
        }

        // Toggle user selection
        fun toggleUser(button: Button, user: User?) {
            if (user == null) return
            if (selectedUsers.contains(user)) {
                selectedUsers.remove(user)
                button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.colorPrimary)
            } else {
                selectedUsers.add(user)
                button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.button_selected)
            }
        }

        user1Button.setOnClickListener { toggleUser(user1Button, availableUsers.getOrNull(0)) }
        user2Button.setOnClickListener { toggleUser(user2Button, availableUsers.getOrNull(1)) }
        user3Button.setOnClickListener { toggleUser(user3Button, availableUsers.getOrNull(2)) }

        val builder = AlertDialog.Builder(this)
            .setTitle("Add Event")
            .setView(dialogView)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()

        dialog.setOnShowListener {
            val addButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            addButton.setOnClickListener {
                val title = titleEditText.text.toString()
                var date = binding.selectedDate.text.toString().removePrefix("Selected Date: ")
                val repeating = repeatToggle.text.toString() == "repeating"
                val taskWorthValue = taskWorth.text.toString().toIntOrNull() ?: 5

                if (title.isNotEmpty() && selectedUsers.isNotEmpty()) {
                    if (date == "None") {
                        date = SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(Date())
                    }

                    val repeatCount = if (repeating) {
                        repeatNumber.text.toString().toIntOrNull() ?: 1
                    } else {
                        1
                    }

                    val selectedRadioId = frequencyRadioGroup.checkedRadioButtonId
                    val frequency = when (selectedRadioId) {
                        R.id.dailyRadioButton -> Calendar.DATE
                        R.id.weeklyRadioButton -> Calendar.WEEK_OF_YEAR
                        R.id.monthlyRadioButton -> Calendar.MONTH
                        R.id.yearlyRadioButton -> Calendar.YEAR
                        else -> Calendar.DATE
                    }

                    val sdf = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
                    val calendar = Calendar.getInstance()

                    try {
                        calendar.time = sdf.parse(date) ?: Date()

                        if (!repeating) {
                            val event = Event(
                                title = title,
                                participants = listOf(selectedUsers[0]),
                                repeating = repeating,
                                taskWorth = taskWorthValue,
                                date = sdf.format(calendar.time)
                            )
                            EventStore.addEvent(event)
                        } else {
                            var userIndex = 0
                            when (frequency) {
                                Calendar.DATE -> {
                                    val maxDays = 365
                                    for (day in 0 until maxDays) {
                                        for (i in 0 until repeatCount) {
                                            val event = Event(
                                                title = title,
                                                participants = listOf(selectedUsers[userIndex % selectedUsers.size]),
                                                repeating = repeating,
                                                taskWorth = taskWorthValue,
                                                date = sdf.format(calendar.time)
                                            )
                                            EventStore.addEvent(event)
                                            Log.d("Event", "Event added on date: ${sdf.format(calendar.time)}, User: ${selectedUsers[userIndex % selectedUsers.size].username}")
                                            userIndex++
                                        }
                                        calendar.add(Calendar.DATE, 1)
                                    }
                                }
                                Calendar.WEEK_OF_YEAR -> {
                                    val maxWeeks = 52
                                    for (week in 0 until maxWeeks) {
                                        val daysInWeek = 7
                                        val daysBetween = if (repeatCount > 1) daysInWeek / repeatCount else 1
                                        for (i in 0 until repeatCount) {
                                            val event = Event(
                                                title = title,
                                                participants = listOf(selectedUsers[userIndex % selectedUsers.size]),
                                                repeating = repeating,
                                                taskWorth = taskWorthValue,
                                                date = sdf.format(calendar.time)
                                            )
                                            EventStore.addEvent(event)
                                            Log.d("Event", "Event added on date: ${sdf.format(calendar.time)}, User: ${selectedUsers[userIndex % selectedUsers.size].username}")
                                            userIndex++
                                            if (i < repeatCount - 1) {
                                                calendar.add(Calendar.DATE, daysBetween)
                                            }
                                        }
                                        calendar.time = sdf.parse(date) ?: Date()
                                        calendar.add(Calendar.WEEK_OF_YEAR, week + 1)
                                    }
                                }
                                Calendar.MONTH -> {
                                    val maxMonths = 12
                                    for (month in 0 until maxMonths) {
                                        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                                        val daysBetween = if (repeatCount > 1) daysInMonth / repeatCount else 1
                                        for (i in 0 until repeatCount) {
                                            val event = Event(
                                                title = title,
                                                participants = listOf(selectedUsers[userIndex % selectedUsers.size]),
                                                repeating = repeating,
                                                taskWorth = taskWorthValue,
                                                date = sdf.format(calendar.time)
                                            )
                                            events.add(event)
                                            Log.d("Event", "Event added on date: ${sdf.format(calendar.time)}, User: ${selectedUsers[userIndex % selectedUsers.size].username}")
                                            userIndex++
                                            if (i < repeatCount - 1) {
                                                calendar.add(Calendar.DATE, daysBetween)
                                            }
                                        }
                                        calendar.time = sdf.parse(date) ?: Date()
                                        calendar.add(Calendar.MONTH, month + 1)
                                    }
                                }
                                Calendar.YEAR -> {
                                    val maxYears = 5
                                    for (year in 0 until maxYears) {
                                        val daysInYear = calendar.getActualMaximum(Calendar.DAY_OF_YEAR)
                                        val daysBetween = if (repeatCount > 1) daysInYear / repeatCount else 1
                                        for (i in 0 until repeatCount) {
                                            val event = Event(
                                                title = title,
                                                participants = listOf(selectedUsers[userIndex % selectedUsers.size]),
                                                repeating = repeating,
                                                taskWorth = taskWorthValue,
                                                date = sdf.format(calendar.time)
                                            )
                                            EventStore.addEvent(event)
                                            Log.d("Event", "Event added on date: ${sdf.format(calendar.time)}, User: ${selectedUsers[userIndex % selectedUsers.size].username}")
                                            userIndex++
                                            if (i < repeatCount - 1) {
                                                calendar.add(Calendar.DATE, daysBetween)
                                            }
                                        }
                                        calendar.time = sdf.parse(date) ?: Date()
                                        calendar.add(Calendar.YEAR, year + 1)
                                    }
                                }
                            }
                        }

                        Toast.makeText(this, "$repeatCount event(s) added!", Toast.LENGTH_SHORT).show()
                        displayEventsForDate(date)
                        dialog.dismiss()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    when {
                        title.isEmpty() -> Toast.makeText(this, "Please enter title!", Toast.LENGTH_SHORT).show()
                        selectedUsers.isEmpty() -> Toast.makeText(this, "Please select at least one participant!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun displayEventsForDate(date: String) {
        val container = findViewById<LinearLayout>(R.id.eventListLayout)
        container.removeAllViews()

        val eventsOnDate = EventStore.getEvents().filter { it.date == date }

        if (eventsOnDate.isEmpty()) {
            val noEventView = TextView(this)
            noEventView.text = "No events for $date"
            container.addView(noEventView)
            return
        }

        for (event in eventsOnDate) {
            val eventView = TextView(this)
            eventView.text = "📅 ${event.title}\n👥 ${event.participants.joinToString { it.username }}\n⭐ Task Worth: ${event.taskWorth}"
            eventView.setPadding(8, 8, 8, 8)
            eventView.textSize = 16f
            eventView.setOnClickListener {
                showDeleteEventDialog(event, date)
            }
            container.addView(eventView)
        }
    }

    private fun showDeleteEventDialog(event: Event, date: String) {
        val builder = AlertDialog.Builder(this)
            .setTitle("Delete Event")
            .setMessage("Are you sure you want to delete '${event.title}' on $date?")
            .setPositiveButton("Delete") { _, _ ->
                events.remove(event)
                Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show()
                displayEventsForDate(date)
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        builder.create().show()
    }
}