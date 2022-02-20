package com.greg.uberclonerider.model

data class TripPlan(
    var riderKey: String? = null, //2 11:22
    var driverId: String? = null,
    var rider: Rider? = null,
    var driverGeolocation: DriverGeolocation? = null,
    var origin: String? = null,
    var originString: String? = null,
    var destination: String? = null,
    var destinationString: String? = null,
    var distancePickup: String? = null,
    var durationPickup: String? = null,
    var distanceDestination: String? = null,
    var durationDestination: String? = null,
    var currentLat: Double = -1.0,
    var currentLng: Double = -1.0,
    var isDone: Boolean = false,
    var isCanceled: Boolean = false,
)
