package si.uni.lj.fe.tnuv.homi

import android.app.Activity.RESULT_OK
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

fun AppCompatActivity.showDeleteEventDialog(event: Event, date: String, eventId: String, groupId: String) {
    val dialog = Dialog(this)
    dialog.setContentView(R.layout.dialog_delete_event)

    // Set dialog title and message
    val titleView = dialog.findViewById<TextView>(R.id.delete_dialog_title)
    val messageView = dialog.findViewById<TextView>(R.id.delete_dialog_message)
    titleView.text = "Delete Event"
    messageView.text = "Do you want to delete '${event.title}' on $date, or all instances of this repeating event?"

    // Initialize buttons
    val deleteSingleButton = dialog.findViewById<Button>(R.id.delete_single_button)
    val deleteAllButton = dialog.findViewById<Button>(R.id.delete_all_button)
    val cancelButton = dialog.findViewById<Button>(R.id.cancel_button)

    // Show "Delete All Instances" button only for repeating events with a repeatGroupId
    if (event.repeating && event.repeatGroupId != null) {
        deleteAllButton.visibility = View.VISIBLE
    } else {
        deleteAllButton.visibility = View.GONE
    }

    val database: DatabaseReference = Firebase.database.reference

    // Delete single event
    deleteSingleButton.setOnClickListener {
        database.child("groups").child(groupId).child("events").child(eventId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show()
                if (this is MainActivity) {
                    displayEventsForDate(date)
                } else if (this is EventActionActivity) {
                    setResult(RESULT_OK)
                    finish()
                }
                dialog.dismiss()
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "Failed to delete event: ${error.message}", Toast.LENGTH_LONG).show()
                Log.e("DeleteEventDialog", "Failed to delete event", error)
                dialog.dismiss()
            }
    }

    // Delete all instances of a repeating event
    deleteAllButton.setOnClickListener {
        database.child("groups").child(groupId).child("events")
            .orderByChild("repeatGroupId").equalTo(event.repeatGroupId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val updates = hashMapOf<String, Any?>()
                    for (eventSnapshot in snapshot.children) {
                        updates["${eventSnapshot.key}"] = null
                    }
                    database.child("groups").child(groupId).child("events").updateChildren(updates)
                        .addOnSuccessListener {
                            Toast.makeText(this@showDeleteEventDialog, "All instances deleted", Toast.LENGTH_SHORT).show()
                            if (this@showDeleteEventDialog is MainActivity) {
                                displayEventsForDate(date)
                            } else if (this@showDeleteEventDialog is EventActionActivity) {
                                setResult(RESULT_OK)
                                finish()
                            }
                            dialog.dismiss()
                        }
                        .addOnFailureListener { error ->
                            Toast.makeText(this@showDeleteEventDialog, "Failed to delete events: ${error.message}", Toast.LENGTH_LONG).show()
                            Log.e("DeleteEventDialog", "Failed to delete all events", error)
                            dialog.dismiss()
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@showDeleteEventDialog, "Error loading events: ${error.message}", Toast.LENGTH_LONG).show()
                    Log.e("DeleteEventDialog", "Error loading events for deletion", error.toException())
                    dialog.dismiss()
                }
            })
    }

    // Cancel button
    cancelButton.setOnClickListener {
        dialog.dismiss()
    }

    dialog.show()
}