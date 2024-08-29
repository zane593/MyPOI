package com.example.mypoi

import CategoryData
import DatabaseHelper
import LocationData
import NearbyPointsManager
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var dbHelper: DatabaseHelper
    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private var isMapInitialized = false
    private lateinit var nearbyButton: AppCompatButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        dbHelper = DatabaseHelper(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        val nearbyButton = findViewById<AppCompatButton>(R.id.nearbyButton)
        nearbyButton.setOnClickListener {
            showNearbyPointsDialog()
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val locationButton = findViewById<Button>(R.id.locationButton)

        locationButton.setBackgroundResource(R.drawable.circle_background)
        locationButton.layoutParams.width = 200
        locationButton.layoutParams.height = 200
        locationButton.setPadding(50, 50, 50, 50)

        locationButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.map_marker_plus)
        locationButton.setOnClickListener {
            getLastLocationAndSave()
        }

        val listButton = findViewById<Button>(R.id.listButton)
        listButton.layoutParams.width = 200
        listButton.layoutParams.height = 200
        listButton.setPadding(50, 50, 50, 50)

        listButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.list)


        val aiButton = findViewById<Button>(R.id.nearbyButton)
        aiButton.layoutParams.width = 150
        aiButton.layoutParams.height = 170
        aiButton.setPadding(50, 50, 50, 50)

        aiButton.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.sparkles)



        listButton.setOnClickListener {
            val intent = Intent(this, CategoryListActivity::class.java)
            startActivity(intent)
        }

        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        loadSavedLocations()
        isMapInitialized = true
    }

    override fun onResume() {
        super.onResume()
        if (isMapInitialized) {
            loadSavedLocations()
        }
    }

    private fun loadSavedLocations() {
        map.clear()
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
        val pointNameEditText = dialogView.findViewById<TextInputEditText>(R.id.pointNameEditText)
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
            val pointName = pointNameEditText.text.toString()
            val category = when {
                selectedCategory != "Crea Una Categoria" -> selectedCategory
                newCategory.isNotEmpty() -> newCategory
                else -> "Senza Categoria"
            }

            if (pointName.isEmpty()) {
                Toast.makeText(this, "Inserisci un nome per il punto", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
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

                dbHelper.addLocation(latLng, categoryId, pointName)

                map.addMarker(MarkerOptions()
                    .position(latLng)
                    .title(pointName)
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

    //Fa apparire il dialogo per cercare i punti vicini

    private fun showNearbyPointsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_nearby_points_search, null)
        val categoryAutoComplete = dialogView.findViewById<AutoCompleteTextView>(R.id.categoryAutoCompleteTextView)
        val pointAutoComplete = dialogView.findViewById<AutoCompleteTextView>(R.id.pointAutoCompleteTextView)
        val searchButton = dialogView.findViewById<Button>(R.id.searchButton)

        val categories = dbHelper.getAllCategories()
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories.map { it.name })
        categoryAutoComplete.setAdapter(categoryAdapter)

        var selectedCategory: CategoryData? = null
        var selectedPoint: LocationData? = null

        categoryAutoComplete.setOnItemClickListener { _, _, position, _ ->
            selectedCategory = categories[position]
            val points = dbHelper.getLocationsByCategory(selectedCategory!!.id)
            val pointAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, points.map { it.name })
            pointAutoComplete.setAdapter(pointAdapter)
            pointAutoComplete.text.clear()
            selectedPoint = null
        }

        pointAutoComplete.setOnItemClickListener { _, _, position, _ ->
            selectedPoint = dbHelper.getLocationsByCategory(selectedCategory!!.id)[position]
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setTitle("Cerca punti di interesse vicini")
            .create()

        searchButton.setOnClickListener {
            if (selectedPoint != null) {
                dialog.dismiss()
                searchNearbyPoints(selectedPoint!!)
            } else {
                Toast.makeText(this, "Seleziona un punto", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun searchNearbyPoints(point: LocationData) {
        val loadingDialog = showLoadingDialog()

        val nearbyPointsManager = NearbyPointsManager(
            this,
            getString(R.string.perplexity_api_key),
            getString(R.string.google_api_key),
            getString(R.string.google_cse_id)
        )

        lifecycleScope.launch {
            try {
                val nearbyPoints = nearbyPointsManager.getNearbyPointsOfInterest(point.latLng.latitude, point.latLng.longitude)
                loadingDialog.dismiss()
                showNearbyPointsResultDialog(nearbyPoints)
            } catch (e: Exception) {
                loadingDialog.dismiss()
                Log.e("MainActivity", "Errore nel recupero dei punti di interesse vicini", e)
                Toast.makeText(this@MainActivity, "Errore nel recupero dei punti vicini: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showLoadingDialog(): AlertDialog {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null)
        return MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
            .apply { show() }
    }


    private fun showNearbyPointsResultDialog(nearbyPoints: List<NearbyPointsManager.PointOfInterest>) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_nearby_points_result, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.nearbyPointsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = NearbyPointsAdapter(nearbyPoints) { point ->
            Toast.makeText(this, "Selezionato: ${point.namePOI}", Toast.LENGTH_SHORT).show()
        }

        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setTitle("Punti di interesse vicini")
            .setPositiveButton("Chiudi") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}