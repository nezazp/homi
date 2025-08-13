package si.uni.lj.fe.tnuv.homi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.FirebaseApp
import si.uni.lj.fe.tnuv.homi.databinding.ActivityMessageBoardBinding
import android.util.Log

class MessageBoardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMessageBoardBinding
    private val currentUser = "User1" // Replace with authentication later
    private lateinit var messageReceiver: BroadcastReceiver
    private lateinit var messageAdapter: MessageAdapter

    // Permission launcher for POST_NOTIFICATIONS
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("MessageBoardActivity", "Notification permission granted")
        } else {
            Log.d("MessageBoardActivity", "Notification permission denied")
            Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        binding = ActivityMessageBoardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                Log.d("MessageBoardActivity", "Notification permission already granted")
            }
        }

        // Set up RecyclerView
        messageAdapter = MessageAdapter()
        binding.messageRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MessageBoardActivity)
            adapter = messageAdapter
        }

        // Load initial messages
        loadMessages()

        // Register BroadcastReceiver for FCM messages
        messageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val title = intent.getStringExtra("title") ?: "System"
                val body = intent.getStringExtra("body") ?: return
                Log.d("MessageBoardActivity", "Received FCM message: $title, $body")
                addMessage(title, body)
            }
        }
        val intentFilter = IntentFilter("NEW_MESSAGE")
        ContextCompat.registerReceiver(
            this,
            messageReceiver,
            intentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

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
            Log.d("MessageBoardActivity", "Adding message: $message")
            MessageStore.addMessage(message)
            messageAdapter.updateMessages(MessageStore.getMessages())
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

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(messageReceiver)
    }

    private fun addMessage(title: String, body: String) {
        val message = Message(
            username = title,
            content = body,
            timestamp = System.currentTimeMillis()
        )
        Log.d("MessageBoardActivity", "Adding FCM message: $message")
        MessageStore.addMessage(message)
        messageAdapter.updateMessages(MessageStore.getMessages())
        updateMessageVisibility()
    }

    private fun loadMessages() {
        messageAdapter.updateMessages(MessageStore.getMessages())
        updateMessageVisibility()
    }

    private fun updateMessageVisibility() {
        val messages = MessageStore.getMessages()
        binding.noMessagesText.visibility = if (messages.isEmpty()) View.VISIBLE else View.GONE
        binding.messageRecyclerView.visibility = if (messages.isEmpty()) View.GONE else View.VISIBLE
    }
}