package com.smartdd.app.presentation.ring.sender

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartdd.app.data.remote.websocket.WebSocketClient

@Composable
fun WaitingScreen(
    sessionId: String,
    roomId: String,
    webSocketClient: WebSocketClient,
    onCancel: () -> Unit,
    onNavigateToAudioCall: (String, String) -> Unit,
    onNavigateToVideoCall: (String, String) -> Unit
) {
    var receiverResponded by remember { mutableStateOf(false) }

    LaunchedEffect(roomId) {
        webSocketClient.events.collect { msg ->
            if (msg.type == "respond" && msg.sessionId == sessionId) {
                val action = msg.action ?: "video"
                if (action == "audio") {
                    onNavigateToAudioCall(roomId, sessionId)
                } else {
                    onNavigateToVideoCall(roomId, sessionId)
                }
                receiverResponded = true
            }
        }
    }

    if (receiverResponded) return

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🔔", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text("Llamando...", fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Spacer(Modifier.height(8.dp))
            Text("Esperando respuesta", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))
            CircularProgressIndicator()
            Spacer(Modifier.height(48.dp))
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth(0.6f)) {
                Icon(Icons.Default.Cancel, null); Spacer(Modifier.width(8.dp))
                Text("Cancelar")
            }
        }
    }
}
