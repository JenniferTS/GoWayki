package pe.edu.tecsup.gowayki.domain.favorites

import pe.edu.tecsup.gowayki.domain.preferences.SavedPlace
import kotlin.test.Test
import kotlin.test.assertEquals

class FavoritesStoreTest {
    @Test
    fun removePlace() {
        val store = FavoritesStore()
        store.upsertPlace(FavoritePlace(SavedPlace("Plaza", -16.4, -71.5)))
        store.removePlace("Plaza")
        assertEquals(emptyList(), store.places())
    }
}
