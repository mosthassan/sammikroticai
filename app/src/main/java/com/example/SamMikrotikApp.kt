package com.example

import android.app.Application
import android.util.Log
import com.example.core.firebase.FirebaseInitializer

/**
 * Main Application class for SamMikrotik.
 * Ensures FirebaseApp is initialized at the earliest possible entry point
 * before any Activity, ViewModel, or background service executes.
 */
class SamMikrotikApp : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            val app = FirebaseInitializer.ensureInitialized(this)
            if (app != null) {
                Log.i("SamMikrotikApp", "FirebaseApp successfully initialized in Application.onCreate: ${app.name}")
            } else {
                Log.w("SamMikrotikApp", "FirebaseInitializer completed in Application.onCreate")
            }
        } catch (e: Exception) {
            Log.e("SamMikrotikApp", "Error during early Firebase initialization: ${e.message}", e)
        }
    }
}
