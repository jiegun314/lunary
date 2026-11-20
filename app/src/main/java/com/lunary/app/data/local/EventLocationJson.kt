package com.lunary.app.data.local

import com.lunary.app.domain.model.EventLocation
import org.json.JSONArray
import org.json.JSONObject

/**
 * JSON codec for the small, ordered list of places attached to an event.
 * Stored in [com.lunary.app.data.local.entity.EventEntity.locationsJson].
 */
internal object EventLocationJson {
    const val MAX_LOCATIONS = 10

    fun normalize(locations: List<EventLocation>): List<EventLocation> =
        locations.mapNotNull(::sanitize).take(MAX_LOCATIONS)

    fun encode(locations: List<EventLocation>): String? {
        val array = toJsonArray(locations)
        return if (array.length() == 0) null else array.toString()
    }

    fun decode(raw: String?): List<EventLocation> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { fromJsonArray(JSONArray(raw)) }.getOrDefault(emptyList())
    }

    fun toJsonArray(locations: List<EventLocation>): JSONArray {
        val array = JSONArray()
        locations.asSequence()
            .mapNotNull(::sanitize)
            .take(MAX_LOCATIONS)
            .forEach { location ->
                array.put(JSONObject().apply {
                    put("name", location.name)
                    put("address", location.address)
                    location.latitude?.let { put("latitude", it) }
                    location.longitude?.let { put("longitude", it) }
                })
            }
        return array
    }

    fun fromJsonArray(array: JSONArray): List<EventLocation> = buildList {
        for (index in 0 until minOf(array.length(), MAX_LOCATIONS)) {
            val obj = array.optJSONObject(index) ?: continue
            val location = sanitize(
                EventLocation(
                    name = obj.optString("name").trim(),
                    address = obj.optString("address").trim(),
                    latitude = obj.optNullableDouble("latitude"),
                    longitude = obj.optNullableDouble("longitude")
                )
            ) ?: continue
            add(location)
        }
    }

    fun fromLegacy(
        name: String?,
        latitude: Double?,
        longitude: Double?
    ): List<EventLocation> {
        val location = sanitize(
            EventLocation(
                name = name.orEmpty().trim(),
                address = "",
                latitude = latitude,
                longitude = longitude
            )
        )
        return listOfNotNull(location)
    }

    private fun sanitize(location: EventLocation): EventLocation? {
        val name = location.name.trim()
        val address = location.address.trim()
        if (name.isEmpty() && address.isEmpty() && location.latitude == null && location.longitude == null) {
            return null
        }
        val hasLatitude = location.latitude != null
        val hasLongitude = location.longitude != null
        return location.copy(
            name = name,
            address = address,
            latitude = if (hasLatitude && hasLongitude) location.latitude else null,
            longitude = if (hasLatitude && hasLongitude) location.longitude else null
        )
    }

    private fun JSONObject.optNullableDouble(key: String): Double? =
        if (!has(key) || isNull(key)) null else optDouble(key)
}
