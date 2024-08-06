import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.google.android.gms.maps.model.LatLng

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    //definiamo le proprietà della tabella del database
    companion object {
        private const val DATABASE_NAME = "LocationsDB"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "locations"
        private const val COLUMN_ID = "id"
        private const val COLUMN_LATITUDE = "latitude"
        private const val COLUMN_LONGITUDE = "longitude"
    }

    //Database viene creato per la prima volta, quindi si va a creare tutta la tabella e la si inizia a riempire
    override fun onCreate(db: SQLiteDatabase) {
        val createTable = "CREATE TABLE $TABLE_NAME ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COLUMN_LATITUDE REAL, $COLUMN_LONGITUDE REAL)"
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    //viene passato la latitudine e longitudine da poi mettere all'interno del database e salvare
    fun addLocation(latLng: LatLng) {
        val db = this.writableDatabase
        //mappa di coppie chiave valore
        val values = ContentValues().apply {
            put(COLUMN_LATITUDE, latLng.latitude)
            put(COLUMN_LONGITUDE, latLng.longitude)
        }
        //inserimento in tabella
        db.insert(TABLE_NAME, null, values)
        db.close()
    }

    //dovrà tornare un lista di lat e lon per poi ciclare creando i marker da vedere quando si apre la mappa
    fun getAllLocations(): List<LatLng> {
        val locations = mutableListOf<LatLng>()
        //operazioni di solo lettura
        val db = this.readableDatabase
        //vogliamo prendere la tabella
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME", null)

        cursor.use {
            //scorre la tabella
            while (it.moveToNext()) {
                //prende i valori dalla colonna di altitudine e longitudine
                val lat = it.getDouble(it.getColumnIndexOrThrow(COLUMN_LATITUDE))
                val lng = it.getDouble(it.getColumnIndexOrThrow(COLUMN_LONGITUDE))
                locations.add(LatLng(lat, lng))
            }
        }
        //ritorna la lista di lat e lon
        return locations
    }
}