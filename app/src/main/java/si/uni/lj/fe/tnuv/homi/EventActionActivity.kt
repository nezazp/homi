package si.uni.lj.fe.tnuv.homi



import android.os.Bundle

import android.widget.Button

import android.widget.TextView

import androidx.appcompat.app.AppCompatActivity



class EventActionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_event_action)



        val event = intent.getSerializableExtra("event") as? Event

        val eventTextView = findViewById<TextView>(R.id.eventDetailText)

        eventTextView.text = if (event != null) {

            "Event: ${event.title}\nDate: ${event.date}\nParticipants: ${event.participants.joinToString { it.username }}\nTask Worth: ${event.taskWorth}\nRepeating: ${if (event.repeating) "Yes" else "No"}"

        } else {

            "No event data available"

        }

        eventTextView.contentDescription = eventTextView.text



        findViewById<Button>(R.id.completeButton).setOnClickListener {

            // Handle completion logic here

            finish()

        }

        findViewById<Button>(R.id.cantButton).setOnClickListener {

            // Handle "I can't" logic here

            finish()

        }

    }

}