package pe.edu.tecsup.gowayki.ui.screens

import pe.edu.tecsup.gowayki.domain.favorites.FavoriteLine
import pe.edu.tecsup.gowayki.domain.favorites.FavoritePlace

data class FavoritesScreenState(
    val places: List<FavoritePlace> = emptyList(),
    val lines: List<FavoriteLine> = emptyList(),
    val empty: Boolean = true,
) {
    companion object {
        fun from(places: List<FavoritePlace>, lines: List<FavoriteLine>) =
            FavoritesScreenState(places, lines, places.isEmpty() && lines.isEmpty())
    }
}
