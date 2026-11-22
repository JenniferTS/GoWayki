package com.myapplication

import android.app.Application
import androidx.profileinstaller.ProfileInstaller
import com.google.firebase.FirebaseApp

class GowaykiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        // Aplica baseline-prof.txt en background (cold start más estable).
        ProfileInstaller.writeProfile(this)
    }
}
