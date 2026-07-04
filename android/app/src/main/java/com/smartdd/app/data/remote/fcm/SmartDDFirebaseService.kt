package com.smartdd.app.data.remote.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.smartdd.app.MainActivity
import com.smartdd.app.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SmartDDFirebaseService : FirebaseMessagingService() {

    @Inject lateinit var deviceRegistrationHelper: DeviceRegistrationHelper

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNewToken(token: String) {
        Log.d("FCM", "Nuevo token: $token")
        deviceRegistrationHelper.registerToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d("FCM", "Mensaje recibido: ${message.from}")

        message.data.let { data ->
            when (data["type"]) {
                "incoming_ring" -> {
                    val sessionId = data["sessionId"] ?: return
                    val roomId = data["roomId"] ?: return
                    val emisorName = data["emisorName"]
                    showRingNotification(sessionId, roomId, emisorName)
                }
                "chat" -> {
                    val sessionId = data["sessionId"] ?: return
                    val msg = data["message"] ?: return
                    showChatNotification(sessionId, msg, data["senderName"])
                }
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_RING, "Timbre", NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Notificaciones de timbre" }
        val channelChat = NotificationChannel(
            CHANNEL_CHAT, "Chat", NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Mensajes de chat" }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
        manager.createNotificationChannel(channelChat)
    }

    private fun showRingNotification(sessionId: String, roomId: String, emisorName: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate", "incoming_call")
            putExtra("sessionId", sessionId)
            putExtra("roomId", roomId)
            putExtra("emisorName", emisorName)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_RING)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("🔔 Alguien toca el timbre")
            .setContentText(emisorName ?: "Alguien está en la puerta")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(RING_NOTIFICATION_ID, notification)
    }

    private fun showChatNotification(sessionId: String, message: String, senderName: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate", "chat")
            putExtra("sessionId", sessionId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_CHAT)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(senderName ?: "Mensaje")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(CHAT_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val CHANNEL_RING = "ring_notifications"
        private const val CHANNEL_CHAT = "chat_notifications"
        const val RING_NOTIFICATION_ID = 1001
        const val CHAT_NOTIFICATION_ID = 1002
    }
}
