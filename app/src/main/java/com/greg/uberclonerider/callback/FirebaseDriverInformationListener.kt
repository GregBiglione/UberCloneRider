package com.greg.uberclonerider.callback

import com.greg.uberclonerider.model.DriverGeolocation

interface FirebaseDriverInformationListener {

    fun onDriverInformationLoadSuccess(driverGeolocation: DriverGeolocation?)
}