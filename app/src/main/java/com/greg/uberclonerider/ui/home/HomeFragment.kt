package com.greg.uberclonerider.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.greg.uberclonerider.Constant.Companion.DEFAULT_ZOOM
import com.greg.uberclonerider.R
import com.greg.uberclonerider.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var map: GoogleMap
    //------------------- Location -----------------------------------------------------------------
    private lateinit var rioDeJaneiro: LatLng

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        homeViewModel =
                ViewModelProvider(this).get(HomeViewModel::class.java)
        binding = FragmentHomeBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(callback)
    }

    //----------------------------------------------------------------------------------------------
    //------------------- Show map when ready & add a maker ----------------------------------------
    //----------------------------------------------------------------------------------------------

    private val callback = OnMapReadyCallback { googleMap ->
        rioDeJaneiro = LatLng(-22.933988, -43.171960)
        map = googleMap
        googleMap.addMarker(MarkerOptions()
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            .position(rioDeJaneiro).title(getString(R.string.user_position_message))
        )
        moveCamera()
        zoomOnLocation()
    }

    //----------------------------------------------------------------------------------------------
    //------------------- Move camera --------------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun moveCamera() {
        map.moveCamera(CameraUpdateFactory.newLatLng(rioDeJaneiro))
    }

    //----------------------------------------------------------------------------------------------
    //------------------- Zoom level ---------------------------------------------------------------
    //----------------------------------------------------------------------------------------------

    private fun zoomOnLocation(){
        map.animateCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM))
    }
}