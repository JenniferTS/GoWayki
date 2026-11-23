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
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
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

@Composable
actual fun CityMap(modifier: Modifier, overlay: MapOverlay) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    var styleFile by remember { mutableStateOf<File?>(null) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

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
                        view.getMapAsync { map ->
                            map.setStyle(Style.Builder().fromUri(styleFile!!.toURI().toString())) { style ->
                                ensureSources(style)
                                applyOverlay(style, map, overlay)
                            }
                        }
                    }
                },
                update = { view ->
                    view.getMapAsync { map ->
                        map.getStyle { style ->
                            ensureSources(style)
                            applyOverlay(style, map, overlay)
                        }
                    }
                },
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapViewRef?.onDestroy()
            mapViewRef = null
        }
    }
}

private const val ROUTE_SOURCE = "gowayki-route"
private const val ROUTE_LAYER = "gowayki-route-line"
private const val MARKER_SOURCE = "gowayki-markers"
private const val MARKER_LAYER = "gowayki-markers-circle"
private const val STOP_SOURCE = "gowayki-stops"
private const val STOP_LAYER = "gowayki-stops-circle"
private const val USER_SOURCE = "gowayki-user"
private const val USER_LAYER = "gowayki-user-circle"

private fun ensureSources(style: Style) {
    if (style.getSource(ROUTE_SOURCE) == null) {
        style.addSource(GeoJsonSource(ROUTE_SOURCE))
        style.addLayer(
            LineLayer(ROUTE_LAYER, ROUTE_SOURCE).withProperties(
                PropertyFactory.lineColor(AndroidColor.parseColor("#B9532F")),
                PropertyFactory.lineWidth(5f),
            ),
        )
    }
    if (style.getSource(STOP_SOURCE) == null) {
        style.addSource(GeoJsonSource(STOP_SOURCE))
        style.addLayer(
            CircleLayer(STOP_LAYER, STOP_SOURCE).withProperties(
                PropertyFactory.circleRadius(4f),
                PropertyFactory.circleColor(AndroidColor.parseColor("#5C6B73")),
                PropertyFactory.circleOpacity(0.7f),
            ),
        )
    }
    if (style.getSource(MARKER_SOURCE) == null) {
        style.addSource(GeoJsonSource(MARKER_SOURCE))
        style.addLayer(
            CircleLayer(MARKER_LAYER, MARKER_SOURCE).withProperties(
                PropertyFactory.circleRadius(7f),
                PropertyFactory.circleColor(AndroidColor.parseColor("#C47A1A")),
                PropertyFactory.circleStrokeWidth(1.5f),
                PropertyFactory.circleStrokeColor(AndroidColor.WHITE),
            ),
        )
    }
    if (style.getSource(USER_SOURCE) == null) {
        style.addSource(GeoJsonSource(USER_SOURCE))
        style.addLayer(
            CircleLayer(USER_LAYER, USER_SOURCE).withProperties(
                PropertyFactory.circleRadius(8f),
                PropertyFactory.circleColor(AndroidColor.parseColor("#1E88E5")),
                PropertyFactory.circleStrokeWidth(2f),
                PropertyFactory.circleStrokeColor(AndroidColor.WHITE),
            ),
        )
    }
}

private fun applyOverlay(
    style: Style,
    map: org.maplibre.android.maps.MapLibreMap,
    overlay: MapOverlay,
) {
    style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE)?.let { routeSource ->
        if (overlay.route.size >= 2) {
            val line = LineString.fromLngLats(overlay.route.map { Point.fromLngLat(it.longitude, it.latitude) })
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
    style.getSourceAs<GeoJsonSource>(USER_SOURCE)?.let { userSource ->
        val u = overlay.user
        if (u != null) {
            userSource.setGeoJson(Feature.fromGeometry(Point.fromLngLat(u.longitude, u.latitude)))
        } else {
            userSource.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        }
    }
    val focus = overlay.camera
        ?: overlay.user
        ?: overlay.route.getOrNull(overlay.route.size / 2)
        ?: overlay.markers.firstOrNull()
        ?: GeoPoint(-16.399, -71.537)
    map.moveCamera(
        CameraUpdateFactory.newLatLngZoom(LatLng(focus.latitude, focus.longitude), overlay.zoom),
    )
}
