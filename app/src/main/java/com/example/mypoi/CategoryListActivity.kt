package com.example.mypoi

import CategoryData
import DatabaseHelper
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class CategoryListActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_list)

        dbHelper = DatabaseHelper(this)

        val categoryListView = findViewById<ListView>(R.id.categoryListView)
        val categories = dbHelper.getAllCategories()

        val adapter = CategoryAdapter(this, categories)
        categoryListView.adapter = adapter

        categoryListView.setOnItemClickListener { _, _, position, _ ->
            val selectedCategory = categories[position]
            val intent = Intent(this, MarkerListActivity::class.java)
            intent.putExtra("CATEGORY_ID", selectedCategory.id)
            intent.putExtra("CATEGORY_NAME", selectedCategory.name)
            startActivity(intent)
        }
    }
}