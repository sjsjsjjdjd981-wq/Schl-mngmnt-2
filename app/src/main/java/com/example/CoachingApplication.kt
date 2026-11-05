package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class CoachingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Log.d("CoachingApp", "FirebaseApp initialized in CoachingApplication")
            }
        } catch (e: Exception) {
            Log.e("CoachingApp", "Failed to initialize FirebaseApp in Application", e)
        }
    }
}
