package com.example.photosapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

/**
 * A base abstract class for Fragments containing a Google MapView.
 * Subclasses must implement the necessary methods for map setup and management.
 * @property mapView The MapView associated with this Fragment
 * @property googleMap The GoogleMap object representing the map
 * @property pendingLocationUpdate The LatLng object that is pending an location update
 */
abstract class BaseMapFragment : Fragment(), OnMapReadyCallback {
    protected var mapView: MapView? = null
    private var googleMap: GoogleMap? = null
    private var pendingLocationUpdate: LatLng? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        mapView = view?.findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
        return view
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        setupMap()
        pendingLocationUpdate?.let {
            updateMapLocation(it)
            pendingLocationUpdate = null
        }

    }

    protected open fun setupMap() {
        mapView?.visibility = View.GONE
    }

    /**
     * Updates the map location to the given LatLng coordinates.
     *
     * @param latLng The LatLng coordinates to update the map to.
     */
    private fun updateMapLocation(latLng: LatLng) {
        googleMap?.apply {
            clear()
            addMarker(MarkerOptions().position(latLng).title("Photo Location"))
            moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10.0f))
            mapView?.visibility = View.VISIBLE
        }
    }

    /**
     * Queues a location update to be applied to the map.
     * If the map is not yet initialized, the update will be pending until the map is ready.
     *
     * @param latLng The LatLng coordinates to update the map to.
     */
    fun queueLocationUpdate(latLng: LatLng) {
        if (googleMap == null) {
            pendingLocationUpdate = latLng
        } else {
            pendingLocationUpdate = latLng
            updateMapLocation(latLng)
        }
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        mapView?.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        mapView?.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }
}