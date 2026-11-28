package pe.edu.tecsup.gowayki.domain.favorites

/** Contrato de sync remoto: users/{uid}/favorites */
interface FavoritesRemote {
    suspend fun pull(uid: String): List<FavoritePlace>
    suspend fun push(uid: String, places: List<FavoritePlace>)
}

class InMemoryFavoritesRemote : FavoritesRemote {
    private val byUser = mutableMapOf<String, List<FavoritePlace>>()
    override suspend fun pull(uid: String): List<FavoritePlace> = byUser[uid].orEmpty()
    override suspend fun push(uid: String, places: List<FavoritePlace>) {
        byUser[uid] = places
    }
}
