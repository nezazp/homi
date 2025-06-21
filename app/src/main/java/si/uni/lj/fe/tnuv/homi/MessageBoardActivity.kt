package si.uni.lj.fe.tnuv.homi

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import si.uni.lj.fe.tnuv.homi.databinding.ActivityMessageBoardBinding

class MessageBoardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMessageBoardBinding
    private val currentUser = "User1" // Replace with authentication later

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageBoardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up RecyclerView
        binding.messageRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MessageBoardActivity)
            adapter = MessageAdapter(MessageStore.getMessages())
        }

        // Update UI based on messages
        updateMessageVisibility()

        // Post message
        binding.postMessageButton.setOnClickListener {
            val content = binding.messageInput.text.toString().trim()
            if (content.isEmpty()) {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val message = Message(
                username = currentUser,
                content = content,
                timestamp = System.currentTimeMillis()
            )
            MessageStore.addMessage(message)

            // Refresh RecyclerView
            binding.messageRecyclerView.adapter = MessageAdapter(MessageStore.getMessages())
            updateMessageVisibility()

            binding.messageInput.text.clear()
            Toast.makeText(this, "Message posted", Toast.LENGTH_SHORT).show()
        }

        // Bottom navigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_calendar -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_messages -> true
                R.id.nav_shopping -> {
                    startActivity(Intent(this, ShoppingListActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
        binding.bottomNavigation.selectedItemId = R.id.nav_messages
    }

    private fun updateMessageVisibility() {
        val messages = MessageStore.getMessages()
        binding.noMessagesText.visibility = if (messages.isEmpty()) View.VISIBLE else View.GONE
        binding.messageRecyclerView.visibility = if (messages.isEmpty()) View.GONE else View.VISIBLE
    }
}