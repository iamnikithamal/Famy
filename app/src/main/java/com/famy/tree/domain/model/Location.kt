package com.famy.tree.domain.model

data class GeocodedLocation(
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
    val address: LocationAddress? = null,
    val type: String? = null,
    val importance: Double = 0.0
) {
    val shortName: String
        get() = address?.let { addr ->
            listOfNotNull(
                addr.city ?: addr.town ?: addr.village,
                addr.state,
                addr.country
            ).joinToString(", ")
        } ?: displayName.split(",").take(3).joinToString(",").trim()
}

data class LocationAddress(
    val houseNumber: String? = null,
    val road: String? = null,
    val neighbourhood: String? = null,
    val suburb: String? = null,
    val city: String? = null,
    val town: String? = null,
    val village: String? = null,
    val county: String? = null,
    val state: String? = null,
    val stateDistrict: String? = null,
    val postcode: String? = null,
    val country: String? = null,
    val countryCode: String? = null
)

sealed class LocationResult<out T> {
    data class Success<T>(val data: T) : LocationResult<T>()
    data class Error(val message: String, val exception: Exception? = null) : LocationResult<Nothing>()
    data object Loading : LocationResult<Nothing>()
}
