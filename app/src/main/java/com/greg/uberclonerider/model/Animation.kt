package com.greg.uberclonerider.model

import android.os.Handler
import android.os.Looper
import com.google.android.gms.maps.model.LatLng
import java.util.*

class Animation(var isRunning: Boolean, var geolocationQuery: GeolocationQuery){
    var polylineList: ArrayList<LatLng?>? = null
    var handler: Handler = Handler(Looper.getMainLooper())
    var index: Int = 0
    var next: Int = 0
    var v: Float = 0.0f
    var latMove: Double = 0.0
    var lngMove: Double = 0.0
    lateinit var start: LatLng
    lateinit var end: LatLng
}
