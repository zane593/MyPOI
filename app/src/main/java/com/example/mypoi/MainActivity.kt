package com.example.mypoi

import DatabaseHelper
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder

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

        findViewById<View>(R.id.locationButton).setOnClickListener {
            getLastLocationAndSave()
        }

        findViewById<View>(R.id.listButton).setOnClickListener {
            val intent = Intent(this, CategoryListActivity::class.java)
            startActivity(intent)
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
                .icon(BitmapDescriptorFactory.defaultMarker(location.color))
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
        val categoryAutoCompleteTextView = dialogView.findViewById<AutoCompleteTextView>(R.id.categoryAutoCompleteTextView)
        val newCategoryEditText = dialogView.findViewById<EditText>(R.id.newCategoryEditText)
        val buttonSave = dialogView.findViewById<Button>(R.id.buttonSave)
        val colorContainer = dialogView.findViewById<View>(R.id.colorContainer)

        val categories = dbHelper.getAllCategories()
        val categoryNames = listOf("Crea Categoria") + categories.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categoryNames)
        categoryAutoCompleteTextView.setAdapter(adapter)

        categoryAutoCompleteTextView.setText("Crea Categoria", false)

        val colorIds = listOf(R.id.color1, R.id.color2, R.id.color3, R.id.color4, R.id.color5, R.id.color6,
            R.id.color7, R.id.color8, R.id.color9, R.id.color10, R.id.color11, R.id.color12)
        val colors = listOf("#e76f51", "#f4a261", "#e9c46a", "#2a9d8f", "#264653", "#ef476f",
            "#588157", "#9c6644", "#a7c957", "#c1121f", "#c77dff", "#5c4d7d")
        var selectedColorView: View? = null
        var selectedColor = colors[0]

        fun updateColorSelection(view: View, color: String) {
            selectedColorView?.isSelected = false
            view.isSelected = true
            selectedColorView = view
            selectedColor = color
        }

        colorIds.forEachIndexed { index, colorId ->
            val colorView = dialogView.findViewById<View>(colorId)
            colorView.background = ContextCompat.getDrawable(this, R.drawable.circle_background)?.apply {
                setColorFilter(Color.parseColor(colors[index]), PorterDuff.Mode.SRC_ATOP)
            }
            colorView.foreground = ContextCompat.getDrawable(this, R.drawable.color_circle_selector)
            colorView.setOnClickListener {
                updateColorSelection(it, colors[index])
            }
        }

        updateColorSelection(dialogView.findViewById(colorIds[0]), colors[0])

        categoryAutoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
            if (position == 0) {
                newCategoryEditText.text?.clear()
                newCategoryEditText.isEnabled = true
                newCategoryEditText.visibility = View.VISIBLE
                colorContainer.visibility = View.VISIBLE
            } else {
                newCategoryEditText.text?.clear()
                newCategoryEditText.isEnabled = false
                newCategoryEditText.visibility = View.GONE
                colorContainer.visibility = View.GONE
                selectedColor = categories[position - 1].color.toString()
            }
        }

        newCategoryEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                categoryAutoCompleteTextView.setText("Crea Una Categoria", false)
                colorContainer.visibility = View.VISIBLE
            }
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        buttonSave.setOnClickListener {
            val selectedCategory = categoryAutoCompleteTextView.text.toString()
            val newCategory = newCategoryEditText.text.toString()
            val category = when {
                selectedCategory != "Crea Una Categoria" -> selectedCategory
                newCategory.isNotEmpty() -> newCategory
                else -> "Senza Categoria"
            }

            try {
                val categoryId: Long
                val markerColor: Float

                if (category !in categories.map { it.name }) {
                    val colorFloat = selectedColor.toFloatColor()
                    categoryId = dbHelper.addCategory(category, colorFloat)
                    markerColor = colorFloat
                } else {
                    categoryId = dbHelper.getCategoryId(category) ?: run {
                        Toast.makeText(this, "Errore: categoria non trovata", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    markerColor = dbHelper.getCategoryColor(category) ?: run {
                        Toast.makeText(this, "Errore: colore non trovato", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }

                dbHelper.addLocation(latLng, categoryId)

                map.addMarker(MarkerOptions()
                    .position(latLng)
                    .title(category)
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor)))
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                Toast.makeText(this, "Posizione salvata", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            } catch (e: Exception) {
                Toast.makeText(this, "Si è verificato un errore durante il salvataggio: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }


        dialog.show()
    }

    fun String.toFloatColor(): Float {
        val color = Color.parseColor(this)
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        return hsv[0]
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
            }
        }
    }
}
