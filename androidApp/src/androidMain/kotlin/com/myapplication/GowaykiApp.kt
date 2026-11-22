package com.myapplication

import android.app.Application
import com.google.firebase.FirebaseApp

class GowaykiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
