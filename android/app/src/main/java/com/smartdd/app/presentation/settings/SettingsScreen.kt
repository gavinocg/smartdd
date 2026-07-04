package com.smartdd.app.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadConfig() }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Configuración") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } })
    }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Modo de respuesta por defecto", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            listOf("chat" to "Chat", "audio" to "Audio", "video" to "Video").forEach { (value, label) ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(selected = state.defaultMode == value, onClick = { viewModel.setDefaultMode(value) })
                    Text(label)
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Opciones habilitadas", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Chat", Modifier.weight(1f))
                Switch(checked = state.chatEnabled, onCheckedChange = viewModel::toggleChat)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Audio", Modifier.weight(1f))
                Switch(checked = state.audioEnabled, onCheckedChange = viewModel::toggleAudio)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Video", Modifier.weight(1f))
                Switch(checked = state.videoEnabled, onCheckedChange = viewModel::toggleVideo)
            }

            if (state.error != null) {
                Spacer(Modifier.height(8.dp))
                Text(state.error!!, color = MaterialTheme.colorScheme.error)
            }

            if (state.saved) {
                Spacer(Modifier.height(8.dp))
                Text("Configuración guardada", color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(24.dp))
            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth(), enabled = !state.isLoading) {
                if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                else Text("Guardar configuración")
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = viewModel::uploadLogs, modifier = Modifier.fillMaxWidth()) {
                Text("Enviar logs de depuración")
            }
        }
    }
}
