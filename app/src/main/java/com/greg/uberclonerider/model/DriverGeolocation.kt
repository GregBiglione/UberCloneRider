package com.greg.uberclonerider.model

import com.firebase.geofire.GeoLocation

class DriverGeolocation{
    var key: String? = null
    var geoLocation: GeoLocation? = null
    var driverInformation: DriverInformation? = null

    constructor(key: String?, geoLocation: GeoLocation?){
        this.key = key
        this.geoLocation = geoLocation!!
    }
}
