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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import si.uni.lj.fe.tnuv.homi.databinding.ActivityMessageBoardBinding
import android.util.Log

class MessageBoardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMessageBoardBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageReceiver: BroadcastReceiver
    private var groupId: String? = null

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

        auth = FirebaseAuth.getInstance()
        database = Firebase.database.reference

        // Check if user is authenticated
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Get user's groupId
        database.child("users").child(currentUser.uid).child("groupId").get()
            .addOnSuccessListener { snapshot ->
                groupId = snapshot.value as? String
                if (groupId == null || groupId!!.isEmpty()) {
                    Toast.makeText(this, "Please join a group to view messages", Toast.LENGTH_LONG).show()
                    binding.noMessagesText.text = "Join a group to view messages"
                    binding.noMessagesText.visibility = View.VISIBLE
                    binding.messageRecyclerView.visibility = View.GONE
                    binding.messageInput.visibility = View.GONE
                    binding.postMessageButton.visibility = View.GONE
                } else {
                    // Set up RecyclerView
                    messageAdapter = MessageAdapter()
                    binding.messageRecyclerView.apply {
                        layoutManager = LinearLayoutManager(this@MessageBoardActivity)
                        adapter = messageAdapter
                    }
                    // Load messages for the group
                    loadMessages()
                }
            }
            .addOnFailureListener { error ->
                Log.e("MessageBoardActivity", "Failed to fetch groupId", error)
                Toast.makeText(this, "Error fetching group: ${error.message}", Toast.LENGTH_LONG).show()
                binding.noMessagesText.text = "Error loading group"
                binding.noMessagesText.visibility = View.VISIBLE
                binding.messageRecyclerView.visibility = View.GONE
                binding.messageInput.visibility = View.GONE
                binding.postMessageButton.visibility = View.GONE
            }

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

        // Register BroadcastReceiver for FCM messages
        messageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val title = intent.getStringExtra("title") ?: "System"
                val body = intent.getStringExtra("body") ?: return
                Log.d("MessageBoardActivity", "Received FCM message: $title, $body")
                if (groupId != null && groupId!!.isNotEmpty()) {
                    val message = Message(
                        username = title,
                        content = body,
                        timestamp = System.currentTimeMillis()
                    )
                    database.child("groups").child(groupId!!).child("messages").push().setValue(message)
                }
            }
        }
        val intentFilter = IntentFilter("NEW_MESSAGE")
        ContextCompat.registerReceiver(
            this,
            messageReceiver,
            intentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // Post message
        binding.postMessageButton.setOnClickListener {
            if (groupId == null || groupId!!.isEmpty()) {
                Toast.makeText(this, "Please join a group to post messages", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val content = binding.messageInput.text.toString().trim()
            if (content.isEmpty()) {
                Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val message = Message(
                username = currentUser.displayName ?: "Anonymous",
                content = content,
                timestamp = System.currentTimeMillis()
            )
            Log.d("MessageBoardActivity", "Posting message: $message")
            database.child("groups").child(groupId!!).child("messages").push().setValue(message)
                .addOnSuccessListener {
                    binding.messageInput.text.clear()
                    Toast.makeText(this, "Message posted", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { error ->
                    Log.e("MessageBoardActivity", "Failed to post message", error)
                    Toast.makeText(this, "Failed to post message: ${error.message}", Toast.LENGTH_LONG).show()
                }
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
        try {
            unregisterReceiver(messageReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w("MessageBoardActivity", "Receiver not registered", e)
        }
    }

    private fun loadMessages() {
        if (groupId == null || groupId!!.isEmpty()) {
            return
        }
        database.child("groups").child(groupId!!).child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messages = mutableListOf<Message>()
                    for (messageSnapshot in snapshot.children) {
                        val message = messageSnapshot.getValue(Message::class.java)
                        if (message != null) {
                            messages.add(message)
                        }
                    }
                    messageAdapter.updateMessages(messages.sortedBy { it.timestamp })
                    updateMessageVisibility()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MessageBoardActivity", "Error loading messages", error.toException())
                    Toast.makeText(this@MessageBoardActivity, "Error loading messages: ${error.message}", Toast.LENGTH_LONG).show()
                    binding.noMessagesText.text = "Error loading messages"
                    binding.noMessagesText.visibility = View.VISIBLE
                    binding.messageRecyclerView.visibility = View.GONE
                }
            })
    }

    private fun updateMessageVisibility() {
        val messages = messageAdapter.itemCount
        binding.noMessagesText.visibility = if (messages == 0) View.VISIBLE else View.GONE
        binding.messageRecyclerView.visibility = if (messages == 0) View.GONE else View.VISIBLE
    }
}