package com.greg.uberclonerider.ui.home

import android.annotation.SuppressLint
import android.content.res.Resources
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.droidman.ktoasty.KToasty
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.greg.uberclonerider.Constant.Companion.ACCESS_FINE_LOCATION
import com.greg.uberclonerider.Constant.Companion.DEFAULT_ZOOM
import com.greg.uberclonerider.R
import com.greg.uberclonerider.databinding.FragmentHomeBinding
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var map: GoogleMap
    private lateinit var mapFragment: SupportMapFragment
    //------------------- Location -----------------------------------------------------------------
    private lateinit var locationRequest: LocationRequest
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var newPosition: LatLng
    private lateinit var userLocation: LatLng

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        homeViewModel =
            ViewModelProvider(this)[HomeViewModel::class.java]
        binding = FragmentHomeBinding.inflate(layoutInflater)
        getLocationRequest()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(callback)
    }

    //----------------------------------------------------------------------------------------------
    //------------------- Show map when ready & add a maker ----------------------------------------
    //----------------------------------------------------------------------------------------------

    private val callback = OnMapReadyCallback { googleMap ->
        map = googleMap
        mapStyle()
        requestDexterPermission()
    }

    //----------------------------------------------------------------------------------------------
    //------------------- Move camera --------------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun moveCamera() {
        map.moveCamera(CameraUpdateFactory.newLatLng(newPosition))
    }

    //----------------------------------------------------------------------------------------------
    //------------------- Zoom level ---------------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun zoomOnLocation(){
        map.animateCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM))
    }

    //----------------------------------------------------------------------------------------------
    //------------------- Custom style -------------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun mapStyle(){
        try {
            val success = map.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.mapstyle))
            if (!success){
                Log.e("Style error", "Style parsing error")
            }
        }
        catch (e: Resources.NotFoundException){
            Log.e("Style error", e.message!!)
        }
    }

    //----------------------------------------------------------------------------------------------
    //------------------- Get location -------------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun getLocationRequest(){
        locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.fastestInterval = 3000
        locationRequest.interval = 5000
        locationRequest.smallestDisplacement = 10f

        locationCallback
        createLocationService()
    }

    //----------------------------------------------------------------------------------------------
    //------------------- Location callback --------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private var locationCallback = object: LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)

            newPosition = LatLng(locationResult.lastLocation.latitude, locationResult.lastLocation.longitude)
            map.addMarker(MarkerOptions()
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .position(newPosition)
            )
            moveCamera()
            zoomOnLocation()
        }
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Get last known location -------------------------------------
    //----------------------------------------------------------------------------------------------

    //-------------------------------- Location service --------------------------------------------

    @SuppressLint("MissingPermission")
    private fun createLocationService() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper()!!)
    }

    override fun onDestroy() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    @SuppressLint("MissingPermission")
    private fun lastKnownLocation(){
        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location ->
                userLocation = LatLng(location.latitude, location.longitude)
                map.addMarker(MarkerOptions()
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .position(userLocation)
                )
                moveCameraToLastKnownLocation()
            }.addOnFailureListener { e ->
                KToasty.error(requireContext(), "$e.message",
                    Toast.LENGTH_SHORT).show()
                Log.d("Failure", "${e.message}")
            }
    }

    //----------------------------------------------------------------------------------------------
    //------------------- Move camera to last know location-----------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun moveCameraToLastKnownLocation() {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, DEFAULT_ZOOM))
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Request Dexter permission -----------------------------------
    //----------------------------------------------------------------------------------------------

    private fun requestDexterPermission(){
        Dexter.withContext(context)
            .withPermission(ACCESS_FINE_LOCATION)
            .withListener(object: PermissionListener {
                override fun onPermissionGranted(permissionGrantedResponse: PermissionGrantedResponse?) {
                    clickOnMyLocation()
                }

                override fun onPermissionDenied(permissionDeniedResponse: PermissionDeniedResponse?) {
                    KToasty.error(requireContext(), "Permission ${permissionDeniedResponse!!.permissionName} was denied",
                        Toast.LENGTH_SHORT).show()
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissionRequest: PermissionRequest?,
                    permissionToken: PermissionToken?
                ) {}
            }).check()
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Center map on my location -----------------------------------
    //----------------------------------------------------------------------------------------------

    private fun clickOnMyLocation(){
        binding.gps.setOnClickListener {
            lastKnownLocation()
        }
    }
}