import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.android.gms.maps.model.LatLng

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "LocationsDB"
        private const val DATABASE_VERSION = 3
        private const val TABLE_LOCATIONS = "locations"
        private const val TABLE_CATEGORIES = "categories"
        private const val COLUMN_ID = "id"
        private const val COLUMN_LATITUDE = "latitude"
        private const val COLUMN_LONGITUDE = "longitude"
        private const val COLUMN_CATEGORY_ID = "category_id"
        private const val COLUMN_CATEGORY_NAME = "category_name"
        private const val COLUMN_COLOR = "color"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createCategoriesTable = """
            CREATE TABLE $TABLE_CATEGORIES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_CATEGORY_NAME TEXT UNIQUE,
                $COLUMN_COLOR INTEGER
            )
        """.trimIndent()
        db.execSQL(createCategoriesTable)

        val createLocationsTable = """
            CREATE TABLE $TABLE_LOCATIONS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_LATITUDE REAL,
                $COLUMN_LONGITUDE REAL,
                $COLUMN_CATEGORY_ID INTEGER,
                FOREIGN KEY($COLUMN_CATEGORY_ID) REFERENCES $TABLE_CATEGORIES($COLUMN_ID)
            )
        """.trimIndent()
        db.execSQL(createLocationsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LOCATIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CATEGORIES")
        onCreate(db)
    }

    fun addLocation(latLng: LatLng, categoryId: Long) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_LATITUDE, latLng.latitude)
            put(COLUMN_LONGITUDE, latLng.longitude)
            put(COLUMN_CATEGORY_ID, categoryId)
        }
        db.insert(TABLE_LOCATIONS, null, values)
        db.close()
    }

    fun addCategory(categoryName: String, color: Float): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CATEGORY_NAME, categoryName)
            put(COLUMN_COLOR, color)
        }
        val id = db.insertWithOnConflict(TABLE_CATEGORIES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
        return id
    }

    fun getLocationsByCategory(categoryId: Long): List<LocationData> {
        val locations = mutableListOf<LocationData>()
        val db = this.readableDatabase
        val query = """
            SELECT l.$COLUMN_ID, l.$COLUMN_LATITUDE, l.$COLUMN_LONGITUDE, c.$COLUMN_CATEGORY_NAME, c.$COLUMN_COLOR
            FROM $TABLE_LOCATIONS l
            JOIN $TABLE_CATEGORIES c ON l.$COLUMN_CATEGORY_ID = c.$COLUMN_ID
            WHERE c.$COLUMN_ID = ?
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(categoryId.toString()))

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID))
                val lat = it.getDouble(it.getColumnIndexOrThrow(COLUMN_LATITUDE))
                val lng = it.getDouble(it.getColumnIndexOrThrow(COLUMN_LONGITUDE))
                val category = it.getString(it.getColumnIndexOrThrow(COLUMN_CATEGORY_NAME))
                val color = it.getInt(it.getColumnIndexOrThrow(COLUMN_COLOR))
                locations.add(LocationData(id, LatLng(lat, lng), category, color))
            }
        }
        return locations
    }


    fun getAllLocations(): List<LocationData> {
        val locations = mutableListOf<LocationData>()
        val db = this.readableDatabase
        val query = """
            SELECT l.$COLUMN_ID, l.$COLUMN_LATITUDE, l.$COLUMN_LONGITUDE, c.$COLUMN_CATEGORY_NAME, c.$COLUMN_COLOR
            FROM $TABLE_LOCATIONS l
            JOIN $TABLE_CATEGORIES c ON l.$COLUMN_CATEGORY_ID = c.$COLUMN_ID
        """.trimIndent()
        val cursor = db.rawQuery(query, null)

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID))
                val lat = it.getDouble(it.getColumnIndexOrThrow(COLUMN_LATITUDE))
                val lng = it.getDouble(it.getColumnIndexOrThrow(COLUMN_LONGITUDE))
                val category = it.getString(it.getColumnIndexOrThrow(COLUMN_CATEGORY_NAME))
                val color = it.getInt(it.getColumnIndexOrThrow(COLUMN_COLOR))
                locations.add(LocationData(id, LatLng(lat, lng), category, color))
            }
        }
        return locations
    }

    fun getAllCategories(): List<CategoryData> {
        val categories = mutableListOf<CategoryData>()
        val db = this.readableDatabase
        val cursor = db.query(TABLE_CATEGORIES, null, null, null, null, null, null)

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID))
                val category = it.getString(it.getColumnIndexOrThrow(COLUMN_CATEGORY_NAME))
                val color = it.getFloat(it.getColumnIndexOrThrow(COLUMN_COLOR))
                categories.add(CategoryData(id, category, color))
            }
        }
        return categories
    }

    fun getCategoryId(categoryName: String): Long? {
        val db = this.readableDatabase
        val cursor = db.query(TABLE_CATEGORIES, arrayOf(COLUMN_ID), "$COLUMN_CATEGORY_NAME = ?", arrayOf(categoryName), null, null, null)

        return cursor.use {
            if (it.moveToFirst()) {
                it.getLong(it.getColumnIndexOrThrow(COLUMN_ID))
            } else {
                null
            }
        }
    }

    fun updateLocation(id: Long, newLocation: LocationData) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_LATITUDE, newLocation.latLng.latitude)
            put(COLUMN_LONGITUDE, newLocation.latLng.longitude)
        }
        db.update(TABLE_LOCATIONS, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
        db.close()
    }

    fun deleteLocation(id: Long) {
        val db = this.writableDatabase
        db.delete(TABLE_LOCATIONS, "$COLUMN_ID = ?", arrayOf(id.toString()))
        db.close()
    }
}



data class LocationData(val id: Long, val latLng: LatLng, val category: String, val color: Int)
data class CategoryData(val id: Long, val name: String, val color: Float)