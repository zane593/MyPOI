package com.example.mypoi

import NearbyPointsManager

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton

class NearbyPointsActivity : AppCompatActivity() {
    private lateinit var nearbyPointsManager: NearbyPointsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nearbyPointsManager = NearbyPointsManager(
            this,
            getString(R.string.perplexity_api_key),
            getString(R.string.google_api_key),
            getString(R.string.google_cse_id)
        )

    }
}

class NearbyPointsAdapter(
    private val nearbyPoints: List<NearbyPointsManager.PointOfInterest>,
    private val onItemClick: (NearbyPointsManager.PointOfInterest) -> Unit
) : RecyclerView.Adapter<NearbyPointsAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val pointImage: ImageView = view.findViewById(R.id.pointImage)
        val pointName: TextView = view.findViewById(R.id.pointName)
        val pointDescription: TextView = view.findViewById(R.id.pointDescription)
        val directionsButton: MaterialButton = view.findViewById(R.id.directionsButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_nearby_point, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val point = nearbyPoints[position]
        holder.pointName.text = point.namePOI
        holder.pointDescription.text = point.description
        Glide.with(holder.itemView.context).load(point.imageUrl).into(holder.pointImage)

        holder.directionsButton.setOnClickListener {
            val gmmIntentUri = Uri.parse("google.navigation:q=${point.lat},${point.lon}")
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            holder.itemView.context.startActivity(mapIntent)
        }

        holder.itemView.setOnClickListener { onItemClick(point) }
    }

    override fun getItemCount() = nearbyPoints.size
}