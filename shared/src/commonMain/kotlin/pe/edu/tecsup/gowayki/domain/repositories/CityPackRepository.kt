package pe.edu.tecsup.gowayki.domain.repositories

import pe.edu.tecsup.gowayki.domain.models.CityMapStop
import pe.edu.tecsup.gowayki.domain.models.CityPackInfo
import pe.edu.tecsup.gowayki.domain.models.CityPattern
import pe.edu.tecsup.gowayki.domain.models.CityPatternStop
import pe.edu.tecsup.gowayki.domain.models.CityPlace
import pe.edu.tecsup.gowayki.domain.models.CityShape
import pe.edu.tecsup.gowayki.domain.models.CityStop
import pe.edu.tecsup.gowayki.domain.models.CityTransfer
import pe.edu.tecsup.gowayki.domain.models.NavigationSearchResult
import pe.edu.tecsup.gowayki.domain.models.TransitLine
import pe.edu.tecsup.gowayki.domain.money.Money
import pe.edu.tecsup.gowayki.domain.planner.PlannerNetwork

/**
 * Lectura del paquete offline de una ciudad.
 * Las funciones son síncronas: quien llama desde Android no debe usar el hilo de interfaz.
 */
interface CityPackRepository {
    fun lines(): List<TransitLine>
    fun stops(limit: Int = 100, offset: Int = 0): List<CityStop>
    fun stop(id: String): CityStop?
    fun patternsForLine(lineId: String): List<CityPattern>
    fun patternStops(patternId: String): List<CityPatternStop>
    fun shape(shapeId: String): CityShape?
    fun transfersFrom(stopId: String): List<CityTransfer>
    fun searchPlaces(query: String, limit: Int = 20): List<CityPlace>
    fun configuration(): Map<String, Any?>
    /** Null si la línea no existe. Ignora el monto 1.0 de default_fare. */
    fun fareForLine(lineId: String): Money?
    fun packageInfo(): CityPackInfo
    fun mapStops(): List<CityMapStop>
    fun plannerNetwork(): PlannerNetwork
    fun searchNavigation(query: String): List<NavigationSearchResult>
    fun close()
}
