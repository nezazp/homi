package si.uni.lj.fe.tnuv.homi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(private val messages: List<Message>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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
        holder.usernameText.text = message.username
        holder.contentText.text = message.content
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        holder.timestampText.text = dateFormat.format(Date(message.timestamp))

        holder.itemView.contentDescription = "Message from ${message.username} on ${dateFormat.format(Date(message.timestamp))}: ${message.content}"
    }

    override fun getItemCount(): Int = messages.size
}