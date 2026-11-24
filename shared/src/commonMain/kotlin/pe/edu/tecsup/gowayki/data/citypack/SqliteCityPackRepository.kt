package pe.edu.tecsup.gowayki.data.citypack

import pe.edu.tecsup.gowayki.domain.models.CityMapStop
import pe.edu.tecsup.gowayki.domain.models.CityPackInfo
import pe.edu.tecsup.gowayki.domain.models.CityPattern
import pe.edu.tecsup.gowayki.domain.models.CityPatternStop
import pe.edu.tecsup.gowayki.domain.models.CityPlace
import pe.edu.tecsup.gowayki.domain.models.CityShape
import pe.edu.tecsup.gowayki.domain.models.CityStop
import pe.edu.tecsup.gowayki.domain.models.CityStopService
import pe.edu.tecsup.gowayki.domain.models.CityTransfer
import pe.edu.tecsup.gowayki.domain.models.NavigationSearchGroup
import pe.edu.tecsup.gowayki.domain.models.NavigationSearchResult
import pe.edu.tecsup.gowayki.domain.models.PlaceResult
import pe.edu.tecsup.gowayki.domain.models.TransitDirection
import pe.edu.tecsup.gowayki.domain.models.TransitDirectionKind
import pe.edu.tecsup.gowayki.domain.models.TransitLine
import pe.edu.tecsup.gowayki.domain.money.Money
import pe.edu.tecsup.gowayki.domain.planner.PlannerNetwork
import pe.edu.tecsup.gowayki.domain.repositories.CityPackRepository
import pe.edu.tecsup.gowayki.domain.repositories.PlaceSearchRepository

/**
 * Consultas de solo lectura sobre una conexión ya abierta.
 * La búsqueda de lugares usa el FTS5 `places_fts` del paquete (o LIKE si el SQLite no trae FTS5).
 */
class SqliteCityPackRepository(
    private val connection: DbConnection,
    private val info: CityPackInfo,
) : CityPackRepository, PlaceSearchRepository {
    private var closed = false
    private var lineCache: List<TransitLine>? = null
    private var ftsAvailable = true

    override fun lines(): List<TransitLine> {
        ensureOpen()
        lineCache?.let { return it }
        val directions = LinkedHashMap<String, MutableList<TransitDirection>>()
        connection.forEachRow(LINES_SQL) { cursor ->
            val geometry = parseJson(cursor.string("geometry_json")) as? Map<*, *>
                ?: throw IllegalArgumentException("Geometría inválida.")
            val coordinates = geometry["coordinates"] as? List<*>
                ?: throw IllegalArgumentException("Geometría inválida.")
            val pairs = coordinates.map { point ->
                val pair = point as List<*>
                listOf((pair[1] as Number).toDouble(), (pair[0] as Number).toDouble())
            }
            val lineId = cursor.string("line_id")
            directions.getOrPut(lineId) { mutableListOf() }.add(
                TransitDirection(
                    kind = if (cursor.string("direction") == "ida") {
                        TransitDirectionKind.Ida
                    } else {
                        TransitDirectionKind.Vuelta
                    },
                    routeId = cursor.string("route_id"),
                    headsign = cursor.string("headsign"),
                    longName = cursor.string("long_name"),
                    geometry = pairs,
                ),
            )
        }
        val lines = ArrayList<TransitLine>()
        connection.forEachRow(LINE_ROWS_SQL) { cursor ->
            val id = cursor.string("line_id")
            lines.add(
                TransitLine(
                    id = id,
                    shortName = cursor.string("name"),
                    color = cursor.string("color"),
                    directions = directions[id].orEmpty(),
                ),
            )
        }
        return lines.also { lineCache = it }
    }

    override fun stops(limit: Int, offset: Int): List<CityStop> {
        ensureOpen()
        require(limit in 1..1000) { "limit" }
        require(offset >= 0) { "offset" }
        val stops = ArrayList<CityStop>()
        connection.forEachRow(STOPS_PAGE_SQL, listOf(limit, offset)) { cursor ->
            stops.add(readStop(cursor))
        }
        return stops
    }

    override fun stop(id: String): CityStop? {
        ensureOpen()
        var found: CityStop? = null
        connection.forEachRow(STOP_SQL, listOf(id)) { cursor ->
            found = readStop(cursor)
        }
        return found
    }

    override fun patternsForLine(lineId: String): List<CityPattern> {
        ensureOpen()
        val patterns = ArrayList<CityPattern>()
        connection.forEachRow(PATTERNS_FOR_LINE_SQL, listOf(lineId)) { cursor ->
            patterns.add(
                CityPattern(
                    id = cursor.string("pattern_id"),
                    lineId = cursor.string("line_id"),
                    routeId = cursor.string("route_id"),
                    shapeId = cursor.string("shape_id"),
                    direction = cursor.string("direction"),
                    headsign = cursor.string("headsign"),
                    lengthMeters = cursor.double("length_m"),
                ),
            )
        }
        return patterns
    }

    override fun patternStops(patternId: String): List<CityPatternStop> {
        ensureOpen()
        val stops = ArrayList<CityPatternStop>()
        connection.forEachRow(PATTERN_STOPS_SQL, listOf(patternId)) { cursor ->
            stops.add(
                CityPatternStop(
                    stop = readStop(cursor),
                    sequence = cursor.long("stop_sequence").toInt(),
                    distanceMeters = cursor.double("distance_m"),
                    offsetMeters = cursor.double("offset_m"),
                    shapeLatitude = cursor.double("shape_latitude"),
                    shapeLongitude = cursor.double("shape_longitude"),
                ),
            )
        }
        return stops
    }

    override fun shape(shapeId: String): CityShape? {
        ensureOpen()
        var found: CityShape? = null
        connection.forEachRow(SHAPE_SQL, listOf(shapeId)) { cursor ->
            val geometry = parseJson(cursor.string("geometry_json")) as? Map<*, *>
                ?: throw IllegalArgumentException("Geometría inválida.")
            val coordinates = (geometry["coordinates"] as List<*>).map { point ->
                (point as List<*>).map { value -> (value as Number).toDouble() }
            }
            found = CityShape(
                id = shapeId,
                coordinates = coordinates,
                vertexDistances = jsonDoubles(parseJson(cursor.string("vertex_distances_json"))),
            )
        }
        return found
    }

    override fun transfersFrom(stopId: String): List<CityTransfer> {
        ensureOpen()
        val transfers = ArrayList<CityTransfer>()
        connection.forEachRow(TRANSFERS_SQL, listOf(stopId)) { cursor ->
            transfers.add(
                CityTransfer(
                    toStopId = cursor.string("to_stop_id"),
                    directMeters = cursor.double("direct_distance_m"),
                    walkMeters = cursor.double("walk_distance_m"),
                ),
            )
        }
        return transfers
    }

    override fun searchPlaces(query: String, limit: Int): List<CityPlace> {
        ensureOpen()
        require(limit in 1..100) { "limit" }
        val expression = placesFtsMatchExpression(query) ?: return emptyList()
        if (ftsAvailable) {
            try {
                return queryPlaces(PackSql.PLACES, listOf(expression, limit))
            } catch (e: Exception) {
                if (e.message?.contains("fts5", ignoreCase = true) != true) throw e
                ftsAvailable = false
            }
        }
        return searchPlacesWithLike(query, limit)
    }

    /**
     * El SQLite del sistema de Android no siempre trae FTS5 (p. ej. Android 16):
     * se busca con LIKE por cada palabra en nombre, alias y dirección.
     */
    private fun searchPlacesWithLike(query: String, limit: Int): List<CityPlace> {
        val tokens = query.split(LIKE_TOKEN_SEPARATOR).filter { it.isNotBlank() }.take(12)
        if (tokens.isEmpty()) return emptyList()
        val where = tokens.joinToString(" AND ") {
            "(name LIKE ? ESCAPE '\\' OR aliases LIKE ? ESCAPE '\\' OR address LIKE ? ESCAPE '\\')"
        }
        val args = ArrayList<Any?>()
        tokens.forEach { token ->
            val pattern = "%" + token.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
            repeat(3) { args.add(pattern) }
        }
        val first = "%" + tokens.first().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
        args.add(first)
        args.add(limit)
        val sql = "SELECT place_id,name,category,latitude,longitude,address,coordinate_source FROM places " +
            "WHERE $where ORDER BY CASE WHEN name LIKE ? ESCAPE '\\' THEN 0 ELSE 1 END, length(name) LIMIT ?"
        return queryPlaces(sql, args)
    }

    private fun queryPlaces(sql: String, args: List<Any?>): List<CityPlace> {
        val places = ArrayList<CityPlace>()
        connection.forEachRow(sql, args) { cursor ->
            places.add(
                CityPlace(
                    id = cursor.string("place_id"),
                    name = cursor.string("name"),
                    category = cursor.string("category"),
                    latitude = cursor.double("latitude"),
                    longitude = cursor.double("longitude"),
                    address = cursor.string("address"),
                    coordinateSource = cursor.string("coordinate_source"),
                ),
            )
        }
        return places
    }

    override fun search(query: String): List<PlaceResult> =
        searchPlaces(query).map { it.toPlaceResult() }

    override fun configuration(): Map<String, Any?> {
        ensureOpen()
        val config = LinkedHashMap<String, Any?>()
        connection.forEachRow(PackSql.CONFIG, listOf(PackSql.CITY)) { cursor ->
            config[cursor.string("key")] = parseJson(cursor.string("value_json"))
        }
        return config
    }

    override fun fareForLine(lineId: String): Money? {
        ensureOpen()
        var exists = false
        connection.forEachRow(LINE_EXISTS_SQL, listOf(lineId)) { exists = true }
        if (!exists) return null
        var amount: Double? = null
        var currency: String? = null
        connection.forEachRow(LINE_FARE_SQL, listOf(lineId)) { cursor ->
            amount = cursor.double("amount")
            currency = cursor.string("currency")
        }
        val fare = configuration()["default_fare"] as? Map<*, *>
            ?: throw IllegalArgumentException("Falta default_fare.")
        val defaultAmount = (fare["amount"] as Number).toDouble()
        val defaultCurrency = fare["currency"] as String
        return Money.resolvePackFare(defaultAmount, amount, currency ?: defaultCurrency)
    }

    override fun packageInfo(): CityPackInfo = info

    override fun mapStops(): List<CityMapStop> {
        ensureOpen()
        val services = LinkedHashMap<String, MutableList<CityStopService>>()
        connection.forEachRow(MAP_SERVICES_SQL) { cursor ->
            val stopId = cursor.string("stop_id")
            services.getOrPut(stopId) { mutableListOf() }.add(
                CityStopService(
                    lineId = cursor.string("line_id"),
                    patternId = cursor.string("pattern_id"),
                    routeId = cursor.string("route_id"),
                    direction = cursor.string("direction"),
                    headsign = cursor.string("headsign"),
                ),
            )
        }
        val stops = ArrayList<CityMapStop>()
        connection.forEachRow(MAP_STOPS_SQL) { cursor ->
            val stop = readStop(cursor)
            stops.add(CityMapStop(stop, services[stop.id].orEmpty()))
        }
        return stops
    }

    override fun plannerNetwork(): PlannerNetwork {
        ensureOpen()
        return SqlitePlannerNetworkLoader.load(connection)
    }

    override fun searchNavigation(query: String): List<NavigationSearchResult> {
        ensureOpen()
        if (query.length > 512) throw IllegalArgumentException("Query too long")
        val results = ArrayList<NavigationSearchResult>()
        var places = 0
        var streets = 0
        for (place in searchPlaces(query, limit = 100)) {
            val street = place.category == "street"
            if (street) {
                if (streets >= 20) continue
                streets++
            } else {
                if (places >= 20) continue
                places++
            }
            results.add(
                NavigationSearchResult(
                    id = place.id,
                    name = place.name,
                    detail = place.address,
                    group = if (street) NavigationSearchGroup.Street else NavigationSearchGroup.Place,
                    point = place.toPlaceResult(),
                ),
            )
        }
        val expression = placesFtsMatchExpression(query) ?: return results
        val like = likePrefix(expression)
        connection.forEachRow(NAV_STOPS_SQL, listOf(like)) { cursor ->
            val stop = readStop(cursor)
            results.add(
                NavigationSearchResult(
                    id = stop.id,
                    name = stop.name,
                    detail = "",
                    group = NavigationSearchGroup.Stop,
                    needsReview = stop.needsReview,
                    point = PlaceResult(stop.id, stop.name, "", stop.latitude, stop.longitude),
                ),
            )
        }
        connection.forEachRow(NAV_LINES_SQL, listOf(like)) { cursor ->
            results.add(
                NavigationSearchResult(
                    id = cursor.string("line_id"),
                    name = cursor.string("name"),
                    detail = "",
                    group = NavigationSearchGroup.Line,
                ),
            )
        }
        return results
    }

    override fun close() {
        if (closed) return
        closed = true
        lineCache = null
        connection.close()
    }

    private fun ensureOpen() {
        if (closed) throw IllegalStateException("El repositorio del paquete está cerrado.")
    }

    private fun likePrefix(expression: String): String {
        val token = expression.substringAfter('"').substringBefore('"')
        return token.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
    }

    private companion object {
        const val LINES_SQL =
            "SELECT p.line_id,p.direction,p.route_id,p.headsign,r.long_name,s.geometry_json " +
                "FROM patterns p JOIN routes r ON r.route_id=p.route_id " +
                "JOIN shapes s ON s.shape_id=p.shape_id " +
                "ORDER BY p.line_id,CASE p.direction WHEN 'ida' THEN 0 ELSE 1 END,p.pattern_id"
        const val LINE_ROWS_SQL = "SELECT line_id,name,color FROM lines ORDER BY line_id"
        const val STOPS_PAGE_SQL =
            "SELECT stop_id,name,latitude,longitude,source_kind,name_source,needs_review,used_in_patterns " +
                "FROM stops ORDER BY stop_id LIMIT ? OFFSET ?"
        const val STOP_SQL =
            "SELECT stop_id,name,latitude,longitude,source_kind,name_source,needs_review,used_in_patterns " +
                "FROM stops WHERE stop_id=?"
        const val PATTERNS_FOR_LINE_SQL =
            "SELECT pattern_id,line_id,route_id,shape_id,direction,headsign,length_m FROM patterns " +
                "WHERE line_id=? ORDER BY CASE direction WHEN 'ida' THEN 0 ELSE 1 END,pattern_id"
        const val PATTERN_STOPS_SQL =
            "SELECT s.stop_id,s.name,s.latitude,s.longitude,s.source_kind,s.name_source,s.needs_review," +
                "s.used_in_patterns,ps.stop_sequence,ps.distance_m,ps.offset_m,ps.shape_latitude,ps.shape_longitude " +
                "FROM pattern_stops ps JOIN stops s ON s.stop_id=ps.stop_id " +
                "WHERE ps.pattern_id=? ORDER BY ps.stop_sequence"
        const val SHAPE_SQL =
            "SELECT geometry_json,vertex_distances_json FROM shapes WHERE shape_id=?"
        const val TRANSFERS_SQL =
            "SELECT to_stop_id,direct_distance_m,walk_distance_m FROM transfer_edges " +
                "WHERE from_stop_id=? ORDER BY walk_distance_m,to_stop_id"
        const val LINE_EXISTS_SQL = "SELECT line_id FROM lines WHERE line_id=?"
        const val LINE_FARE_SQL = "SELECT amount,currency FROM line_fares WHERE line_id=?"
        const val MAP_SERVICES_SQL =
            "SELECT DISTINCT ps.stop_id,p.line_id,p.pattern_id,p.route_id,p.direction,p.headsign " +
                "FROM pattern_stops ps JOIN patterns p ON p.pattern_id=ps.pattern_id " +
                "ORDER BY ps.stop_id,p.line_id,p.pattern_id"
        const val MAP_STOPS_SQL =
            "SELECT stop_id,name,latitude,longitude,source_kind,name_source,needs_review,used_in_patterns " +
                "FROM stops WHERE used_in_patterns=1 ORDER BY stop_id"
        const val NAV_STOPS_SQL =
            "SELECT stop_id,name,latitude,longitude,source_kind,name_source,needs_review,used_in_patterns " +
                "FROM stops WHERE used_in_patterns=1 AND name LIKE ? ESCAPE '\\' ORDER BY stop_id LIMIT 20"
        const val NAV_LINES_SQL =
            "SELECT line_id,name FROM lines WHERE name LIKE ? ESCAPE '\\' ORDER BY line_id LIMIT 20"
    }
}

private val LIKE_TOKEN_SEPARATOR = Regex("[^\\p{L}\\p{N}]+")

