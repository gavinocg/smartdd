package com.smartdd.app.presentation.qr.generate

import android.Manifest
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices
import java.util.Base64

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun GenerateQRScreen(
    onBack: () -> Unit,
    viewModel: GenerateQRViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (!locationPermission.status.isGranted) {
            locationPermission.launchPermissionRequest()
        } else {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            fusedClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    viewModel.setLocation(location.latitude, location.longitude)
                } else {
                    fusedClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener { loc ->
                            if (loc != null) viewModel.setLocation(loc.latitude, loc.longitude)
                        }
                }
            }
        }
    }

    val qrBitmap = remember(state.qrImageUrl) {
        state.qrImageUrl?.let { url ->
            try {
                val base64 = url.substringAfter("base64,")
                val bytes = Base64.getDecoder().decode(base64)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (_: Exception) { null }
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Generar QR") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } })
    }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (state.qrImageUrl != null && qrBitmap != null) {
                Text("¡QR generado!", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR",
                    modifier = Modifier.size(250.dp)
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { viewModel.shareQR(context) }) {
                        Icon(Icons.Default.Share, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Compartir")
                    }
                    OutlinedButton(onClick = { viewModel.exportPDF(context) }) {
                        Icon(Icons.Default.Download, null)
                        Spacer(Modifier.width(4.dp))
                        Text("PDF")
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = onBack) { Text("Listo") }
            } else {
                if (!locationPermission.status.isGranted) {
                    Text("Permiso de ubicación requerido para generar QR")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { locationPermission.launchPermissionRequest() }) { Text("Conceder permiso") }
                } else {
                    Text("Radio de validación: ${state.radius}m")
                    Spacer(Modifier.height(8.dp))
                    Slider(value = state.radius.toFloat(), onValueChange = { viewModel.setRadius(it.toInt()) }, valueRange = 10f..500f)
                    Spacer(Modifier.height(24.dp))

                    if (state.error != null) {
                        Text(state.error!!, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                    }

                    Button(onClick = viewModel::generate, modifier = Modifier.fillMaxWidth(), enabled = !state.isLoading && state.lat != null) {
                        if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        else Text(if (state.lat == null) "Obteniendo ubicación..." else "Generar QR en mi ubicación")
                    }
                }
            }
        }
    }
}
