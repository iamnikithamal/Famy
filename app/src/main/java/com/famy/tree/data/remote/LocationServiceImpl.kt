package com.famy.tree.data.remote

import com.famy.tree.domain.model.GeocodedLocation
import com.famy.tree.domain.model.LocationAddress
import com.famy.tree.domain.model.LocationResult
import com.famy.tree.domain.service.LocationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationServiceImpl @Inject constructor() : LocationService {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    companion object {
        private const val NOMINATIM_SEARCH_URL = "https://nominatim.openstreetmap.org/search"
        private const val NOMINATIM_REVERSE_URL = "https://nominatim.openstreetmap.org/reverse"
        private const val PHOTON_SEARCH_URL = "https://photon.komoot.io/api/"
        private const val PHOTON_REVERSE_URL = "https://photon.komoot.io/reverse"
        private const val USER_AGENT = "FamyFamilyTreeApp/1.0"
        private const val CONNECT_TIMEOUT = 10000
        private const val READ_TIMEOUT = 15000
    }

    override suspend fun searchLocations(query: String, limit: Int): LocationResult<List<GeocodedLocation>> {
        return withContext(Dispatchers.IO) {
            try {
                val photonResult = searchWithPhoton(query, limit)
                if (photonResult is LocationResult.Success && photonResult.data.isNotEmpty()) {
                    return@withContext photonResult
                }

                val nominatimResult = searchWithNominatim(query, limit)
                if (nominatimResult is LocationResult.Success && nominatimResult.data.isNotEmpty()) {
                    return@withContext nominatimResult
                }

                if (photonResult is LocationResult.Error && nominatimResult is LocationResult.Error) {
                    LocationResult.Error("Both geocoding services failed: ${nominatimResult.message}")
                } else {
                    LocationResult.Success(emptyList())
                }
            } catch (e: Exception) {
                LocationResult.Error("Search failed: ${e.message}", e)
            }
        }
    }

    override suspend fun reverseGeocode(latitude: Double, longitude: Double): LocationResult<GeocodedLocation> {
        return withContext(Dispatchers.IO) {
            try {
                val photonResult = reverseWithPhoton(latitude, longitude)
                if (photonResult is LocationResult.Success) {
                    return@withContext photonResult
                }

                val nominatimResult = reverseWithNominatim(latitude, longitude)
                if (nominatimResult is LocationResult.Success) {
                    return@withContext nominatimResult
                }

                LocationResult.Error("Reverse geocoding failed")
            } catch (e: Exception) {
                LocationResult.Error("Reverse geocoding failed: ${e.message}", e)
            }
        }
    }

    private fun searchWithNominatim(query: String, limit: Int): LocationResult<List<GeocodedLocation>> {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val urlString = "$NOMINATIM_SEARCH_URL?q=$encodedQuery&format=json&addressdetails=1&limit=$limit"
            val response = makeRequest(urlString)

            val results: List<NominatimSearchResult> = json.decodeFromString(response)
            LocationResult.Success(results.map { it.toGeocodedLocation() })
        } catch (e: Exception) {
            LocationResult.Error("Nominatim search failed: ${e.message}", e)
        }
    }

    private fun searchWithPhoton(query: String, limit: Int): LocationResult<List<GeocodedLocation>> {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val urlString = "$PHOTON_SEARCH_URL?q=$encodedQuery&limit=$limit"
            val response = makeRequest(urlString)

            val results: PhotonResponse = json.decodeFromString(response)
            LocationResult.Success(results.features.map { it.toGeocodedLocation() })
        } catch (e: Exception) {
            LocationResult.Error("Photon search failed: ${e.message}", e)
        }
    }

    private fun reverseWithNominatim(latitude: Double, longitude: Double): LocationResult<GeocodedLocation> {
        return try {
            val urlString = "$NOMINATIM_REVERSE_URL?lat=$latitude&lon=$longitude&format=json&addressdetails=1"
            val response = makeRequest(urlString)

            val result: NominatimSearchResult = json.decodeFromString(response)
            LocationResult.Success(result.toGeocodedLocation())
        } catch (e: Exception) {
            LocationResult.Error("Nominatim reverse failed: ${e.message}", e)
        }
    }

    private fun reverseWithPhoton(latitude: Double, longitude: Double): LocationResult<GeocodedLocation> {
        return try {
            val urlString = "$PHOTON_REVERSE_URL?lat=$latitude&lon=$longitude"
            val response = makeRequest(urlString)

            val result: PhotonResponse = json.decodeFromString(response)
            val feature = result.features.firstOrNull()
                ?: return LocationResult.Error("No results found")
            LocationResult.Success(feature.toGeocodedLocation())
        } catch (e: Exception) {
            LocationResult.Error("Photon reverse failed: ${e.message}", e)
        }
    }

    private fun makeRequest(urlString: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
            connectTimeout = CONNECT_TIMEOUT
            readTimeout = READ_TIMEOUT
        }

        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP error: ${connection.responseCode}")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}

@Serializable
private data class NominatimSearchResult(
    val place_id: Long? = null,
    val licence: String? = null,
    val osm_type: String? = null,
    val osm_id: Long? = null,
    val lat: String,
    val lon: String,
    val display_name: String,
    val type: String? = null,
    val importance: Double? = null,
    val address: NominatimAddress? = null
) {
    fun toGeocodedLocation(): GeocodedLocation = GeocodedLocation(
        displayName = display_name,
        latitude = lat.toDoubleOrNull() ?: 0.0,
        longitude = lon.toDoubleOrNull() ?: 0.0,
        address = address?.toLocationAddress(),
        type = type,
        importance = importance ?: 0.0
    )
}

@Serializable
private data class NominatimAddress(
    val house_number: String? = null,
    val road: String? = null,
    val neighbourhood: String? = null,
    val suburb: String? = null,
    val city: String? = null,
    val town: String? = null,
    val village: String? = null,
    val county: String? = null,
    val state: String? = null,
    val state_district: String? = null,
    val postcode: String? = null,
    val country: String? = null,
    val country_code: String? = null
) {
    fun toLocationAddress(): LocationAddress = LocationAddress(
        houseNumber = house_number,
        road = road,
        neighbourhood = neighbourhood,
        suburb = suburb,
        city = city,
        town = town,
        village = village,
        county = county,
        state = state,
        stateDistrict = state_district,
        postcode = postcode,
        country = country,
        countryCode = country_code
    )
}

@Serializable
private data class PhotonResponse(
    val features: List<PhotonFeature> = emptyList()
)

@Serializable
private data class PhotonFeature(
    val geometry: PhotonGeometry,
    val properties: PhotonProperties
) {
    fun toGeocodedLocation(): GeocodedLocation {
        val props = properties
        val displayParts = listOfNotNull(
            props.name,
            props.street,
            props.city ?: props.town ?: props.village,
            props.state,
            props.country
        ).filter { it.isNotBlank() }

        return GeocodedLocation(
            displayName = displayParts.joinToString(", "),
            latitude = geometry.coordinates.getOrNull(1) ?: 0.0,
            longitude = geometry.coordinates.getOrNull(0) ?: 0.0,
            address = LocationAddress(
                houseNumber = props.housenumber,
                road = props.street,
                suburb = props.district,
                city = props.city,
                town = props.town,
                village = props.village,
                county = props.county,
                state = props.state,
                postcode = props.postcode,
                country = props.country,
                countryCode = props.countrycode
            ),
            type = props.osm_type,
            importance = 0.0
        )
    }
}

@Serializable
private data class PhotonGeometry(
    val type: String = "",
    val coordinates: List<Double> = emptyList()
)

@Serializable
private data class PhotonProperties(
    val osm_id: Long? = null,
    val osm_type: String? = null,
    val osm_key: String? = null,
    val osm_value: String? = null,
    val name: String? = null,
    val housenumber: String? = null,
    val street: String? = null,
    val district: String? = null,
    val city: String? = null,
    val town: String? = null,
    val village: String? = null,
    val county: String? = null,
    val state: String? = null,
    val postcode: String? = null,
    val country: String? = null,
    val countrycode: String? = null
)
