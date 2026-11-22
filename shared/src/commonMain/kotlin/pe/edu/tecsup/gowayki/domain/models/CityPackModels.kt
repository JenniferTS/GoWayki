package pe.edu.tecsup.gowayki.domain.models

/** Candidato de origen o punto generado. No es un paradero certificado. */
data class CityStop(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val sourceKind: String,
    val nameSource: String,
    val needsReview: Boolean,
    val usedInPatterns: Boolean,
) {
    val isVirtual: Boolean get() = sourceKind == "generated_virtual"
}

data class CityPattern(
    val id: String,
    val lineId: String,
    val routeId: String,
    val shapeId: String,
    val direction: String,
    val headsign: String,
    val lengthMeters: Double,
)

data class CityPatternStop(
    val stop: CityStop,
    val sequence: Int,
    /** Chainage del shape original. También lo usa CityShape.vertexDistances. */
    val distanceMeters: Double,
    val offsetMeters: Double,
    val shapeLatitude: Double,
    val shapeLongitude: Double,
)

data class CityPlace(
    val id: String,
    val name: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val coordinateSource: String,
) {
    fun toPlaceResult(): PlaceResult = PlaceResult(
        id = id,
        name = name,
        address = address,
        latitude = latitude,
        longitude = longitude,
    )
}

data class CityTransfer(
    val toStopId: String,
    val directMeters: Double,
    val walkMeters: Double,
)

data class CityShape(
    val id: String,
    /** Pares GeoJSON [longitud, latitud]. */
    val coordinates: List<List<Double>>,
    /** Chainage original de cada vértice conservado, en metros. */
    val vertexDistances: List<Double>,
)

/** Monto tal como está en el paquete, antes de pasarlo a [pe.edu.tecsup.gowayki.domain.money.Money]. */
data class CityFare(
    val amount: Double,
    val currency: String,
)

data class CityPackInfo(
    val cityCode: String,
    val version: Int,
    val generatedAtEpochMicros: Long,
    val sha256: String,
)

data class CityStopService(
    val lineId: String,
    val patternId: String,
    val routeId: String,
    val direction: String,
    val headsign: String,
)

data class CityMapStop(
    val stop: CityStop,
    val services: List<CityStopService>,
)

data class PlaceResult(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
)

enum class NavigationSearchGroup { Place, Stop, Line, Street }

data class NavigationSearchResult(
    val id: String,
    val name: String,
    val detail: String,
    val group: NavigationSearchGroup,
    val point: PlaceResult? = null,
    val needsReview: Boolean = false,
)

data class JourneyEndpoints(
    val origin: PlaceResult,
    val destination: PlaceResult,
)

enum class TransitDirectionKind { Ida, Vuelta }

data class TransitDirection(
    val kind: TransitDirectionKind,
    val routeId: String,
    val headsign: String,
    val longName: String,
    /** Pares [lat, lon], el orden que consume el mapa. */
    val geometry: List<List<Double>>,
)

data class TransitLine(
    val id: String,
    val shortName: String,
    val color: String,
    val directions: List<TransitDirection>,
) {
    val ida: TransitDirection? get() = directions.firstOrNull { it.kind == TransitDirectionKind.Ida }
    val vuelta: TransitDirection? get() = directions.firstOrNull { it.kind == TransitDirectionKind.Vuelta }
}
