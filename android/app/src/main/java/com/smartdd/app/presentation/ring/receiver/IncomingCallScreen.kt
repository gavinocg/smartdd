package com.smartdd.app.presentation.ring.receiver

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomingCallScreen(
    sessionId: String,
    roomId: String,
    emisorName: String?,
    onRespond: (action: String, mode: String?) -> Unit,
    onEndCall: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToAudioCall: (String, String) -> Unit,
    onNavigateToVideoCall: (String, String) -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🔔", fontSize = 80.sp)
            Spacer(Modifier.height(16.dp))
            Text("Llamada entrante", fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
            Text(emisorName ?: "Alguien está en la puerta", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(24.dp))

            Surface(Modifier.fillMaxWidth().height(200.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("📹 Video en vivo", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Responder", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = {
                    onRespond("chat", null)
                    onNavigateToChat(sessionId)
                }) { Text("💬 Chat") }

                Button(onClick = {
                    onRespond("accept", "audio")
                    onNavigateToAudioCall(roomId, sessionId)
                }) { Text("🎤 Audio") }

                Button(onClick = {
                    onRespond("accept", "video")
                    onNavigateToVideoCall(roomId, sessionId)
                }) { Text("📹 Video") }
            }

            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = {
                onRespond("reject", null)
                onEndCall()
            }) {
                Icon(Icons.Default.Close, null); Spacer(Modifier.width(8.dp))
                Text("Rechazar")
            }
        }
    }
}
