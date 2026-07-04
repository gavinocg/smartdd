package com.smartdd.app

import android.app.Application
import com.smartdd.app.data.local.DebugLog
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmartDDApp : Application() {
    override fun onCreate() {
        super.onCreate()
        DebugLog.init(this)
        DebugLog.i("App", "Application started")

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            DebugLog.e("CRASH", "Uncaught exception on ${thread.name}", throwable)
            DebugLog.i("CRASH", "Attempting to upload crash log...")
            kotlinx.coroutines.runBlocking {
                DebugLog.uploadToServer()
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
