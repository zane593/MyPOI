package com.example.mypoi

import DatabaseHelper
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity

class MarkerListActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_marker_list)

        dbHelper = DatabaseHelper(this)

        val categoryId = intent.getLongExtra("CATEGORY_ID", -1)
        val categoryName = intent.getStringExtra("CATEGORY_NAME") ?: "Unknown Category"

        title = "Markers in $categoryName"

        val markerListView = findViewById<ListView>(R.id.markerListView)
        val markers = dbHelper.getLocationsByCategory(categoryId)

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, markers.map {
            "Lat: ${it.latLng.latitude}, Lng: ${it.latLng.longitude}"
        })
        markerListView.adapter = adapter
    }
}