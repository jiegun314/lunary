package com.lunary.app.ui.location

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lunary.app.domain.model.PickedLocation
import java.net.URLEncoder
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

private const val NOMINATIM = "https://nominatim.openstreetmap.org"
private const val USER_AGENT = "Lunary/0.6.2 (Android)"

private data class PlaceResult(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerDialog(
    initial: PickedLocation?,
    onDismiss: () -> Unit,
    onConfirm: (PickedLocation) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<PlaceResult>>(emptyList()) }
    var selected by remember { mutableStateOf(initial) }
    var searching by remember { mutableStateOf(false) }
    var reverseLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(initial?.let { 15.0 } ?: 4.5)
            controller.setCenter(
                initial?.let { GeoPoint(it.latitude, it.longitude) }
                    ?: GeoPoint(35.8617, 104.1954)
            )
        }
    }
    val marker = remember {
        Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            isEnabled = true
        }
    }

    fun applySelection(place: PickedLocation) {
        selected = place
        val point = GeoPoint(place.latitude, place.longitude)
        marker.position = point
        marker.title = place.name
        marker.snippet = place.address
        if (!mapView.overlays.contains(marker)) mapView.overlays.add(marker)
        mapView.controller.animateTo(point)
        mapView.controller.setZoom(16.0)
        mapView.invalidate()
    }

    fun reverse(point: GeoPoint) {
        val provisional = PickedLocation(
            name = "地图选点",
            address = "%.5f, %.5f".format(point.latitude, point.longitude),
            latitude = point.latitude,
            longitude = point.longitude
        )
        applySelection(provisional)
        reverseLoading = true
        scope.launch {
            val resolved = withContext(Dispatchers.IO) { reverseGeocode(point) }
            reverseLoading = false
            if (resolved != null) applySelection(resolved)
        }
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    LaunchedEffect(Unit) {
        if (initial != null) applySelection(initial)
        mapView.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                reverse(p)
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean = false
        }))
    }

    fun search() {
        val q = query.trim()
        if (q.isEmpty() || searching) return
        searching = true
        error = null
        scope.launch {
            val found = withContext(Dispatchers.IO) { searchPlaces(q) }
            searching = false
            results = found
            if (found.isEmpty()) error = "没有找到地点，试试更具体的关键词"
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("选择地点") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "关闭")
                        }
                    }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("搜索地点，例如：西湖") },
                        trailingIcon = {
                            if (searching) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        }
                    )
                    Button(
                        onClick = ::search,
                        enabled = query.isNotBlank() && !searching
                    ) { Text("搜索") }
                }
                if (results.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 190.dp)
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        tonalElevation = 3.dp
                    ) {
                        LazyColumn {
                            items(results) { result ->
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            applySelection(
                                                PickedLocation(
                                                    name = result.name,
                                                    address = result.address,
                                                    latitude = result.latitude,
                                                    longitude = result.longitude
                                                )
                                            )
                                            results = emptyList()
                                        }
                                        .padding(12.dp)
                                ) {
                                    Text(result.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        result.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    AndroidView(
                        factory = { mapView },
                        modifier = Modifier.fillMaxSize(),
                        update = { it.invalidate() }
                    )
                    if (reverseLoading) {
                        CircularProgressIndicator(
                            Modifier.align(Alignment.TopEnd).padding(12.dp).size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    selected?.let { place ->
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(12.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            tonalElevation = 4.dp
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(place.name, style = MaterialTheme.typography.titleSmall)
                                if (place.address.isNotBlank()) {
                                    Text(
                                        place.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                }
                                Text(
                                    "%.5f, %.5f".format(place.latitude, place.longitude),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "点击地图可重新选点",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f).align(Alignment.CenterVertically)
                    )
                    Button(
                        onClick = { selected?.let(onConfirm) },
                        enabled = selected != null
                    ) { Text("确认") }
                }
            }
        }
    }
}

@Composable
fun LocationMapPreview(
    latitude: Double,
    longitude: Double,
    title: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(false)
            setBuiltInZoomControls(false)
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(latitude, longitude))
        }
    }
    val marker = remember {
        Marker(mapView).apply {
            position = GeoPoint(latitude, longitude)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            this.title = title
            isEnabled = true
        }
    }
    DisposableEffect(mapView) {
        mapView.onResume()
        mapView.overlays.add(marker)
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }
    AndroidView(
        factory = { mapView },
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        update = {
            marker.position = GeoPoint(latitude, longitude)
            it.controller.setCenter(GeoPoint(latitude, longitude))
            it.invalidate()
        }
    )
}

private fun searchPlaces(query: String): List<PlaceResult> {
    val encoded = URLEncoder.encode(query, "UTF-8")
    val url = "$NOMINATIM/search?format=jsonv2&limit=6&accept-language=zh-CN&q=$encoded"
    val json = httpGet(url) ?: return emptyList()
    return runCatching {
        val array = JSONArray(json)
        (0 until array.length()).mapNotNull { index ->
            val obj = array.optJSONObject(index) ?: return@mapNotNull null
            val display = obj.optString("display_name").ifBlank { obj.optString("name") }
            val name = obj.optString("name").ifBlank { display.substringBefore(",") }
            PlaceResult(
                name = name.ifBlank { "地点" },
                address = display,
                latitude = obj.optString("lat").toDoubleOrNull() ?: return@mapNotNull null,
                longitude = obj.optString("lon").toDoubleOrNull() ?: return@mapNotNull null
            )
        }
    }.getOrDefault(emptyList())
}

private fun reverseGeocode(point: GeoPoint): PickedLocation? {
    val url = "$NOMINATIM/reverse?format=jsonv2&zoom=18&accept-language=zh-CN&lat=${point.latitude}&lon=${point.longitude}"
    val json = httpGet(url) ?: return null
    return runCatching {
        val obj = org.json.JSONObject(json)
        val display = obj.optString("display_name")
        if (display.isBlank()) return null
        val name = obj.optString("name").ifBlank { display.substringBefore(",") }
        PickedLocation(name, display, point.latitude, point.longitude)
    }.getOrNull()
}

private fun httpGet(url: String): String? {
    var connection: HttpURLConnection? = null
    return try {
        connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.setRequestProperty("Accept", "application/json")
        if (connection.responseCode !in 200..299) return null
        connection.inputStream.bufferedReader().use { it.readText() }
    } catch (_: Exception) {
        null
    } finally {
        connection?.disconnect()
    }
}
