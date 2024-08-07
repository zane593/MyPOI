package com.example.mypoi

import CategoryData
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class CategoryAdapter(context: Context, private val categories: List<CategoryData>) :
    ArrayAdapter<CategoryData>(context, 0, categories) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.category_item, parent, false)

        val category = categories[position]
        val nameTextView = view.findViewById<TextView>(R.id.categoryName)
        val colorView = view.findViewById<View>(R.id.categoryColor)

        nameTextView.text = category.name

        // Converti il colore float in un colore intero
        val colorInt = Color.HSVToColor(floatArrayOf(category.color, 1f, 1f))

        // Imposta il colore del background
        colorView.background.setColorFilter(colorInt, PorterDuff.Mode.SRC_ATOP)

        return view
    }
}