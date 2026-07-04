package com.smartdd.app.presentation.qr.generate

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateQRScreen(
    onBack: () -> Unit,
    viewModel: GenerateQRViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(topBar = {
        TopAppBar(title = { Text("Generar QR") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } })
    }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (state.qrImageUrl != null) {
                Text("¡QR generado!", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                // Image(painter = rememberAsyncImagePainter(state.qrImageUrl), contentDescription = "QR", modifier = Modifier.size(250.dp))
                Spacer(Modifier.height(16.dp))
                Button(onClick = onBack) { Text("Listo") }
            } else {
                Text("Radio de validación: ${state.radius}m")
                Spacer(Modifier.height(8.dp))
                Slider(value = state.radius.toFloat(), onValueChange = { viewModel.setRadius(it.toInt()) }, valueRange = 10f..500f)
                Spacer(Modifier.height(24.dp))

                if (state.error != null) {
                    Text(state.error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                }

                Button(onClick = viewModel::generate, modifier = Modifier.fillMaxWidth(), enabled = !state.isLoading) {
                    if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    else Text("Generar QR en mi ubicación")
                }
            }
        }
    }
}
