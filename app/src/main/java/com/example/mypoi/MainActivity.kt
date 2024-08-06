package com.example.mypoi

import DatabaseHelper
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var dbHelper: DatabaseHelper
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        findViewById<Button>(R.id.locationButton).setOnClickListener {
            getLastLocationAndSave()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        loadSavedLocations()
    }

    private fun loadSavedLocations() {
        val locations = dbHelper.getAllLocations()
        for (location in locations) {
            map.addMarker(MarkerOptions().position(location))
        }
        if (locations.isNotEmpty()) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(locations.last(), 10f))
        }
    }

    private fun getLastLocationAndSave() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        val latLng = LatLng(it.latitude, it.longitude)
                        dbHelper.addLocation(latLng)
                        map.addMarker(MarkerOptions().position(latLng))
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                        Toast.makeText(this, "Posizione salvata", Toast.LENGTH_SHORT).show()
                    } ?: Toast.makeText(this, "Posizione non disponibile", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    getLastLocationAndSave()
                } else {
                    Toast.makeText(this, "Permesso di localizzazione negato", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }
}