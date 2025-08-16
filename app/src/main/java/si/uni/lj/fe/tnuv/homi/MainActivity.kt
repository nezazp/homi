package si.uni.lj.fe.tnuv.homi

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
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
import java.util.UUID

data class Event(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val participants: List<User>,
    val repeating: Boolean,
    val taskWorth: Int,
    val date: String,
    val repeatGroupId: String? = null // Links events in a repeating series
) : java.io.Serializable

data class User(
    val username: String = "",
    val email: String = "",
    val groupId: String = "",
    val points: Number = 0
) : java.io.Serializable

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var database: DatabaseReference
    private var groupId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        database = Firebase.database.reference

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        // Get current user's groupId
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            database.child("users").child(uid).child("groupId").get()
                .addOnSuccessListener { snapshot ->
                    groupId = snapshot.value as? String
                    if (groupId == null || groupId!!.isEmpty()) {
                        Toast.makeText(this, "Please join a group to view events", Toast.LENGTH_LONG).show()
                    } else {
                        val sdf = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
                        val currentDate = sdf.format(Date())
                        binding.selectedDate.text = "Selected Date: $currentDate"
                        binding.calendarView.setDate(System.currentTimeMillis(), true, true)
                        displayEventsForDate(currentDate)
                    }
                }
                .addOnFailureListener { error ->
                    Toast.makeText(this, "Error fetching group: ${error.message}", Toast.LENGTH_LONG).show()
                    Log.e("MainActivity", "Failed to fetch groupId", error)
                }
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = "$dayOfMonth/${month + 1}/$year"
            binding.selectedDate.text = "Selected Date: $selectedDate"
            displayEventsForDate(selectedDate)
        }

        binding.addEventButton.setOnClickListener {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                Toast.makeText(this, "User not authenticated", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (groupId == null || groupId!!.isEmpty()) {
                showGroupOptionsDialog(uid)
                return@setOnClickListener
            }
            showAddEventDialog()
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
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
        binding.bottomNavigation.selectedItemId = R.id.nav_calendar
    }

    private fun showGroupOptionsDialog(uid: String) {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Group Options")
            .setMessage("Would you like to create a new group or join an existing one?")
            .setPositiveButton("Create Group") { _, _ ->
                showCreateGroupDialog(uid)
            }
            .setNegativeButton("Join Group") { _, _ ->
                showJoinGroupDialog(uid)
            }
            .setNeutralButton("Skip") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        alertDialog.show()
    }

    private fun showCreateGroupDialog(uid: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_group, null)
        val groupNameEditText = dialogView.findViewById<EditText>(R.id.group_name_edit_text)

        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Create a Group")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val groupName = groupNameEditText.text.toString().trim()
                if (groupName.isEmpty()) {
                    showError("Please enter a group name")
                    return@setPositiveButton
                }
                createGroup(uid, groupName)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                showGroupOptionsDialog(uid)
            }
            .create()
        alertDialog.show()
    }

    private fun createGroup(uid: String, groupName: String) {
        val groupId = UUID.randomUUID().toString().replace("-", "").substring(0, 8)
        val groupsRef = database.child("groups").child(groupId)
        val usersRef = database.child("users").child(uid)

        val groupData = mapOf(
            "name" to groupName,
            "members" to mapOf(uid to true)
        )
        groupsRef.setValue(groupData)
            .addOnCompleteListener { groupTask ->
                if (groupTask.isSuccessful) {
                    usersRef.child("groupId").setValue(groupId)
                        .addOnCompleteListener { userTask ->
                            if (userTask.isSuccessful) {
                                this.groupId = groupId
                                runOnUiThread {
                                    Toast.makeText(this, "Group created! ID: $groupId", Toast.LENGTH_LONG).show()
                                    showAddEventDialog()
                                }
                            } else {
                                runOnUiThread {
                                    showError("Failed to update user: ${userTask.exception?.message}")
                                }
                                Log.e("MainActivity", "Failed to update user group", userTask.exception)
                            }
                        }
                } else {
                    runOnUiThread {
                        showError("Failed to create group: ${groupTask.exception?.message}")
                    }
                    Log.e("MainActivity", "Failed to create group", groupTask.exception)
                }
            }
    }

    private fun showJoinGroupDialog(uid: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_join_group, null)
        val groupIdEditText = dialogView.findViewById<EditText>(R.id.group_id_edit_text)

        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Join a Group")
            .setView(dialogView)
            .setPositiveButton("Join") { _, _ ->
                val groupId = groupIdEditText.text.toString().trim()
                if (groupId.isEmpty()) {
                    showError("Please enter a group ID")
                    return@setPositiveButton
                }
                joinGroup(uid, groupId)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                showGroupOptionsDialog(uid)
            }
            .create()
        alertDialog.show()
    }

    private fun joinGroup(uid: String, groupId: String) {
        val groupsRef = database.child("groups").child(groupId).child("members")
        val usersRef = database.child("users").child(uid)

        groupsRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful && task.result.exists()) {
                groupsRef.child(uid).setValue(true)
                    .addOnCompleteListener { memberTask ->
                        if (memberTask.isSuccessful) {
                            usersRef.child("groupId").setValue(groupId)
                                .addOnCompleteListener { userTask ->
                                    if (userTask.isSuccessful) {
                                        this.groupId = groupId
                                        runOnUiThread {
                                            Toast.makeText(this, "Joined group $groupId!", Toast.LENGTH_SHORT).show()
                                            showAddEventDialog()
                                        }
                                    } else {
                                        runOnUiThread {
                                            showError("Failed to update user group: ${userTask.exception?.message}")
                                        }
                                        Log.e("MainActivity", "Failed to update user group", userTask.exception)
                                    }
                                }
                        } else {
                            runOnUiThread {
                                showError("Failed to join group: ${memberTask.exception?.message}")
                            }
                            Log.e("MainActivity", "Failed to join group", memberTask.exception)
                        }
                    }
            } else {
                runOnUiThread {
                    showError("Group $groupId does not exist")
                }
                Log.e("MainActivity", "Group $groupId not found")
            }
        }
    }

    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    private fun listUsersFromFirebase() {
        if (isFinishing) return
        val container = findViewById<LinearLayout>(R.id.userListLayout)
        container.removeAllViews()

        if (groupId == null || groupId!!.isEmpty()) {
            val noGroupView = TextView(this)
            noGroupView.text = "Join a group to view users"
            container.addView(noGroupView)
            return
        }

        database.child("groups").child(groupId!!).child("members").get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                    val noUsersView = TextView(this)
                    noUsersView.text = "No users in this group"
                    container.addView(noUsersView)
                    Toast.makeText(this, "No users found in group", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val users = mutableListOf<User>()
                val uids = snapshot.children.mapNotNull { it.key }
                var completedQueries = 0

                for (uid in uids) {
                    database.child("users").child(uid).get().addOnSuccessListener { userSnapshot ->
                        try {
                            val user = userSnapshot.getValue(User::class.java)
                            if (user != null && user.username.isNotEmpty() && user.email.isNotEmpty()) {
                                users.add(user)
                                val userView = TextView(this)
                                userView.text = "👤 ${user.username} (${user.email})"
                                userView.setPadding(8, 8, 8, 8)
                                userView.textSize = 16f
                                container.addView(userView)
                            }
                        } catch (e: DatabaseException) {
                            Log.e("FirebaseUsers", "Error parsing user data for key: $uid", e)
                        }
                        completedQueries++
                        if (completedQueries == uids.size) {
                            Toast.makeText(this, "Loaded ${users.size} user(s)", Toast.LENGTH_SHORT).show()
                            Log.d("FirebaseUsers", "Total users loaded: ${users.size}")
                        }
                    }.addOnFailureListener { error ->
                        Log.e("FirebaseUsers", "Error fetching user $uid: ${error.message}", error)
                        completedQueries++
                        if (completedQueries == uids.size) {
                            Toast.makeText(this, "Loaded ${users.size} user(s)", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "Error reading users: ${error.message}", Toast.LENGTH_LONG).show()
                Log.e("FirebaseUsers", "Error reading group members: ${error.message}", error)
            }
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

        val availableUsers = mutableListOf<User>()
        val selectedUsers = mutableListOf<User>()

        if (groupId == null || groupId!!.isEmpty()) {
            Toast.makeText(this, "Please join a group to add events", Toast.LENGTH_SHORT).show()
            return
        }

        database.child("groups").child(groupId!!).child("members").get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                    user1Button.text = "No User"
                    user2Button.text = "No User"
                    user3Button.text = "No User"
                    Toast.makeText(this, "No users in your group", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val uids = snapshot.children.mapNotNull { it.key }
                var completedQueries = 0

                for (uid in uids) {
                    database.child("users").child(uid).get().addOnSuccessListener { userSnapshot ->
                        try {
                            val user = userSnapshot.getValue(User::class.java)
                            if (user != null && user.username.isNotEmpty() && user.email.isNotEmpty()) {
                                availableUsers.add(user)
                            }
                        } catch (e: DatabaseException) {
                            Log.e("FirebaseUsers", "Error parsing user data for key: $uid", e)
                        }
                        completedQueries++
                        if (completedQueries == uids.size) {
                            user1Button.text = availableUsers.getOrNull(0)?.username ?: "No User"
                            user2Button.text = availableUsers.getOrNull(1)?.username ?: "No User"
                            user3Button.text = availableUsers.getOrNull(2)?.username ?: "No User"
                        }
                    }.addOnFailureListener { error ->
                        user1Button.text = "No User"
                        user2Button.text = "No User"
                        user3Button.text = "No User"
                        Toast.makeText(this, "Error loading group users: ${error.message}", Toast.LENGTH_LONG).show()
                        Log.e("FirebaseUsers", "Error reading group members: ${error.message}", error)
                    }
                }
            }

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
                    val eventsRef = database.child("groups").child(groupId!!).child("events")
                    val repeatGroupId = if (repeating) UUID.randomUUID().toString() else null

                    try {
                        calendar.time = sdf.parse(date) ?: Date()

                        if (!repeating) {
                            val eventId = eventsRef.push().key ?: return@setOnClickListener
                            val event = mapOf(
                                "id" to eventId,
                                "title" to title,
                                "participants" to selectedUsers.map { mapOf("username" to it.username, "email" to it.email) },
                                "repeating" to repeating,
                                "taskWorth" to taskWorthValue,
                                "date" to sdf.format(calendar.time),
                                "repeatGroupId" to null
                            )
                            eventsRef.child(eventId).setValue(event)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Event added!", Toast.LENGTH_SHORT).show()
                                    displayEventsForDate(date)
                                    dialog.dismiss()
                                }
                                .addOnFailureListener { error ->
                                    Toast.makeText(this, "Failed to add event: ${error.message}", Toast.LENGTH_LONG).show()
                                    Log.e("MainActivity", "Failed to add event", error)
                                }
                        } else {
                            var userIndex = 0
                            when (frequency) {
                                Calendar.DATE -> {
                                    val maxDays = 365
                                    for (day in 0 until maxDays) {
                                        for (i in 0 until repeatCount) {
                                            val eventId = eventsRef.push().key ?: continue
                                            val event = mapOf(
                                                "id" to eventId,
                                                "title" to title,
                                                "participants" to listOf(mapOf("username" to selectedUsers[userIndex % selectedUsers.size].username, "email" to selectedUsers[userIndex % selectedUsers.size].email)),
                                                "repeating" to repeating,
                                                "taskWorth" to taskWorthValue,
                                                "date" to sdf.format(calendar.time),
                                                "repeatGroupId" to repeatGroupId
                                            )
                                            eventsRef.child(eventId).setValue(event)
                                            userIndex++
                                        }
                                        calendar.add(Calendar.DATE, 1)
                                    }
                                    Toast.makeText(this, "$repeatCount event(s) added!", Toast.LENGTH_SHORT).show()
                                    displayEventsForDate(date)
                                    dialog.dismiss()
                                }
                                Calendar.WEEK_OF_YEAR -> {
                                    val maxWeeks = 52
                                    for (week in 0 until maxWeeks) {
                                        val daysInWeek = 7
                                        val daysBetween = if (repeatCount > 1) daysInWeek / repeatCount else 1
                                        for (i in 0 until repeatCount) {
                                            val eventId = eventsRef.push().key ?: continue
                                            val event = mapOf(
                                                "id" to eventId,
                                                "title" to title,
                                                "participants" to listOf(mapOf("username" to selectedUsers[userIndex % selectedUsers.size].username, "email" to selectedUsers[userIndex % selectedUsers.size].email)),
                                                "repeating" to repeating,
                                                "taskWorth" to taskWorthValue,
                                                "date" to sdf.format(calendar.time),
                                                "repeatGroupId" to repeatGroupId
                                            )
                                            eventsRef.child(eventId).setValue(event)
                                            userIndex++
                                            if (i < repeatCount - 1) {
                                                calendar.add(Calendar.DATE, daysBetween)
                                            }
                                        }
                                        calendar.time = sdf.parse(date) ?: Date()
                                        calendar.add(Calendar.WEEK_OF_YEAR, week + 1)
                                    }
                                    Toast.makeText(this, "$repeatCount event(s) added!", Toast.LENGTH_SHORT).show()
                                    displayEventsForDate(date)
                                    dialog.dismiss()
                                }
                                Calendar.MONTH -> {
                                    val maxMonths = 12
                                    for (month in 0 until maxMonths) {
                                        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                                        val daysBetween = if (repeatCount > 1) daysInMonth / repeatCount else 1
                                        for (i in 0 until repeatCount) {
                                            val eventId = eventsRef.push().key ?: continue
                                            val event = mapOf(
                                                "id" to eventId,
                                                "title" to title,
                                                "participants" to listOf(mapOf("username" to selectedUsers[userIndex % selectedUsers.size].username, "email" to selectedUsers[userIndex % selectedUsers.size].email)),
                                                "repeating" to repeating,
                                                "taskWorth" to taskWorthValue,
                                                "date" to sdf.format(calendar.time),
                                                "repeatGroupId" to repeatGroupId
                                            )
                                            eventsRef.child(eventId).setValue(event)
                                            userIndex++
                                            if (i < repeatCount - 1) {
                                                calendar.add(Calendar.DATE, daysBetween)
                                            }
                                        }
                                        calendar.time = sdf.parse(date) ?: Date()
                                        calendar.add(Calendar.MONTH, month + 1)
                                    }
                                    Toast.makeText(this, "$repeatCount event(s) added!", Toast.LENGTH_SHORT).show()
                                    displayEventsForDate(date)
                                    dialog.dismiss()
                                }
                                Calendar.YEAR -> {
                                    val maxYears = 5
                                    for (year in 0 until maxYears) {
                                        val daysInYear = calendar.getActualMaximum(Calendar.DAY_OF_YEAR)
                                        val daysBetween = if (repeatCount > 1) daysInYear / repeatCount else 1
                                        for (i in 0 until repeatCount) {
                                            val eventId = eventsRef.push().key ?: continue
                                            val event = mapOf(
                                                "id" to eventId,
                                                "title" to title,
                                                "participants" to listOf(mapOf("username" to selectedUsers[userIndex % selectedUsers.size].username, "email" to selectedUsers[userIndex % selectedUsers.size].email)),
                                                "repeating" to repeating,
                                                "taskWorth" to taskWorthValue,
                                                "date" to sdf.format(calendar.time),
                                                "repeatGroupId" to repeatGroupId
                                            )
                                            eventsRef.child(eventId).setValue(event)
                                            userIndex++
                                            if (i < repeatCount - 1) {
                                                calendar.add(Calendar.DATE, daysBetween)
                                            }
                                        }
                                        calendar.time = sdf.parse(date) ?: Date()
                                        calendar.add(Calendar.YEAR, year + 1)
                                    }
                                    Toast.makeText(this, "$repeatCount event(s) added!", Toast.LENGTH_SHORT).show()
                                    displayEventsForDate(date)
                                    dialog.dismiss()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show()
                        Log.e("MainActivity", "Error parsing date", e)
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

    fun displayEventsForDate(date: String) {
        val container = findViewById<LinearLayout>(R.id.eventListLayout)
        container.removeAllViews()

        if (groupId == null || groupId!!.isEmpty()) {
            val noGroupView = TextView(this)
            noGroupView.text = "Join a group to view events"
            container.addView(noGroupView)
            return
        }

        database.child("groups").child(groupId!!).child("events")
            .orderByChild("date").equalTo(date)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.childrenCount == 0L) {
                        val noEventView = TextView(this@MainActivity)
                        noEventView.text = "No events for $date"
                        container.addView(noEventView)
                        return
                    }

                    for (eventSnapshot in snapshot.children) {
                        val eventMap = eventSnapshot.value as? Map<String, Any> ?: continue
                        val id = eventMap["id"] as? String ?: ""
                        val title = eventMap["title"] as? String ?: ""
                        val repeating = eventMap["repeating"] as? Boolean ?: false
                        val taskWorth = (eventMap["taskWorth"] as? Long)?.toInt() ?: 5
                        val eventDate = eventMap["date"] as? String ?: ""
                        val repeatGroupId = eventMap["repeatGroupId"] as? String
                        val participantsList = eventMap["participants"] as? List<Map<String, String>> ?: emptyList()
                        val participants = participantsList.map { User(it["username"] ?: "", it["email"] ?: "") }

                        val event = Event(id, title, participants, repeating, taskWorth, eventDate, repeatGroupId)
                        val eventView = TextView(this@MainActivity)
                        eventView.text = "📅 ${event.title}\n👥 ${event.participants.joinToString { it.username }}\n⭐ Task Worth: ${event.taskWorth}"
                        eventView.setPadding(8, 8, 8, 8)
                        eventView.textSize = 16f
                        eventView.setOnClickListener {
                            showDeleteEventDialog(event, date, eventSnapshot.key!!, groupId!!)
                        }
                        container.addView(eventView)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MainActivity, "Error loading events: ${error.message}", Toast.LENGTH_LONG).show()
                    Log.e("MainActivity", "Error loading events", error.toException())
                }
            })
    }

    override fun onBackPressed() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
        super.onBackPressed()
    }
}