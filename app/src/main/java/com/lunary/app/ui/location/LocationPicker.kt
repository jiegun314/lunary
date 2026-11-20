@file:Suppress("DEPRECATION")

package com.lunary.app.ui.location

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.MarkerOptions
import com.amap.api.services.core.AMapException
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.core.PoiItem
import com.amap.api.services.geocoder.GeocodeSearch
import com.amap.api.services.geocoder.RegeocodeQuery
import com.amap.api.services.geocoder.RegeocodeResult
import com.amap.api.services.poisearch.PoiResult
import com.amap.api.services.poisearch.PoiSearch
import com.lunary.app.BuildConfig
import com.lunary.app.domain.model.PickedLocation

private val CHINA_CENTER = LatLng(35.8617, 104.1954)
private const val CHINA_ZOOM = 4.5f
private const val SELECTED_ZOOM = 16f
private val AMAP_KEY_CONFIGURED = BuildConfig.AMAP_API_KEY.isNotBlank()

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
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<PlaceResult>>(emptyList()) }
    var selected by remember { mutableStateOf(initial) }
    var searching by remember { mutableStateOf(false) }
    var reverseLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var marker by remember { mutableStateOf<Marker?>(null) }
    var geocodeSearch by remember { mutableStateOf<GeocodeSearch?>(null) }
    var poiSearch by remember { mutableStateOf<PoiSearch?>(null) }

    val mapView = remember(context) {
        if (AMAP_KEY_CONFIGURED) createMapView(context) else null
    }

    fun applySelection(place: PickedLocation) {
        selected = place
        val map = mapView?.map ?: return
        marker?.remove()
        marker = map.addMarker(
            MarkerOptions()
                .position(LatLng(place.latitude, place.longitude))
                .title(place.name)
                .snippet(place.address)
        )
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(place.latitude, place.longitude),
                SELECTED_ZOOM
            )
        )
    }

    fun reverse(latLng: LatLng) {
        val provisional = PickedLocation(
            name = "地图选点",
            address = "%.5f, %.5f".format(latLng.latitude, latLng.longitude),
            latitude = latLng.latitude,
            longitude = latLng.longitude
        )
        applySelection(provisional)
        if (!AMAP_KEY_CONFIGURED) return
        reverseLoading = true
        val search = runCatching { GeocodeSearch(context) }.getOrNull()
        if (search == null) {
            reverseLoading = false
            error = "地址解析不可用，请稍后重试"
            return
        }
        geocodeSearch = search
        search.setOnGeocodeSearchListener(object : GeocodeSearch.OnGeocodeSearchListener {
            override fun onRegeocodeSearched(result: RegeocodeResult?, code: Int) {
                reverseLoading = false
                if (code != AMapException.CODE_AMAP_SUCCESS) {
                    error = "地址解析失败（$code）"
                    return
                }
                val address = result?.regeocodeAddress ?: return
                val formatted = address.formatAddress.orEmpty()
                val name = address.pois?.firstOrNull()?.title?.takeIf { it.isNotBlank() }
                    ?: address.building?.takeIf { it.isNotBlank() }
                    ?: address.neighborhood?.takeIf { it.isNotBlank() }
                    ?: formatted.substringBefore(",").takeIf { it.isNotBlank() }
                    ?: "地图选点"
                val display = formatted.ifBlank {
                    "%.5f, %.5f".format(provisional.latitude, provisional.longitude)
                }
                applySelection(
                    PickedLocation(
                        name = name,
                        address = display,
                        latitude = provisional.latitude,
                        longitude = provisional.longitude
                    )
                )
                error = null
            }

            override fun onGeocodeSearched(result: com.amap.api.services.geocoder.GeocodeResult?, code: Int) = Unit
        })
        search.getFromLocationAsyn(
            RegeocodeQuery(
                LatLonPoint(provisional.latitude, provisional.longitude),
                200f,
                GeocodeSearch.AMAP
            )
        )
    }

    DisposableEffect(mapView) {
        mapView?.onResume()
        onDispose {
            mapView?.onPause()
            mapView?.onDestroy()
        }
    }

    LaunchedEffect(mapView) {
        val map = mapView?.map ?: return@LaunchedEffect
        map.uiSettings.apply {
            setZoomControlsEnabled(false)
            setCompassEnabled(false)
            setRotateGesturesEnabled(false)
            setTiltGesturesEnabled(false)
            setMyLocationButtonEnabled(false)
        }
        map.setOnMapClickListener { latLng -> reverse(latLng) }
        val start = initial?.let { LatLng(it.latitude, it.longitude) } ?: CHINA_CENTER
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(start, initial?.let { SELECTED_ZOOM } ?: CHINA_ZOOM))
        initial?.let(::applySelection)
    }

    fun search() {
        val q = query.trim()
        if (q.isEmpty() || searching) return
        if (!AMAP_KEY_CONFIGURED) {
            error = "请先在 local.properties 配置 amap.api.key"
            return
        }
        searching = true
        error = null
        val poiQuery = PoiSearch.Query(q, "", "全国").apply {
            setPageSize(8)
            setCityLimit(false)
        }
        val search = runCatching { PoiSearch(context, poiQuery) }.getOrNull()
        if (search == null) {
            searching = false
            error = "地点搜索不可用，请稍后重试"
            return
        }
        poiSearch = search
        search.setOnPoiSearchListener(object : PoiSearch.OnPoiSearchListener {
            override fun onPoiSearched(result: PoiResult?, code: Int) {
                searching = false
                if (code != AMapException.CODE_AMAP_SUCCESS) {
                    results = emptyList()
                    error = "没有找到地点（$code），试试更具体的关键词"
                    return
                }
                results = result?.pois?.mapNotNull { it.toPlaceResult() }.orEmpty()
                if (results.isEmpty()) error = "没有找到地点，试试更具体的关键词"
            }

            override fun onPoiItemSearched(item: PoiItem?, code: Int) = Unit
        })
        search.searchPOIAsyn()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                val focusManager = LocalFocusManager.current
                val keyboard = LocalSoftwareKeyboardController.current
                val view = LocalView.current
                val imm = remember(context) {
                    context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                }
                fun hideKeyboard() {
                    focusManager.clearFocus(force = true)
                    keyboard?.hide()
                    imm.hideSoftInputFromWindow(view.rootView.windowToken, 0)
                }
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
                        onClick = {
                            hideKeyboard()
                            search()
                        },
                        enabled = query.isNotBlank() && !searching && AMAP_KEY_CONFIGURED
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
                                            hideKeyboard()
                                            applySelection(
                                                PickedLocation(
                                                    name = result.name,
                                                    address = result.address,
                                                    latitude = result.latitude,
                                                    longitude = result.longitude
                                                )
                                            )
                                            results = emptyList()
                                            error = null
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
                    if (mapView != null) {
                        AndroidView(
                            factory = { mapView },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceContainerLow),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "未配置高德地图 Key\n请在 local.properties 添加 amap.api.key",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
                        if (AMAP_KEY_CONFIGURED) "点击地图可重新选点" else "配置 Key 后可在地图上选点",
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
    if (!AMAP_KEY_CONFIGURED) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "地图未配置高德 Key",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    val mapView = remember(context) {
        createMapView(context).apply {
            map.uiSettings.apply {
                setZoomControlsEnabled(false)
                setCompassEnabled(false)
                setRotateGesturesEnabled(false)
                setTiltGesturesEnabled(false)
                setMyLocationButtonEnabled(false)
                setScrollGesturesEnabled(false)
                setZoomGesturesEnabled(false)
            }
        }
    }
    var marker by remember { mutableStateOf<Marker?>(null) }
    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDestroy()
        }
    }
    LaunchedEffect(latitude, longitude, title) {
        val map = mapView.map
        marker?.remove()
        marker = map.addMarker(
            MarkerOptions()
                .position(LatLng(latitude, longitude))
                .title(title)
        )
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 15f))
    }
    AndroidView(
        factory = { mapView },
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
    )
}

private fun createMapView(context: Context): MapView {
    return MapView(context).apply {
        onCreate(null)
        map.mapType = AMap.MAP_TYPE_NORMAL
    }
}

private fun PoiItem.toPlaceResult(): PlaceResult? {
    val point = latLonPoint ?: return null
    val address = snippet?.takeIf { it.isNotBlank() } ?: listOfNotNull(
        provinceName?.takeIf { it.isNotBlank() },
        cityName?.takeIf { it.isNotBlank() },
        adName?.takeIf { it.isNotBlank() }
    ).joinToString("")
    return PlaceResult(
        name = title?.takeIf { it.isNotBlank() } ?: "地点",
        address = address.ifBlank { "%.5f, %.5f".format(point.latitude, point.longitude) },
        latitude = point.latitude,
        longitude = point.longitude
    )
}
