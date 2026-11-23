package pe.edu.tecsup.gowayki.ui.map

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import pe.edu.tecsup.gowayki.data.map.OfflineMapInstaller
import pe.edu.tecsup.gowayki.domain.planner.GeoPoint
import pe.edu.tecsup.gowayki.ui.theme.GowaykiColors
import java.io.File
import kotlin.math.abs

@Composable
actual fun CityMap(modifier: Modifier, overlay: MapOverlay) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    var styleFile by remember { mutableStateOf<File?>(null) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    val fitHolder = remember { FitKeyHolder() }

    LaunchedEffect(Unit) {
        runCatching {
            MapLibre.getInstance(context.applicationContext)
            styleFile = OfflineMapInstaller(context).installStyle(dark = false)
        }.onFailure { error = it.message ?: "mapa" }
    }

    Box(modifier) {
        if (error != null) {
            Text(
                "Mapa offline no disponible",
                color = GowaykiColors.Stone,
                modifier = Modifier.align(Alignment.Center),
            )
        } else if (styleFile != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx).also { view ->
                        mapViewRef = view
                        view.onCreate(null)
                        view.onStart()
                        view.onResume()
                        view.getMapAsync { map ->
                            map.uiSettings.isCompassEnabled = false
                            map.setStyle(Style.Builder().fromUri(styleFile!!.toURI().toString())) { style ->
                                ensureSources(style)
                                fitHolder.key = applyOverlay(style, map, overlay, fitHolder.key, forceFit = true)
                            }
                        }
                    }
                },
                update = { view ->
                    view.getMapAsync { map ->
                        map.getStyle { style ->
                            ensureSources(style)
                            fitHolder.key = applyOverlay(style, map, overlay, fitHolder.key, forceFit = false)
                        }
                    }
                },
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val view = mapViewRef
            mapViewRef = null
            if (view != null) {
                runCatching {
                    view.onPause()
                    view.onStop()
                    view.onDestroy()
                }
            }
        }
    }
}

private class FitKeyHolder(var key: String = "")

private const val ROUTE_SOURCE = "gowayki-route"
private const val ROUTE_CASING = "gowayki-route-casing"
private const val ROUTE_LAYER = "gowayki-route-line"
private const val MARKER_SOURCE = "gowayki-markers"
private const val MARKER_LAYER = "gowayki-markers-circle"
private const val STOP_SOURCE = "gowayki-stops"
private const val STOP_LAYER = "gowayki-stops-circle"
private const val USER_SOURCE = "gowayki-user"
private const val USER_LAYER = "gowayki-user-circle"
private const val ENDPOINT_SOURCE = "gowayki-endpoints"
private const val ENDPOINT_LAYER = "gowayki-endpoints-circle"

private fun ensureSources(style: Style) {
    if (style.getSource(ROUTE_SOURCE) == null) {
        style.addSource(GeoJsonSource(ROUTE_SOURCE))
        style.addLayer(
            LineLayer(ROUTE_CASING, ROUTE_SOURCE).withProperties(
                PropertyFactory.lineColor(AndroidColor.parseColor("#FFFFFF")),
                PropertyFactory.lineWidth(10f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                PropertyFactory.lineOpacity(0.92f),
            ),
        )
        style.addLayer(
            LineLayer(ROUTE_LAYER, ROUTE_SOURCE).withProperties(
                PropertyFactory.lineColor(AndroidColor.parseColor("#C45A32")),
                PropertyFactory.lineWidth(5.5f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            ),
        )
    }
    if (style.getSource(STOP_SOURCE) == null) {
        style.addSource(GeoJsonSource(STOP_SOURCE))
        style.addLayer(
            CircleLayer(STOP_LAYER, STOP_SOURCE).withProperties(
                PropertyFactory.circleRadius(4.5f),
                PropertyFactory.circleColor(AndroidColor.parseColor("#5C6B73")),
                PropertyFactory.circleStrokeWidth(1.2f),
                PropertyFactory.circleStrokeColor(AndroidColor.WHITE),
                PropertyFactory.circleOpacity(0.85f),
            ),
        )
    }
    if (style.getSource(MARKER_SOURCE) == null) {
        style.addSource(GeoJsonSource(MARKER_SOURCE))
        style.addLayer(
            CircleLayer(MARKER_LAYER, MARKER_SOURCE).withProperties(
                PropertyFactory.circleRadius(7.5f),
                PropertyFactory.circleColor(AndroidColor.parseColor("#C47A1A")),
                PropertyFactory.circleStrokeWidth(2f),
                PropertyFactory.circleStrokeColor(AndroidColor.WHITE),
            ),
        )
    }
    if (style.getSource(ENDPOINT_SOURCE) == null) {
        style.addSource(GeoJsonSource(ENDPOINT_SOURCE))
        style.addLayer(
            CircleLayer(ENDPOINT_LAYER, ENDPOINT_SOURCE).withProperties(
                PropertyFactory.circleRadius(9f),
                PropertyFactory.circleColor(AndroidColor.parseColor("#1F6FE5")),
                PropertyFactory.circleStrokeWidth(2.5f),
                PropertyFactory.circleStrokeColor(AndroidColor.WHITE),
            ),
        )
    }
    if (style.getSource(USER_SOURCE) == null) {
        style.addSource(GeoJsonSource(USER_SOURCE))
        style.addLayer(
            CircleLayer(USER_LAYER, USER_SOURCE).withProperties(
                PropertyFactory.circleRadius(8.5f),
                PropertyFactory.circleColor(AndroidColor.parseColor("#1E88E5")),
                PropertyFactory.circleStrokeWidth(2.5f),
                PropertyFactory.circleStrokeColor(AndroidColor.WHITE),
            ),
        )
    }
}

private fun routeFingerprint(route: List<GeoPoint>): String {
    if (route.isEmpty()) return "empty"
    val first = route.first()
    val last = route.last()
    val mid = route[route.size / 2]
    return "${route.size}|${first.latitude},${first.longitude}|${mid.latitude},${mid.longitude}|${last.latitude},${last.longitude}"
}

private fun applyOverlay(
    style: Style,
    map: org.maplibre.android.maps.MapLibreMap,
    overlay: MapOverlay,
    previousFitKey: String,
    forceFit: Boolean,
): String {
    val route = dedupeNearby(overlay.route)
    style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE)?.let { routeSource ->
        if (route.size >= 2) {
            val line = LineString.fromLngLats(route.map { Point.fromLngLat(it.longitude, it.latitude) })
            routeSource.setGeoJson(Feature.fromGeometry(line))
        } else {
            routeSource.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        }
    }
    style.getSourceAs<GeoJsonSource>(STOP_SOURCE)?.setGeoJson(
        FeatureCollection.fromFeatures(
            overlay.stops.take(120).map {
                Feature.fromGeometry(Point.fromLngLat(it.longitude, it.latitude))
            },
        ),
    )
    style.getSourceAs<GeoJsonSource>(MARKER_SOURCE)?.setGeoJson(
        FeatureCollection.fromFeatures(
            overlay.markers.take(40).map {
                Feature.fromGeometry(Point.fromLngLat(it.longitude, it.latitude))
            },
        ),
    )
    val endpoints = buildList {
        if (route.size >= 2) {
            add(route.first())
            add(route.last())
        }
    }
    style.getSourceAs<GeoJsonSource>(ENDPOINT_SOURCE)?.setGeoJson(
        FeatureCollection.fromFeatures(
            endpoints.map {
                Feature.fromGeometry(Point.fromLngLat(it.longitude, it.latitude))
            },
        ),
    )
    style.getSourceAs<GeoJsonSource>(USER_SOURCE)?.let { userSource ->
        val u = overlay.user
        if (u != null) {
            userSource.setGeoJson(Feature.fromGeometry(Point.fromLngLat(u.longitude, u.latitude)))
        } else {
            userSource.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        }
    }

    val fitKey = routeFingerprint(route) + "|" +
        (overlay.camera?.let { "${it.latitude},${it.longitude}" } ?: "") + "|${overlay.zoom}"
    if (forceFit || fitKey != previousFitKey) {
        if (route.size >= 2) {
            val builder = LatLngBounds.Builder()
            route.forEach { builder.include(LatLng(it.latitude, it.longitude)) }
            overlay.user?.let { builder.include(LatLng(it.latitude, it.longitude)) }
            overlay.markers.take(8).forEach { builder.include(LatLng(it.latitude, it.longitude)) }
            runCatching {
                map.easeCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 56), 450)
            }.onFailure {
                val mid = route[route.size / 2]
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(mid.latitude, mid.longitude), overlay.zoom))
            }
        } else {
            val focus = overlay.camera
                ?: overlay.user
                ?: overlay.markers.firstOrNull()
                ?: GeoPoint(-16.399, -71.537)
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(focus.latitude, focus.longitude), overlay.zoom),
            )
        }
    }
    return fitKey
}

private fun dedupeNearby(points: List<GeoPoint>): List<GeoPoint> {
    if (points.size < 2) return points
    val out = ArrayList<GeoPoint>(points.size)
    out.add(points.first())
    for (i in 1 until points.size) {
        val prev = out.last()
        val cur = points[i]
        if (abs(prev.latitude - cur.latitude) > 1e-7 || abs(prev.longitude - cur.longitude) > 1e-7) {
            out.add(cur)
        }
    }
    return out
}
