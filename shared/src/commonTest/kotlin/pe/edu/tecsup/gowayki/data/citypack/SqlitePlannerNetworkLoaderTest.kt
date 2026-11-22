package pe.edu.tecsup.gowayki.data.citypack

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import pe.edu.tecsup.gowayki.domain.models.CityPackInfo
import pe.edu.tecsup.gowayki.domain.money.Money
import pe.edu.tecsup.gowayki.domain.planner.UtcDateTime

class SqlitePlannerNetworkLoaderTest {
    @Test
    fun defaultFareOfOneSolBecomesTheBaseAndALineFareBecomesMinorUnits() {
        val network = SqlitePlannerNetworkLoader.load(fixture())
        val fares = network.patterns.associate { it.id to it.fare }
        assertEquals(Money.BASE, fares.getValue("p-base"))
        assertEquals(Money(130L, "PEN"), fares.getValue("p-base"))
        assertEquals(Money(250L, "PEN"), fares.getValue("p-line"))
        assertEquals(Money(100L, "PEN"), fares.getValue("p-one"))
        assertEquals(75.0, network.settings.walkMetersPerMinute)
        assertEquals(800.0, network.settings.accessMeters)
        assertEquals("s2", network.transfers.getValue("s1").single().toStopId)
        assertEquals("s1", network.transfers.getValue("s2").single().toStopId)
    }

    @Test
    fun placeSearchQuotesFtsTokensAndSkipsSymbols() {
        assertEquals(""""Tecsup"* AND "Arequipa"*""", placesFtsMatchExpression("Tecsup Arequipa"))
        assertNull(placesFtsMatchExpression("..."))
        val repository = SqliteCityPackRepository(
            object : DbConnection {
                override fun query(sql: String, args: List<Any?>): DbCursor = error("No debía consultar: $sql")
                override fun close() = Unit
            },
            CityPackInfo("arequipa", 2, 0L, "abc"),
        )
        assertTrue(repository.searchPlaces("???").isEmpty())
        assertTrue(repository.search("!!").isEmpty())
    }

    @Test
    fun fareForLineIgnoresThePackDefault() {
        val repository = SqliteCityPackRepository(fareConnection(), CityPackInfo("arequipa", 2, 0L, "abc"))
        assertEquals(Money.BASE, repository.fareForLine("plain"))
        assertEquals(Money(250L, "PEN"), repository.fareForLine("priced"))
        assertNull(repository.fareForLine("missing"))
    }

    @Test
    fun manifestTimestampIsUtc() {
        val info = cityPackInfoFromManifest(
            """{"city_code":"arequipa","version":2,"sha256":"abc","generated_at":"2026-11-06T21:55:58.439926+00:00"}""",
        )
        assertEquals("arequipa", info.cityCode)
        assertEquals(2, info.version)
        assertEquals(UtcDateTime.of(2026, 11, 6, 21, 55, 58, 439926).epochMicros, info.generatedAtEpochMicros)
    }

    private fun fixture(): DbConnection = object : DbConnection {
        override fun query(sql: String, args: List<Any?>): DbCursor = MapCursor(
            when (sql) {
                PackSql.CONFIG -> configRows()
                PackSql.STOPS -> listOf(stop("s1", -71.5), stop("s2", -71.49))
                PackSql.SHAPES -> listOf(
                    mapOf(
                        "shape_id" to "sh",
                        "geometry_json" to """{"coordinates":[[-71.5,-16.4],[-71.49,-16.4]]}""",
                        "vertex_distances_json" to "[0,100]",
                    ),
                )
                PackSql.VISITS -> listOf("p-base", "p-line", "p-one").flatMap { id ->
                    listOf(
                        mapOf("pattern_id" to id, "stop_id" to "s1", "distance_m" to 0.0),
                        mapOf("pattern_id" to id, "stop_id" to "s2", "distance_m" to 100.0),
                    )
                }
                PackSql.PATTERNS -> listOf(
                    pattern("p-base", null),
                    pattern("p-line", 2.5),
                    pattern("p-one", 1.0),
                )
                PackSql.TRANSFERS -> listOf(
                    mapOf(
                        "stop_a" to "s1",
                        "stop_b" to "s2",
                        "direct_distance_m" to 100.0,
                        "walk_distance_m" to 130.0,
                    ),
                )
                else -> error("SQL inesperado: $sql")
            },
        )

        override fun close() = Unit
    }

    private fun fareConnection(): DbConnection = object : DbConnection {
        override fun query(sql: String, args: List<Any?>): DbCursor {
            val rows = when {
                sql.contains("FROM lines WHERE line_id") ->
                    if (args.single() == "missing") emptyList() else listOf(mapOf("line_id" to args.single()))
                sql.contains("FROM line_fares") ->
                    if (args.single() == "priced") {
                        listOf(mapOf("amount" to 2.5, "currency" to "PEN"))
                    } else {
                        emptyList()
                    }
                sql == PackSql.CONFIG -> configRows()
                else -> error("SQL inesperado: $sql")
            }
            return MapCursor(rows)
        }

        override fun close() = Unit
    }
}

private fun configRows(): List<Map<String, Any?>> = listOf(
    cfg("timezone", "\"America/Lima\""),
    cfg("default_fare", """{"amount":1.0,"currency":"PEN"}"""),
    cfg("frequency_minutes_by_period", """{"night":15,"off_peak":10,"peak":6}"""),
    cfg(
        "speed_bands",
        """[{"end":"07:00","speed_kmh":22,"start":"05:00"},{"end":"09:00","speed_kmh":14,"start":"07:00"},{"end":"12:00","speed_kmh":18,"start":"09:00"},{"end":"14:00","speed_kmh":15,"start":"12:00"},{"end":"17:00","speed_kmh":18,"start":"14:00"},{"end":"20:00","speed_kmh":13,"start":"17:00"},{"end":"23:00","speed_kmh":22,"start":"20:00"},{"end":"05:00","speed_kmh":null,"start":"23:00"}]""",
    ),
    cfg(
        "frequency_bands",
        """[{"end":"07:00","period":"off_peak","start":"05:00"},{"end":"09:00","period":"peak","start":"07:00"},{"end":"17:00","period":"off_peak","start":"09:00"},{"end":"20:00","period":"peak","start":"17:00"},{"end":"23:00","period":"night","start":"20:00"},{"end":"05:00","period":null,"start":"23:00"}]""",
    ),
    cfg("walking_speed_m_per_min", "75"),
    cfg("access_max_m", "800"),
    cfg("access_fallback_max_m", "1500"),
)

private fun cfg(key: String, json: String): Map<String, Any?> = mapOf("key" to key, "value_json" to json)

private fun stop(id: String, longitude: Double): Map<String, Any?> = mapOf(
    "stop_id" to id,
    "name" to "Paradero $id",
    "latitude" to -16.4,
    "longitude" to longitude,
    "source_kind" to "gtfs_candidate",
    "name_source" to "gtfs_source",
    "needs_review" to 0,
    "used_in_patterns" to 1,
)

private fun pattern(id: String, amount: Double?): Map<String, Any?> = mapOf(
    "pattern_id" to id,
    "line_id" to id,
    "line_name" to "Línea $id",
    "color" to "#123456",
    "direction" to "ida",
    "headsign" to "Centro",
    "shape_id" to "sh",
    "fare_amount" to amount,
    "fare_currency" to if (amount == null) null else "PEN",
)

private class MapCursor(private val rows: List<Map<String, Any?>>) : DbCursor {
    private var index = -1
    private val row: Map<String, Any?> get() = rows[index]

    override fun next(): Boolean = ++index < rows.size

    override fun isNull(column: String): Boolean = row[column] == null

    override fun string(column: String): String = row.getValue(column) as String

    override fun double(column: String): Double = (row.getValue(column) as Number).toDouble()

    override fun long(column: String): Long = (row.getValue(column) as Number).toLong()

    override fun close() = Unit
}
