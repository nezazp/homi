package si.uni.lj.fe.tnuv.homi

import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {
    private val messages = mutableListOf<Message>()
    private val currentUser = FirebaseAuth.getInstance().currentUser

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageContainer: LinearLayout = itemView.findViewById(R.id.messageContainer)
        val usernameText: TextView = itemView.findViewById(R.id.messageUsername)
        val contentText: TextView = itemView.findViewById(R.id.messageContent)
        val timestampText: TextView = itemView.findViewById(R.id.messageTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        val isCurrentUser = currentUser?.displayName?.let { it == message.username } ?: false

        // Set text
        holder.usernameText.text = message.username
        holder.contentText.text = message.content
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        holder.timestampText.text = dateFormat.format(Date(message.timestamp))

        // Set content description for accessibility
        holder.itemView.contentDescription = "Message from ${message.username} on ${
            dateFormat.format(Date(message.timestamp))
        }: ${message.content}"

        // Adjust layout based on whether it's the current user's message
        if (isCurrentUser) {
            holder.messageContainer.gravity = Gravity.END
            holder.contentText.setBackgroundResource(R.drawable.message_background_current_user)
            holder.contentText.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.black))
        } else {
            holder.messageContainer.gravity = Gravity.START
            holder.contentText.setBackgroundResource(R.drawable.message_background_current_user)
            holder.contentText.setTextColor(ContextCompat.getColor(holder.itemView.context, android.R.color.black))
        }
    }

    override fun getItemCount(): Int {
        Log.d("MessageAdapter", "Item count: ${messages.size}")
        return messages.size
    }

    fun updateMessages(newMessages: List<Message>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }
}