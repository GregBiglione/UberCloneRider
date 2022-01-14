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
import com.google.android.gms.maps.model.LatLng
import com.greg.uberclonerider.R
import com.greg.uberclonerider.model.Rider
import com.greg.uberclonerider.utils.Constant.Companion.NOTIFICATION_CHANNEL_ID
import java.lang.Math.abs
import java.lang.Math.atan


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
            pendingIntent = PendingIntent.getActivity(
                context,
                id,
                intent,
                PendingIntent.FLAG_ONE_SHOT
            )
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Uber clone",
                NotificationManager.IMPORTANCE_HIGH
            )
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

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Decode poly -------------------------------------------------
    //----------------------------------------------------------------------------------------------

    fun decodePoly(encoded: String): List<LatLng?> {
        val poly = ArrayList<LatLng?>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dLat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dLat
            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dLng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dLng
            val p = LatLng(
                lat.toDouble() / 1E5,
                lng.toDouble() / 1E5
            )
            poly.add(p)
        }
        return poly
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Get bearing when moving -------------------------------------
    //----------------------------------------------------------------------------------------------

    fun getBearing(begin: LatLng, end: LatLng): Float {
        val lat = abs(begin.latitude - end.latitude)
        val lng = abs(begin.longitude - end.longitude)
        if (begin.latitude < end.latitude && begin.longitude < end.longitude) {
            return Math.toDegrees(atan(lng / lat)).toFloat()
        }
        else if (begin.latitude >= end.latitude && begin.longitude < end.longitude) {
            return (90 - Math.toDegrees(atan(lng / lat)) + 90).toFloat()
        }
        else if (begin.latitude >= end.latitude && begin.longitude >= end.longitude){
            return (Math.toDegrees(atan(lng / lat)) + 180).toFloat()
        }
        else if (begin.latitude < end.latitude && begin.longitude >= end.longitude){
            return (90 - Math.toDegrees(atan(lng / lat)) + 270).toFloat()
        }
        return (-1).toFloat()
    }
}