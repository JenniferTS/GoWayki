package pe.edu.tecsup.gowayki.platform

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object TripOngoingNotifier {
    const val NOTIFICATION_ID = 42001
    const val ACTION_FINISH = "pe.edu.tecsup.gowayki.action.FINISH_TRIP"
    const val ACTION_MUTE = "pe.edu.tecsup.gowayki.action.MUTE_ALARM"

    fun show(context: Context, title: String, body: String, phaseLabel: String) {
        TripNotificationChannels.ensure(context)
        val finish = PendingIntent.getBroadcast(
            context, 1, Intent(ACTION_FINISH).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val mute = PendingIntent.getBroadcast(
            context, 2, Intent(ACTION_MUTE).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, TripNotificationChannels.ONGOING)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle(title)
            .setContentText("$phaseLabel · $body")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, "Finalizar", finish)
            .addAction(0, "Silenciar", mute)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
