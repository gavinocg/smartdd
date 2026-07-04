package com.smartdd.app.presentation.call.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.smartdd.app.BuildConfig
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions
import org.jitsi.meet.sdk.JitsiMeetView
import java.net.URL

@Composable
fun VideoCallScreen(
    roomId: String,
    sessionId: String,
    onEndCall: () -> Unit
) {
    val context = LocalContext.current
    var isMuted by remember { mutableStateOf(false) }
    var isVideoOn by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                JitsiMeetView(ctx).apply {
                    val options = JitsiMeetConferenceOptions.Builder()
                        .setRoom(roomId)
                        .setServerURL(URL("https://${BuildConfig.JITSI_DOMAIN}"))
                        .setAudioMuted(false)
                        .setVideoMuted(false)
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
            modifier = Modifier.fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = Color(0x80000000)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconToggleButton(
                        checked = isMuted,
                        onCheckedChange = { checked ->
                            isMuted = checked
                        },
                        colors = IconButtonDefaults.iconToggleButtonColors(
                            containerColor = if (isMuted) Color(0xFFFF5252) else Color.Transparent,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = if (isMuted) "Activar micrófono" else "Silenciar"
                        )
                    }

                    FilledIconButton(
                        onClick = onEndCall,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color(0xFFFF1744),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "Colgar", modifier = Modifier.size(28.dp))
                    }

                    IconToggleButton(
                        checked = !isVideoOn,
                        onCheckedChange = { checked ->
                            isVideoOn = !checked
                        },
                        colors = IconButtonDefaults.iconToggleButtonColors(
                            containerColor = if (!isVideoOn) Color(0xFFFF5252) else Color.Transparent,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            if (isVideoOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = if (isVideoOn) "Apagar cámara" else "Encender cámara"
                        )
                    }
                }
            }
        }
    }
}
