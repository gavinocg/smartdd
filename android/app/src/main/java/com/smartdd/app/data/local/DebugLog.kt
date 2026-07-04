package com.smartdd.app.data.local

import android.content.Context
import android.util.Log
import com.smartdd.app.data.remote.api.SmartDDApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLog {
    private const val TAG = "SMARTDD"
    private const val MAX_LOG_SIZE = 1024 * 100
    private var logFile: File? = null

    fun init(context: Context) {
        logFile = File(context.filesDir, "debug_log.txt")
        logFile?.let {
            if (it.length() > MAX_LOG_SIZE) it.writeText("")
        }
        i("DebugLog", "Logger initialized")
    }

    private fun writeToFile(level: String, tag: String, msg: String, tr: Throwable? = null) {
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val trace = tr?.let { "\n${Log.getStackTraceString(it)}" } ?: ""
        val line = "$ts [$level] $tag: $msg$trace\n"
        logFile?.appendText(line)
    }

    fun d(tag: String, msg: String) { Log.d(TAG, "[$tag] $msg"); writeToFile("D", tag, msg) }
    fun i(tag: String, msg: String) { Log.i(TAG, "[$tag] $msg"); writeToFile("I", tag, msg) }
    fun w(tag: String, msg: String, tr: Throwable? = null) { Log.w(TAG, "[$tag] $msg", tr); writeToFile("W", tag, msg, tr) }
    fun e(tag: String, msg: String, tr: Throwable? = null) { Log.e(TAG, "[$tag] $msg", tr); writeToFile("E", tag, msg, tr) }

    fun getLogContent(): String = logFile?.readText() ?: ""

    fun clear() { logFile?.writeText(""); i("DebugLog", "Log cleared") }

    fun getLogFile(): File? = logFile

    suspend fun uploadToServer(): Boolean = withContext(Dispatchers.IO) {
        try {
            val content = getLogContent()
            if (content.isBlank()) return@withContext false
            val json = org.json.JSONObject().apply { put("content", content) }.toString()
            val client = OkHttpClient()
            val body = json.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("${SmartDDApi.BASE_URL}/api/v1/debug/log")
                .post(body)
                .build()
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Upload log failed: ${e.message}")
            false
        }
    }
}
