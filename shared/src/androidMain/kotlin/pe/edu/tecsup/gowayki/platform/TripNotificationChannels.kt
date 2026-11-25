package pe.edu.tecsup.gowayki.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object TripNotificationChannels {
    const val ONGOING = "gowayki_trip_ongoing"
    const val ALERT = "gowayki_trip_alert"
    const val SILENT = "gowayki_trip_silent"

    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(ONGOING, "Viaje en curso", NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(ALERT, "Avisos de paradero", NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel(SILENT, "Viaje silencioso", NotificationManager.IMPORTANCE_MIN))
    }

    fun channelFor(mode: AlarmMode): String = when (mode) {
        AlarmMode.Off -> SILENT
        AlarmMode.Vibration, AlarmMode.Sound -> ALERT
    }
}
