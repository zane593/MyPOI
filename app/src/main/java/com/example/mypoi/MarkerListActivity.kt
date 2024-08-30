package com.example.mypoi

import DatabaseHelper
import LocationData
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.gms.maps.model.LatLng

class MarkerListActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: ArrayAdapter<LocationData>
    private lateinit var markers: MutableList<LocationData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_marker_list)

        dbHelper = DatabaseHelper(this)

        val categoryId = intent.getLongExtra("CATEGORY_ID", -1)
        val categoryName = intent.getStringExtra("CATEGORY_NAME") ?: "Unknown Category"

        title = "Markers in $categoryName"

        val markerListView = findViewById<ListView>(R.id.markerListView)
        markers = dbHelper.getLocationsByCategory(categoryId).toMutableList()

        adapter = object : ArrayAdapter<LocationData>(this, R.layout.marker_list_item, markers) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: layoutInflater.inflate(R.layout.marker_list_item, parent, false)
                val marker = getItem(position)
                view.findViewById<TextView>(R.id.tvPointName).text = marker?.name
                view.findViewById<Button>(R.id.btnDelete).setOnClickListener {
                    dbHelper.deleteLocation(marker!!.id)
                    markers.remove(marker)
                    notifyDataSetChanged()
                }
                return view
            }
        }
        markerListView.adapter = adapter

        markerListView.setOnItemClickListener { _, _, position, _ ->
            showEditDialog(markers[position])
        }

        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    }

    private fun showEditDialog(location: LocationData) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_marker, null)
        val etPointName = dialogView.findViewById<TextInputEditText>(R.id.etPointName)
        val etLatitude = dialogView.findViewById<TextInputEditText>(R.id.etLatitude)
        val etLongitude = dialogView.findViewById<TextInputEditText>(R.id.etLongitude)
        val btnModify = dialogView.findViewById<Button>(R.id.btnModify)

        etPointName.setText(location.name)
        etLatitude.setText(location.latLng.latitude.toString())
        etLongitude.setText(location.latLng.longitude.toString())

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnModify.setOnClickListener {
            val newName = etPointName.text.toString()
            val newLat = etLatitude.text.toString().toDoubleOrNull()
            val newLng = etLongitude.text.toString().toDoubleOrNull()
            if (newLat != null && newLng != null && isValidLatLng(newLat, newLng)) {
                val newLocation = LocationData(location.id, LatLng(newLat, newLng), location.category, location.color, newName)
                dbHelper.updateLocation(location.id, newLocation)
                val index = markers.indexOf(location)
                markers[index] = newLocation
                adapter.notifyDataSetChanged()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Invalid coordinates. Latitude must be between -90 and 90, Longitude between -180 and 180", Toast.LENGTH_LONG).show()
            }
        }

        dialog.show()
    }
    private fun isValidLatLng(lat: Double, lng: Double): Boolean {
        return lat in -90.0..90.0 && lng in -180.0..180.0
    }
}