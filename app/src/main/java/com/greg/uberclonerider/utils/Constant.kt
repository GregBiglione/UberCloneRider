package com.greg.uberclonerider.utils

import android.Manifest

class Constant {
    companion object{
        const val RIDER_INFORMATION = "RiderInformation"
        const val RIDER_LOCATION = "RiderLocation"
        const val DEFAULT_ZOOM = 17.0f
        const val ACCESS_FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
        const val INFO_CONNECTED = ".info/connected"
        //-------------------------------- Camera & gallery ----------------------------------------
        const val ACCESS_CAMERA = Manifest.permission.CAMERA
        const val READ_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE
        //-------------------------------- Notification --------------------------------------------
        const val TOKEN = "Token"
        const val NOTIFICATION_TITLE = "Title test FCM"
        const val NOTIFICATION_BODY = "Message test FCM"
        const val NOTIFICATION_CHANNEL_ID = "Uber_clone_channel"
    }
}