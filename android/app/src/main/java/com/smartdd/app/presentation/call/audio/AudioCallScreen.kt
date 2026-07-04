package com.smartdd.app.presentation.call.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.smartdd.app.BuildConfig
import kotlinx.coroutines.delay
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions
import org.jitsi.meet.sdk.JitsiMeetView
import java.net.URL

@Composable
fun AudioCallScreen(
    roomId: String,
    sessionId: String,
    callerName: String?,
    onEndCall: () -> Unit
) {
    val context = LocalContext.current
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            elapsedSeconds++
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1A1A2E))) {
        AndroidView(
            modifier = Modifier.size(1.dp),
            factory = { ctx ->
                JitsiMeetView(ctx).apply {
                    val options = JitsiMeetConferenceOptions.Builder()
                        .setRoom(roomId)
                        .setServerURL(URL("https://${BuildConfig.JITSI_DOMAIN}"))
                        .setAudioMuted(false)
                        .setVideoMuted(true)
                        .setFeatureFlag("chat.enabled", false)
                        .setFeatureFlag("invite.enabled", false)
                        .setFeatureFlag("kickout.enabled", false)
                        .setFeatureFlag("prejoinpage.enabled", false)
                        .setFeatureFlag("welcomepage.enabled", false)
                        .build()
                    join(options)
                }
            },
            onRelease = { it.dispose() }
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
                tint = Color(0xFF4CAF50)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                callerName ?: "Llamada",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )

            Spacer(Modifier.height(8.dp))

            val minutes = elapsedSeconds / 60
            val seconds = elapsedSeconds % 60
            Text(
                String.format("%02d:%02d", minutes, seconds),
                color = Color.Gray,
                fontSize = 18.sp
            )

            Spacer(Modifier.height(48.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconToggleButton(
                    checked = isMuted,
                    onCheckedChange = { checked ->
                        isMuted = checked
                    },
                    modifier = Modifier.size(64.dp),
                    colors = IconButtonDefaults.iconToggleButtonColors(
                        containerColor = if (isMuted) Color(0xFFFF5252) else Color(0xFF2D2D44),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (isMuted) "Activar micrófono" else "Silenciar",
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(
                    onClick = onEndCall,
                    modifier = Modifier.size(72.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color(0xFFFF1744),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.Default.CallEnd,
                        contentDescription = "Colgar",
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconToggleButton(
                    checked = isSpeakerOn,
                    onCheckedChange = { checked ->
                        isSpeakerOn = checked
                    },
                    modifier = Modifier.size(64.dp),
                    colors = IconButtonDefaults.iconToggleButtonColors(
                        containerColor = if (isSpeakerOn) Color(0xFF4CAF50) else Color(0xFF2D2D44),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = if (isSpeakerOn) "Altavoz activado" else "Altavoz desactivado",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
