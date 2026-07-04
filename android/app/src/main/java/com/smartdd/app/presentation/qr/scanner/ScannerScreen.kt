package com.smartdd.app.presentation.qr.scanner

import android.Manifest
import android.os.Handler
import android.os.Looper
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.smartdd.app.data.local.DebugLog
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onQrDetected: (String) -> Unit,
    onBack: () -> Unit
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    var torchEnabled by remember { mutableStateOf(false) }
    var qrScanned by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val analyzer = remember { QRCodeAnalyzer { rawValue ->
        if (!qrScanned) {
            qrScanned = true
            val uuid = rawValue.substringAfterLast("/")
            DebugLog.d("Scanner", "QR raw: $rawValue -> uuid: $uuid")
            mainHandler.post { onQrDetected(uuid) }
        }
    }}

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Escanear QR") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
            actions = {
                IconButton(onClick = { torchEnabled = !torchEnabled }) {
                    Icon(if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff, "Flash")
                }
            })
    }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (cameraPermission.status.isGranted) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        try {
                            val previewView = PreviewView(ctx)
                            DebugLog.i("Scanner", "PreviewView created")
                            val cameraProvider = ProcessCameraProvider.getInstance(ctx).get()
                            DebugLog.i("Scanner", "CameraProvider obtained")
                            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setTargetResolution(Size(1280, 720))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build().also { it.setAnalyzer(Executors.newSingleThreadExecutor(), analyzer) }
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
                            DebugLog.i("Scanner", "Camera bound to lifecycle")
                            previewView
                        } catch (e: Exception) {
                            DebugLog.e("Scanner", "Camera init failed", e)
                            PreviewView(ctx)
                        }
                    }
                )

                Surface(
                    modifier = Modifier.fillMaxWidth().padding(32.dp).align(Alignment.BottomCenter),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ) {
                    Text("Enfoca el código QR", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Permiso de cámara requerido", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { cameraPermission.launchPermissionRequest() }) { Text("Conceder permiso") }
                    }
                }
            }

            Box(Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
                Surface(Modifier.size(250.dp), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("□", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.displayLarge)
                    }
                }
            }
        }
    }
}

class QRCodeAnalyzer(private val onQrDetected: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient(BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        try {
            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(inputImage).addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    barcode.rawValue?.let { uuid ->
                        DebugLog.d("QRCodeAnalyzer", "QR detected: $uuid")
                        onQrDetected.invoke(uuid)
                        imageProxy.close()
                        return@addOnSuccessListener
                    }
                }
            }.addOnCompleteListener { imageProxy.close() }
        } catch (e: Exception) {
            DebugLog.e("QRCodeAnalyzer", "Analysis error", e)
            imageProxy.close()
        }
    }
}
