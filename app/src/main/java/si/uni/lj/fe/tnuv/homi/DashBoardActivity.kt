package si.uni.lj.fe.tnuv.homi

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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

    // Register Activity Result Launcher for EventActionActivity
    private val eventActionResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Refresh event lists when EventActionActivity returns with a reassignment
            displayTodaysEvents()
            displayUpcomingEvents()
        }
    }

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
                    updateGroupUI()
                }
                .addOnFailureListener { error ->
                    Log.e("DashboardActivity", "Failed to fetch groupId", error)
                    binding.groupIdButton.text = "Error loading group"
                    binding.groupIdButton.visibility = View.VISIBLE
                    binding.groupButtonsLayout.visibility = View.GONE
                    binding.groupMembersHeader.visibility = View.GONE
                    binding.userListLayout.visibility = View.GONE
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
                    startActivity(Intent(this, MainActivity::class.java))
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

    private fun updateGroupUI() {
        if (groupId == null || groupId!!.isEmpty()) {
            binding.groupIdButton.visibility = View.GONE
            binding.groupButtonsLayout.visibility = View.VISIBLE
            binding.groupMembersHeader.visibility = View.GONE
            binding.userListLayout.visibility = View.GONE

            binding.createGroupButton.setOnClickListener {
                showCreateGroupDialog(auth.currentUser!!.uid)
            }
            binding.joinGroupButton.setOnClickListener {
                showJoinGroupDialog(auth.currentUser!!.uid)
            }

            binding.noTodaysEventsText.text = "Join a group to view events"
            binding.noTodaysEventsText.visibility = View.VISIBLE
            binding.TodaysEventsContainer.visibility = View.GONE
            binding.noEventsText.text = "Join a group to view events"
            binding.noEventsText.visibility = View.VISIBLE
            binding.upcomingEventsContainer.visibility = View.GONE
        } else {
            binding.groupIdButton.text = "Group ID: $groupId"
            binding.groupIdButton.visibility = View.VISIBLE
            binding.groupButtonsLayout.visibility = View.GONE
            binding.groupMembersHeader.visibility = View.VISIBLE
            binding.userListLayout.visibility = View.VISIBLE

            // Set up click listener to copy groupId to clipboard
            binding.groupIdButton.setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Group ID", groupId)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Group ID copied to clipboard!", Toast.LENGTH_SHORT).show()
            }

            listUsersFromFirebase()
            displayTodaysEvents()
            displayUpcomingEvents()
        }
    }

    private fun listUsersFromFirebase() {
        if (isFinishing) return
        val container = binding.userListLayout
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
                                // Skip current user
                                if (uid != auth.currentUser?.uid) {
                                    users.add(user)
                                    val userView = TextView(this)
                                    userView.text = "👤 ${user.username}"
                                    userView.setPadding(8, 8, 8, 8)
                                    userView.textSize = 16f
                                    container.addView(userView)
                                }
                            }
                        } catch (e: DatabaseException) {
                            Log.e("DashboardActivity", "Error parsing user data for key: $uid", e)
                        }
                        completedQueries++
                        if (completedQueries == uids.size) {
                            Toast.makeText(this, "Loaded ${users.size} user(s)", Toast.LENGTH_SHORT).show()
                            Log.d("DashboardActivity", "Total users loaded: ${users.size}")
                        }
                    }.addOnFailureListener { error ->
                        Log.e("DashboardActivity", "Error fetching user $uid: ${error.message}", error)
                        completedQueries++
                        if (completedQueries == uids.size) {
                            Toast.makeText(this, "Loaded ${users.size} user(s)", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "Error reading users: ${error.message}", Toast.LENGTH_LONG).show()
                Log.e("DashboardActivity", "Error reading group members: ${error.message}", error)
            }
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
                                    updateGroupUI()
                                }
                            } else {
                                runOnUiThread {
                                    showError("Failed to update user: ${userTask.exception?.message}")
                                }
                                Log.e("DashboardActivity", "Failed to update user group", userTask.exception)
                            }
                        }
                } else {
                    runOnUiThread {
                        showError("Failed to create group: ${groupTask.exception?.message}")
                    }
                    Log.e("DashboardActivity", "Failed to create group", groupTask.exception)
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
                                            updateGroupUI()
                                        }
                                    } else {
                                        runOnUiThread {
                                            showError("Failed to update user group: ${userTask.exception?.message}")
                                        }
                                        Log.e("DashboardActivity", "Failed to update user group", userTask.exception)
                                    }
                                }
                        } else {
                            runOnUiThread {
                                showError("Failed to join group: ${memberTask.exception?.message}")
                            }
                            Log.e("DashboardActivity", "Failed to join group", memberTask.exception)
                        }
                    }
            } else {
                runOnUiThread {
                    showError("Group $groupId does not exist")
                }
                Log.e("DashboardActivity", "Group $groupId not found")
            }
        }
    }

    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
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
        val currentUserEmail = auth.currentUser?.email ?: return

        database.child("groups").child(groupId!!).child("events")
            .orderByChild("date").equalTo(currentDate)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val eventList = mutableListOf<Pair<Event, String>>()
                    for (eventSnapshot in snapshot.children) {
                        val eventMap = eventSnapshot.value as? Map<String, Any> ?: continue
                        val title = eventMap["title"] as? String ?: ""
                        val repeating = eventMap["repeating"] as? Boolean ?: false
                        val taskWorth = (eventMap["taskWorth"] as? Long)?.toInt() ?: 5
                        val eventDate = eventMap["date"] as? String ?: ""
                        val participantsList = eventMap["participants"] as? List<Map<String, String>> ?: emptyList()
                        val participants = participantsList.map { User(it["username"] ?: "", it["email"] ?: "") }

                        // Check if current user is a participant
                        if (participants.any { it.email == currentUserEmail }) {
                            val event = Event(title, participants, repeating, taskWorth, eventDate)
                            eventList.add(Pair(event, eventSnapshot.key ?: ""))
                        }
                    }

                    if (eventList.isEmpty()) {
                        binding.noTodaysEventsText.text = "No events for you today"
                        binding.noTodaysEventsText.visibility = View.VISIBLE
                        binding.TodaysEventsContainer.visibility = View.GONE
                        binding.TodaysEventsContainer.adapter = EventAdapter(emptyList()) { /* No-op */ }
                    } else {
                        binding.noTodaysEventsText.visibility = View.GONE
                        binding.TodaysEventsContainer.visibility = View.VISIBLE
                        binding.TodaysEventsContainer.apply {
                            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                            adapter = EventAdapter(eventList.map { it.first }) { event ->
                                val eventId = eventList.find { it.first == event }?.second ?: ""
                                Log.d("DashboardActivity", "Starting EventActionActivity with event: ${event.title}")
                                try {
                                    val intent = Intent(this@DashboardActivity, EventActionActivity::class.java).apply {
                                        putExtra("event", event)
                                        putExtra("eventId", eventId)
                                        putExtra("groupId", groupId)
                                    }
                                    eventActionResultLauncher.launch(intent)
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
        val currentUserEmail = auth.currentUser?.email ?: return

        database.child("groups").child(groupId!!).child("events")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val eventList = mutableListOf<Pair<Event, String>>()
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
                                // Check if current user is a participant
                                if (participants.any { it.email == currentUserEmail }) {
                                    val event = Event(title, participants, repeating, taskWorth, eventDate)
                                    eventList.add(Pair(event, eventSnapshot.key ?: ""))
                                }
                            }
                        } catch (e: Exception) {
                            Log.w("DashboardActivity", "Invalid date format for event: $title, date: $eventDate")
                        }
                    }

                    val sortedEvents = eventList.sortedBy { sdf.parse(it.first.date)?.time ?: 0 }

                    if (sortedEvents.isEmpty()) {
                        binding.noEventsText.text = "No upcoming events for you"
                        binding.noEventsText.visibility = View.VISIBLE
                        binding.upcomingEventsContainer.visibility = View.GONE
                        binding.upcomingEventsContainer.adapter = EventAdapter(emptyList()) { /* No-op */ }
                    } else {
                        binding.noEventsText.visibility = View.GONE
                        binding.upcomingEventsContainer.visibility = View.VISIBLE
                        binding.upcomingEventsContainer.apply {
                            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                            adapter = EventAdapter(sortedEvents.map { it.first }) { event ->
                                val eventId = sortedEvents.find { it.first == event }?.second ?: ""
                                Log.d("DashboardActivity", "Starting EventActionActivity with event: ${event.title}")
                                try {
                                    val intent = Intent(this@DashboardActivity, EventActionActivity::class.java).apply {
                                        putExtra("event", event)
                                        putExtra("eventId", eventId)
                                        putExtra("groupId", groupId)
                                    }
                                    eventActionResultLauncher.launch(intent)
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