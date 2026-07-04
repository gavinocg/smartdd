package com.smartdd.app.presentation.qr.generate

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartdd.app.data.repository.QRRepository
import com.smartdd.app.domain.model.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Base64
import javax.inject.Inject

data class GenerateQRUiState(
    val lat: Double? = null, val lng: Double? = null, val radius: Int = 50,
    val isLoading: Boolean = false, val error: String? = null, val qrImageUrl: String? = null
)

@HiltViewModel
class GenerateQRViewModel @Inject constructor(
    private val qrRepository: QRRepository
) : ViewModel() {
    private val _state = MutableStateFlow(GenerateQRUiState())
    val state: StateFlow<GenerateQRUiState> = _state.asStateFlow()

    fun setLocation(lat: Double, lng: Double) {
        _state.value = _state.value.copy(lat = lat, lng = lng)
    }

    fun setRadius(radius: Int) { _state.value = _state.value.copy(radius = radius) }

    fun generate() {
        val current = _state.value
        if (current.lat == null || current.lng == null) {
            _state.value = current.copy(error = "Esperando ubicación GPS...")
            return
        }
        viewModelScope.launch {
            _state.value = current.copy(isLoading = true, error = null)
            when (val result = qrRepository.createQR(current.lat, current.lng, current.radius)) {
                is Result.Success -> _state.value = _state.value.copy(isLoading = false, qrImageUrl = result.data.qr.imageUrl)
                is Result.Error -> _state.value = _state.value.copy(isLoading = false, error = result.message)
            }
        }
    }

    fun decodeQRBitmap(): Bitmap? {
        val url = _state.value.qrImageUrl ?: return null
        return try {
            val base64 = url.substringAfter("base64,")
            val bytes = Base64.getDecoder().decode(base64)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) { null }
    }

    fun shareQR(context: Context) {
        val bitmap = decodeQRBitmap() ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val dir = File(context.cacheDir, "shared")
                    dir.mkdirs()
                    val file = File(dir, "smartdd_qr.png")
                    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartir QR"))
                } catch (_: Exception) {}
            }
        }
    }

    fun exportPDF(context: Context) {
        val bitmap = decodeQRBitmap() ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val document = PdfDocument()
                    val pageInfo = PdfDocument.PageInfo.Builder(400, 500, 1).create()
                    val page = document.startPage(pageInfo)
                    val canvas: Canvas = page.canvas

                    val paint = Paint().apply { textSize = 24f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
                    canvas.drawText("SMARTDD - Código QR", 200f, 40f, paint)
                    val qrSize = 300
                    val left = (400 - qrSize) / 2f
                    val top = 80f
                    canvas.drawBitmap(bitmap, null, android.graphics.RectF(left, top, left + qrSize, top + qrSize), null)
                    document.finishPage(page)

                    val dir = File(context.cacheDir, "pdf")
                    dir.mkdirs()
                    val file = File(dir, "smartdd_qr.pdf")
                    FileOutputStream(file).use { document.writeTo(it) }
                    document.close()

                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Exportar QR"))
                } catch (_: Exception) {}
            }
        }
    }
}
