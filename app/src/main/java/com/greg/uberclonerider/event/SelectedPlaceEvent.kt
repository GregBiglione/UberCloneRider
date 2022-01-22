package com.greg.uberclonerider.event

import com.google.android.gms.maps.model.LatLng
import com.greg.uberclonerider.utils.Common

class SelectedPlaceEvent(var origin:LatLng, var destination: LatLng) {
    val originString = Common.buildRouteOrigin(origin)
    val destinationString = Common.buildRouteDestination(destination)
}