package com.example.tippscores.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {

    const val CHANNEL_ID = "goal_alerts"

    fun ensureChannel(context: Context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (manager.getNotificationChannel(CHANNEL_ID) == null) {

                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Gólértesítések",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description =
                        "Értesítés, ha egy kedvenc meccsen vagy kedvenc csapatnál gól születik"
                }

                manager.createNotificationChannel(channel)
            }
        }
    }

    fun showGoalNotification(
        context: Context,
        notificationId: Int,
        title: String,
        text: String
    ) {

        ensureChannel(context)

        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Nincs POST_NOTIFICATIONS engedély megadva - csendben elnyeljük.
            // A háttérellenőrzés legközelebb újra megpróbálja, ha időközben
            // megadták az engedélyt.
        }
    }
}
