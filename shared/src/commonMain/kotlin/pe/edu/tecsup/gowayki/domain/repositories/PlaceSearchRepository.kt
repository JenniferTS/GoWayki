package pe.edu.tecsup.gowayki.domain.repositories

import pe.edu.tecsup.gowayki.domain.models.PlaceResult

interface PlaceSearchRepository {
    fun search(query: String): List<PlaceResult>
}
