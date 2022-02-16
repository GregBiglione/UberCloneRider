package com.greg.uberclonerider.ui.activity

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.droidman.ktoasty.KToasty
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.maps.android.ui.IconGenerator
import com.greg.uberclonerider.R
import com.greg.uberclonerider.event.DeclineRequestEventFromDriver
import com.greg.uberclonerider.event.SelectedPlaceEvent
import com.greg.uberclonerider.model.DriverGeolocation
import com.greg.uberclonerider.remote.RetrofitService
import com.greg.uberclonerider.ui.home.HomeFragment
import com.greg.uberclonerider.utils.Common
import com.greg.uberclonerider.utils.Constant.Companion.DESIRED_NUMBER_OF_SPIN
import com.greg.uberclonerider.utils.Constant.Companion.DESIRED_SECONDS_FOR_ONE_FULL_ROTATION
import com.greg.uberclonerider.utils.Constant.Companion.DURATION
import com.greg.uberclonerider.utils.Constant.Companion.TILT_ZOOM
import com.greg.uberclonerider.utils.UserUtils
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class RequestDriverActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var selectedPlaceEvent: SelectedPlaceEvent? = null
    private lateinit var locationButton: FloatingActionButton
    private lateinit var mapFragment: SupportMapFragment
    //------------------- Routes -------------------------------------------------------------------
    private val compositeDisposable = CompositeDisposable()
    private lateinit var iRetrofitService: RetrofitService
    private var blackPolyline: Polyline? = null
    private var greyPolyline: Polyline? = null
    private var polylineOptions: PolylineOptions? = null
    private var blackPolylineOptions: PolylineOptions? = null
    private var polylineList: ArrayList<LatLng?>? = null
    private var originMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private lateinit var jsonArray: JSONArray
    private lateinit var latLngBound: LatLngBounds
    //------------------- Confirm Uber -------------------------------------------------------------
    private lateinit var confirmUberBtn: Button
    private lateinit var confirmPickUpLayout: CardView
    private lateinit var confirmUberLayout: CardView
    private lateinit var pickUpAddressTv: TextView
    private lateinit var startAddress: String
    private lateinit var icon: Bitmap
    //------------------- Confirm pick up spot -----------------------------------------------------
    private lateinit var confirmPickUpBtn: Button
    private lateinit var cameraPosition: CameraPosition
    private lateinit var fillMap: View
    private lateinit var findYourRiderLayout: CardView
    //------------------- Pulsating effect ---------------------------------------------------------
    private var lastUserCircle: Circle? = null
    private var lastPulseAnimator: ValueAnimator? = null
    //------------------- Camera rotation ----------------------------------------------------------
    private var animator: ValueAnimator? = null
    //------------------- Find nearby driver -------------------------------------------------------
    private lateinit var mainLayout: RelativeLayout
    //------------------- Decline request ----------------------------------------------------------
    private var declinedRequestEvent: DeclineRequestEventFromDriver? = null
    private var lastDriverCall: DriverGeolocation? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_driver)
        initializeRetrofit()
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_request_driver) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        drawPath(selectedPlaceEvent!!)
        /*clickOnMyLocation()
        enableZoom()*/
        mapStyle()
    }

    /**-----------------------------------------------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
     *----------------------- Estimate routes --------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
    ------------------------------------------------------------------------------------------------------------------------------------------------------*/


    //----------------------------------------------------------------------------------------------
    //-------------------------------- Selected place ----------------------------------------------
    //----------------------------------------------------------------------------------------------

    override fun onStart() {
        if (!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this)
        }
        super.onStart()
    }

    override fun onStop() {
        compositeDisposable.clear()
        if (EventBus.getDefault().hasSubscriberForEvent(SelectedPlaceEvent::class.java)){
            EventBus.getDefault().removeStickyEvent(SelectedPlaceEvent::class.java)
        }
        if (EventBus.getDefault().hasSubscriberForEvent(DeclineRequestEventFromDriver::class.java)){
            EventBus.getDefault().removeStickyEvent(DeclineRequestEventFromDriver::class.java)
        }
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onSelectedPlace(event: SelectedPlaceEvent){
        selectedPlaceEvent = event
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onDeclinedRequest(event: DeclineRequestEventFromDriver){
        if (lastDriverCall != null){
            Common.driverFound[lastDriverCall!!.key]!!.isDeclined = true
            findNearbyDriver(selectedPlaceEvent!!.origin)
        }
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Initialize retrofit -----------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun initializeRetrofit(){
        iRetrofitService = RetrofitService.getInstance()
        //-------------------------------- Click on confirm Uber -----------------------------------
        clickOnConfirmUber()
        //-------------------------------- Click on confirm pick up spot ---------------------------
        clickOnConfirmPickUp()
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Center map on my location -----------------------------------
    //----------------------------------------------------------------------------------------------

    /*private fun clickOnMyLocation(){
        locationButton = findViewById(R.id.gps_request_driver)
        locationButton.setOnClickListener {
            moveCameraToSelectedPlace()
        }
    }*/

    /*//----------------------------------------------------------------------------------------------
    //-------------------------------- Move camera to selected place origin ------------------------
    //----------------------------------------------------------------------------------------------

    private fun moveCameraToSelectedPlace() {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedPlaceEvent!!.origin, Constant.DEFAULT_ZOOM))
    }*/

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Enable zoom -------------------------------------------------
    //----------------------------------------------------------------------------------------------

    /*private fun enableZoom(){
        mMap.uiSettings.isZoomControlsEnabled = true
    }*/

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Custom style ------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun mapStyle(){
        try {
            val success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyle))
            if (!success){
                Snackbar.make(mapFragment.requireView(), getString(R.string.load_map_style_failed), Snackbar.LENGTH_LONG).show()
            }
        }
        catch (e: Resources.NotFoundException){
            Snackbar.make(mapFragment.requireView(), e.message!!, Snackbar.LENGTH_LONG).show()
        }
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Draw path ---------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun drawPath(selectedPlaceEvent: SelectedPlaceEvent) {
        //-------------------------------- Request api ---------------------------------------------
        compositeDisposable.add(iRetrofitService.getDirections(
                "driving",
                "less_driving",
                selectedPlaceEvent.originString,
                selectedPlaceEvent.destinationString,
                getString(R.string.ApiKey))
                !!.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { returnResult ->
                    Log.d("Api return", returnResult)
                    try {
                        val jsonObject = JSONObject(returnResult)
                        jsonArray = jsonObject.getJSONArray("routes")

                        for (j in 0 until jsonArray.length()){
                            val route = jsonArray.getJSONObject(j)
                            val poly = route.getJSONObject("overview_polyline")
                            val polyline = poly.getString("points")
                            polylineList = Common.decodePoly(polyline)
                        }

                        //-------------------------------- Polyline options ------------------------
                        pathStyle()
                        //-------------------------------- Animation -------------------------------
                        animate()

                    } catch (e: Exception) {
                        Snackbar.make(mapFragment.requireView(), e.message!!, Snackbar.LENGTH_LONG).show()
                    }
                }
        )
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Path style --------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun pathStyle(){
        polylineOptions = PolylineOptions()
        polylineOptions!!.color(Color.GRAY)
        polylineOptions!!.width(12f)
        polylineOptions!!.startCap(SquareCap())
        polylineOptions!!.jointType(JointType.ROUND)
        polylineOptions!!.addAll(polylineList!!)
        greyPolyline = mMap.addPolyline(polylineOptions!!)
        blackPolylineStyle()
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Black polyline -----------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun blackPolylineStyle(){
        blackPolylineOptions = PolylineOptions()
        blackPolylineOptions!!.color(Color.BLACK)
        blackPolylineOptions!!.width(5f)
        blackPolylineOptions!!.startCap(SquareCap())
        blackPolylineOptions!!.jointType(JointType.ROUND)
        blackPolylineOptions!!.addAll(polylineList!!)
        blackPolyline = mMap.addPolyline(blackPolylineOptions!!)
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Animation ---------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun animate(){
        val valueAnimator = ValueAnimator.ofInt(0,100)
        valueAnimator.duration = 1100
        valueAnimator.repeatCount = ValueAnimator.INFINITE
        valueAnimator.interpolator = LinearInterpolator()

        valueAnimator.addUpdateListener { value ->
            val points =  greyPolyline!!.points
            val percentValue = value.animatedValue.toString().toInt()
            val size = points.size
            val newPoints = (size * (percentValue / 100))
            val p = points.subList(0, newPoints)
            blackPolyline!!.points = (p)
        }

        valueAnimator.start()

        latLngBound = LatLngBounds.Builder().include(selectedPlaceEvent!!.origin)
                .include(selectedPlaceEvent!!.destination)
                .build()

        //-------------------------------- Add tie fighter icon ------------------------------------
        addTieFighterIcon()
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Add tie fighter icon ----------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun addTieFighterIcon(){
        val objects = jsonArray.getJSONObject(0)
        val legs = objects.getJSONArray("legs")
        val legsObject = legs.getJSONObject(0)

        val time = legsObject.getJSONObject("duration")
        val duration = time.getString("text")

        startAddress = legsObject.getString("start_address")
        val endAddress = legsObject.getString("end_address")

        addOriginMarker(duration, startAddress)
        addDestinationMarker(endAddress)
        moveCameraToLatLngBounds()
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Move camera to lat lng bounds -------------------------------
    //----------------------------------------------------------------------------------------------

    private fun moveCameraToLatLngBounds() {
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBound, 160))
        zoomToBounds()
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Zoom to bounds ----------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun zoomToBounds(){
        mMap.moveCamera(CameraUpdateFactory.zoomTo(mMap.cameraPosition.zoom-1))
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Add origin marker -------------------------------------------
    //----------------------------------------------------------------------------------------------

    @SuppressLint("InflateParams")
    private fun addOriginMarker(duration: String, startAddress: String) {
        val view = layoutInflater.inflate(R.layout.origin_info_window, null)

        val timeTv = view.findViewById<View>(R.id.time_tv) as TextView
        val originTv = view.findViewById<View>(R.id.origin_tv) as TextView

        timeTv.text = Common.formatDuration(duration)
        originTv.text = Common.formatAddress(startAddress)

        generateIcon(view)

        originMarker = mMap.addMarker(MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(icon))
                .position(selectedPlaceEvent!!.origin))
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Add destination marker --------------------------------------
    //----------------------------------------------------------------------------------------------

    @SuppressLint("InflateParams")
    private fun addDestinationMarker(endAddress: String) {
        val view = layoutInflater.inflate(R.layout.destination_info_window, null)

        val destinationTv = view.findViewById<View>(R.id.destination_tv) as TextView
        destinationTv.text = Common.formatAddress(endAddress)

        generateIcon(view)

        destinationMarker = mMap.addMarker(MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(icon))
                .position(selectedPlaceEvent!!.destination))
    }

    /**-----------------------------------------------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
     *----------------------- Confirm Uber -----------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
    ------------------------------------------------------------------------------------------------------------------------------------------------------*/

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Click on confirm Uber ---------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun clickOnConfirmUber(){
        confirmUberBtn = findViewById(R.id.confirm_uber_btn)
        confirmUberBtn.setOnClickListener {
            showConfirmPickUpLayout()
        }
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Show confirm pickup layout ----------------------------------
    //----------------------------------------------------------------------------------------------

    private fun showConfirmPickUpLayout() {
        confirmPickUpLayout = findViewById(R.id.confirm_pickup_cv)
        confirmUberLayout = findViewById(R.id.confirm_uber_cv)

        confirmPickUpLayout.visibility = View.VISIBLE
        confirmUberLayout.visibility = View.GONE
        setPickUpData()
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Set pick up data --------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun setPickUpData() {
        pickUpAddressTv = findViewById(R.id.pickup_address_tv)
        if (startAddress != null){
            pickUpAddressTv.text = startAddress
        }
        else{
            pickUpAddressTv.text = getString(R.string.none)
        }
        mMap.clear()
        addPickUpMarker()
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Add pickup marker -------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun addPickUpMarker() {
        val view = layoutInflater.inflate(R.layout.pickup_info_window, null)

        generateIcon(view)

        originMarker = mMap.addMarker(MarkerOptions().icon(BitmapDescriptorFactory.fromBitmap(icon)).position(selectedPlaceEvent!!.origin))
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Generate icon -----------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun generateIcon(view: View){
        val generator = IconGenerator(this)
        generator.setContentView(view)
        generator.setBackground(ColorDrawable(Color.TRANSPARENT))
        icon = generator.makeIcon()
    }

    /**-----------------------------------------------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
     *----------------------- Confirm pick up spot ---------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
    ------------------------------------------------------------------------------------------------------------------------------------------------------*/

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Click on confirm pick up spot -------------------------------
    //----------------------------------------------------------------------------------------------

    private fun clickOnConfirmPickUp(){
        confirmPickUpBtn = findViewById(R.id.confirm_pickup_btn)
        confirmPickUpBtn.setOnClickListener {
            if (mMap == null){
                return@setOnClickListener
            }
            if (selectedPlaceEvent == null){
                return@setOnClickListener
            }
            mMap.clear()
            tiltCamera()
        }
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Rotate camera at 360° ---------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun tiltCamera(){
        cameraPosition = CameraPosition.Builder().target(selectedPlaceEvent!!.origin)
                .tilt(45f)
                .zoom(TILT_ZOOM)
                .build()
        moveTiltCamera()
        addMarkerWithPulseAnimator()
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Move tilt camera --------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun moveTiltCamera() {
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Animate tilt camera -----------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun addMarkerWithPulseAnimator(){
        confirmPickUpLayout = findViewById(R.id.confirm_pickup_cv)
        fillMap = findViewById(R.id.fill_map)
        findYourRiderLayout = findViewById(R.id.finding_your_ride_cv)

        confirmPickUpLayout.visibility = View.GONE
        fillMap.visibility = View.VISIBLE
        findYourRiderLayout.visibility = View.VISIBLE

        originMarker = mMap.addMarker(MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .position(selectedPlaceEvent!!.origin))
        addPulsatingEffect(selectedPlaceEvent!!.origin)
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Pulsating effect --------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun addPulsatingEffect(origin: LatLng) {
        if (lastPulseAnimator != null){
            lastPulseAnimator!!.cancel()
        }
        if (lastUserCircle != null){
            lastUserCircle!!.center = origin
        }
        lastPulseAnimator = Common.valueAnimate(DURATION, object: ValueAnimator.AnimatorUpdateListener{
            override fun onAnimationUpdate(valueAnimator: ValueAnimator) {
                if (lastUserCircle != null){
                    lastUserCircle!!.radius = valueAnimator.animatedValue.toString().toDouble()
                }
                else{
                    lastUserCircle = mMap.addCircle(CircleOptions()
                            .center(origin)
                            .radius(valueAnimator.animatedValue.toString().toDouble())
                            .strokeColor(Color.WHITE)
                            .fillColor(ContextCompat.getColor(this@RequestDriverActivity, R.color.map_darker))
                    )
                }
            }
        })
        //-------------------------------- Start rotating camera -----------------------------------
        startMapCameraSpinningAnimation(mMap.cameraPosition.target)
    }

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Camera rotation ---------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun startMapCameraSpinningAnimation(target: LatLng) {
        if (animator != null){
            animator!!.cancel()
        }
        animator = ValueAnimator.ofFloat(0f, (DESIRED_NUMBER_OF_SPIN * 360).toFloat())
        animator!!.duration = (DESIRED_NUMBER_OF_SPIN * DESIRED_SECONDS_FOR_ONE_FULL_ROTATION * 1000).toLong()
        animator!!.interpolator = LinearInterpolator()
        animator!!.startDelay = 100
        animator!!.addUpdateListener { valueAnimator ->
            val newBearingValue = valueAnimator.animatedValue as Float

            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder()
                    .target(target)
                    .zoom(TILT_ZOOM)
                    .tilt(45f)
                    .bearing(newBearingValue)
                    .build()))
        }
        animator!!.start()
        findNearbyDriver(target)
    }

    override fun onDestroy() {
        if (animator != null){
            animator!!.end()
        }
        super.onDestroy()
    }

    /**-----------------------------------------------------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
     *----------------------- Find nearby driver -----------------------------------------------------------------------------------------------------------
     *------------------------------------------------------------------------------------------------------------------------------------------------------
    ------------------------------------------------------------------------------------------------------------------------------------------------------*/

    //----------------------------------------------------------------------------------------------
    //-------------------------------- Find nearby driver ------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun findNearbyDriver(target: LatLng) {
        mainLayout = findViewById(R.id.main_layout)
        if(Common.driverFound.isNotEmpty()){
            //-------------------------------- Get the first driver by default ---------------------
            var min = 0f
            var foundDriver: DriverGeolocation? = null //= Common.driverFound[Common.driverFound.keys.iterator().next()]
            val currentRiderLocation = Location("")
            currentRiderLocation.latitude = target.latitude
            currentRiderLocation.longitude = target.longitude

            for(key in Common.driverFound.keys){
                val driverLocation = Location("")
                driverLocation.latitude = Common.driverFound[key]!!.geoLocation!!.latitude
                driverLocation.longitude = Common.driverFound[key]!!.geoLocation!!.longitude

                //-------------------------------- Init min val & found first driver in list -------
                if (min == 0f){
                    min = driverLocation.distanceTo(currentRiderLocation)
                    if (!Common.driverFound[key]!!.isDeclined) {
                        foundDriver = Common.driverFound[key]
                        //-------------------------------- Exit loop driver already found ----------
                        break
                    } else {
                        //-------------------------------- If already declined ---------------------
                        continue
                    }
                }
                else if (driverLocation.distanceTo(currentRiderLocation) < min){
                    min = driverLocation.distanceTo(currentRiderLocation)
                    if (!Common.driverFound[key]!!.isDeclined) {
                        foundDriver = Common.driverFound[key]
                        //-------------------------------- Exit loop driver already found ----------
                        break
                    } else {
                        //-------------------------------- If already declined ---------------------
                        continue
                    }
                }
            }
            //Snackbar.make(mainLayout, Common.foundDriver(foundDriver!!), Snackbar.LENGTH_LONG).show()
            if (foundDriver != null) {
                UserUtils.sendRequestToDriver(this@RequestDriverActivity, mainLayout, foundDriver, target)
                lastDriverCall = foundDriver
            }
            else{
                KToasty.info(this, getString(R.string.no_driver_accept), Toast.LENGTH_LONG).show()
                lastDriverCall = null
                finish()
            }
        }
        else{
            Snackbar.make(mainLayout, getString(R.string.drivers_not_found), Snackbar.LENGTH_LONG).show()
            lastDriverCall = null
            finish()
        }
    }
}