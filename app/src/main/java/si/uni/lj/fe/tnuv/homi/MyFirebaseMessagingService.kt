package si.uni.lj.fe.tnuv.homi

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.util.Log

class MyFirebaseMessagingService : FirebaseMessagingService() {
    companion object {
        private const val TAG = "MyFirebaseMsgService"
        private const val CHANNEL_ID = "message_channel"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message received: ${remoteMessage.data}")

        // Handle data payload
        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"] ?: "System"
            val body = remoteMessage.data["body"]
            storeAndDisplayMessage(title, body)
            sendNotification(title, body)
        }

        // Handle notification payload (for background notifications)
        remoteMessage.notification?.let {
            val title = it.title ?: "System"
            val body = it.body
            storeAndDisplayMessage(title, body)
            sendNotification(title, body)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendNotification(title: String?, messageBody: String?) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Messages", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with your app's icon
            .setContentTitle(title)
            .setContentText(messageBody)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(0, builder.build())
    }

    private fun storeAndDisplayMessage(title: String?, body: String?) {
        if (title != null && body != null) {
            val message = Message(
                username = title,
                content = body,
                timestamp = System.currentTimeMillis()
            )
            MessageStore.addMessage(message)
            val intent = Intent("NEW_MESSAGE")
            intent.putExtra("title", title)
            intent.putExtra("body", body)
            sendBroadcast(intent)
        }
    }

    private fun sendRegistrationToServer(token: String) {
        // Implement server token registration if needed
    }
}