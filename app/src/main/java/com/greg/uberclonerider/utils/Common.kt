package com.greg.uberclonerider.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.greg.uberclonerider.R
import com.greg.uberclonerider.model.Rider
import com.greg.uberclonerider.utils.Constant.Companion.NOTIFICATION_CHANNEL_ID

object Common {
    var currentRider: Rider? = null

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Welcome message builder -------------------------------------
    //----------------------------------------------------------------------------------------------

    fun buildWelcomeMessage(): String {
        return StringBuilder("Welcome")
            .append(" ")
            .append(currentRider!!.firstName!!.trim())
            .append(" ")
            .append(currentRider!!.lastName!!.trim())
            .toString()
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Notification ------------------------------------------------
    //----------------------------------------------------------------------------------------------

    fun showNotification(context: Context, id: Int, title: String?, body: String?, intent: Intent?) {
        var pendingIntent: PendingIntent? = null

        if (intent != null){
            pendingIntent = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_ONE_SHOT)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "Uber clone", NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.description = "Uber clone"
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            notificationChannel.enableVibration(true)

            notificationManager.createNotificationChannel(notificationChannel)
        }

        val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
        notificationBuilder.setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_VIBRATE)
            .setSmallIcon(R.drawable.ic_baseline_directions_car_24)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.drawable.ic_baseline_directions_car_24
                )
            )

        if (pendingIntent != null){
            notificationBuilder.setContentIntent(pendingIntent)
            val notification = notificationBuilder.build()
            notificationManager.notify(id, notification)
        }
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Driver name builder -----------------------------------------
    //----------------------------------------------------------------------------------------------

    fun buildDriverName(firstName: String?, lastName: String?): String? {
        return StringBuilder(firstName!!)
                .append(" ")
                .append(lastName)
                .toString()
    }
}