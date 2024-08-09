package com.example.mypoi

import CategoryData
import DatabaseHelper
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class CategoryAdapter(
    private val context: Context,
    private val categories: MutableList<CategoryData>,
    private val dbHelper: DatabaseHelper
) : ArrayAdapter<CategoryData>(context, 0, categories) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.category_item, parent, false)

        val category = categories[position]
        val nameTextView = view.findViewById<TextView>(R.id.categoryName)
        val colorView = view.findViewById<View>(R.id.categoryColor)
        val editButton = view.findViewById<Button>(R.id.editCategoryButton)

        nameTextView.text = category.name

        val colorInt = Color.HSVToColor(floatArrayOf(category.color, 1f, 1f))

        colorView.background.setColorFilter(colorInt, PorterDuff.Mode.SRC_ATOP)

        editButton.setOnClickListener {
            showEditCategoryDialog(category, position)
        }

        view.setOnClickListener {
            val intent = Intent(context, MarkerListActivity::class.java)
            intent.putExtra("CATEGORY_ID", category.id)
            intent.putExtra("CATEGORY_NAME", category.name)
            context.startActivity(intent)
        }

        return view
    }

    private fun showEditCategoryDialog(category: CategoryData, position: Int) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_location, null)
        val categoryAutoCompleteTextView = dialogView.findViewById<AutoCompleteTextView>(R.id.categoryAutoCompleteTextView)
        val newCategoryEditText = dialogView.findViewById<EditText>(R.id.newCategoryEditText)
        val colorContainer = dialogView.findViewById<View>(R.id.colorContainer)
        val buttonSave = dialogView.findViewById<Button>(R.id.buttonSave)

        categoryAutoCompleteTextView.visibility = View.GONE

        newCategoryEditText.setText(category.name)
        colorContainer.visibility = View.VISIBLE

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
            colorView.background = ContextCompat.getDrawable(context, R.drawable.circle_background)?.apply {
                setColorFilter(Color.parseColor(colors[index]), PorterDuff.Mode.SRC_ATOP)
            }
            colorView.foreground = ContextCompat.getDrawable(context, R.drawable.color_circle_selector)
            colorView.setOnClickListener {
                updateColorSelection(it, colors[index])
            }
        }

        updateColorSelection(dialogView.findViewById(colorIds[0]), colors[0])

        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .create()

        buttonSave.setOnClickListener {
            val newName = newCategoryEditText.text.toString()
            if (newName.isNotEmpty()) {
                val newColor = selectedColor.toFloatColor()
                // Aggiorna il database
                dbHelper.updateCategory(category.id, newName, newColor)
                dialog.dismiss()
                // Aggiorna i dati locali e notifica il cambiamento
                categories[position] = CategoryData(category.id, newName, newColor)
                notifyDataSetChanged()
            } else {
                Toast.makeText(context, "Inserisci un nome valido", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun String.toFloatColor(): Float {
        val color = Color.parseColor(this)
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        return hsv[0]
    }
}