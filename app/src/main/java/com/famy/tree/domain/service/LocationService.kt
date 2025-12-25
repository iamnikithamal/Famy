package com.famy.tree.domain.service

import com.famy.tree.domain.model.GeocodedLocation
import com.famy.tree.domain.model.LocationResult

interface LocationService {
    suspend fun searchLocations(query: String, limit: Int = 10): LocationResult<List<GeocodedLocation>>
    suspend fun reverseGeocode(latitude: Double, longitude: Double): LocationResult<GeocodedLocation>
}
