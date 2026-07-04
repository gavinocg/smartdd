package com.smartdd.app.presentation.qr.ar

import android.Manifest
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.smartdd.app.presentation.ring.CallViewModel
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ARDoorbellScreen(
    qrUuid: String,
    onRingSent: (sessionId: String, roomId: String) -> Unit,
    onBack: () -> Unit,
    callViewModel: CallViewModel = hiltViewModel()
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var qrPosition by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var isCentered by remember { mutableStateOf(false) }
    var isRinging by remember { mutableStateOf(false) }
    val callState by callViewModel.state.collectAsState()

    LaunchedEffect(callState.sessionCreated, callState.error) {
        if (callState.sessionCreated) {
            onRingSent(callState.sessionId, callState.roomId)
        }
        if (callState.error.isNotEmpty()) {
            isRinging = false
        }
    }

    val qrAnalyzer = remember {
        CameraXQRTracker { x, y, w, h ->
            qrPosition = Pair(x, y)
            val centerX = 0.5f
            val centerY = 0.5f
            isCentered = kotlin.math.abs(x - centerX) < 0.15f && kotlin.math.abs(y - centerY) < 0.15f
        }
    }

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Timbre Virtual") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } })
    }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (cameraPermission.status.isGranted) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProvider = ProcessCameraProvider.getInstance(ctx).get()
                        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(1280, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build().also { it.setAnalyzer(Executors.newSingleThreadExecutor(), qrAnalyzer) }
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
                        } catch (_: Exception) {}
                        previewView
                    }
                )

                if (qrPosition == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Buscando QR...", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Permiso de cámara requerido")
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { cameraPermission.launchPermissionRequest() }) { Text("Conceder permiso") }
                    }
                }
            }

            if (!isRinging) {
                Button(
                    onClick = {
                        isRinging = true
                        callViewModel.ring(qrUuid)
                    },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp).fillMaxWidth(0.7f).height(56.dp),
                    enabled = isCentered
                ) {
                    Text("🔔  Tocar timbre", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Column(Modifier.align(Alignment.BottomCenter).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.7f))
                    if (callState.error.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                callState.error,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

class CameraXQRTracker(private val onPosition: (Float, Float, Float, Float) -> Unit) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    )

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(inputImage).addOnSuccessListener { barcodes ->
            for (barcode in barcodes) {
                val box = barcode.boundingBox ?: continue
                val imgW = imageProxy.width.toFloat()
                val imgH = imageProxy.height.toFloat()
                val cx = (box.centerX().toFloat() / imgW).coerceIn(0f, 1f)
                val cy = (box.centerY().toFloat() / imgH).coerceIn(0f, 1f)
                val bw = box.width().toFloat() / imgW
                val bh = box.height().toFloat() / imgH
                onPosition(cx, cy, bw, bh)
                imageProxy.close()
                return@addOnSuccessListener
            }
        }.addOnCompleteListener { imageProxy.close() }
    }
}
