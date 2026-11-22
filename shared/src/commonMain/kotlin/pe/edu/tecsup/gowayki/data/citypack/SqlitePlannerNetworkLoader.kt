package pe.edu.tecsup.gowayki.data.citypack

import pe.edu.tecsup.gowayki.domain.models.CityStop
import pe.edu.tecsup.gowayki.domain.models.CityTransfer
import pe.edu.tecsup.gowayki.domain.money.Money
import pe.edu.tecsup.gowayki.domain.planner.GeoPoint
import pe.edu.tecsup.gowayki.domain.planner.PatternVisit
import pe.edu.tecsup.gowayki.domain.planner.PlannerNetwork
import pe.edu.tecsup.gowayki.domain.planner.PlannerPattern
import pe.edu.tecsup.gowayki.domain.planner.PlannerSettings
import pe.edu.tecsup.gowayki.domain.planner.PlannerShape

object PackSql {
    const val CITY = "arequipa"

    const val CONFIG = "SELECT key,value_json FROM config WHERE city_code=?"

    const val STOPS =
        "SELECT stop_id,name,latitude,longitude,source_kind,name_source,needs_review,used_in_patterns FROM stops"

    const val SHAPES = "SELECT shape_id,geometry_json,vertex_distances_json FROM shapes"

    const val VISITS =
        "SELECT pattern_id,stop_id,distance_m FROM pattern_stops ORDER BY pattern_id,stop_sequence"

    const val PATTERNS =
        "SELECT p.pattern_id,p.line_id,l.name AS line_name,l.color,p.direction,p.headsign,p.shape_id," +
            "f.amount AS fare_amount,f.currency AS fare_currency " +
            "FROM patterns p JOIN lines l ON l.line_id=p.line_id " +
            "LEFT JOIN line_fares f ON f.line_id=p.line_id ORDER BY p.pattern_id"

    const val TRANSFERS = "SELECT stop_a,stop_b,direct_distance_m,walk_distance_m FROM transfers"

    const val PLACES =
        "SELECT p.place_id,p.name,p.category,p.latitude,p.longitude,p.address,p.coordinate_source " +
            "FROM places_fts JOIN places p ON p.id=places_fts.rowid " +
            "WHERE places_fts MATCH ? ORDER BY bm25(places_fts),p.place_id LIMIT ?"
}

private val PLACE_TOKEN = Regex("""[\p{L}\p{N}]+""")

/** Tokens entre comillas y con prefijo. Nunca se pega el texto crudo al SQL. */
fun placesFtsMatchExpression(query: String): String? {
    if (query.length > 512) throw IllegalArgumentException("Query too long")
    val tokens = PLACE_TOKEN.findAll(query).take(12).map { it.value }.toList()
    if (tokens.isEmpty()) return null
    return tokens.joinToString(" AND ") { token -> "\"$token\"*" }
}

object SqlitePlannerNetworkLoader {
    fun load(connection: DbConnection, cityCode: String = PackSql.CITY): PlannerNetwork {
        val config = LinkedHashMap<String, Any?>()
        connection.forEachRow(PackSql.CONFIG, listOf(cityCode)) { cursor ->
            val key = cursor.string("key")
            config[key] = parseJson(cursor.string("value_json"))
        }
        val settings = PlannerSettings.fromConfig(config)
        val defaultFare = config["default_fare"] as? Map<*, *>
            ?: throw IllegalArgumentException("Falta default_fare.")
        val defaultAmount = (defaultFare["amount"] as Number).toDouble()
        val defaultCurrency = defaultFare["currency"] as String

        val stops = LinkedHashMap<String, CityStop>()
        connection.forEachRow(PackSql.STOPS) { cursor ->
            val stop = readStop(cursor)
            stops[stop.id] = stop
        }

        val shapes = LinkedHashMap<String, PlannerShape>()
        connection.forEachRow(PackSql.SHAPES) { cursor ->
            val geometry = parseJson(cursor.string("geometry_json")) as? Map<*, *>
                ?: throw IllegalArgumentException("Geometría inválida.")
            val coordinates = geometry["coordinates"] as? List<*>
                ?: throw IllegalArgumentException("Geometría inválida.")
            val points = coordinates.map { pair ->
                val values = pair as List<*>
                GeoPoint(
                    latitude = (values[1] as Number).toDouble(),
                    longitude = (values[0] as Number).toDouble(),
                )
            }
            shapes[cursor.string("shape_id")] = PlannerShape(
                points = points,
                distances = jsonDoubles(parseJson(cursor.string("vertex_distances_json"))),
            )
        }

        val visits = LinkedHashMap<String, MutableList<PatternVisit>>()
        connection.forEachRow(PackSql.VISITS) { cursor ->
            visits.getOrPut(cursor.string("pattern_id")) { mutableListOf() }.add(
                PatternVisit(cursor.string("stop_id"), cursor.double("distance_m")),
            )
        }

        val patterns = ArrayList<PlannerPattern>()
        connection.forEachRow(PackSql.PATTERNS) { cursor ->
            val patternId = cursor.string("pattern_id")
            val shapeId = cursor.string("shape_id")
            val amount = if (cursor.isNull("fare_amount")) null else cursor.double("fare_amount")
            val currency = if (cursor.isNull("fare_currency")) defaultCurrency else cursor.string("fare_currency")
            patterns.add(
                PlannerPattern(
                    id = patternId,
                    lineId = cursor.string("line_id"),
                    lineName = cursor.string("line_name"),
                    color = cursor.string("color"),
                    direction = cursor.string("direction"),
                    headsign = cursor.string("headsign"),
                    visits = visits[patternId] ?: throw IllegalArgumentException("Visitas ausentes."),
                    shape = shapes[shapeId] ?: throw IllegalArgumentException("Shape ausente."),
                    fare = Money.resolvePackFare(defaultAmount, amount, currency),
                ),
            )
        }

        val transfers = LinkedHashMap<String, MutableList<CityTransfer>>()
        connection.forEachRow(PackSql.TRANSFERS) { cursor ->
            val origin = cursor.string("stop_a")
            val destination = cursor.string("stop_b")
            val direct = cursor.double("direct_distance_m")
            val walk = cursor.double("walk_distance_m")
            transfers.getOrPut(origin) { mutableListOf() }.add(
                CityTransfer(destination, direct, walk),
            )
            transfers.getOrPut(destination) { mutableListOf() }.add(
                CityTransfer(origin, direct, walk),
            )
        }
        return PlannerNetwork(
            stops = stops,
            patterns = patterns,
            transfers = transfers.mapValues { (_, edges) -> edges.toList() },
            settings = settings,
        )
    }
}

internal fun readStop(cursor: DbCursor): CityStop = CityStop(
    id = cursor.string("stop_id"),
    name = cursor.string("name"),
    latitude = cursor.double("latitude"),
    longitude = cursor.double("longitude"),
    sourceKind = cursor.string("source_kind"),
    nameSource = cursor.string("name_source"),
    needsReview = cursor.longFlag("needs_review"),
    usedInPatterns = cursor.longFlag("used_in_patterns"),
)
