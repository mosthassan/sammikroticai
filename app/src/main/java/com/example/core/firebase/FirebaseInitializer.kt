package com.example.core.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.json.JSONObject

/**
 * Robust, production-grade Firebase initialization helper.
 * Guarantees FirebaseApp is initialized before any Auth, Firestore, or Cloud Sync call.
 * Tries resource-based initialization, asset-based fallback, and verified project config fallback.
 */
object FirebaseInitializer {

    private const val TAG = "FirebaseInitializer"

    @Synchronized
    fun ensureInitialized(context: Context): FirebaseApp? {
        val appContext = context.applicationContext ?: context

        // 1. Check if already initialized
        if (FirebaseApp.getApps(appContext).isNotEmpty()) {
            return try {
                FirebaseApp.getInstance()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get default FirebaseApp: ${e.message}")
                null
            }
        }

        // 2. Try default resource-based initialization
        try {
            val app = FirebaseApp.initializeApp(appContext)
            if (app != null) {
                Log.i(TAG, "FirebaseApp initialized via default resources")
                return app
            }
        } catch (e: Exception) {
            Log.w(TAG, "Standard FirebaseApp.initializeApp failed: ${e.message}. Trying assets/options fallback.")
        }

        // 3. Try reading credentials from assets/google-services.json
        try {
            val assetJsonString = appContext.assets.open("google-services.json").bufferedReader().use { it.readText() }
            val root = JSONObject(assetJsonString)
            val projectInfo = root.getJSONObject("project_info")
            val projectId = projectInfo.getString("project_id")
            val projectNumber = projectInfo.optString("project_number", "")
            val storageBucket = projectInfo.optString("storage_bucket", "")

            val clientArray = root.getJSONArray("client")
            if (clientArray.length() > 0) {
                val clientObj = clientArray.getJSONObject(0)
                val clientInfo = clientObj.getJSONObject("client_info")
                val appId = clientInfo.getString("mobilesdk_app_id")

                val apiKeys = clientObj.optJSONArray("api_key")
                val apiKey = if (apiKeys != null && apiKeys.length() > 0) {
                    apiKeys.getJSONObject(0).getString("current_key")
                } else ""

                if (appId.isNotBlank() && apiKey.isNotBlank()) {
                    val options = FirebaseOptions.Builder()
                        .setApplicationId(appId)
                        .setApiKey(apiKey)
                        .setProjectId(projectId)
                        .apply {
                            if (storageBucket.isNotBlank()) setStorageBucket(storageBucket)
                            if (projectNumber.isNotBlank()) setGcmSenderId(projectNumber)
                        }
                        .build()

                    val app = FirebaseApp.initializeApp(appContext, options)
                    Log.i(TAG, "FirebaseApp successfully initialized from assets/google-services.json for project: $projectId")
                    return app
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Asset parsing for google-services.json failed: ${e.message}. Trying hardcoded config fallback.")
        }

        // 4. Fallback options from verified project configuration
        return try {
            val fallbackOptions = FirebaseOptions.Builder()
                .setApplicationId("1:668455931031:android:6415c94d790b822f3b2dd2")
                .setApiKey("AIzaSyAPnh9iJyZQdrhF2sYtzq2CKFdibKvK9J4")
                .setProjectId("sam-mikrotic")
                .setStorageBucket("sam-mikrotic.firebasestorage.app")
                .setGcmSenderId("668455931031")
                .build()

            val app = FirebaseApp.initializeApp(appContext, fallbackOptions)
            Log.i(TAG, "FirebaseApp initialized via fallback options for project sam-mikrotic")
            app
        } catch (e: Exception) {
            Log.e(TAG, "All FirebaseApp initialization attempts failed: ${e.message}", e)
            null
        }
    }
}
