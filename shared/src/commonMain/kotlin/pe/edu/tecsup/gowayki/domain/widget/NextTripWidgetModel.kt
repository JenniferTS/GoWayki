package pe.edu.tecsup.gowayki.domain.widget

import pe.edu.tecsup.gowayki.domain.preferences.RecentTrip
import pe.edu.tecsup.gowayki.domain.preferences.SavedPlace

data class NextTripWidgetModel(
    val originName: String,
    val destinationName: String,
    val etaLabel: String?,
)

fun nextTripFrom(favorites: List<SavedPlace>, recents: List<RecentTrip>): NextTripWidgetModel? {
    val recent = recents.firstOrNull()
    if (recent != null) {
        return NextTripWidgetModel(recent.from.name, recent.to.name, etaLabel = null)
    }
    val fav = favorites.firstOrNull() ?: return null
    return NextTripWidgetModel(originName = "Mi ubicación", destinationName = fav.name, etaLabel = null)
}
