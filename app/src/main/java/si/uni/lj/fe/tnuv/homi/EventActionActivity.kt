package si.uni.lj.fe.tnuv.homi

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.Random

class EventActionActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_action)

        auth = FirebaseAuth.getInstance()
        database = Firebase.database.reference

        val event = intent.getSerializableExtra("event") as? Event
        val eventId = intent.getStringExtra("eventId")
        val groupId = intent.getStringExtra("groupId")
        val eventTextView = findViewById<TextView>(R.id.eventDetailText)
        eventTextView.text = if (event != null) {
            "Task: ${event.title}\nDate: ${event.date}\nParticipants: ${event.participants.joinToString { it.username }}\nTask Worth: ${event.taskWorth}\nRepeating: ${if (event.repeating) "Yes" else "No"}"
        } else {
            "No event data available"
        }
        eventTextView.contentDescription = eventTextView.text

        findViewById<Button>(R.id.completeButton).setOnClickListener {
            if (event == null || eventId == null || groupId == null) {
                Toast.makeText(this, "Error: Event or group data missing", Toast.LENGTH_LONG).show()
                Log.e("EventActionActivity", "Missing event, eventId, or groupId")
                setResult(Activity.RESULT_CANCELED)
                finish()
                return@setOnClickListener
            }

            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(this, "Error: User not authenticated", Toast.LENGTH_LONG).show()
                Log.e("EventActionActivity", "User not authenticated")
                setResult(Activity.RESULT_CANCELED)
                finish()
                return@setOnClickListener
            }

            // Fetch current user's data to get current points
            database.child("users").child(currentUser.uid).get()
                .addOnSuccessListener { snapshot ->
                    val user = snapshot.getValue(User::class.java)
                    if (user == null) {
                        Toast.makeText(this, "Error: User data not found", Toast.LENGTH_LONG).show()
                        Log.e("EventActionActivity", "User data not found for UID: ${currentUser.uid}")
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                        return@addOnSuccessListener
                    }

                    // Calculate new points
                    val currentPoints = user.points ?: 0
                    val newPoints = currentPoints + event.taskWorth

                    // Update user's points in Firebase
                    database.child("users").child(currentUser.uid).child("points").setValue(newPoints)
                        .addOnSuccessListener {
                            // Delete the event from Firebase
                            database.child("groups").child(groupId).child("events").child(eventId).removeValue()
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Task completed! Added ${event.taskWorth} points. Total: $newPoints. Event deleted.", Toast.LENGTH_SHORT).show()
                                    Log.d("EventActionActivity", "Points updated for user ${currentUser.uid}: $newPoints, Event $eventId deleted")
                                    setResult(Activity.RESULT_OK)
                                    finish()
                                }
                                .addOnFailureListener { error ->
                                    Toast.makeText(this, "Failed to delete event: ${error.message}", Toast.LENGTH_LONG).show()
                                    Log.e("EventActionActivity", "Failed to delete event $eventId", error)
                                    setResult(Activity.RESULT_OK) // Still return OK since points were updated
                                    finish()
                                }
                        }
                        .addOnFailureListener { error ->
                            Toast.makeText(this, "Failed to update points: ${error.message}", Toast.LENGTH_LONG).show()
                            Log.e("EventActionActivity", "Failed to update points for user ${currentUser.uid}", error)
                            setResult(Activity.RESULT_CANCELED)
                            finish()
                        }
                }
                .addOnFailureListener { error ->
                    Toast.makeText(this, "Error fetching user data: ${error.message}", Toast.LENGTH_LONG).show()
                    Log.e("EventActionActivity", "Error fetching user data for UID: ${currentUser.uid}", error)
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
        }

        findViewById<Button>(R.id.cantButton).setOnClickListener {
            if (event == null || eventId == null || groupId == null) {
                Toast.makeText(this, "Error: Event or group data missing", Toast.LENGTH_LONG).show()
                Log.e("EventActionActivity", "Missing event, eventId, or groupId")
                setResult(Activity.RESULT_CANCELED)
                finish()
                return@setOnClickListener
            }

            val currentUserEmail = auth.currentUser?.email
            if (currentUserEmail == null) {
                Toast.makeText(this, "Error: User not authenticated", Toast.LENGTH_LONG).show()
                Log.e("EventActionActivity", "User not authenticated")
                setResult(Activity.RESULT_CANCELED)
                finish()
                return@setOnClickListener
            }

            // Fetch group members
            database.child("groups").child(groupId).child("members").get()
                .addOnSuccessListener { snapshot ->
                    if (!snapshot.exists() || snapshot.childrenCount <= 1) {
                        Toast.makeText(this, "No other users in the group to reassign to", Toast.LENGTH_LONG).show()
                        Log.w("EventActionActivity", "No other group members found")
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                        return@addOnSuccessListener
                    }

                    val uids = snapshot.children.mapNotNull { it.key }
                        .filter { it != auth.currentUser?.uid } // Exclude current user
                    if (uids.isEmpty()) {
                        Toast.makeText(this, "No other users in the group to reassign to", Toast.LENGTH_LONG).show()
                        Log.w("EventActionActivity", "No eligible users for reassignment")
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                        return@addOnSuccessListener
                    }

                    // Fetch user details for a random member
                    val randomUid = uids[Random().nextInt(uids.size)]
                    database.child("users").child(randomUid).get()
                        .addOnSuccessListener { userSnapshot ->
                            val user = userSnapshot.getValue(User::class.java)
                            if (user == null || user.username.isEmpty() || user.email.isEmpty()) {
                                Toast.makeText(this, "Error: Invalid user data", Toast.LENGTH_LONG).show()
                                Log.e("EventActionActivity", "Invalid user data for UID: $randomUid")
                                setResult(Activity.RESULT_CANCELED)
                                finish()
                                return@addOnSuccessListener
                            }

                            // Update event participants
                            val newParticipants = event.participants
                                .filter { it.email != currentUserEmail } // Remove current user
                                .toMutableList()
                            newParticipants.add(user) // Add new user

                            val updatedEvent = mapOf(
                                "title" to event.title,
                                "participants" to newParticipants.map { mapOf("username" to it.username, "email" to it.email) },
                                "repeating" to event.repeating,
                                "taskWorth" to event.taskWorth,
                                "date" to event.date
                            )

                            database.child("groups").child(groupId).child("events").child(eventId)
                                .setValue(updatedEvent)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Task reassigned to ${user.username}", Toast.LENGTH_LONG).show()
                                    Log.d("EventActionActivity", "Event $eventId reassigned to ${user.username}")
                                    setResult(Activity.RESULT_OK)
                                    finish()
                                }
                                .addOnFailureListener { error ->
                                    Toast.makeText(this, "Failed to reassign event: ${error.message}", Toast.LENGTH_LONG).show()
                                    Log.e("EventActionActivity", "Failed to reassign event $eventId", error)
                                    setResult(Activity.RESULT_CANCELED)
                                    finish()
                                }
                        }
                        .addOnFailureListener { error ->
                            Toast.makeText(this, "Error fetching user: ${error.message}", Toast.LENGTH_LONG).show()
                            Log.e("EventActionActivity", "Error fetching user $randomUid: ${error.message}", error)
                            setResult(Activity.RESULT_CANCELED)
                            finish()
                        }
                }
                .addOnFailureListener { error ->
                    Toast.makeText(this, "Error fetching group members: ${error.message}", Toast.LENGTH_LONG).show()
                    Log.e("EventActionActivity", "Error fetching group members: ${error.message}", error)
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
        }

        val deleteButton = findViewById<Button>(R.id.deleteButton)
        deleteButton.setOnClickListener {
            if (event == null || eventId == null || groupId == null) {
                Toast.makeText(this, "Error: Event or group data missing", Toast.LENGTH_LONG).show()
                Log.e("EventActionActivity", "Missing event, eventId, or groupId")
                return@setOnClickListener
            }
            showDeleteEventDialog(event, event.date, eventId, groupId)
        }
    }

    private fun showDeleteEventDialog(event: Event, date: String, eventId: String, groupId: String) {
        // Implementation of showDeleteEventDialog (not provided in the original code)
        // Add dialog to confirm deletion and remove event from Firebase
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete the task '${event.title}' on $date?")
            .setPositiveButton("Delete") { _, _ ->
                database.child("groups").child(groupId).child("events").child(eventId).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Event deleted successfully", Toast.LENGTH_SHORT).show()
                        Log.d("EventActionActivity", "Event $eventId deleted")
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                    .addOnFailureListener { error ->
                        Toast.makeText(this, "Failed to delete event: ${error.message}", Toast.LENGTH_LONG).show()
                        Log.e("EventActionActivity", "Failed to delete event $eventId", error)
                        setResult(Activity.RESULT_CANCELED)
                        finish()
                    }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        alertDialog.show()
    }
}