package pe.edu.tecsup.gowayki.ui.map

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import pe.edu.tecsup.gowayki.ui.theme.GowaykiSpace
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
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
import pe.edu.tecsup.gowayki.ui.LocalStrings
import pe.edu.tecsup.gowayki.ui.theme.GowaykiColors
import pe.edu.tecsup.gowayki.ui.theme.GowaykiTheme
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material.MaterialTheme
import java.io.File
import kotlin.math.abs

@Composable
actual fun CityMap(modifier: Modifier, overlay: MapOverlay) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    var styleFile by remember { mutableStateOf<File?>(null) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    val fitHolder = remember { FitKeyHolder() }

    val dark = GowaykiTheme.colors.isDark
    LaunchedEffect(dark) {
        runCatching {
            MapLibre.getInstance(context.applicationContext)
            styleFile = withContext(Dispatchers.IO) {
                OfflineMapInstaller(context.applicationContext).installStyle(dark = dark)
            }
        }.onFailure { error = it.message ?: "mapa" }
    }

    Box(modifier) {
        if (error != null) {
            Text(
                LocalStrings.current.mapUnavailable,
                style = MaterialTheme.typography.body2,
                color = GowaykiTheme.colors.onSurfaceMuted,
                modifier = Modifier.align(Alignment.Center),
            )
        } else if (styleFile != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    MapView(ctx, MapLibreMapOptions.createFromAttributes(ctx).textureMode(true)).also { view ->
                        mapViewRef = view
                        view.onCreate(null)
                        view.onStart()
                        view.onResume()
                        view.getMapAsync { map ->
                            map.uiSettings.isCompassEnabled = true
                            map.uiSettings.isAttributionEnabled = false
                            map.uiSettings.isLogoEnabled = false
                            val uri = "file://" + styleFile!!.absolutePath
                            fitHolder.styleUri = uri
                            map.setStyle(Style.Builder().fromUri(uri)) { style ->
                                runCatching {
                                    ensureSources(style)
                                    fitHolder.key = applyOverlay(
                                        style,
                                        map,
                                        overlay,
                                        fitHolder.key,
                                        forceFit = true,
                                        density = view.resources.displayMetrics.density,
                                        viewHeightPx = view.height,
                                    )
                                }
                            }
                        }
                    }
                },
                update = { view ->
                    val uri = "file://" + styleFile!!.absolutePath
                    view.getMapAsync { map ->
                        if (fitHolder.styleUri != null && fitHolder.styleUri != uri) {
                            fitHolder.styleUri = uri
                            map.setStyle(Style.Builder().fromUri(uri)) { style ->
                                runCatching {
                                    ensureSources(style)
                                    fitHolder.key = applyOverlay(
                                        style,
                                        map,
                                        overlay,
                                        fitHolder.key,
                                        forceFit = true,
                                        density = view.resources.displayMetrics.density,
                                        viewHeightPx = view.height,
                                    )
                                }
                            }
                            return@getMapAsync
                        }
                        map.getStyle { style ->
                            runCatching {
                                ensureSources(style)
                                fitHolder.key = applyOverlay(
                                    style,
                                    map,
                                    overlay,
                                    fitHolder.key,
                                    forceFit = false,
                                    density = view.resources.displayMetrics.density,
                                    viewHeightPx = view.height,
                                )
                            }.onFailure {
                                // No tumbar la Activity si el estilo aún no está listo o la cota es inválida.
                            }
                        }
                    }
                },
            )
            // Atribución propia en Compose (la de MapLibre es un “i” sin texto visible y su diálogo
            // no maneja bien atribuciones sin enlace). Abajo a la izquierda: el FAB va a la derecha
            // y en Resultados sube sobre la hoja con [MapOverlay.fitPaddingBottomDp].
            MapAttribution(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = GowaykiSpace.xs, bottom = overlay.fitPaddingBottomDp.dp),
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

private class FitKeyHolder(var key: String = "", var styleUri: String? = null)

private const val WALK_SOURCE = "gowayki-walk"
private const val WALK_LAYER = "gowayki-walk-line"
private const val TRANSIT_SOURCE = "gowayki-transit"
private const val TRANSIT_CASING = "gowayki-transit-casing"
private const val TRANSIT_LAYER = "gowayki-transit-line"
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
    if (style.getSource(WALK_SOURCE) == null) {
        style.addSource(GeoJsonSource(WALK_SOURCE))
        style.addLayer(
            LineLayer(WALK_LAYER, WALK_SOURCE).withProperties(
                PropertyFactory.lineColor(GowaykiColors.Walk.toArgb()),
                PropertyFactory.lineWidth(4.5f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                PropertyFactory.lineDasharray(arrayOf(1.2f, 1.6f)),
                PropertyFactory.lineOpacity(0.95f),
            ),
        )
    }
    if (style.getSource(TRANSIT_SOURCE) == null) {
        style.addSource(GeoJsonSource(TRANSIT_SOURCE))
        style.addLayer(
            LineLayer(TRANSIT_CASING, TRANSIT_SOURCE).withProperties(
                PropertyFactory.lineColor(AndroidColor.WHITE),
                PropertyFactory.lineWidth(11f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                PropertyFactory.lineOpacity(0.95f),
            ),
        )
        style.addLayer(
            LineLayer(TRANSIT_LAYER, TRANSIT_SOURCE).withProperties(
                PropertyFactory.lineColor(Expression.get("color")),
                PropertyFactory.lineWidth(6f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            ),
        )
    }
    if (style.getSource(ROUTE_SOURCE) == null) {
        style.addSource(GeoJsonSource(ROUTE_SOURCE))
        style.addLayer(
            LineLayer(ROUTE_CASING, ROUTE_SOURCE).withProperties(
                PropertyFactory.lineColor(AndroidColor.WHITE),
                PropertyFactory.lineWidth(10f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                PropertyFactory.lineOpacity(0.92f),
            ),
        )
        style.addLayer(
            LineLayer(ROUTE_LAYER, ROUTE_SOURCE).withProperties(
                PropertyFactory.lineColor(GowaykiColors.BrandOrange.toArgb()),
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
                PropertyFactory.circleColor(GowaykiColors.Walk.toArgb()),
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
                PropertyFactory.circleColor(GowaykiColors.MapStop.toArgb()),
                PropertyFactory.circleStrokeWidth(2f),
                PropertyFactory.circleStrokeColor(AndroidColor.WHITE),
            ),
        )
    }
    if (style.getSource(ENDPOINT_SOURCE) == null) {
        style.addSource(GeoJsonSource(ENDPOINT_SOURCE))
        style.addLayer(
            CircleLayer(ENDPOINT_LAYER, ENDPOINT_SOURCE).withProperties(
                PropertyFactory.circleRadius(10f),
                PropertyFactory.circleColor(Expression.get("role")),
                PropertyFactory.circleStrokeWidth(3f),
                PropertyFactory.circleStrokeColor(AndroidColor.WHITE),
            ),
        )
    }
    if (style.getSource(USER_SOURCE) == null) {
        style.addSource(GeoJsonSource(USER_SOURCE))
        style.addLayer(
            CircleLayer(USER_LAYER, USER_SOURCE).withProperties(
                PropertyFactory.circleRadius(9f),
                PropertyFactory.circleColor(GowaykiColors.UserDot.toArgb()),
                PropertyFactory.circleStrokeWidth(3f),
                PropertyFactory.circleStrokeColor(AndroidColor.WHITE),
            ),
        )
    }
}

private fun routeFingerprint(route: List<GeoPoint>, segments: List<RouteSegment>): String {
    val base = if (segments.isNotEmpty()) {
        segments.joinToString(";") {
            val pts = it.points
            if (pts.isEmpty()) it.kind.name
            else "${it.kind}:${pts.size}:${pts.first().latitude},${pts.last().longitude}"
        }
    } else if (route.isEmpty()) {
        "empty"
    } else {
        val first = route.first()
        val last = route.last()
        "${route.size}|${first.latitude},${first.longitude}|${last.latitude},${last.longitude}"
    }
    return base
}

private fun toLineFeature(points: List<GeoPoint>, colorHex: String? = null): Feature? {
    val clean = dedupeNearby(points)
    if (clean.size < 2) return null
    val line = LineString.fromLngLats(clean.map { Point.fromLngLat(it.longitude, it.latitude) })
    return Feature.fromGeometry(line).also { f ->
        if (colorHex != null) f.addStringProperty("color", colorHex)
    }
}

private fun applyOverlay(
    style: Style,
    map: org.maplibre.android.maps.MapLibreMap,
    overlay: MapOverlay,
    previousFitKey: String,
    forceFit: Boolean,
    density: Float,
    viewHeightPx: Int,
): String {
    val segments = overlay.segments
    val fallbackRoute = dedupeNearby(overlay.route)

    if (segments.isNotEmpty()) {
        val walkFeatures = segments.filter { it.kind == RouteKind.Walk }.mapNotNull {
            toLineFeature(it.points)
        }
        val transitFeatures = segments.filter { it.kind == RouteKind.Transit }.mapNotNull {
            toLineFeature(it.points, it.colorHex)
        }
        style.getSourceAs<GeoJsonSource>(WALK_SOURCE)
            ?.setGeoJson(FeatureCollection.fromFeatures(walkFeatures))
        style.getSourceAs<GeoJsonSource>(TRANSIT_SOURCE)
            ?.setGeoJson(FeatureCollection.fromFeatures(transitFeatures))
        style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE)
            ?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
    } else {
        style.getSourceAs<GeoJsonSource>(WALK_SOURCE)
            ?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        style.getSourceAs<GeoJsonSource>(TRANSIT_SOURCE)
            ?.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE)?.let { routeSource ->
            if (fallbackRoute.size >= 2) {
                val line = LineString.fromLngLats(
                    fallbackRoute.map { Point.fromLngLat(it.longitude, it.latitude) },
                )
                routeSource.setGeoJson(Feature.fromGeometry(line))
            } else {
                routeSource.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            }
        }
    }

    style.getSourceAs<GeoJsonSource>(STOP_SOURCE)?.setGeoJson(
        FeatureCollection.fromFeatures(
            overlay.stops.take(140).map {
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

    val path = overlay.effectiveRoute.let(::dedupeNearby)
    val endpoints = buildList {
        if (path.size >= 2) {
            add(path.first() to "#34A853") // Google-like green start
            add(path.last() to "#EA4335") // Google-like red end
        }
    }
    style.getSourceAs<GeoJsonSource>(ENDPOINT_SOURCE)?.setGeoJson(
        FeatureCollection.fromFeatures(
            endpoints.map { (p, color) ->
                Feature.fromGeometry(Point.fromLngLat(p.longitude, p.latitude)).also {
                    it.addStringProperty("role", color)
                }
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

    val fitKey = routeFingerprint(fallbackRoute, segments) + "|" +
        (overlay.camera?.let { "${it.latitude},${it.longitude}" } ?: "") +
        "|${overlay.zoom}|fu=${overlay.followEpoch}|pb=${overlay.fitPaddingBottomDp}"
    if (forceFit || fitKey != previousFitKey) {
        val maxVertical = if (viewHeightPx > 0) viewHeightPx * 0.2f else Float.MAX_VALUE
        val bottomPad = overlay.fitPaddingBottomDp.takeIf { it > 0 }?.let { (it * density).toInt() }
            ?: (72 * density).coerceAtMost(maxVertical).toInt()
        val side = (32 * density).toInt()
        val top = (96 * density).coerceAtMost(maxVertical).toInt()
        val prevFollow = Regex("""\|fu=(\d+)""").find(previousFitKey)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
        val followUser = overlay.followEpoch > 0L && overlay.user != null &&
            overlay.followEpoch != prevFollow
        when {
            followUser -> {
                val u = overlay.user
                map.easeCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(u.latitude, u.longitude), 15.6),
                    420,
                )
            }
            path.size >= 2 -> {
                val builder = LatLngBounds.Builder()
                path.forEach { builder.include(LatLng(it.latitude, it.longitude)) }
                overlay.user?.let { builder.include(LatLng(it.latitude, it.longitude)) }
                runCatching {
                    map.easeCamera(
                        CameraUpdateFactory.newLatLngBounds(
                            builder.build(),
                            side,
                            top,
                            side,
                            bottomPad,
                        ),
                        480,
                    )
                }.onFailure {
                    val mid = path[path.size / 2]
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(mid.latitude, mid.longitude), overlay.zoom))
                }
            }
            else -> {
                val focus = overlay.user
                    ?: overlay.camera
                    ?: overlay.markers.firstOrNull()
                    ?: GeoPoint(-16.399, -71.537)
                map.easeCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(focus.latitude, focus.longitude), overlay.zoom),
                    380,
                )
            }
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
