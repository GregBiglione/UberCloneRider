package com.greg.uberclonerider.ui.home

import android.Manifest
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.droidman.ktoasty.KToasty
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQuery
import com.firebase.geofire.GeoQueryEventListener
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.greg.uberclonerider.R
import com.greg.uberclonerider.callback.FirebaseDriverInformationListener
import com.greg.uberclonerider.callback.FirebaseFailedListener
import com.greg.uberclonerider.databinding.FragmentHomeBinding
import com.greg.uberclonerider.event.SelectedPlaceEvent
import com.greg.uberclonerider.model.Animation
import com.greg.uberclonerider.model.Driver
import com.greg.uberclonerider.model.DriverGeolocation
import com.greg.uberclonerider.model.GeolocationQuery
import com.greg.uberclonerider.remote.RetrofitService
import com.greg.uberclonerider.ui.activity.RequestDriverActivity
import com.greg.uberclonerider.utils.Common
import com.greg.uberclonerider.utils.Constant.Companion.ACCESS_FINE_LOCATION
import com.greg.uberclonerider.utils.Constant.Companion.DEFAULT_ZOOM
import com.greg.uberclonerider.utils.Constant.Companion.DRIVER_INFORMATION
import com.greg.uberclonerider.utils.Constant.Companion.DRIVER_LOCATION
import com.greg.uberclonerider.utils.Constant.Companion.GEO_CODER_TAG
import com.greg.uberclonerider.utils.Constant.Companion.INFO_CONNECTED
import com.greg.uberclonerider.utils.Constant.Companion.LIMIT_RANGE
import com.greg.uberclonerider.utils.Constant.Companion.RIDER_LOCATION
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject
import java.io.IOException
import java.util.*
import kotlin.collections.HashMap

class HomeFragment : Fragment(), FirebaseDriverInformationListener{

    private lateinit var binding: FragmentHomeBinding
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var map: GoogleMap
    private lateinit var mapFragment: SupportMapFragment
    //------------------- Sliding up panel ---------------------------------------------------------
    private lateinit var slidingUpPanelLayout: SlidingUpPanelLayout
    private lateinit var autocompleteFragment: AutocompleteSupportFragment
    //------------------- Location -----------------------------------------------------------------
    private var locationRequest: LocationRequest? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private lateinit var newPosition: LatLng
    private lateinit var userLocation: LatLng
    //------------------- Online system ------------------------------------------------------------
    private lateinit var riderGeoFire: GeoFire
    private lateinit var driverGeoFire: GeoFire
    private lateinit var geoQuery: GeoQuery
    private lateinit var onlineDatabaseReference: DatabaseReference
    private lateinit var currentRiderReference: DatabaseReference
    private lateinit var riderLocationReference: DatabaseReference
    private lateinit var driverLocationReference: DatabaseReference
    //------------------- Load available driver ----------------------------------------------------
    private var distance = 1.0
    private var previousLocation: Location? = null
    private var currentLocation: Location? = null
    private var firstTime = true
    //------------------- Listener -----------------------------------------------------------------
    lateinit var iFirebaseDriverInformationListener: FirebaseDriverInformationListener
    lateinit var iFirebaseFailedListener: FirebaseFailedListener
    //------------------- Geo coder ----------------------------------------------------------------
    private lateinit var geoCoder: Geocoder
    private var cityName: String = ""
    private var lat: Double = 0.0
    private var lng: Double = 0.0
    private lateinit var locationResultPosition: Location
    private lateinit var riderLocation: Location
    private var markerList: MutableMap<String, Marker> = HashMap()
    private lateinit var driverGeo: DriverGeolocation
    //------------------- Marker animation ---------------------------------------------------------
    private var driverSubscribe: MutableMap<String, Animation> = HashMap()
    private val compositeDisposable = CompositeDisposable()
    private lateinit var iRetrofitService: RetrofitService
    //------------------- Runnable -----------------------------------------------------------------
    private lateinit var newKey : String
    private var newMarker: Marker? = null
    private lateinit var newAnimation : Animation
    //------------------- Estimates routes ---------------------------------------------------------
    private lateinit var selectedPlace: Place
    private lateinit var origin: LatLng
    private lateinit var destination: LatLng

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        homeViewModel =
            ViewModelProvider(this)[HomeViewModel::class.java]
        binding = FragmentHomeBinding.inflate(layoutInflater)
        //getLocationRequest()
        initializeViews(binding.root)
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
        getRiderLocationFromDatabase()
        getRealTimeRiderLocation()
        registerOnlineSystem()
        initializePlaces()
        initializeRetrofit()
        iFirebaseDriverInformationListener()
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            //Snackbar.make(requireContext(), getString(R.string.permission_required), Snackbar.LENGTH_LONG).show()
            return
        }
        buildLocationRequest()
        buildLocationCallback()
        createLocationService()
    }

    //----------------------------------------------------------------------------------------------
    //------------------- Location request ---------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun buildLocationRequest(){
        if (locationRequest == null) {
            locationRequest = LocationRequest.create()
            locationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            locationRequest!!.fastestInterval = 3000
            locationRequest!!.interval = 5000
            locationRequest!!.smallestDisplacement = 10f
        }
    }

    //----------------------------------------------------------------------------------------------
    //------------------- Location callback --------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun buildLocationCallback(){
        if (locationCallback == null){
            locationCallback
        }
    }

    private var locationCallback: LocationCallback? = object: LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)

            locationResultPosition = locationResult.lastLocation
            newPosition = LatLng(locationResult.lastLocation.latitude, locationResult.lastLocation.longitude)
            map.addMarker(MarkerOptions()
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .position(newPosition)
            )
            moveCamera()
            zoomOnLocation()
            //------------------- Calculate distance  ----------------------------------------------
            calculateDistance()
            //------------------- Update real time location  ---------------------------------------
            riderGeoFire.setLocation(
                FirebaseAuth.getInstance().currentUser!!.uid, GeoLocation(locationResult.lastLocation.latitude, locationResult.lastLocation.longitude)
            ){ _: String?, databaseError: DatabaseError? ->
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

    private fun createLocationService() {
        if (fusedLocationProviderClient == null) {
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                //Snackbar.make(requireView(), getString(R.string.permission_required), Snackbar.LENGTH_LONG).show()
                return
            }
            fusedLocationProviderClient!!.requestLocationUpdates(locationRequest!!, locationCallback!!, Looper.myLooper()!!)
            loadAvailableDrivers()
        }
    }

    override fun onDestroy() {
        fusedLocationProviderClient!!.removeLocationUpdates(locationCallback!!)
        removeLocation()
        removeOnlineListener()
        super.onDestroy()
    }

    private fun lastKnownLocation(){
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            //Snackbar.make(requireView(), getString(R.string.permission_required), Snackbar.LENGTH_LONG).show()
            return
        }
        fusedLocationProviderClient!!.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    userLocation = LatLng(location.latitude, location.longitude)
                    map.addMarker(MarkerOptions()
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                        .position(userLocation)
                    )
                    moveCameraToLastKnownLocation()
                }
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
                    buildLocationRequest()
                    buildLocationCallback()
                    createLocationService()
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
        enableZoom()
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Center map on my location -----------------------------------
    //----------------------------------------------------------------------------------------------

    private fun clickOnMyLocation(){
        binding.gps.setOnClickListener {
            lastKnownLocation()
        }
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Enable zoom -------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun enableZoom(){
        map.uiSettings.isZoomControlsEnabled = true
        map.setPadding(16, 16, 0, 496)
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
        riderGeoFire.removeLocation(FirebaseAuth.getInstance().currentUser!!.uid)
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

    private fun getRiderLocationFromDatabase(){
        onlineDatabaseReference = FirebaseDatabase.getInstance().reference.child(INFO_CONNECTED)
        riderLocationReference = FirebaseDatabase.getInstance().getReference(RIDER_LOCATION)
        currentRiderReference = FirebaseDatabase.getInstance().reference.child(
            FirebaseAuth.getInstance().currentUser!!.uid
        )
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Get rider's realtime location ------------------------------
    //----------------------------------------------------------------------------------------------

    private fun getRealTimeRiderLocation(){
        riderGeoFire = GeoFire(riderLocationReference)
    }

    /**-----------------------------------------------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
     *----------------------- Calculate distance between previous & current location -----------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
    ------------------------------------------------------------------------------------------------------------------------------------------------------*/

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Calculate distance  -----------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun calculateDistance(){
        if (firstTime){
            previousLocation = locationResultPosition
            currentLocation = locationResultPosition
            //-------------------------------- Restrict places in country  -------------------------
            setRestrictPlacesInCountry(locationResultPosition)
            firstTime = false
        }
        else{
            previousLocation = currentLocation
            currentLocation = locationResultPosition
        }
        if (previousLocation!!.distanceTo(currentLocation)/1000 <= LIMIT_RANGE){
            loadAvailableDrivers()
        }
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Load available drivers --------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun loadAvailableDrivers(){
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationProviderClient!!.lastLocation
                .addOnSuccessListener { location ->
                    //-------------------------------- Get city ------------------------------------
                    if (location != null) {
                        riderLocation = location
                        getCityNameFromLocation(location.latitude, location.longitude)
                    }
                }
                .addOnFailureListener { e ->
                    Snackbar.make(requireView(), e.message!!, Snackbar.LENGTH_SHORT).show()
                }
    }

    /**-----------------------------------------------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
     *----------------------- Geo coder --------------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
    ------------------------------------------------------------------------------------------------------------------------------------------------------*/

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Geo coder ---------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun initializeGeoCoder(){
        geoCoder = Geocoder(requireContext(), Locale.getDefault())
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Get city name from location ---------------------------------
    //----------------------------------------------------------------------------------------------

    private fun getCityNameFromLocation(latitude: Double, longitude: Double): String {
        initializeGeoCoder()
        lat = latitude
        lng = longitude
        try {
            val addressList = geoCoder.getFromLocation(latitude, longitude, 1)
            if (addressList != null && addressList.size > 0){
                val address = (addressList as MutableList<Address>)[0]

                if (address.adminArea == null){
                    cityName = address.locality
                    //-------------------------------- Load all drivers in city --------------------
                    getDriverLocationFromDatabase()
                }
                if (address.locality == null){
                    cityName = address.adminArea
                    //-------------------------------- Load all drivers in city --------------------
                    getDriverLocationFromDatabase()
                }
            }

        } catch (e: IOException) {
            Log.e(GEO_CODER_TAG, "Unable to connect to GeoCoder", e)
        }
        return cityName
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Get driver's location from database -------------------------
    //----------------------------------------------------------------------------------------------

    private fun getDriverLocationFromDatabase(){
        if (!TextUtils.isEmpty(cityName)) {
            driverLocationReference = FirebaseDatabase.getInstance()
                    .getReference(DRIVER_LOCATION)
                    .child(cityName)
            getRealTimeDriverLocation()
        }
        else{
            Snackbar.make(requireView(), getString(R.string.city_name_not_found), Snackbar.LENGTH_SHORT).show()
        }
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Get driver's realtime location ------------------------------
    //----------------------------------------------------------------------------------------------

    private fun getRealTimeDriverLocation(){
        driverGeoFire = GeoFire(driverLocationReference)
        driverGeoQuery()
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Geo query ---------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun driverGeoQuery(){
        geoQuery = driverGeoFire.queryAtLocation(GeoLocation(lat, lng), distance)
        geoQuery.removeAllListeners()
        addGeoQueryEventListener()
        addChildEventListener()
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Add geo query event listener --------------------------------
    //----------------------------------------------------------------------------------------------

    private fun addGeoQueryEventListener(){
        geoQuery.addGeoQueryEventListener(object: GeoQueryEventListener{
            override fun onKeyEntered(key: String?, location: GeoLocation?) {
                //driverFound.add(DriverGeolocation(key!!, location!!))
                if (!Common.driverFound.containsKey(key)){
                    Common.driverFound[key!!] = DriverGeolocation(key, location)
                }
            }

            override fun onKeyExited(key: String?) {}

            override fun onKeyMoved(key: String?, location: GeoLocation?) {}

            override fun onGeoQueryReady() {
                if (distance <= LIMIT_RANGE){
                    distance++
                    loadAvailableDrivers()
                }
                else{
                    distance = 0.0
                    addDriverMarker()
                }
            }

            override fun onGeoQueryError(error: DatabaseError?) {
                Snackbar.make(requireView(), error!!.message, Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Add children event listener ---------------------------------
    //----------------------------------------------------------------------------------------------

    private fun addChildEventListener(){
        driverLocationReference.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val geolocationQuery = snapshot.getValue(GeolocationQuery::class.java)
                val geolocation = GeoLocation(geolocationQuery!!.l!![0], geolocationQuery.l!![1])
                val driverGeolocation = DriverGeolocation(snapshot.key, geolocation)

                val newDriverGeolocation = Location("")
                newDriverGeolocation.latitude = geolocation.latitude
                newDriverGeolocation.longitude = geolocation.longitude

                val newDistance = riderLocation.distanceTo(newDriverGeolocation)/1000
                if (newDistance <= LIMIT_RANGE){
                    findDriverByKey(driverGeolocation)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onChildRemoved(snapshot: DataSnapshot) {}

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(error: DatabaseError) {
                Snackbar.make(requireView(), error.message, Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Add driver marker -------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun addDriverMarker() {
        if (Common.driverFound.isNotEmpty()){
            Observable.fromIterable(Common.driverFound.keys)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            { key: String? ->
                                findDriverByKey(Common.driverFound[key!!])
                            },
                            {
                                t: Throwable? ->
                                Snackbar.make(requireView(), t!!.message!!, Snackbar.LENGTH_SHORT).show()
                            }
                    )
        }
        else{
            Snackbar.make(requireView(), getString(R.string.drivers_not_found), Snackbar.LENGTH_SHORT).show()
        }
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Find available driver ---------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun findDriverByKey(driverGeolocation: DriverGeolocation?) {
        FirebaseDatabase.getInstance()
                .getReference(DRIVER_INFORMATION)
                .child(driverGeolocation!!.key!!)
                .addListenerForSingleValueEvent(object: ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.hasChildren()){
                            driverGeolocation.driver = (snapshot.getValue(Driver::class.java))
                            Common.driverFound[driverGeolocation.key!!]!!.driver = (snapshot.getValue(Driver::class.java))
                            iFirebaseDriverInformationListener.onDriverInformationLoadSuccess(driverGeolocation)
                        }
                        else{
                            iFirebaseFailedListener.onFirebaseFailed(getString(R.string.key_not_found) + driverGeolocation.key)
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        iFirebaseFailedListener.onFirebaseFailed(error.message)
                    }
                })
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Firebase driver information listener ------------------------
    //----------------------------------------------------------------------------------------------

    private fun iFirebaseDriverInformationListener(){
        iFirebaseDriverInformationListener = this
    }

    override fun onDriverInformationLoadSuccess(driverGeolocation: DriverGeolocation?) {
        val key = driverGeolocation!!.key
        driverGeo = driverGeolocation

        if (!markerList.containsKey(key)){
            markerList[key!!] = map.addMarker(
                    MarkerOptions()
                            .position(LatLng(driverGeolocation.geoLocation!!.latitude, driverGeolocation.geoLocation!!.longitude))
                            .flat(true)
                            .title(Common.buildDriverName(
                                    driverGeolocation.driver!!.firstName,
                                    driverGeolocation.driver!!.lastName)
                            )
                            .snippet(driverGeolocation.driver!!.phoneNumber)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.vader_tie))
            )!!
        }
        if (!TextUtils.isEmpty(cityName)){
            removeDriverLocationMarker()
        }
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Get driver's location ---------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun removeDriverLocationMarker(){
        val driverLocation = FirebaseDatabase.getInstance()
                .getReference(DRIVER_LOCATION)
                .child(cityName)
                .child(driverGeo.key!!)
        driverLocation.addValueEventListener(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.hasChildren()){
                    if (markerList[driverGeo.key!!] != null){
                        val marker = markerList[driverGeo.key!!]!!
                        marker.remove()
                        markerList.remove(driverGeo.key!!)
                        //-------------------------------- Remove driver's information -------------
                        driverSubscribe.remove(driverGeo.key)
                        //-------------------------------- When driver decline request, they can accept again if they stop & open the app -------------
                        if(Common.driverFound != null && Common.driverFound[driverGeo.key!!] != null){
                            Common.driverFound.remove(driverGeo.key)
                        }
                        driverLocation.removeEventListener(this)
                    }
                }
                else{
                    if (markerList[driverGeo.key] != null){
                        val geolocationQuery = snapshot.getValue(GeolocationQuery::class.java)
                        val animation = Animation(false, geolocationQuery!!)

                        if(driverSubscribe[driverGeo.key!!] != null){
                            val marker = markerList[driverGeo.key]
                            val oldPosition = driverSubscribe[driverGeo.key!!]

                            val fromLat = oldPosition!!.geolocationQuery.l!![0]
                            val fromLng = oldPosition.geolocationQuery.l!![1]
                            val toLat = animation.geolocationQuery.l!![0]
                            val toLng = animation.geolocationQuery.l!![1]

                            val from = Common.buildDriverFrom(fromLat, fromLng)
                            val to = Common.buildDriverTo(toLat, toLng)

                            moveMarkerAnimation(driverGeo.key!!, animation, marker, from, to)
                        }
                        else{
                            //-------------------------------- Initialize first location -----------
                            driverSubscribe[driverGeo.key!!] = animation
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Snackbar.make(requireView(), error.message, Snackbar.LENGTH_LONG).show()
            }

        })
    }

    /**-----------------------------------------------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
     *----------------------- Marker animation -------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
    ------------------------------------------------------------------------------------------------------------------------------------------------------*/

    override fun onStop() {
        compositeDisposable.clear()
        super.onStop()
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Initialize retrofit -----------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun initializeRetrofit(){
        iRetrofitService = RetrofitService.getInstance()
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Move animation marker when vehicle is moving ----------------
    //----------------------------------------------------------------------------------------------

    private fun moveMarkerAnimation(key: String, newData: Animation, marker: Marker?, from: String, to: String) {
        newKey = key
        newAnimation = newData
        newMarker = marker
        if(!newData.isRunning){
            //-------------------------------- Request api -----------------------------------------
            compositeDisposable.add(iRetrofitService.getDirections(
                    "driving",
                    "less_driving",
                    from,
                    to,
                    getString(R.string.ApiKey))
                    !!.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { returnResult ->
                        Log.d("Api return", returnResult)
                        try {
                            val jsonObject = JSONObject(returnResult)
                            val jsonArray = jsonObject.getJSONArray("routes")

                            for (j in 0 until jsonArray.length()){
                                val route = jsonArray.getJSONObject(j)
                                val poly = route.getJSONObject("overview_polyline")
                                val polyline = poly.getString("points")
                                //polylineList = Common.decodePoly(polyline)
                                newData.polylineList = Common.decodePoly(polyline)
                            }
                            //-------------------------------- Moving ------------------------------
                            newData.index = -1
                            newData.next = 1

                            newData.handler.postDelayed(runnable, 1500)

                        } catch (e: Exception) {
                            Snackbar.make(requireView(), e.message!!, Snackbar.LENGTH_LONG).show()
                        }

                    }
            )
        }
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Runnable ----------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private val runnable = object : Runnable{
        override fun run() {
            if (newAnimation.polylineList != null && newAnimation.polylineList!!.size > 1){
                if (newAnimation.index < newAnimation.polylineList!!.size - 2){
                    newAnimation.index++
                    newAnimation.next = newAnimation.index+1
                    newAnimation.start = newAnimation.polylineList!![newAnimation.index]!!
                    newAnimation.end = newAnimation.polylineList!![newAnimation.next]!!
                }

                val valueAnimator = ValueAnimator.ofInt(0, 1)
                valueAnimator.duration = 3000
                valueAnimator.interpolator = LinearInterpolator()
                valueAnimator.addUpdateListener { value ->
                    newAnimation.v = value.animatedFraction
                    newAnimation.latMove = newAnimation.v * newAnimation.end.latitude + (1-newAnimation.v) * newAnimation.start.latitude
                    newAnimation.lngMove = newAnimation.v * newAnimation.end.longitude + (1-newAnimation.v) * newAnimation.start.longitude
                    val newPosition = LatLng(newAnimation.latMove, newAnimation.lngMove)
                    newMarker!!.position = newPosition
                    newMarker!!.setAnchor(0.5f, 0.5f)
                    newMarker!!.rotation = Common.getBearing(newAnimation.start, newPosition)
                }

                valueAnimator.start()

                if (newAnimation.index < newAnimation.polylineList!!.size - 2){
                    newAnimation.handler.postDelayed(this, 1500)
                }
                else if (newAnimation.index < newAnimation.polylineList!!.size - 1){
                    newAnimation.isRunning = false
                    //-------------------------------- Update --------------------------------------
                    driverSubscribe[newKey] = newAnimation
                }
            }
        }
    }

    /**-----------------------------------------------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
     *----------------------- Autocomplete -----------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
    ------------------------------------------------------------------------------------------------------------------------------------------------------*/

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Initialize views --------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun initializeViews(view: View){
        slidingUpPanelLayout = view.findViewById(R.id.activity_main) as SlidingUpPanelLayout
        Common.setWelcomeMessage(binding.welcomeTv)
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Initialize places -------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun initializePlaces(){
        Places.initialize(requireContext(), getString(R.string.ApiKey))
        initializeAutocomplete()
        /*val intent = Intent(requireContext(), HomeActivity::class.java)
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)*/
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Initialize autocomplete -------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun initializeAutocomplete() {
        autocompleteFragment = childFragmentManager.findFragmentById(R.id.autocomplete) as AutocompleteSupportFragment
        autocompleteFragment.setPlaceFields(listOf(
                Place.Field.ID,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG,
                Place.Field.NAME
        ))

        autocompleteFragment.setOnPlaceSelectedListener(object: PlaceSelectionListener{
            override fun onError(status: Status) {
                Snackbar.make(requireView(), status.statusMessage!!, Snackbar.LENGTH_LONG).show()
                Log.e("Error status", status.statusMessage!!)
            }

            override fun onPlaceSelected(place: Place) {
                //Snackbar.make(requireView(), "" + place.latLng, Snackbar.LENGTH_LONG).show()
                selectedPlace = place
                calculateRoute()
                /*Log.d("Address origin 2", origin.toString())
                Log.d("Address destination 2", destination.toString())
                EventBus.getDefault().post(SelectedPlaceEvent(origin, destination))*/
            }
        })
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Restrict places in country  ---------------------------------
    //----------------------------------------------------------------------------------------------

    private fun setRestrictPlacesInCountry(location: Location?){
        initializeGeoCoder()
        try {
            val addressList = geoCoder.getFromLocation(location!!.latitude, location.longitude, 1)
            if (addressList.size > 0){
                autocompleteFragment.setCountry(addressList[0].countryCode)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**-----------------------------------------------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
     *----------------------- Estimate routes --------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
    ------------------------------------------------------------------------------------------------------------------------------------------------------*/

    //----------------------------------------------------------------------------------------------
    //-------------------------------- calculate routes --------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun calculateRoute(){
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(mapFragment.requireView(), getString(R.string.permission_location_required), Snackbar.LENGTH_LONG).show()
            return
        }
        fusedLocationProviderClient!!.lastLocation.addOnSuccessListener { location ->
            origin = LatLng(location.latitude, location.longitude)
            destination = LatLng(selectedPlace.latLng!!.latitude, selectedPlace.latLng!!.longitude)
            Log.d("Address origin 1", origin.toString())
            Log.d("Address destination 1", destination.toString())

            startActivity(Intent(requireContext(), RequestDriverActivity::class.java))
            EventBus.getDefault().postSticky(SelectedPlaceEvent(origin, destination))
        }
    }
}