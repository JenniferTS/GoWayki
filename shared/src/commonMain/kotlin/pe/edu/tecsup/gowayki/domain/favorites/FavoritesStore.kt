package pe.edu.tecsup.gowayki.domain.favorites

class FavoritesStore {
    private val places = linkedMapOf<String, FavoritePlace>()
    private val lines = linkedMapOf<String, FavoriteLine>()

    fun upsertPlace(item: FavoritePlace) {
        places[item.place.name] = item
    }

    fun removePlace(name: String) {
        places.remove(name)
    }

    fun places(): List<FavoritePlace> = places.values.toList()

    fun upsertLine(item: FavoriteLine) {
        lines[item.lineId] = item
    }

    fun removeLine(id: String) {
        lines.remove(id)
    }

    fun lines(): List<FavoriteLine> = lines.values.toList()
}
