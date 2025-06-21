package si.uni.lj.fe.tnuv.homi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EventAdapter(
    private val events: List<Event>,
    private val onEventClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.eventTitle)
        val participantsTextView: TextView = itemView.findViewById(R.id.eventParticipants)
        val dateTextView: TextView = itemView.findViewById(R.id.eventDate)
        val taskWorthTextView: TextView = itemView.findViewById(R.id.eventTaskWorth)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        holder.titleTextView.text = "📅 ${event.title}"
        holder.participantsTextView.text = "👥 ${event.participants.joinToString { it.username }}"
        holder.dateTextView.text = "📆 ${event.date}"
        holder.taskWorthTextView.text = "⭐ Task Worth: ${event.taskWorth}"

        // Set content description for accessibility
        holder.itemView.contentDescription = "Event: ${event.title}, Date: ${event.date}, " +
                "Participants: ${event.participants.joinToString { it.username }}, " +
                "Task Worth: ${event.taskWorth}, Repeating: ${if (event.repeating) "Yes" else "No"}"

        holder.itemView.setOnClickListener {
            onEventClick(event)
        }
    }

    override fun getItemCount(): Int = events.size
}