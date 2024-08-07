package com.example.mypoi

import DatabaseHelper
import LocationData
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
                view.findViewById<TextView>(R.id.tvLatitude).text = "Lat: ${marker?.latLng?.latitude}"
                view.findViewById<TextView>(R.id.tvLongitude).text = "Lng: ${marker?.latLng?.longitude}"
                return view
            }
        }
        markerListView.adapter = adapter

        markerListView.setOnItemClickListener { _, _, position, _ ->
            showEditDialog(markers[position])
        }
    }

    private fun showEditDialog(location: LocationData) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_marker, null)
        val etLatitude = dialogView.findViewById<EditText>(R.id.etLatitude)
        val etLongitude = dialogView.findViewById<EditText>(R.id.etLongitude)
        val btnModify = dialogView.findViewById<Button>(R.id.btnModify)
        val btnDelete = dialogView.findViewById<Button>(R.id.btnDelete)

        etLatitude.setText(location.latLng.latitude.toString())
        etLongitude.setText(location.latLng.longitude.toString())

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnModify.setOnClickListener {
            val newLat = etLatitude.text.toString().toDoubleOrNull()
            val newLng = etLongitude.text.toString().toDoubleOrNull()
            if (newLat != null && newLng != null) {
                val newLocation = LocationData(location.id, LatLng(newLat, newLng), location.category, location.color)
                dbHelper.updateLocation(location.id, newLocation)
                val index = markers.indexOf(location)
                markers[index] = newLocation
                adapter.notifyDataSetChanged()
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Invalid coordinates", Toast.LENGTH_SHORT).show()
            }
        }

        btnDelete.setOnClickListener {
            dbHelper.deleteLocation(location.id)
            markers.remove(location)
            adapter.notifyDataSetChanged()
            dialog.dismiss()
        }

        dialog.show()
    }
}