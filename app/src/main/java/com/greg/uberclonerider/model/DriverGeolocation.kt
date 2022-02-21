package com.greg.uberclonerider.model

import com.firebase.geofire.GeoLocation

class DriverGeolocation{
    var key: String? = null
    var geoLocation: GeoLocation? = null
    var driver: Driver? = null
    var isDeclined: Boolean = false

    constructor(key: String?, geoLocation: GeoLocation?){
        this.key = key
        this.geoLocation = geoLocation!!
    }
}
