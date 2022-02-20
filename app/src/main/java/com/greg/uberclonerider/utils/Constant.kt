package com.greg.uberclonerider.utils

import android.Manifest
import com.greg.uberclonerider.R

class Constant {
    companion object{
        //-------------------------------- Rider ---------------------------------------------------
        const val RIDER_INFORMATION = "RiderInformation"
        const val RIDER_LOCATION = "RiderLocation"
        const val RIDER_KEY = "RiderKey"
        const val DEFAULT_ZOOM = 17.0f
        const val ACCESS_FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION
        const val INFO_CONNECTED = ".info/connected"
        //-------------------------------- Driver --------------------------------------------------
        const val DRIVER_LOCATION = "DriverLocation"
        const val DRIVER_INFORMATION = "DriverInformation"
        const val REQUEST_DRIVER_TITLE = "RequestDriver"
        const val REQUEST_DRIVER_BODY = "This message represent for request driver action"
        const val PICKUP_LOCATION = "PickupLocation"
        const val PICKUP_LOCATION_STRING = "PickupLocationString"
        const val DESTINATION_LOCATION = "DestinationLocation"
        const val DESTINATION_LOCATION_STRING = "DestinationLocationString"
        const val REQUEST_DRIVER_DECLINE = "Decline"
        const val REQUEST_DRIVER_ACCEPT = "Accept"
        const val TRIP_KEY = "TripKey"
        //-------------------------------- Camera & gallery ----------------------------------------
        const val ACCESS_CAMERA = Manifest.permission.CAMERA
        const val READ_STORAGE = Manifest.permission.READ_EXTERNAL_STORAGE
        //-------------------------------- Notification --------------------------------------------
        const val TOKEN = "Token"
        const val NOTIFICATION_TITLE = "Title test FCM"
        const val NOTIFICATION_BODY = "Message test FCM"
        const val NOTIFICATION_CHANNEL_ID = "Uber_clone_channel"
        //------------------- Load available driver ------------------------------------------------
        const val LIMIT_RANGE = 10.0
        //-------------------------------- Log -----------------------------------------------------
        const val GEO_CODER_TAG = "GeoCodingLocation"
        //-------------------------------- Url -----------------------------------------------------
        const val BASE_URL = "https://maps.googleapis.com/"
        const val BASE_URL_FCM = "https://fcm.googleapis.com/"
        //-------------------------------- Spin camera ---------------------------------------------
        const val TILT_ZOOM = 16.0f
        const val DURATION = 1000
        const val DESIRED_NUMBER_OF_SPIN = 5
        const val DESIRED_SECONDS_FOR_ONE_FULL_ROTATION = 40
        //-------------------------------- Firebase key --------------------------------------------
        const val FIREBASE_KEY = R.string.FirebaseKey
    }
}