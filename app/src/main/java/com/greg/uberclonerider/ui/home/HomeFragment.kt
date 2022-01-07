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
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.greg.uberclonerider.utils.Constant.Companion.ACCESS_FINE_LOCATION
import com.greg.uberclonerider.utils.Constant.Companion.DEFAULT_ZOOM
import com.greg.uberclonerider.utils.Constant.Companion.INFO_CONNECTED
import com.greg.uberclonerider.utils.Constant.Companion.RIDER_LOCATION
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
    //------------------- Online system ------------------------------------------------------------
    private lateinit var geoFire: GeoFire
    private lateinit var onlineDatabaseReference: DatabaseReference
    private lateinit var currentRiderReference: DatabaseReference
    private lateinit var riderLocationReference: DatabaseReference

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
        getDriverLocationFromDatabase()
        getRealTimeLocation()
        registerOnlineSystem()
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
            //------------------- Update real time location  ---------------------------------------
            geoFire.setLocation(
                FirebaseAuth.getInstance().currentUser!!.uid, GeoLocation(locationResult.lastLocation.latitude, locationResult.lastLocation.longitude)
            ){key: String?, databaseError: DatabaseError? ->
                if (databaseError != null){
                    Snackbar.make(mapFragment.requireView(), databaseError.message, Snackbar.LENGTH_LONG).show()
                }
                else{
                    Snackbar.make(mapFragment.requireView(), "You're online!", Snackbar.LENGTH_SHORT).show()
                }
            }
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
        removeLocation()
        removeOnlineListener()
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

    /**-----------------------------------------------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
     *----------------------- Online system ----------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
    ------------------------------------------------------------------------------------------------------------------------------------------------------*/

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Online value event listener ---------------------------------
    //----------------------------------------------------------------------------------------------

    private val onlineValueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.exists()){
                currentRiderReference.onDisconnect().removeValue()
            }
        }

        override fun onCancelled(error: DatabaseError) {
            Snackbar.make(mapFragment.requireView(), error.message, Snackbar.LENGTH_LONG).show()
        }
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Remove location ---------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun removeLocation(){
        geoFire.removeLocation(FirebaseAuth.getInstance().currentUser!!.uid)
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Remove online value event listener --------------------------
    //----------------------------------------------------------------------------------------------

    private fun removeOnlineListener(){
        onlineDatabaseReference.removeEventListener(onlineValueEventListener)
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Register online system --------------------------------------
    //----------------------------------------------------------------------------------------------

    override fun onResume() {
        super.onResume()
        registerOnlineSystem()
    }

    private fun registerOnlineSystem() {
        onlineDatabaseReference.addValueEventListener(onlineValueEventListener)
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Get rider's location from database --------------------------
    //----------------------------------------------------------------------------------------------

    private fun getDriverLocationFromDatabase(){
        onlineDatabaseReference = FirebaseDatabase.getInstance().reference.child(INFO_CONNECTED)
        riderLocationReference = FirebaseDatabase.getInstance().getReference(RIDER_LOCATION)
        currentRiderReference = FirebaseDatabase.getInstance().reference.child(
            FirebaseAuth.getInstance().currentUser!!.uid
        )
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Get driver's realtime location ------------------------------
    //----------------------------------------------------------------------------------------------

    private fun getRealTimeLocation(){
        geoFire = GeoFire(riderLocationReference)
    }
}