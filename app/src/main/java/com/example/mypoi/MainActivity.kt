package com.example.mypoi

import DatabaseHelper
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
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
            val markerOptions = MarkerOptions()
                .position(location.latLng)
                .title(location.category)
                .icon(BitmapDescriptorFactory.defaultMarker(location.color.toFloat()))
            map.addMarker(markerOptions)
        }
        if (locations.isNotEmpty()) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(locations.last().latLng, 10f))
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
                        showAddLocationDialog(LatLng(it.latitude, it.longitude))
                    } ?: Toast.makeText(this, "Posizione non disponibile", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showAddLocationDialog(latLng: LatLng) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_location, null)
        val editTextCategory = dialogView.findViewById<EditText>(R.id.editTextCategory)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val buttonSave = dialogView.findViewById<Button>(R.id.buttonSave)
        val colorContainer = dialogView.findViewById<LinearLayout>(R.id.colorContainer)

        val categories = dbHelper.getAllCategories()
        val categoryNames = categories.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, listOf("Nuova Categoria") + categoryNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        var selectedColor = BitmapDescriptorFactory.HUE_RED
        val colorRed = dialogView.findViewById<View>(R.id.colorRed)
        val colorGreen = dialogView.findViewById<View>(R.id.colorGreen)
        val colorBlue = dialogView.findViewById<View>(R.id.colorBlue)
        val borderRed = dialogView.findViewById<View>(R.id.borderRed)
        val borderGreen = dialogView.findViewById<View>(R.id.borderGreen)
        val borderBlue = dialogView.findViewById<View>(R.id.borderBlue)

        fun updateColorSelection(color: Float, selectedBorder: View) {
            selectedColor = color
            borderRed.setBackgroundColor(Color.TRANSPARENT)
            borderGreen.setBackgroundColor(Color.TRANSPARENT)
            borderBlue.setBackgroundColor(Color.TRANSPARENT)
            selectedBorder.setBackgroundColor(Color.YELLOW)
        }

        colorRed.setOnClickListener { updateColorSelection(BitmapDescriptorFactory.HUE_RED, borderRed) }
        colorGreen.setOnClickListener { updateColorSelection(BitmapDescriptorFactory.HUE_GREEN, borderGreen) }
        colorBlue.setOnClickListener { updateColorSelection(BitmapDescriptorFactory.HUE_BLUE, borderBlue) }

        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    editTextCategory.isEnabled = true
                    colorContainer.visibility = View.VISIBLE
                } else {
                    editTextCategory.setText("")
                    editTextCategory.isEnabled = false
                    colorContainer.visibility = View.GONE
                    selectedColor = categories[position - 1].color.toFloat()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        buttonSave.setOnClickListener {
            val category = if (editTextCategory.text.toString().isNotEmpty()) {
                editTextCategory.text.toString()
            } else {
                spinnerCategory.selectedItem?.toString() ?: "Senza Categoria"
            }

            val categoryId: Long
            if (category !in categoryNames) {
                categoryId = dbHelper.addCategory(category, selectedColor.toInt())
            } else {
                categoryId = dbHelper.getCategoryId(category) ?: return@setOnClickListener
            }

            dbHelper.addLocation(latLng, categoryId)
            val color = dbHelper.getCategoryColor(category) ?: selectedColor.toInt()

            map.addMarker(MarkerOptions()
                .position(latLng)
                .title(category)
                .icon(BitmapDescriptorFactory.defaultMarker(color.toFloat()))
            )
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            Toast.makeText(this, "Posizione salvata", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
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