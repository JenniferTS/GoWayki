package pe.edu.tecsup.gowayki.domain.favorites

import pe.edu.tecsup.gowayki.domain.preferences.RecentTrip
import pe.edu.tecsup.gowayki.domain.preferences.SavedPlace

fun renameRecentTrip(trip: RecentTrip, newToName: String): RecentTrip {
    val name = newToName.trim()
    require(name.isNotEmpty()) { "nombre vacío" }
    return trip.copy(to = SavedPlace(name, trip.to.latitude, trip.to.longitude))
}

fun pinRecentAsFavorite(trip: RecentTrip): FavoritePlace =
    FavoritePlace(place = trip.to, pinned = true)
