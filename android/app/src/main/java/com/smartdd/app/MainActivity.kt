package com.smartdd.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.smartdd.app.navigation.SmartDDNavGraph
import com.smartdd.app.presentation.theme.SmartDDTheme
import dagger.hilt.android.AndroidEntryPoint

data class NotificationPayload(
    val navigate: String? = null,
    val sessionId: String? = null,
    val roomId: String? = null,
    val emisorName: String? = null
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val notificationPayload = NotificationPayload(
            navigate = intent?.getStringExtra("navigate"),
            sessionId = intent?.getStringExtra("sessionId"),
            roomId = intent?.getStringExtra("roomId"),
            emisorName = intent?.getStringExtra("emisorName")
        )

        setContent {
            SmartDDTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SmartDDNavGraph(notificationPayload = notificationPayload)
                }
            }
        }

        intent?.removeExtra("navigate")
        intent?.removeExtra("sessionId")
        intent?.removeExtra("roomId")
        intent?.removeExtra("emisorName")
    }
}
