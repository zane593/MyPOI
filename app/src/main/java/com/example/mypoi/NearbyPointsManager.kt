import android.content.Context

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class NearbyPointsManager(
    private val context: Context,
    private val perplexityApiKey: String,
    private val googleApiKey: String,
    private val googleCseId: String
) {
    suspend fun getNearbyPointsOfInterest(lat: Double, lon: Double): List<PointOfInterest> = withContext(Dispatchers.IO) {
        val city = getCityFromCoordinates(lat, lon) ?: return@withContext emptyList()
        getNearbyPOI(lat, lon, city)
    }

    private suspend fun getNearbyPOI(lat: Double, lon: Double, city: String): List<PointOfInterest> {
        val url = URL("https://api.perplexity.ai/chat/completions")
        val connection = withContext(Dispatchers.IO) {
            url.openConnection()
        } as HttpsURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Bearer $perplexityApiKey")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        val message = """
        Find 3 points of interest in $city that are different from the exact location of these coordinates: latitude $lat, longitude $lon.
        Respond with a JSON array containing 3 objects in this format:
        [
          {
            "namePOI": "",
            "specificName": "",
            "lat": "",
            "lon": "",
            "description": ""
          },
          {
            "namePOI": "",
            "specificName": "",
            "lat": "",
            "lon": "",
            "description": ""
          },
          {
            "namePOI": "",
            "specificName": "",
            "lat": "",
            "lon": "",
            "description": ""
          }
        ]
        The 'namePOI' should be the name of the point of interest.
        The 'specificName' should include the name of the POI and the city for more precise identification.
        The 'lat' and 'lon' should be the coordinates of the point of interest.
        The 'description' should be a brief 2-line description of the point of interest.
        Ensure the points of interest are within the city but not at the exact coordinates provided.
        Do not include any other text in your response.
    """.trimIndent()

        val body = JSONObject().apply {
            put("model", "llama-3.1-sonar-small-128k-chat")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are an AI assistant that provides information about points of interest. Respond only with a JSON array containing the requested information.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", message)
                })
            })
        }

        connection.outputStream.use { it.write(body.toString().toByteArray()) }

        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val jsonResponse = JSONObject(response)
        val content = jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        val poiArray = JSONArray(content)

        return (0 until poiArray.length()).map { i ->
            val poiData = poiArray.getJSONObject(i)
            val imageUrl = getGoogleImage(poiData.getString("specificName"))
            PointOfInterest(
                poiData.getString("namePOI"),
                poiData.getString("specificName"),
                poiData.getDouble("lat"),
                poiData.getDouble("lon"),
                imageUrl,
                city,
                poiData.getString("description")
            )
        }
    }

    private suspend fun getCityFromCoordinates(lat: Double, lon: Double): String? = withContext(Dispatchers.IO) {
        val url = URL("https://maps.googleapis.com/maps/api/geocode/json?latlng=$lat,$lon&key=$googleApiKey")
        val response = url.readText()
        val jsonResponse = JSONObject(response)

        if (jsonResponse.getString("status") == "OK") {
            val results = jsonResponse.getJSONArray("results")
            for (i in 0 until results.length()) {
                val result = results.getJSONObject(i)
                val addressComponents = result.getJSONArray("address_components")
                for (j in 0 until addressComponents.length()) {
                    val component = addressComponents.getJSONObject(j)
                    val types = component.getJSONArray("types")
                    if (types.toString().contains("locality")) {
                        return@withContext component.getString("long_name")
                    }
                }
            }
        }
        null
    }

    private suspend fun getGoogleImage(query: String): String? = withContext(Dispatchers.IO) {
        val url = URL("https://www.googleapis.com/customsearch/v1?key=$googleApiKey&cx=$googleCseId&q=$query&searchType=image&num=1&siteSearch=wikipedia.org")
        val response = url.readText()
        val jsonResponse = JSONObject(response)

        if (jsonResponse.has("items") && jsonResponse.getJSONArray("items").length() > 0) {
            jsonResponse.getJSONArray("items").getJSONObject(0).getString("link")
        } else {
            null
        }
    }

    data class PointOfInterest(
        val namePOI: String,
        val specificName: String,
        val lat: Double,
        val lon: Double,
        val imageUrl: String?,
        val city: String,
        val description: String
    )
}