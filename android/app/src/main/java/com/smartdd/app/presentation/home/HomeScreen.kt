package com.smartdd.app.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToGenerate: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadUserInfo() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMARTDD") },
                actions = {
                    IconButton(onClick = onNavigateToProfile) { Icon(Icons.Default.Person, "Perfil") }
                    IconButton(onClick = onNavigateToSettings) { Icon(Icons.Default.Settings, "Config") }
                }
            )
        },
        floatingActionButton = {
            if (state.isReceptor) {
                FloatingActionButton(onClick = onNavigateToGenerate) {
                    Icon(Icons.Default.Add, "Generar QR")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(selected = !state.isReceptor, onClick = { viewModel.setRole(false) },
                    label = { Text("Visitante") }, leadingIcon = { Icon(Icons.Default.QrCodeScanner, null) })
                FilterChip(selected = state.isReceptor, onClick = { viewModel.setRole(true) },
                    label = { Text("Receptor") }, leadingIcon = { Icon(Icons.Default.Home, null) })
            }

            Spacer(Modifier.height(16.dp))

            if (state.isReceptor) {
                if (state.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                } else {
                    Text("Tus QRs (${state.currentQRCount}/${state.qrLimit})", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn {
                        items(state.qrList) { qr ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text("QR #${qr.uuid.take(8)}", fontWeight = FontWeight.Medium)
                                        Text("${qr.lat}, ${qr.lng}", style = MaterialTheme.typography.bodySmall)
                                        Text(if (qr.active) "Activo" else "Inactivo", color = if (qr.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                                    }
                                    IconButton(onClick = { viewModel.deleteQR(qr.uuid) }) { Icon(Icons.Default.Delete, "Eliminar") }
                                }
                            }
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button(onClick = onNavigateToScanner, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Icon(Icons.Default.QrCodeScanner, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Escanear QR")
                    }
                }
            }
        }
    }
}
