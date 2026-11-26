package pe.edu.tecsup.gowayki.domain.favorites

import pe.edu.tecsup.gowayki.domain.preferences.SavedPlace

data class FavoritePlace(
    val place: SavedPlace,
    val pinned: Boolean = false,
)

data class FavoriteLine(
    val lineId: String,
    val name: String,
)
